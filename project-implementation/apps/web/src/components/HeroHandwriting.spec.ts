import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { mount, type VueWrapper } from '@vue/test-utils'
import HeroHandwriting from './HeroHandwriting.vue'

let wrapper: VueWrapper | undefined
let now = 0
let nextFrame = 0
let reducedMotion = false
let media: MediaQueryList
const frames = new Map<number, FrameRequestCallback>()

function advanceTo(time: number) {
  now = time
  const pending = [...frames.values()]
  frames.clear()
  pending.forEach(callback => callback(now))
}

function render() {
  wrapper = mount(HeroHandwriting)
  return wrapper
}

beforeEach(() => {
  now = 0
  nextFrame = 0
  reducedMotion = false
  frames.clear()
  vi.spyOn(performance, 'now').mockImplementation(() => now)
  vi.stubGlobal('requestAnimationFrame', vi.fn((callback: FrameRequestCallback) => {
    frames.set(++nextFrame, callback)
    return nextFrame
  }))
  vi.stubGlobal('cancelAnimationFrame', vi.fn((id: number) => { frames.delete(id) }))
  // jsdom has no SVG geometry; supply a deterministic length, keeping real masks and DOM.
  vi.stubGlobal('SVGPathElement', SVGElement)
  Object.defineProperty(SVGElement.prototype, 'getTotalLength', { configurable: true, value: () => 500 })
  media = Object.assign(new EventTarget(), { media: '(prefers-reduced-motion: reduce)', onchange: null }) as MediaQueryList
  Object.defineProperty(media, 'matches', { get: () => reducedMotion })
  vi.stubGlobal('matchMedia', vi.fn(() => media))
})

afterEach(() => {
  wrapper?.unmount()
  wrapper = undefined
  Reflect.deleteProperty(SVGElement.prototype, 'getTotalLength')
  vi.unstubAllGlobals()
  vi.restoreAllMocks()
})

describe('首页逐笔书写', () => {
  it('沿独立笔画推进，再依次书写第二行和中文，完整标题始终可读', () => {
    const title = render()
    const lines = title.findAll('.handwritten-line')
    expect(title.get('h1').text()).toBe('Life is better at play.')
    expect(title.get('.home-chinese').text()).toBe('把日常，玩出新花样。')
    expect(title.findAll('svg').every(svg => svg.attributes('aria-hidden') === 'true')).toBe(true)
    expect(title.findAll('[data-pen][visibility="visible"]')).toHaveLength(0)

    advanceTo(220)
    const firstPen = lines[0]!.get('[data-pen]')
    expect(Number(firstPen.attributes('stroke-dashoffset'))).toBeGreaterThan(0)
    expect(Number(firstPen.attributes('stroke-dashoffset'))).toBeLessThan(1)
    expect(lines[1]!.findAll('[data-pen][visibility="visible"]')).toHaveLength(0)
    expect(lines[2]!.findAll('[data-pen][visibility="visible"]')).toHaveLength(0)

    advanceTo(2700)
    expect(lines[0]!.findAll('[data-ink][mask]')).toHaveLength(0)
    expect(lines[1]!.findAll('[data-pen][visibility="visible"]').length).toBeGreaterThan(0)
    expect(lines[2]!.findAll('[data-pen][visibility="visible"]')).toHaveLength(0)

    advanceTo(4200)
    expect(lines[2]!.findAll('[data-pen][visibility="visible"]').length).toBeGreaterThan(0)
    advanceTo(6300)
    expect(title.findAll('[data-ink][mask]')).toHaveLength(0)
    expect(frames.size).toBe(0)
  })

  it('减少动态效果时直接显示完整字形，不启动动画', () => {
    reducedMotion = true
    const title = render()
    expect(title.findAll('[data-ink][mask]')).toHaveLength(0)
    expect(requestAnimationFrame).not.toHaveBeenCalled()
  })

  it('运行中开启减少动态效果立即完成且停止调度', () => {
    const title = render()
    advanceTo(600)
    reducedMotion = true
    media.dispatchEvent(new Event('change'))
    expect(title.findAll('[data-ink][mask]')).toHaveLength(0)
    expect(frames.size).toBe(0)
  })

  it('缺少 SVG 测量能力时显示完整标题', () => {
    Reflect.deleteProperty(SVGElement.prototype, 'getTotalLength')
    const title = render()
    expect(title.findAll('[data-ink][mask]')).toHaveLength(0)
    expect(frames.size).toBe(0)
  })

  it('离开首页取消动画帧并移除偏好监听器', () => {
    const removeListener = vi.spyOn(media, 'removeEventListener')
    render()
    advanceTo(700)
    expect(frames.size).toBe(1)
    wrapper!.unmount()
    wrapper = undefined
    expect(frames.size).toBe(0)
    expect(removeListener).toHaveBeenCalledWith('change', expect.any(Function))
  })
})
