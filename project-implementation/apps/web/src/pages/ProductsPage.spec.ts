import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount, type VueWrapper } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { createMemoryHistory, createRouter, type Router } from 'vue-router'
import ProductsPage from './ProductsPage.vue'
import { api } from '../services/http'
import { fixtures } from '../dev/fixtures'
import type { ApiEnvelope, ProductCard } from '../types'

vi.mock('../services/http', async (original) => ({
  ...(await original<typeof import('../services/http')>()),
  api: vi.fn(),
}))

const mockedApi = vi.mocked(api)
const forbiddenFetch = vi.fn(() => { throw new Error('商品页组件测试不得访问真实后端') })
const products: ProductCard[] = Array.from({ length: 25 }, (_, index) => ({
  ...fixtures.products[index % fixtures.products.length]!,
  id: `00000000-0000-4000-8000-${String(index + 1).padStart(12, '0')}`,
  sku: `WM-TEST-${index + 1}`,
  name: `测试商品 ${index + 1}`,
}))
let wrapper: VueWrapper | undefined
let router: Router

function responseFor(path: string): ApiEnvelope<unknown> {
  if (path === '/categories') return { data: fixtures.categories }
  if (path === '/product-options') return { data: fixtures.productOptions }
  if (path.startsWith('/products?')) {
    const query = new URL(path, 'https://test.invalid').searchParams
    const page = Number(query.get('page'))
    const pageSize = Number(query.get('pageSize'))
    return { data: products.slice((page - 1) * pageSize, page * pageSize), meta: { page, pageSize, totalItems: products.length, totalPages: Math.ceil(products.length / pageSize) } }
  }
  throw new Error(`未预期的接口：${path}`)
}

function productCalls() { return mockedApi.mock.calls.filter(([path]) => path.startsWith('/products?')) }
function lastProductQuery() {
  return new URL(productCalls().at(-1)![0], 'https://test.invalid').searchParams
}
async function settle() { await flushPromises(); await flushPromises() }
async function render(query: Record<string, string> = {}) {
  const pinia = createPinia()
  setActivePinia(pinia)
  router = createRouter({ history: createMemoryHistory(), routes: [{ path: '/products', component: { template: '<div />' } }, { path: '/:pathMatch(.*)*', component: { template: '<div />' } }] })
  await router.push({ path: '/products', query })
  wrapper = mount(ProductsPage, { attachTo: document.body, global: { plugins: [pinia, router] } })
  await router.isReady()
  await settle()
  return wrapper
}
function deferred<T>() {
  let resolve!: (value: T) => void
  let reject!: (reason: Error) => void
  const promise = new Promise<T>((yes, no) => { resolve = yes; reject = no })
  return { promise, resolve, reject }
}

beforeEach(() => {
  mockedApi.mockReset()
  mockedApi.mockImplementation(async (path) => responseFor(path))
  vi.stubGlobal('fetch', forbiddenFetch)
})
afterEach(async () => {
  wrapper?.unmount()
  wrapper = undefined
  await settle()
  document.body.innerHTML = ''
  expect(forbiddenFetch).not.toHaveBeenCalled()
  forbiddenFetch.mockClear()
  vi.unstubAllGlobals()
})

