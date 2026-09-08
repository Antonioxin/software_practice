import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount, type VueWrapper } from '@vue/test-utils'
import AdminContentPage from './AdminContentPage.vue'
import { readArticles, readBanners, readFaqs, readAllMedia, readProducts, saveRecord, setPublication } from '../../features/content/api'
import type { Article, Faq, Media, PageResult } from '../../features/content/types'
vi.mock('../../features/content/api', () => ({ readArticles: vi.fn(), readBanners: vi.fn(), readFaqs: vi.fn(), readAllMedia: vi.fn(), readProducts: vi.fn(), saveRecord: vi.fn(), setPublication: vi.fn() }))
const article: Article = { id: 'article1', title: '一起去户外', summary: '每天留一点玩的时间', pageDescription: '', body: '<p>从一个小小的游戏开始。</p>', category: '玩法灵感', productIds: ['product1'], mediaIds: [], status: 'DRAFT', sortOrder: 0, version: 7, createdAt: '2026-09-08', updatedAt: '2026-09-08', publishedAt: null }
const faq: Faq = { id: 'faq1', question: '如何清洁？', answer: '<p>用软布擦拭。</p>', category: '使用与保养', productIds: [], status: 'PUBLISHED', sortOrder: 1, version: 3, createdAt: '2026-09-08', updatedAt: '2026-09-08', publishedAt: '2026-09-08' }
const page = <T,>(items: T[]): PageResult<T> => ({ items, meta: { page: 1, pageSize: 20, totalItems: items.length, totalPages: 1 } })
let view: VueWrapper | undefined
beforeEach(() => {
  vi.mocked(readArticles).mockReset().mockResolvedValue(page([article])); vi.mocked(readFaqs).mockReset().mockResolvedValue(page([faq])); vi.mocked(readBanners).mockReset().mockResolvedValue(page([]))
  vi.mocked(readAllMedia).mockReset().mockResolvedValue([]); vi.mocked(readProducts).mockReset().mockResolvedValue([{ id: 'product1', name: '平衡石', status: 'PUBLISHED' }]); vi.mocked(saveRecord).mockReset(); vi.mocked(setPublication).mockReset()
})
afterEach(() => view?.unmount())
async function render() { view = mount(AdminContentPage, { global: { stubs: { SiteShell: { template: '<main><slot /></main>' }, RouterLink: { template: '<a><slot /></a>' }, ElDialog: { props: ['modelValue'], template: '<section v-if="modelValue" class="test-dialog"><slot /><slot name="footer" /></section>' } } } }); await flushPromises(); return view }
function button(wrapper: VueWrapper, label: string) { return wrapper.findAll('button').find(item => item.text() === label)! }
const imageFixture = (id: string, altText: string): Media => ({ id, altText, url: `/api/v1/media/${id}/content`, filename: 'image.png', mimeType: 'image/png', width: 100, height: 100, sizeBytes: 100, version: 1, referenceCount: 1, publicReferenceCount: 0, createdAt: '' })

