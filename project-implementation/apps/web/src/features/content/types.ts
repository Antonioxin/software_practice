import type { PageMeta } from '../../types'
export type ContentStatus = 'DRAFT' | 'PUBLISHED' | 'OFFLINE'
export interface Publication { id: string; status: ContentStatus; sortOrder: number; version: number; createdAt: string; updatedAt: string; publishedAt: string | null }
export interface Article extends Publication { title: string; summary: string; pageDescription?: string; body: string; category: string; productIds: string[]; mediaIds: string[] }
export interface Faq extends Publication { question: string; answer: string; category: string; productIds: string[] }
export interface Banner extends Publication { title: string; imageId: string | null; buttonText: string; targetUrl: string }
export interface SiteSettings { brandName: string; brandDescription: string; contactEmail: string; contactPhone: string; contactAddress: string; logoMediaId: string | null; termsText: string; termsVersion: string; privacyText: string; privacyVersion: string; version: number }
export interface HomeSettings { recommendedProductIds: string[]; featuredArticleIds: string[]; version: number }
export interface ContentFile { id: string; title: string; type: string; versionNote: string; productIds: string[]; visibility: 'PUBLIC' | 'DEALER' | 'INTERNAL'; status: ContentStatus; version: number; downloadId: string; downloadUrl: string; filename: string; mimeType: string; sizeBytes: number; updatedAt: string; createdAt: string }
export interface Media { id: string; url: string; altText: string; mimeType: string; width: number; height: number; filename: string; sizeBytes: number; version: number; referenceCount: number; publicReferenceCount: number; createdAt: string }
export interface ProductChoice { id: string; name: string; status?: string }
export interface PageResult<T> { items: T[]; meta: PageMeta }
export const statusLabels: Record<ContentStatus, string> = { DRAFT: '草稿', PUBLISHED: '已发布', OFFLINE: '已下线' }
export const visibilityLabels = { PUBLIC: '所有访客', DEALER: '经销商', INTERNAL: '仅后台' }
export const formatDate = (value: string | null | undefined) => value ? new Date(value).toLocaleDateString('zh-CN') : '尚未发布'
export const formatSize = (value: number) => value < 1024 * 1024 ? `${Math.ceil(value / 1024)} KB` : `${(value / 1024 / 1024).toFixed(1)} MB`
