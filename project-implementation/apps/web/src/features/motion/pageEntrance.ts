import { ENTRANCE_EXCLUSIONS, ENTRANCE_TARGETS } from './entranceTargets'

type Entry = { state: 'waiting' | 'running' | 'done' | 'grouped'; animation?: Animation }

/** Shared reveal for page sections and async content. data-reveal="fade" / "none"
 * can override a component. The DOM stays visible if animation APIs are unavailable.
 */
export function createPageEntrance(root: HTMLElement) {
  const entries = new Map<HTMLElement, Entry>()
  const preference = window.matchMedia?.('(prefers-reduced-motion: reduce)')
  let intersection: IntersectionObserver | undefined
  let mutation: MutationObserver | undefined
  let destroyed = false
  let scanQueued = false
  const repeatable = (element: HTMLElement) => element.matches('.el-dialog, .el-message-box')
  const allowed = (element: HTMLElement) => !element.closest(ENTRANCE_EXCLUSIONS)

  function finish(element: HTMLElement, entry: Entry) {
    const animation = entry.animation
    entry.animation = undefined
    entry.state = 'done'
    element.dataset.revealState = 'done'
    if (animation) {
      animation.onfinish = null
      animation.oncancel = null
      animation.cancel()
    }
    if (!repeatable(element)) intersection?.unobserve(element)
  }

  function play(element: HTMLElement, delay: number) {
    const entry = entries.get(element)
    if (!entry || entry.state !== 'waiting') return
    const focused = element.contains(document.activeElement)
      && !(repeatable(element) && document.activeElement === element)
    if (preference?.matches || focused || !element.animate) {
      finish(element, entry)
      return
    }
    const style = getComputedStyle(element)
    const fadeOnly = element.dataset.reveal === 'fade'
      || element.closest('.wm-header') !== null
      || (style.translate && style.translate !== 'none')
    const first: Keyframe = { opacity: 0 }
    const last: Keyframe = { opacity: style.opacity || '1' }
    // Individual translate leaves CSS rotations and Three.js matrix3d intact.
    if (!fadeOnly) {
      first.translate = '0 14px'
      last.translate = '0 0'
    }
    try {
      entry.state = 'running'
      element.dataset.revealState = 'running'
      const animation = element.animate([first, last], {
        duration: fadeOnly ? 420 : 520,
        delay,
        easing: 'cubic-bezier(0.22, 1, 0.36, 1)',
        fill: 'backwards',
      })
      entry.animation = animation
      animation.onfinish = () => finish(element, entry)
      animation.oncancel = () => finish(element, entry)
    } catch {
      finish(element, entry)
    }
  }

  function scan() {
    if (destroyed) return
    // Vue may remove a whole page or replace async cards. Release those nodes.
    for (const [element, entry] of entries) {
      if (!root.contains(element)) {
        finish(element, entry)
        intersection?.unobserve(element)
        entries.delete(element)
        delete element.dataset.revealState
      }
    }
    for (const element of root.querySelectorAll<HTMLElement>(ENTRANCE_TARGETS)) {
      if (entries.has(element) || !allowed(element)) continue
      const ancestor = element.parentElement?.closest<HTMLElement>(ENTRANCE_TARGETS)
      const parent = ancestor && allowed(ancestor) ? entries.get(ancestor) : undefined
      // Initial children enter with their card. New async content can enter later.
      if (parent && parent.state !== 'done') {
        entries.set(element, { state: 'grouped' })
        continue
      }
      const entry: Entry = { state: 'waiting' }
      entries.set(element, entry)
      if (!intersection || preference?.matches) {
        finish(element, entry)
      } else {
        element.dataset.revealState = 'waiting'
        intersection.observe(element)
      }
    }
  }

  function queueScan() {
    if (destroyed || scanQueued) return
    scanQueued = true
    queueMicrotask(() => {
      scanQueued = false
      scan()
    })
  }

  function refresh(replay = false) {
    if (destroyed) return
    if (replay) {
      intersection?.disconnect()
      for (const [element, entry] of entries) {
        finish(element, entry)
        delete element.dataset.revealState
      }
      entries.clear()
    }
    scan()
  }

  function preferenceChanged() {
    if (preference?.matches) {
      for (const [element, entry] of entries) finish(element, entry)
    }
    // Re-enabling motion affects new content; it does not replay the whole page.
  }

  function revealFocused(event: Event) {
    if (!(event.target instanceof Node)) return
    for (const [element, entry] of entries) {
      // Element Plus focuses the dialog container automatically when opening it.
      if (event.type === 'focusin' && repeatable(element) && event.target === element) continue
      if ((entry.state === 'running' || entry.state === 'waiting') && element.contains(event.target)) {
        finish(element, entry)
      }
    }
  }

  if (typeof IntersectionObserver !== 'undefined' && typeof HTMLElement.prototype.animate === 'function') {
    intersection = new IntersectionObserver((changes) => {
      if (destroyed) return
      const visible: HTMLElement[] = []
      for (const change of changes) {
        const element = change.target as HTMLElement
        const entry = entries.get(element)
        if (!entry || !root.contains(element)) continue
        if (change.isIntersecting && entry.state === 'waiting') visible.push(element)
        else if (!change.isIntersecting && repeatable(element) && entry.state !== 'waiting') {
          finish(element, entry)
          entry.state = 'waiting'
          element.dataset.revealState = 'waiting'
        }
      }
      // Short, bounded staggering in reading order; long lists never queue seconds.
      visible.sort((a, b) => {
        const first = a.getBoundingClientRect(), second = b.getBoundingClientRect()
        return Math.abs(first.top - second.top) > 32 ? first.top - second.top : first.left - second.left
      })
      visible.forEach((element, index) => play(element, Math.min(index * 45, 180)))
    }, { threshold: 0.01, rootMargin: '0px 0px -12px 0px' })
  }
  if (typeof MutationObserver !== 'undefined') {
    mutation = new MutationObserver(queueScan)
    // Do not observe style: Three.js updates its rendering matrix every frame.
    mutation.observe(root, { childList: true, subtree: true })
  }
  preference?.addEventListener('change', preferenceChanged)
  root.addEventListener('focusin', revealFocused)
  root.addEventListener('pointerdown', revealFocused)
  scan()

  return {
    refresh,
    destroy() {
      destroyed = true
      mutation?.disconnect()
      intersection?.disconnect()
      preference?.removeEventListener('change', preferenceChanged)
      root.removeEventListener('focusin', revealFocused)
      root.removeEventListener('pointerdown', revealFocused)
      for (const [element, entry] of entries) {
        finish(element, entry)
        delete element.dataset.revealState
      }
      entries.clear()
    },
  }
}