describe('后台内容编辑与发布', () => {
  it('空白内容可保存为草稿，发布仍由独立操作完成', async () => {
    const wrapper = await render(); await button(wrapper, '新建文章').trigger('click')
    vi.mocked(saveRecord).mockResolvedValue({ ...article, id: 'draft', title: '', body: '' })
    expect((wrapper.get('.content-form').element as HTMLFormElement).checkValidity()).toBe(true)
    await wrapper.get('.content-form').trigger('submit'); await flushPromises()
    expect(saveRecord).toHaveBeenCalledWith('/admin/articles', expect.objectContaining({ title: '', body: '' }), undefined)
    expect(setPublication).not.toHaveBeenCalled()
    expect(wrapper.get('[role="status"]').text()).toContain('已保存为草稿')
  })
  it('100 个 emoji 标题按 Unicode 码点计数，完整提交草稿', async () => {
    const wrapper = await render(); await button(wrapper, '新建文章').trigger('click')
    const emojiTitle = '🌿'.repeat(100)
    vi.mocked(saveRecord).mockResolvedValue({ ...article, id: 'emoji-draft', title: emojiTitle })
    const input = wrapper.get('.content-form > label input')
    expect((input.element as HTMLInputElement).maxLength).toBeGreaterThanOrEqual(emojiTitle.length)
    await input.setValue(emojiTitle)
    expect(wrapper.get('.content-form').text()).toContain('100 / 100 字符')
    await wrapper.get('.content-form').trigger('submit'); await flushPromises()
    expect(saveRecord).toHaveBeenCalledWith('/admin/articles', expect.objectContaining({ title: emojiTitle }), undefined)
    expect(wrapper.get('[role="status"]').text()).toContain('已保存为草稿')
  })
  it('101 个字符的标题在提交前明确拒绝，保留原文', async () => {
    const wrapper = await render(); await button(wrapper, '新建文章').trigger('click')
    await wrapper.get('.content-form > label input').setValue('🌿'.repeat(101))
    await wrapper.get('.content-form').trigger('submit'); await flushPromises()
    expect(saveRecord).not.toHaveBeenCalled()
    expect(wrapper.get('.test-dialog [role="alert"]').text()).toContain('当前为 101 个')
    expect((wrapper.get('.content-form > label input').element as HTMLInputElement).value).toBe('🌿'.repeat(101))
  })
  it('编辑保留关联商品与当前版本，保存中禁止再次提交', async () => {
    const wrapper = await render(); await button(wrapper, '编辑').trigger('click')
    let finish!: (value: unknown) => void
    vi.mocked(saveRecord).mockImplementation(() => new Promise(resolve => { finish = resolve }))
    await wrapper.get('.content-form > label input').setValue('新的户外玩法')
    await wrapper.get('.content-form').trigger('submit'); await flushPromises()
    expect(saveRecord).toHaveBeenCalledWith('/admin/articles', expect.objectContaining({ title: '新的户外玩法', expectedVersion: 7, productIds: ['product1'] }), 'article1')
    expect(button(wrapper, '正在保存…').attributes('disabled')).toBeDefined()
    await wrapper.get('.content-form').trigger('submit'); expect(saveRecord).toHaveBeenCalledTimes(1)
    finish({ ...article, version: 8 }); await flushPromises()
    expect(wrapper.find('.test-dialog').exists()).toBe(false)
    expect(wrapper.get('[role="status"]').text()).toContain('文章已保存')
  })
  it('服务器拒绝保存后，保留编辑内容并展示原因', async () => {
    const wrapper = await render(); await button(wrapper, '编辑').trigger('click')
    vi.mocked(saveRecord).mockRejectedValue(new Error('内容已被另一位管理员更新，请刷新后再试。'))
    await wrapper.get('.content-form > label input').setValue('我未保存的修改')
    await wrapper.get('.content-form').trigger('submit'); await flushPromises()
    expect(wrapper.get('.test-dialog [role="alert"]').text()).toContain('另一位管理员更新')
    expect((wrapper.get('.content-form > label input').element as HTMLInputElement).value).toBe('我未保存的修改')
    expect(button(wrapper, '保存修改').attributes('disabled')).toBeUndefined()
  })
  it('移除已保存的正文媒体后不残留为末尾图库，并保留独立封面', async () => {
    const inline = imageFixture('e1000000-0000-4000-8000-000000000010', '正文玩法图')
    const cover = imageFixture('e1000000-0000-4000-8000-000000000011', '独立封面图')
    vi.mocked(readArticles).mockResolvedValue(page([{ ...article, body: `${article.body}<figure><img src="${inline.url}" alt="正文玩法图"></figure>`, mediaIds: [cover.id, inline.id] }]))
    vi.mocked(readAllMedia).mockResolvedValue([inline, cover])
    const wrapper = await render(); await button(wrapper, '编辑').trigger('click')
    const choices = wrapper.findAll('.content-media-options input')
    expect(choices[0]!.attributes('disabled')).toBeDefined()
    expect((choices[0]!.element as HTMLInputElement).checked).toBe(false)
    expect((choices[1]!.element as HTMLInputElement).checked).toBe(true)
    expect(wrapper.get('.content-media-options').text()).toContain('已用于正文 · 在图文块中管理')
    await wrapper.get('[aria-label="移除第 2 块"]').trigger('click')
    expect(choices[0]!.attributes('disabled')).toBeUndefined()
    await wrapper.get('.content-form').trigger('submit'); await flushPromises()
    expect(saveRecord).toHaveBeenCalledWith('/admin/articles', expect.objectContaining({ body: article.body, mediaIds: [cover.id] }), article.id)
  })
  it('独立配图移入正文后交由区块管理，随后删除不会重新作为额外图片保存', async () => {
    const image = imageFixture('e1000000-0000-4000-8000-000000000010', '待编排图片')
    vi.mocked(readArticles).mockResolvedValue(page([{ ...article, mediaIds: [image.id] }]))
    vi.mocked(readAllMedia).mockResolvedValue([image])
    const wrapper = await render(); await button(wrapper, '编辑').trigger('click')
    expect((wrapper.get('.content-media-options input').element as HTMLInputElement).checked).toBe(true)
    await button(wrapper, '添加图文块').trigger('click')
    await wrapper.get('.article-image-picker select').setValue(image.id)
    const choice = wrapper.get('.content-media-options input')
    expect(choice.attributes('disabled')).toBeDefined()
    expect((choice.element as HTMLInputElement).checked).toBe(false)
    await wrapper.get('[aria-label="移除第 2 块"]').trigger('click')
    await wrapper.get('.content-form').trigger('submit'); await flushPromises()
    expect(saveRecord).toHaveBeenCalledWith('/admin/articles', expect.objectContaining({ body: article.body, mediaIds: [] }), article.id)
  })
  it('FAQ 下线通过独立状态操作提交预期版本，并更新列表', async () => {
    const wrapper = await render(); await button(wrapper, '常见问题').trigger('click'); await flushPromises()
    vi.mocked(setPublication).mockResolvedValue({ ...faq, status: 'OFFLINE', version: 4 })
    await button(wrapper, '下线').trigger('click')
    expect(wrapper.get('.test-dialog').text()).toContain('访客将无法继续访问')
    await button(wrapper, '确认').trigger('click'); await flushPromises()
    expect(setPublication).toHaveBeenCalledWith('/admin/faqs', expect.objectContaining({ id: 'faq1', version: 3 }), false)
    expect(wrapper.get('[role="status"]').text()).toContain('常见问题已下线')
    expect(readFaqs).toHaveBeenCalledTimes(2)
  })
})