describe('商品目录筛选与路由行为', () => {
  it('年龄数字输入提交后，切换分类和排序仍保留 age=6 且不产生组件异常', async () => {
    const page = await render()
    const errors: unknown[] = []
    page.vm.$.appContext.config.errorHandler = error => { errors.push(error) }
    await page.get('.shop-filter-fields input[type="number"]').setValue('6')
    await page.get('form[aria-label="商品筛选"]').trigger('submit')
    await settle()
    expect(router.currentRoute.value.query.age).toBe('6')
    expect(lastProductQuery().get('age')).toBe('6')
    const category = fixtures.categories[1]!
    await page.findAll('nav[aria-label="商品分类"] button').find(button => button.text().includes(category.name))!.trigger('click')
    await settle()
    expect(router.currentRoute.value.query).toMatchObject({ age: '6', categoryId: category.id })
    expect(lastProductQuery().get('age')).toBe('6')
    await page.get('.shop-toolbar select').setValue('priceAsc')
    await settle()
    expect(router.currentRoute.value.query).toMatchObject({ age: '6', categoryId: category.id, sort: 'priceAsc' })
    expect(lastProductQuery().get('age')).toBe('6')
    expect((page.get('.shop-filter-fields input[type="number"]').element as HTMLInputElement).value).toBe('6')
    expect(errors).toEqual([])
  })

  it('切换分类保留已提交条件和预览上下文，并回到第一页', async () => {
    const original = { keyword: '平衡', categoryId: fixtures.categories[0]!.id, age: '6', playType: 'BALANCE', scene: 'INDOOR', sort: 'priceDesc', page: '3', preview: '1', role: 'guest', state: 'normal' }
    const page = await render(original)
    const selected = fixtures.categories[1]!
    await page.findAll('nav[aria-label="商品分类"] button').find(button => button.text().includes(selected.name))!.trigger('click')
    await settle()
    const { page: _page, ...expected } = original
    expect(router.currentRoute.value.query).toEqual({ ...expected, categoryId: selected.id })
    expect(lastProductQuery().get('page')).toBe('1')
    expect(lastProductQuery().get('categoryId')).toBe(selected.id)
    expect(page.get('nav[aria-label="商品分类"] button[aria-pressed="true"]').text()).toContain(selected.name)
    expect(page.get('.shop-toolbar h2').text()).toBe(selected.name)
  })

  it('翻页保留 query，边界按钮禁用，商品详情链接继续携带筛选', async () => {
    const original = { keyword: '游戏', categoryId: fixtures.categories[0]!.id, sort: 'priceAsc', preview: '1', role: 'user', state: 'normal', page: '1' }
    const page = await render(original)
    expect(page.findAll('.shop-tile')).toHaveLength(12)
    expect(page.findAll('.shop-tile')[0]!.text()).toContain(products[0]!.sku)
    expect(page.findAll('nav[aria-label="商品分页"] button')[0]!.attributes('disabled')).toBeDefined()
    await page.findAll('nav[aria-label="商品分页"] button')[1]!.trigger('click')
    await settle()
    expect(router.currentRoute.value.query).toEqual({ ...original, page: '2' })
    expect(lastProductQuery().get('page')).toBe('2')
    expect(page.findAll('.shop-tile')).toHaveLength(12)
    await page.findAll('nav[aria-label="商品分页"] button')[1]!.trigger('click')
    await settle()
    expect(page.findAll('.shop-tile')).toHaveLength(1)
    expect(page.findAll('nav[aria-label="商品分页"] button')[1]!.attributes('disabled')).toBeDefined()
    const detailUrl = new URL(page.get('.shop-tile').attributes('href')!, 'https://test.invalid')
    expect(Object.fromEntries(detailUrl.searchParams)).toEqual({ ...original, page: '3' })
  })

  it('搜索与清除保留预览参数，但所有商品 API 请求均只含业务参数', async () => {
    const context = { preview: '1', role: 'guest', state: 'normal' }
    const page = await render({ ...context, keyword: '旧关键词', categoryId: fixtures.categories[0]!.id, age: '6', page: '2' })
    await page.get('.shop-search input').setValue('  新搜索  ')
    await page.get('form[aria-label="商品筛选"]').trigger('submit')
    await settle()
    expect(router.currentRoute.value.query).toMatchObject({ ...context, keyword: '新搜索' })
    expect(router.currentRoute.value.query).not.toHaveProperty('page')
    await page.get('.shop-clear').trigger('click')
    await settle()
    expect(router.currentRoute.value.query).toEqual(context)
    expect(lastProductQuery().get('sort')).toBe('recommended')
    expect(lastProductQuery().get('keyword')).toBeNull()
    expect(lastProductQuery().get('categoryId')).toBeNull()
    for (const [path] of productCalls()) {
      const query = new URL(path, 'https://test.invalid').searchParams
      for (const key of ['preview', 'role', 'state']) expect(query.has(key)).toBe(false)
    }
  })

  it.each(['搜索', '清除'] as const)('同一 URL 再次%s仍主动重载，允许恢复失败请求', async (action) => {
    let calls = 0
    mockedApi.mockImplementation(async (path) => {
      if (path.startsWith('/products?') && ++calls === 1) throw new Error('首次读取失败')
      return responseFor(path)
    })
    const page = await render(action === '搜索' ? { keyword: '平衡', sort: 'recommended' } : {})
    expect(page.text()).toContain('暂时无法取得商品')
    const sameUrl = router.currentRoute.value.fullPath
    if (action === '搜索') await page.get('form[aria-label="商品筛选"]').trigger('submit')
    else await page.get('.shop-clear').trigger('click')
    await settle()
    expect(router.currentRoute.value.fullPath).toBe(sameUrl)
    expect(productCalls()).toHaveLength(2)
    expect(page.findAll('.shop-tile')).toHaveLength(12)
    expect(page.text()).not.toContain('暂时无法取得商品')
  })
})

