import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { createPageEntrance } from './pageEntrance'

class MockAnimation extends EventTarget {
  playState: AnimationPlayState = 'running'
  onfinish: ((event: Event) => void) | null = null
  oncancel: ((event: Event) => void) | null = null
  finished = new Promise<Animation>(() => {})
  finish = vi.fn(() => {
    this.playState = 'finished'
    const event = new Event('finish')
    this.dispatchEvent(event)
    this.onfinish?.(event)
  })
  cancel = vi.fn(() => {
    this.playState = 'idle'
    const event = new Event('cancel')
    this.dispatchEvent(event)
    this.oncancel?.(event)
  })
}

class MockIntersectionObserver {
  static instances: MockIntersectionObserver[] = []
  targets = new Set<Element>()
  observe = vi.fn((target: Element) => { this.targets.add(target) })
  unobserve = vi.fn((target: Element) => { this.targets.delete(target) })
  disconnect = vi.fn(() => { this.targets.clear() })
  constructor(readonly callback: IntersectionObserverCallback) {
    MockIntersectionObserver.instances.push(this)
  }
}

class MockMutationObserver {
  static instances: MockMutationObserver[] = []
  observe = vi.fn()
  disconnect = vi.fn()
  constructor(readonly callback: MutationCallback) {
    MockMutationObserver.instances.push(this)
  }
}

let root: HTMLElement
let controller: ReturnType<typeof createPageEntrance> | undefined
let reducedMotion = false
let media: MediaQueryList
let animateDescriptor: PropertyDescriptor | undefined
const animations: { element: HTMLElement; animation: MockAnimation; keyframes: Keyframe[] | PropertyIndexedKeyframes; options?: number | KeyframeAnimationOptions }[] = []

function render(markup: string) {
  root.innerHTML = markup
  controller = createPageEntrance(root)
  return controller
}

function element(selector: string) {
  return root.querySelector<HTMLElement>(selector)!
}

function observed() {
  return new Set(MockIntersectionObserver.instances.flatMap(observer => [...observer.targets]))
}

function intersect(targets: Element[], isIntersecting = true) {
  for (const observer of MockIntersectionObserver.instances) {
    const entries = targets.filter(target => observer.targets.has(target)).map(target => ({
      target,
      isIntersecting,
      intersectionRatio: isIntersecting ? 1 : 0,
      boundingClientRect: target.getBoundingClientRect(),
    } as IntersectionObserverEntry))
    if (entries.length) observer.callback(entries, observer as unknown as IntersectionObserver)
  }
}

async function flush() {
  await Promise.resolve()
  await vi.runOnlyPendingTimersAsync()
}

async function addContent(markup: string) {
  const holder = document.createElement('div')
  holder.innerHTML = markup
  const nodes = [...holder.childNodes]
  root.append(...nodes)
  for (const observer of MockMutationObserver.instances) {
    observer.callback([{ type: 'childList', target: root, addedNodes: nodes, removedNodes: [] } as unknown as MutationRecord], observer as unknown as MutationObserver)
  }
  await flush()
}

function keyframeValues(keyframes: Keyframe[] | PropertyIndexedKeyframes, key: string) {
  if (!Array.isArray(keyframes)) return keyframes[key]
  const values = keyframes.map(frame => frame[key])
  return values.every(value => value === undefined) ? undefined : values
}

beforeEach(() => {
  vi.useFakeTimers()
  reducedMotion = false
  animations.length = 0
  MockIntersectionObserver.instances = []
  MockMutationObserver.instances = []
  root = document.createElement('main')
  document.body.append(root)
  vi.spyOn(HTMLElement.prototype, 'getBoundingClientRect').mockReturnValue(new DOMRect(20, 20, 200, 120))
  vi.stubGlobal('IntersectionObserver', MockIntersectionObserver)
  vi.stubGlobal('MutationObserver', MockMutationObserver)
  vi.stubGlobal('requestAnimationFrame', (callback: FrameRequestCallback) => window.setTimeout(() => callback(performance.now()), 16))
  vi.stubGlobal('cancelAnimationFrame', (id: number) => window.clearTimeout(id))
  media = Object.assign(new EventTarget(), { media: '(prefers-reduced-motion: reduce)', onchange: null }) as MediaQueryList
  Object.defineProperty(media, 'matches', { get: () => reducedMotion })
  vi.stubGlobal('matchMedia', vi.fn(() => media))
  animateDescriptor = Object.getOwnPropertyDescriptor(HTMLElement.prototype, 'animate')
  Object.defineProperty(HTMLElement.prototype, 'animate', {
    configurable: true,
    value: vi.fn(function (this: HTMLElement, keyframes: Keyframe[] | PropertyIndexedKeyframes, options?: number | KeyframeAnimationOptions) {
      const animation = new MockAnimation()
      animations.push({ element: this, animation, keyframes, options })
      return animation
    }),
  })
})

