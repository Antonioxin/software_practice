import { isDevelopmentPreview } from '../../services/http'
import { inlineImages } from './bodyMarkup'
import type { Article } from './types'
export function mediaUrl(id: string) {
  if (import.meta.env.DEV && isDevelopmentPreview) {
    const fixtures: Record<string, string> = { 'e1000000-0000-4000-8000-000000000001': '/assets/products/balance-stones.png', 'e1000000-0000-4000-8000-000000000002': '/assets/products/ring-toss.png' }
    if (fixtures[id]) return fixtures[id]
  }
  return `/api/v1/media/${encodeURIComponent(id)}/content`
}
const dedicatedArticleCovers: Record<string, string> = {
  'e1000000-0000-4000-8000-000000000001': '/assets/articles/living-room-adventure-cover-v2.png',
  'e2000000-0000-4000-8000-000000000001': '/assets/articles/living-room-adventure-cover-v2.png',
}
export function articleCover(article: Pick<Article, 'mediaIds' | 'body'> & Partial<Pick<Article, 'id'>>) {
  const images = inlineImages(article.body)
  const inlineSources = new Set(images.map(image => image.src))
  const cover = article.mediaIds.map(mediaUrl).find(src => !inlineSources.has(src))
  return cover ?? (article.id ? dedicatedArticleCovers[article.id] : undefined) ?? images[0]?.src
}
export function messageOf(error: unknown) { return error instanceof Error ? error.message : '操作暂时没有完成，请重试。' }