describe('筛选参考数据和并发查询的恢复能力', () => {
  it.each(['/categories', '/product-options'])('%s部分失败保留另一份选项，重试后补齐', async (failedPath) => {
    let attempts = 0
    mockedApi.mockImplementation(async (path) => {
      if (path === failedPath && ++attempts === 1) throw new Error('参考数据暂时不可用')
      return responseFor(path)
    })
    const page = await render()
    expect(page.findAll('.shop-tile')).toHaveLength(12)
    expect(page.text()).toContain('部分筛选选项暂时无法加载')
    expect(page.findAll('nav[aria-label="商品分类"] button')).toHaveLength(failedPath === '/categories' ? 1 : fixtures.categories.length + 1)
    expect(page.findAll('.shop-filter-fields select option')).toHaveLength(failedPath === '/product-options' ? 2 : fixtures.productOptions.playTypes.length + fixtures.productOptions.scenes.length + 2)
    await page.get('.shop-reference-error button').trigger('click')
    await settle()
    expect(page.find('.shop-reference-error').exists()).toBe(false)
    expect(page.findAll('nav[aria-label="商品分类"] button')).toHaveLength(fixtures.categories.length + 1)
    expect(page.findAll('.shop-filter-fields select option')).toHaveLength(fixtures.productOptions.playTypes.length + fixtures.productOptions.scenes.length + 2)
    expect(productCalls()).toHaveLength(1)
  })

  it('较旧成功响应晚到，不覆盖新查询的商品与分页结果', async () => {
    const older = deferred<ApiEnvelope<ProductCard[]>>()
    const newer = deferred<ApiEnvelope<ProductCard[]>>()
    mockedApi.mockImplementation(async (path) => {
      const keyword = new URL(path, 'https://test.invalid').searchParams.get('keyword')
      if (keyword === '旧查询') return older.promise
      if (keyword === '新查询') return newer.promise
      return responseFor(path)
    })
    const page = await render()
    await page.get('.shop-search input').setValue('旧查询')
    await page.get('form[aria-label="商品筛选"]').trigger('submit')
    await settle()
    await page.get('.shop-search input').setValue('新查询')
    await page.get('form[aria-label="商品筛选"]').trigger('submit')
    await settle()
    newer.resolve({ data: [{ ...products[0]!, name: '新查询的结果' }], meta: { page: 1, pageSize: 12, totalItems: 1, totalPages: 1 } })
    await settle()
    expect(page.text()).toContain('新查询的结果')
    older.resolve({ data: [{ ...products[1]!, name: '过时的结果' }], meta: { page: 1, pageSize: 12, totalItems: 25, totalPages: 3 } })
    await settle()
    expect(page.text()).toContain('新查询的结果')
    expect(page.text()).not.toContain('过时的结果')
    expect(page.get('.shop-toolbar').text()).toContain('1 件商品')
    expect(page.find('nav[aria-label="商品分页"]').exists()).toBe(false)
  })

  it('旧请求失败不结束新请求的加载状态，也不显示过时错误', async () => {
    const older = deferred<ApiEnvelope<ProductCard[]>>()
    const newer = deferred<ApiEnvelope<ProductCard[]>>()
    mockedApi.mockImplementation(async (path) => {
      const keyword = new URL(path, 'https://test.invalid').searchParams.get('keyword')
      if (keyword === '旧查询') return older.promise
      if (keyword === '新查询') return newer.promise
      return responseFor(path)
    })
    const page = await render()
    await page.get('.shop-search input').setValue('旧查询')
    await page.get('form[aria-label="商品筛选"]').trigger('submit')
    await settle()
    await page.get('.shop-search input').setValue('新查询')
    await page.get('form[aria-label="商品筛选"]').trigger('submit')
    await settle()
    older.reject(new Error('旧查询失败'))
    await settle()
    expect(page.get('.shop-state[role="status"]').text()).toContain('正在整理商品')
    expect(page.find('.shop-state[role="alert"]').exists()).toBe(false)
    newer.resolve({ data: [{ ...products[0]!, name: '最终商品' }] })
    await settle()
    expect(page.text()).toContain('最终商品')
    expect(page.find('.shop-state').exists()).toBe(false)
    expect(page.get('.shop-toolbar').text()).toContain('1 件商品')
  })
})
