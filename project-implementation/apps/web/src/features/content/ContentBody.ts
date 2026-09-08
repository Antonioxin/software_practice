import { defineComponent, h, type VNodeChild } from 'vue'
import { safeImage } from './bodyMarkup'
const allowed = new Set(['p', 'br', 'strong', 'b', 'em', 'i', 'u', 's', 'h2', 'h3', 'h4', 'ul', 'ol', 'li', 'blockquote', 'code', 'pre', 'a', 'hr', 'figure', 'figcaption', 'section', 'div'])
const discard = new Set(['script', 'style', 'iframe', 'object', 'embed', 'svg', 'math', 'form', 'input', 'template'])
export function safeLink(value: string) { return /^(https?:\/\/|\/(?!\/)|#)/i.test(value) && !/[\\\u0000-\u0020]/.test(value) ? value : undefined }
export default defineComponent({
  name: 'ContentBody', props: { body: { type: String, default: '' } },
  setup(props) {
    function node(value: Node): VNodeChild {
      if (value.nodeType === Node.TEXT_NODE) return value.textContent
      if (!(value instanceof Element)) return null
      const tag = value.tagName.toLowerCase()
      if (discard.has(tag)) return null
      if (tag === 'img') {
        const src = safeImage(value.getAttribute('src') ?? '')
        return src ? h('img', { src, alt: value.getAttribute('alt') ?? '', loading: 'lazy', decoding: 'async' }) : null
      }
      const children = Array.from(value.childNodes).map(node)
      if (!allowed.has(tag)) return children
      const attributes: Record<string, string | undefined> = {}
      if (tag === 'a') { attributes.href = safeLink(value.getAttribute('href') ?? ''); attributes.rel = 'noopener noreferrer' }
      if (tag === 'section') {
        const layout = value.getAttribute('data-layout')
        if (layout === 'image-left' || layout === 'image-right') attributes['data-layout'] = layout
      }
      return h(tag, attributes, children)
    }
    return () => {
      if (!/<[a-z][\s\S]*>/i.test(props.body)) return h('div', { class: 'content-body content-body-plain' }, props.body)
      const doc = new DOMParser().parseFromString(props.body, 'text/html')
      return h('div', { class: 'content-body' }, Array.from(doc.body.childNodes).map(node))
    }
  },
})
