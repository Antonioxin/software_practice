// Keep article images on the same origin: the six bundled illustrations or
// registered media whose access and references are checked by the content API.
const illustration = /^\/assets\/products\/guides\/(balance-stones|rainbow-arch|ring-toss|team-board|forest-kit|skip-rope)-guide\.png$/
const media = /^\/api\/v1\/media\/[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\/content$/
export function safeImage(value: string): string | undefined {
  if (/[\\\u0000-\u0020]/.test(value)) return undefined
  return illustration.test(value) || media.test(value) ? value : undefined
}
export function inlineImages(body: string): Array<{ src: string; alt: string }> {
  const doc = new DOMParser().parseFromString(body, 'text/html')
  doc.querySelectorAll('script, style, iframe, object, embed, svg, math, form, template').forEach(node => node.remove())
  return Array.from(doc.querySelectorAll('img')).flatMap(image => {
    const src = safeImage(image.getAttribute('src') ?? '')
    return src ? [{ src, alt: image.getAttribute('alt') ?? '' }] : []
  })
}
