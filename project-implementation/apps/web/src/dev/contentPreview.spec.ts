import { describe, expect, it } from 'vitest'
import { contentFixtures, contentPreview } from './contentPreview'
import { handlePreviewRequest } from './preview'
import type { ApiEnvelope } from '../types'

describe('内容预览的可见性与只读边界', () => {
  it('游客只看已发布文章，草稿与下线旧地址均不可读', () => {
    const result = contentPreview<{ id: string; status: string }[]>('/articles', new URLSearchParams(), { role: 'guest', state: 'normal' })!
    expect(result.data).toHaveLength(2)
    expect(result.data.every(item => item.status === 'PUBLISHED')).toBe(true)
    for (const article of contentFixtures.articles.filter(item => item.status !== 'PUBLISHED')) {
      expect(() => contentPreview(`/articles/${article.id}`, new URLSearchParams(), { role: 'guest', state: 'normal' })).toThrow('不存在或尚未公开')
    }
  })
  it('下载中心不返回私有资料的标题或下载地址', () => {
    const result = contentPreview('/files', new URLSearchParams(), { role: 'user', state: 'normal' }) as ApiEnvelope<typeof contentFixtures.files>
    expect(result.data).toHaveLength(1)
    expect(result.data[0]!.visibility).toBe('PUBLIC')
    expect(JSON.stringify(result)).not.toContain('经销合作 · 产品资料册')
    expect(() => contentPreview(`/files/${contentFixtures.files[1]!.id}`, new URLSearchParams(), { role: 'guest', state: 'normal' })).toThrow()
  })
  it('后台预览有完整状态，但不能执行发布或上传', () => {
    expect(handlePreviewRequest<unknown[]>('/admin/articles', {}, { role: 'admin', state: 'normal' }).data).toHaveLength(4)
    expect(() => handlePreviewRequest('/admin/articles', {}, { role: 'guest', state: 'normal' })).toThrow('管理员')
    expect(() => handlePreviewRequest('/admin/articles', { method: 'POST' }, { role: 'admin', state: 'normal' })).toThrow('只读')
    expect(() => handlePreviewRequest('/admin/files', { method: 'POST', body: new FormData() }, { role: 'admin', state: 'normal' })).toThrow('只读')
  })
  it('筛选分页保留实际总数，空状态清空内容', () => {
    const result = contentPreview<unknown[]>('/articles', new URLSearchParams('pageSize=1&page=2'), { role: 'guest', state: 'normal' })!
    expect(result.data).toHaveLength(1)
    expect(result.meta?.totalItems).toBe(2)
    expect(contentPreview<unknown[]>('/articles', new URLSearchParams(), { role: 'guest', state: 'empty' })!.data).toEqual([])
  })
})
