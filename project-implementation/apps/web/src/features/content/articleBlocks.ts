import { safeImage } from './bodyMarkup'
import { safeLink } from './ContentBody'

export const articleIllustrations = [
  { key: 'balance-stones', label: '平衡石 · 小小探险家', alt: '卡通孩子在真实彩色平衡石之间迈步' },
  { key: 'rainbow-arch', label: '彩虹拱桥 · 想象力游戏', alt: '卡通孩子与真实彩虹拱桥一起玩耍' },
  { key: 'ring-toss', label: '套圈 · 一起瞄准', alt: '卡通孩子使用真实木质套圈玩具' },
  { key: 'team-board', label: '协作板 · 团队挑战', alt: '卡通孩子合作体验真实团队平衡板' },
  { key: 'forest-kit', label: '森林套装 · 户外发现', alt: '卡通孩子带着真实森林探索套装发现自然' },
  { key: 'skip-rope', label: '跳绳 · 跳出好心情', alt: '卡通孩子使用真实跳绳进行运动游戏' },
].map(item => ({ ...item, src: `/assets/products/guides/${item.key}-guide.png` }))

export interface TextBlock { id: string; type: 'text'; html: string }
export interface ImageBlock { id: string; type: 'image'; layout: 'image-left' | 'image-right' | 'full'; src: string; alt: string; caption: string; title: string; html: string }
export type ArticleBlock = TextBlock | ImageBlock
let sequence = 0
export const blockId = () => `article-block-${++sequence}`
export const escapeHtml = (value: string) => value.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;').replace(/'/g, '&#39;')

/** Preserve all text markup while recognizing only the image layouts we own. */
export function parseArticleBlocks(body: string): ArticleBlock[] {
  const document = new DOMParser().parseFromString(body, 'text/html')
  const blocks: ArticleBlock[] = []
  let pending = ''
  const flush = () => { if (pending.trim()) blocks.push({ id: blockId(), type: 'text', html: pending }); pending = '' }
  for (const node of Array.from(document.body.childNodes)) {
    const element = node instanceof Element ? node : null
    const isFigure = element?.tagName === 'FIGURE'
    const layout = element?.getAttribute('data-layout')
    const isSection = element?.tagName === 'SECTION' && (layout === 'image-left' || layout === 'image-right')
    const figure = isFigure ? element : isSection ? element?.querySelector(':scope > figure') : null
    const image = figure?.querySelector(':scope > img')
    const copy = isSection ? element?.querySelector(':scope > div')?.cloneNode(true) as Element | undefined : undefined
    if (figure && image && safeImage(image.getAttribute('src') ?? '') && (!isSection || copy)) {
      flush()
      const heading = copy?.querySelector(':scope > h3')
      const title = heading?.textContent ?? ''
      heading?.remove()
      blocks.push({ id: blockId(), type: 'image', layout: isFigure ? 'full' : layout as 'image-left' | 'image-right', src: image.getAttribute('src')!, alt: image.getAttribute('alt') ?? '', caption: figure.querySelector('figcaption')?.textContent ?? '', title, html: copy?.innerHTML ?? '' })
    } else pending += element ? element.outerHTML : node.nodeType === Node.TEXT_NODE ? escapeHtml(node.textContent ?? '') : ''
  }
  flush()
  return blocks
}

export function serializeArticleBlocks(blocks: ArticleBlock[]): string {
  return blocks.map(block => {
    if (block.type === 'text') return block.html
    const image = safeImage(block.src)
    if (!image) return block.html
    const figure = `<figure><img src="${escapeHtml(image)}" alt="${escapeHtml(block.alt)}">${block.caption ? `<figcaption>${escapeHtml(block.caption)}</figcaption>` : ''}</figure>`
    const copy = `${block.title ? `<h3>${escapeHtml(block.title)}</h3>` : ''}${block.html}`
    if (block.layout === 'full') return `${figure}${copy}`
    return `<section data-layout="${block.layout}">${figure}<div>${copy}</div></section>`
  }).join('\n')
}

/** Safe, editable markup. Scripts and event attributes never enter contenteditable. */
export function editableHtml(html: string): string {
  const doc = new DOMParser().parseFromString(html, 'text/html')
  const allowed = new Set(['p', 'div', 'br', 'strong', 'b', 'em', 'i', 'u', 's', 'h2', 'h3', 'h4', 'ul', 'ol', 'li', 'blockquote', 'code', 'pre', 'a', 'hr', 'figure', 'figcaption', 'section'])
  doc.querySelectorAll('script, style, iframe, object, embed, svg, math, form, input, template').forEach(node => node.remove())
  for (const element of Array.from(doc.body.querySelectorAll('*')).reverse()) {
    const tag = element.tagName.toLowerCase()
    const href = tag === 'a' ? safeLink(element.getAttribute('href') ?? '') : undefined
    const src = tag === 'img' ? safeImage(element.getAttribute('src') ?? '') : undefined
    const alt = element.getAttribute('alt') ?? ''
    const layout = tag === 'section' ? element.getAttribute('data-layout') : null
    if (!allowed.has(tag) && !(tag === 'img' && src)) { element.replaceWith(...element.childNodes); continue }
    for (const name of element.getAttributeNames()) element.removeAttribute(name)
    if (href) { element.setAttribute('href', href); element.setAttribute('rel', 'noopener noreferrer') }
    if (src) { element.setAttribute('src', src); element.setAttribute('alt', alt) }
    if (layout === 'image-left' || layout === 'image-right') element.setAttribute('data-layout', layout)
  }
  return doc.body.innerHTML
}