afterEach(() => {
  controller?.destroy()
  controller = undefined
  root.remove()
  if (animateDescriptor) Object.defineProperty(HTMLElement.prototype, 'animate', animateDescriptor)
  else delete (HTMLElement.prototype as Partial<HTMLElement>).animate
  vi.clearAllTimers()
  vi.useRealTimers()
  vi.unstubAllGlobals()
  vi.restoreAllMocks()
})

describe('通用页面入场效果', () => {
  it('观察通用组件及显式标记，仅让最外层入场，并尊重 none 子树排除', async () => {
    render(`<header class="wm-header"><a id="brand" class="wm-brand">WEMOVE</a><nav id="navigation" class="wm-navigation">导航</nav></header>
      <section id="section" class="paper-section"><span id="nested-icon" class="sketch-icon"></span></section>
      <article id="tile" class="shop-tile"></article><span id="icon" class="sketch-icon"></span>
      <div id="explicit" data-reveal><div id="nested-explicit" data-reveal></div></div>
      <div data-reveal="none"><section id="excluded" class="paper-section"></section></div>`)
    await flush()
    const targets = ['#brand', '#navigation', '#section', '#tile', '#icon', '#explicit'].map(element)
    expect(observed()).toEqual(new Set(targets))
    expect(animations).toHaveLength(0)
    intersect(targets)
    await flush()
    expect(new Set(animations.map(record => record.element))).toEqual(new Set(targets))
    for (const { options } of animations) {
      const delay = typeof options === 'object' ? Number(options.delay ?? 0) : 0
      expect(delay).toBeGreaterThanOrEqual(0)
      expect(delay).toBeLessThanOrEqual(180)
    }
  })

  it('离屏内容只观察，进入视口后播放；同页刷新和再次交叉都不会重复播放', async () => {
    const entrance = render('<section class="paper-section">订单</section>')
    await flush()
    const target = element('section')
    intersect([target], false)
    await flush()
    expect(animations).toHaveLength(0)
    intersect([target])
    await flush()
    expect(animations).toHaveLength(1)
    animations[0]!.animation.finish()
    entrance.refresh()
    await flush()
    intersect([target])
    await flush()
    expect(animations).toHaveLength(1)
  })

  it('数据异步加载后新增的卡片也会被观察，已有卡片不重播', async () => {
    render('<section id="existing" class="paper-section">已有内容</section>')
    await flush()
    intersect([element('#existing')])
    await flush()
    animations[0]!.animation.finish()
    await addContent('<article id="loaded" class="shop-tile">加载的商品</article>')
    expect(observed().has(element('#loaded'))).toBe(true)
    intersect([element('#loaded'), element('#existing')])
    await flush()
    expect(animations.map(record => record.element.id)).toEqual(['existing', 'loaded'])
  })

  it('滑动渐显保留原来的旋转变换，fade 标记仅改变透明度', async () => {
    render('<article id="slide" data-reveal style="transform: rotate(-3deg)"></article><div id="fade" data-reveal="fade"></div>')
    await flush()
    intersect([element('#slide'), element('#fade')])
    await flush()
    const slide = animations.find(record => record.element.id === 'slide')!
    const fade = animations.find(record => record.element.id === 'fade')!
    expect((keyframeValues(slide.keyframes, 'opacity') as unknown[]).map(Number)).toEqual(expect.arrayContaining([0, 1]))
    expect(keyframeValues(slide.keyframes, 'translate')).toBeDefined()
    expect((keyframeValues(fade.keyframes, 'opacity') as unknown[]).map(Number)).toEqual(expect.arrayContaining([0, 1]))
    expect(keyframeValues(fade.keyframes, 'translate')).toBeUndefined()
    expect(keyframeValues(slide.keyframes, 'transform')).toBeUndefined()
    slide.animation.finish()
    fade.animation.finish()
    expect(element('#slide').style.transform).toBe('rotate(-3deg)')
    for (const target of [element('#slide'), element('#fade')]) {
      expect(target.style.opacity).toBe('')
      expect(target.style.translate).toBe('')
    }
    expect(slide.animation.cancel).toHaveBeenCalled()
    expect(fade.animation.cancel).toHaveBeenCalled()
  })

  it('路由刷新重播已出现内容，并清理还未结束的上一页动画', async () => {
    const entrance = render('<section class="paper-section">页面内容</section>')
    await flush()
    const target = element('section')
    intersect([target])
    await flush()
    const previous = animations[0]!.animation
    entrance.refresh(true)
    await flush()
    expect(previous.cancel).toHaveBeenCalled()
    expect(observed().has(target)).toBe(true)
    intersect([target])
    await flush()
    expect(animations).toHaveLength(2)
  })

  it('键盘焦点进入时立即完成该容器的入场，不中止其他组件', async () => {
    render('<section id="form" class="paper-section"><input aria-label="邮箱"></section><article id="other" class="shop-tile"></article>')
    await flush()
    intersect([element('#form'), element('#other')])
    await flush()
    const formAnimation = animations.find(record => record.element.id === 'form')!.animation
    const otherAnimation = animations.find(record => record.element.id === 'other')!.animation
    element('input').dispatchEvent(new FocusEvent('focusin', { bubbles: true }))
    await flush()
    expect(formAnimation.finish.mock.calls.length + formAnimation.cancel.mock.calls.length).toBeGreaterThan(0)
    expect(element('#form').style.opacity).toBe('')
    expect(element('#form').style.translate).toBe('')
    expect(otherAnimation.finish).not.toHaveBeenCalled()
    expect(otherAnimation.cancel).not.toHaveBeenCalled()
  })

  it('弹窗容器自动获得焦点仍正常入场，用户聚焦其中输入框时立即显示完整弹窗', async () => {
    render('<section class="el-dialog" tabindex="-1" role="dialog"><input aria-label="收货人"></section>')
    await flush()
    const dialog = element('.el-dialog')
    dialog.focus()
    expect(document.activeElement).toBe(dialog)
    intersect([dialog])
    await flush()
    expect(animations).toHaveLength(1)
    const active = animations[0]!.animation
    dialog.dispatchEvent(new FocusEvent('focusin', { bubbles: true }))
    await flush()
    expect(active.cancel).not.toHaveBeenCalled()
    expect(active.finish).not.toHaveBeenCalled()
    element('input').focus()
    await flush()
    expect(active.cancel).toHaveBeenCalled()
    expect(dialog.style.opacity).toBe('')
    expect(dialog.style.translate).toBe('')
  })

  it('同一个弹窗关闭后再次打开会重播入场动画', async () => {
    render('<section class="el-dialog" tabindex="-1" role="dialog">订单操作</section>')
    await flush()
    const dialog = element('.el-dialog')
    intersect([dialog])
    await flush()
    expect(animations).toHaveLength(1)
    animations[0]!.animation.finish()
    intersect([dialog], false)
    await flush()
    expect(observed().has(dialog)).toBe(true)
    intersect([dialog])
    await flush()
    expect(animations).toHaveLength(2)
    expect(animations[1]!.element).toBe(dialog)
  })

  it('开启减少动态效果时，初始和异步内容都保持直接可见', async () => {
    reducedMotion = true
    const entrance = render('<section class="paper-section">页面内容</section>')
    await flush()
    await addContent('<article class="shop-tile">异步内容</article>')
    entrance.refresh(true)
    await flush()
    intersect([...root.children])
    await flush()
    expect(animations).toHaveLength(0)
    for (const target of root.querySelectorAll<HTMLElement>('.paper-section, .shop-tile')) {
      expect(getComputedStyle(target).opacity).not.toBe('0')
      expect(getComputedStyle(target).visibility).not.toBe('hidden')
    }
  })

  it('用户在动画中切换减少动态效果时立即显示内容并停止后续动画', async () => {
    render('<section class="paper-section">页面内容</section>')
    await flush()
    intersect([element('section')])
    await flush()
    const active = animations[0]!.animation
    reducedMotion = true
    media.dispatchEvent(new Event('change'))
    await flush()
    expect(active.cancel).toHaveBeenCalled()
    expect(element('section').style.opacity).toBe('')
    await addContent('<article class="shop-tile">新增内容</article>')
    intersect([element('article')])
    await flush()
    expect(animations).toHaveLength(1)
  })

  it('销毁时取消动画、断开观察器并移除媒体偏好监听', async () => {
    const removeListener = vi.spyOn(media, 'removeEventListener')
    const entrance = render('<section class="paper-section">页面内容</section>')
    await flush()
    intersect([element('section')])
    await flush()
    const active = animations[0]!.animation
    entrance.destroy()
    expect(active.cancel).toHaveBeenCalled()
    expect(MockIntersectionObserver.instances.every(observer => observer.disconnect.mock.calls.length > 0)).toBe(true)
    expect(MockMutationObserver.instances.every(observer => observer.disconnect.mock.calls.length > 0)).toBe(true)
    expect(removeListener).toHaveBeenCalledWith('change', expect.any(Function))
    expect(element('section').style.opacity).toBe('')
    expect(element('section').style.translate).toBe('')
    controller = undefined
  })
})
