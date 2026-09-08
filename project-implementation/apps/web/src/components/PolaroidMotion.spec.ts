import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { mount, type VueWrapper } from '@vue/test-utils'
import { defineComponent, h, onUnmounted } from 'vue'
import PolaroidMotion from './PolaroidMotion.vue'

let wrapper: VueWrapper | undefined
let now = 1000
let nextFrame = 0
let reducedMotion = false
let media: MediaQueryList
const frames = new Map<number, FrameRequestCallback>()
const disconnectObserver = vi.fn()

function advanceFrames(count = 1) {
  for (let index = 0; index < count; index++) {
    now += 16
    const callbacks = [...frames.values()]
    frames.clear()
    callbacks.forEach(callback => callback(now))
  }
}

function settleMotion() {
  for (let index = 0; index < 240 && frames.size; index++) advanceFrames()
  expect(frames.size).toBe(0)
}

function render() {
  wrapper = mount(PolaroidMotion, {
    attachTo: document.body,
    slots: { default: '<img alt="商品相纸" src="/product.png" />' },
  })
  return wrapper
}

function paperTransform() {
  return (wrapper!.get('.polaroid-motion-paper').element as HTMLElement).style.transform
}

beforeEach(() => {
  now = 1000
  nextFrame = 0
  reducedMotion = false
  frames.clear()
  disconnectObserver.mockClear()
  vi.spyOn(performance, 'now').mockImplementation(() => now)
  vi.spyOn(HTMLElement.prototype, 'getBoundingClientRect').mockReturnValue(new DOMRect(10, 20, 300, 360))
  vi.stubGlobal('requestAnimationFrame', vi.fn((callback: FrameRequestCallback) => {
    frames.set(++nextFrame, callback)
    return nextFrame
  }))
  vi.stubGlobal('cancelAnimationFrame', vi.fn((id: number) => { frames.delete(id) }))
  vi.stubGlobal('ResizeObserver', class {
    observe = vi.fn()
    disconnect = disconnectObserver
  })
  media = Object.assign(new EventTarget(), {
    media: '(prefers-reduced-motion: reduce)',
    onchange: null,
  }) as MediaQueryList
  Object.defineProperty(media, 'matches', { get: () => reducedMotion })
  vi.stubGlobal('matchMedia', vi.fn(() => media))
})

afterEach(() => {
  wrapper?.unmount()
  wrapper = undefined
  document.body.innerHTML = ''
  vi.unstubAllGlobals()
  vi.restoreAllMocks()
})

describe('Three.js 拍立得交互', () => {
  it.each([
    { label: '触摸输入', pointerType: 'touch', reduce: false },
    { label: '减少动态效果偏好', pointerType: 'mouse', reduce: true },
  ])('$label保持相纸静止且不启动动画循环', async ({ pointerType, reduce }) => {
    reducedMotion = reduce
    const paper = render()
    const resting = paperTransform()
    expect(resting).toContain('matrix3d(')
    await paper.trigger('pointerenter', { pointerType, clientX: 300, clientY: 30 })
    await paper.trigger('pointermove', { pointerType, clientX: 20, clientY: 350 })
    advanceFrames(60)
    expect(paperTransform()).toBe(resting)
    expect(requestAnimationFrame).not.toHaveBeenCalled()
    expect(paper.get('img').attributes('alt')).toBe('商品相纸')
  })

  it('鼠标悬停改变姿态，移动跟随鼠标，离开后完整归位并停止调度', async () => {
    const paper = render()
    const resting = paperTransform()
    await paper.trigger('pointerenter', { pointerType: 'mouse', clientX: 290, clientY: 40 })
    settleMotion()
    const upperRight = paperTransform()
    expect(upperRight).not.toBe(resting)
    await paper.trigger('pointermove', { pointerType: 'mouse', clientX: 30, clientY: 360 })
    settleMotion()
    expect(paperTransform()).not.toBe(upperRight)
    await paper.trigger('pointerleave', { pointerType: 'mouse' })
    settleMotion()
    expect(paperTransform()).toBe(resting)
  })

  it('运行中开启减少动态效果立即归位，并阻止后续悬停动画', async () => {
    const paper = render()
    const resting = paperTransform()
    await paper.trigger('pointerenter', { pointerType: 'mouse', clientX: 290, clientY: 40 })
    advanceFrames(5)
    expect(paperTransform()).not.toBe(resting)
    expect(frames.size).toBeGreaterThan(0)
    reducedMotion = true
    media.dispatchEvent(new Event('change'))
    expect(paperTransform()).toBe(resting)
    expect(frames.size).toBe(0)
    await paper.trigger('pointerenter', { pointerType: 'mouse', clientX: 30, clientY: 360 })
    expect(frames.size).toBe(0)
  })

  it('动画中卸载取消调度和尺寸观察，并正常卸载被渲染器移动的 Vue 插槽', async () => {
    const slotUnmounted = vi.fn()
    const SlotContent = defineComponent({
      setup() {
        onUnmounted(slotUnmounted)
        return () => h('span', '商品插槽')
      },
    })
    const removeMediaListener = vi.spyOn(media, 'removeEventListener')
    wrapper = mount(PolaroidMotion, {
      attachTo: document.body,
      slots: { default: () => h(SlotContent) },
    })
    const paper = wrapper.get('.polaroid-motion-paper').element
    const renderer = wrapper.get('.polaroid-motion-renderer').element
    expect(renderer.contains(paper)).toBe(true)
    await wrapper.trigger('pointerenter', { pointerType: 'mouse', clientX: 290, clientY: 40 })
    advanceFrames(5)
    expect(frames.size).toBeGreaterThan(0)
    expect(() => wrapper!.unmount()).not.toThrow()
    wrapper = undefined
    expect(frames.size).toBe(0)
    expect(disconnectObserver).toHaveBeenCalledOnce()
    expect(removeMediaListener).toHaveBeenCalledWith('change', expect.any(Function))
    expect(slotUnmounted).toHaveBeenCalledOnce()
    expect(paper.isConnected).toBe(false)
    expect(renderer.isConnected).toBe(false)
    expect(document.body.textContent).not.toContain('商品插槽')
  })
})
