import { api, apiDownload, ApiProblem, newIdempotencyKey } from '../../services/http'
import type { Article, Banner, ContentFile, Faq, HomeSettings, Media, PageResult, ProductChoice, SiteSettings } from './types'

export function buildQuery(filters: Record<string, string | number | undefined>) {
  const query = new URLSearchParams()
  Object.entries(filters).forEach(([key, value]) => { if (value !== undefined && String(value).trim()) query.set(key, String(value).trim()) })
  return query.toString()
}
export async function readPage<T>(path: string, filters: Record<string, string | number | undefined> = {}): Promise<PageResult<T>> {
  const envelope = await api<T[]>(`${path}?${buildQuery(filters)}`)
  const meta = envelope.meta as (NonNullable<typeof envelope.meta> & { total?: number }) | undefined
  return { items: envelope.data, meta: { page: meta?.page ?? 1, pageSize: meta?.pageSize ?? 20, totalItems: meta?.totalItems ?? meta?.total ?? envelope.data.length, totalPages: meta?.totalPages ?? 1 } }
}
export const readArticles = (filters = {}, admin = false) => readPage<Article>(`${admin ? '/admin' : ''}/articles`, filters)
export const readArticle = async (id: string) => (await api<Article>(`/articles/${encodeURIComponent(id)}`)).data
export const readFaqs = (filters = {}, admin = false) => readPage<Faq>(`${admin ? '/admin' : ''}/faqs`, filters)
export const readBanners = (filters = {}) => readPage<Banner>('/admin/banners', filters)
export const readFiles = (filters = {}, admin = false) => readPage<ContentFile>(`${admin ? '/admin' : ''}/files`, filters)
export const readMedia = (filters = {}) => readPage<Media>('/admin/media', filters)
export async function readAll<T>(path: string, filters: Record<string, string | number | undefined> = {}): Promise<T[]> {
  const first = await readPage<T>(path, { ...filters, page: 1, pageSize: 50 })
  const items = [...first.items]
  for (let page = 2; page <= first.meta.totalPages; page++) items.push(...(await readPage<T>(path, { ...filters, page, pageSize: 50 })).items)
  return items
}
export const readProducts = (admin = false) => readAll<ProductChoice>(`${admin ? '/admin' : ''}/products`)
export const readAllMedia = () => readAll<Media>('/admin/media')
export const readAllArticles = () => readAll<Article>('/admin/articles')
export const readSettings = async (admin = false) => (await api<SiteSettings>(`${admin ? '/admin' : ''}/site-settings`)).data
export const readHomeSettings = async () => (await api<HomeSettings>('/admin/home')).data
// Keep the same key while a command's outcome is unknown; a retry must not create a second record.
const pendingCommands = new Map<string, string>()
async function command<T>(path: string, body: unknown): Promise<T> {
  const serialized = JSON.stringify(body)
  const fingerprint = `${path}:${serialized}`
  const key = pendingCommands.get(fingerprint) ?? newIdempotencyKey()
  pendingCommands.set(fingerprint, key)
  try {
    const result = (await api<T>(path, { method: 'POST', headers: { 'Idempotency-Key': key }, body: serialized })).data
    pendingCommands.delete(fingerprint)
    return result
  } catch (cause) {
    if (cause instanceof ApiProblem && cause.problem.status >= 400 && cause.problem.status < 500) pendingCommands.delete(fingerprint)
    throw cause
  }
}
export async function saveRecord<T>(path: string, body: unknown, id?: string) {
  if (!id) return command<T>(path, body)
  return (await api<T>(`${path}/${encodeURIComponent(id)}`, { method: 'PATCH', body: JSON.stringify(body) })).data
}
export async function setPublication<T>(path: string, item: { id: string; version: number }, publish: boolean) {
  return command<T>(`${path}/${encodeURIComponent(item.id)}/${publish ? 'publish' : 'unpublish'}`, { expectedVersion: item.version })
}
export const patchSettings = async (path: '/admin/site-settings' | '/admin/home', body: unknown) => (await api(path, { method: 'PATCH', body: JSON.stringify(body) })).data
export async function uploadFile(file: File, metadata: unknown) {
  const body = new FormData(); body.append('file', file); body.append('metadata', new Blob([JSON.stringify(metadata)], { type: 'application/json' }))
  return (await api<ContentFile>('/admin/files', { method: 'POST', body })).data
}
export async function replaceFile(item: ContentFile, file: File, versionNote: string) {
  const body = new FormData(); body.append('file', file); body.append('expectedVersion', String(item.version)); body.append('versionNote', versionNote)
  return (await api<ContentFile>(`/admin/files/${item.id}/replace`, { method: 'POST', body })).data
}
export async function uploadMedia(file: File, altText: string) {
  const body = new FormData(); body.append('file', file); body.append('altText', altText)
  return (await api<Media>('/admin/media', { method: 'POST', body })).data
}
export const deleteMedia = async (item: Media) => api(`/admin/media/${item.id}?expectedVersion=${item.version}`, { method: 'DELETE' })
export async function downloadFile(item: ContentFile) {
  // Only consume a same-origin API download path; never send credentials to a supplied external URL.
  if (!item.downloadUrl.startsWith('/api/v1/files/')) throw new Error('文件下载地址无效，请刷新后重试。')
  const blob = await apiDownload(item.downloadUrl.slice('/api/v1'.length))
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a'); link.href = url; link.download = item.filename; document.body.append(link); link.click(); link.remove()
  window.setTimeout(() => URL.revokeObjectURL(url), 1000)
}
