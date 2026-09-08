import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount, type VueWrapper } from '@vue/test-utils'
import AdminSettingsPage from './AdminSettingsPage.vue'
import { patchSettings, readAllArticles, readHomeSettings, readAllMedia, readProducts, readSettings } from '../../features/content/api'
import type { Article, SiteSettings } from '../../features/content/types'
vi.mock('../../features/content/api', () => ({ patchSettings: vi.fn(), readAllArticles: vi.fn(), readHomeSettings: vi.fn(), readAllMedia: vi.fn(), readProducts: vi.fn(), readSettings: vi.fn() }))
const settings: SiteSettings = { brandName: 'WEMOVE', brandDescription: '<p>一起玩</p>', contactEmail: 'hello@example.test', contactPhone: '', contactAddress: '', logoMediaId: null, termsText: '<p>服务条款</p>', termsVersion: '2026-09', privacyText: '<p>隐私政策</p>', privacyVersion: '2026-09', version: 7 }
let view: VueWrapper | undefined
beforeEach(() => {
  vi.mocked(readSettings).mockReset().mockResolvedValue(settings)
  vi.mocked(readHomeSettings).mockReset().mockResolvedValue({ recommendedProductIds: [], featuredArticleIds: [], version: 2 })
  vi.mocked(readAllArticles).mockReset().mockResolvedValue([]); vi.mocked(readAllMedia).mockReset().mockResolvedValue([]); vi.mocked(readProducts).mockReset().mockResolvedValue([])
  vi.mocked(patchSettings).mockReset().mockResolvedValue({ ...settings, version: 8 })
})
afterEach(() => view?.unmount())
describe('品牌设置请求契约', () => {
  it('下线或已失效的首页推荐可以取消，新推荐只允许选择已发布内容', async () => {
    vi.mocked(readHomeSettings).mockResolvedValue({ recommendedProductIds: ['p-old', 'p-missing'], featuredArticleIds: ['a-old', 'a-missing'], version: 2 })
    vi.mocked(readProducts).mockResolvedValue([{ id: 'p-old', name: '已下架商品', status: 'UNLISTED' }, { id: 'p-draft', name: '草稿商品', status: 'DRAFT' }])
    vi.mocked(readAllArticles).mockResolvedValue([{ id: 'a-old', title: '下线文章', status: 'OFFLINE' }, { id: 'a-draft', title: '草稿文章', status: 'DRAFT' }] as Article[])
    view = mount(AdminSettingsPage, { global: { stubs: { SiteShell: { template: '<main><slot /></main>' }, RouterLink: { template: '<a><slot /></a>' } } } })
    await flushPromises()
    const form = view.findAll('form')[1]!
    for (const id of ['p-old', 'p-missing', 'a-old', 'a-missing']) {
      const input = form.get(`input[value="${id}"]`)
      expect(input.attributes('disabled')).toBeUndefined()
      await input.setValue(false)
    }
    for (const id of ['p-draft', 'a-draft']) expect(form.get(`input[value="${id}"]`).attributes('disabled')).toBeDefined()
    await form.trigger('submit'); await flushPromises()
    expect(patchSettings).toHaveBeenCalledExactlyOnceWith('/admin/home', { recommendedProductIds: [], featuredArticleIds: [], expectedVersion: 2 })
  })
  it('保存只提交设置字段与 expectedVersion，不泄漏响应中的 version 字段', async () => {
    view = mount(AdminSettingsPage, { global: { stubs: { SiteShell: { template: '<main><slot /></main>' }, RouterLink: { template: '<a><slot /></a>' } } } })
    await flushPromises()
    await view.get('form').trigger('submit'); await flushPromises()
    expect(patchSettings).toHaveBeenCalledExactlyOnceWith('/admin/site-settings', {
      brandName: 'WEMOVE', brandDescription: '<p>一起玩</p>', contactEmail: 'hello@example.test', contactPhone: '', contactAddress: '', logoMediaId: null,
      termsText: '<p>服务条款</p>', termsVersion: '2026-09', privacyText: '<p>隐私政策</p>', privacyVersion: '2026-09', expectedVersion: 7,
    })
    expect(vi.mocked(patchSettings).mock.calls[0]?.[1]).not.toHaveProperty('version')
    const versionInputs = view.findAll('label').filter(label => label.text().startsWith('服务条款版本') || label.text().startsWith('隐私政策版本')).map(label => label.get('input'))
    expect(versionInputs).toHaveLength(2)
    expect(versionInputs.every(input => input.attributes('maxlength') === '32')).toBe(true)
    expect(view.get('[role="status"]').text()).toContain('品牌与联系信息已保存')
  })
})
