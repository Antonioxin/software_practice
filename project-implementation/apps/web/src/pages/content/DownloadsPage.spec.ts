import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount, type VueWrapper } from '@vue/test-utils'
import { createMemoryHistory, createRouter } from 'vue-router'
import DownloadsPage from './DownloadsPage.vue'
import { downloadFile, readFiles, readProducts } from '../../features/content/api'
import { ApiProblem } from '../../services/http'
import type { ContentFile, PageResult } from '../../features/content/types'
vi.mock('../../features/content/api', () => ({ downloadFile: vi.fn(), readFiles: vi.fn(), readProducts: vi.fn() }))
const record: ContentFile = { id: 'f1', title: '平衡石使用指南', type: '产品说明', versionNote: 'v1', productIds: [], visibility: 'PUBLIC', status: 'PUBLISHED', version: 1, downloadId: 'd1', downloadUrl: '/api/v1/files/f1/versions/d1/content', filename: 'guide.pdf', mimeType: 'application/pdf', sizeBytes: 2048, createdAt: '2026-09-08T00:00:00Z', updatedAt: '2026-09-08T00:00:00Z' }
const page = (items: ContentFile[]): PageResult<ContentFile> => ({ items, meta: { page: 1, pageSize: 12, totalItems: items.length, totalPages: 1 } })
let view: VueWrapper | undefined
beforeEach(() => { vi.mocked(readFiles).mockReset().mockResolvedValue(page([record])); vi.mocked(readProducts).mockReset().mockResolvedValue([{ id: 'product1', name: '平衡石' }]); vi.mocked(downloadFile).mockReset() })
afterEach(() => { view?.unmount() })
async function render() { const router = createRouter({ history: createMemoryHistory(), routes: [{ path: '/downloads', component: { template: '<div />' } }] }); await router.push('/downloads'); view = mount(DownloadsPage, { global: { plugins: [router], stubs: { PublicShell: { template: '<main><slot /></main>' } } } }); await flushPromises(); return view }

describe('下载中心行为', () => {
  it('按关键词、类型和商品查询资料', async () => {
    const wrapper = await render()
    await wrapper.get('input[type="search"]').setValue('平衡')
    await wrapper.findAll('input')[1]!.setValue('产品说明')
    await wrapper.get('select').setValue('product1')
    await wrapper.get('form').trigger('submit'); await flushPromises()
    expect(readFiles).toHaveBeenLastCalledWith({ keyword: '平衡', type: '产品说明', productId: 'product1', page: 1, pageSize: 12 })
  })
  it('下载被拒后显示服务端错误，重新获取列表并清除旧资料', async () => {
    const wrapper = await render()
    vi.mocked(downloadFile).mockRejectedValue(new ApiProblem({ type: 'about:blank', title: 'Forbidden', status: 403, detail: '经销商资格已失效', code: 'FORBIDDEN' }))
    vi.mocked(readFiles).mockResolvedValue(page([]))
    await wrapper.get('.content-file-card button').trigger('click'); await flushPromises()
    expect(wrapper.get('[role="alert"]').text()).toContain('经销商资格已失效')
    expect(wrapper.findAll('.content-file-card')).toHaveLength(0)
    expect(wrapper.text()).not.toContain('已准备下载')
    expect(readFiles).toHaveBeenCalledTimes(2)
  })

  it('身份改变时立即清除旧可见资料并重新查询', async () => {
    const wrapper = await render()
    let resolve!: (value: PageResult<ContentFile>) => void
    vi.mocked(readFiles).mockImplementation(() => new Promise(done => { resolve = done }))
    window.dispatchEvent(new CustomEvent('wemove:account-changed'))
    await flushPromises()
    expect(wrapper.findAll('.content-file-card')).toHaveLength(0)
    expect(wrapper.text()).not.toContain('平衡石使用指南')
    resolve(page([])); await flushPromises()
    expect(wrapper.text()).toContain('暂时没有可下载的资料')
  })
  it('较旧的列表请求晚完成时不会覆盖新的筛选结果', async () => {
    const wrapper = await render()
    let resolveOlder!: (value: PageResult<ContentFile>) => void
    vi.mocked(readFiles).mockImplementationOnce(() => new Promise(resolve => { resolveOlder = resolve })).mockResolvedValueOnce(page([{ ...record, id: 'new', title: '新筛选的资料' }]))
    await wrapper.get('form').trigger('submit')
    await wrapper.get('input[type="search"]').setValue('新的')
    await wrapper.get('form').trigger('submit'); await flushPromises()
    resolveOlder(page([{ ...record, id: 'old', title: '旧筛选的资料' }]))
    await flushPromises()
    expect(wrapper.text()).toContain('新筛选的资料')
    expect(wrapper.text()).not.toContain('旧筛选的资料')
  })
})
