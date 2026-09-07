import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount, type VueWrapper } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { createMemoryHistory, createRouter, type Router } from 'vue-router'
import ProductDetailPage from './ProductDetailPage.vue'
import ProductArtwork from '../components/ProductArtwork.vue'
import { api } from '../services/http'
import { useSessionStore } from '../stores/session'
import { fixtures } from '../dev/fixtures'
import type { ApiEnvelope, PublicProduct } from '../types'

vi.mock('../services/http', async (original) => ({
  ...(await original<typeof import('../services/http')>()),
  api: vi.fn(),
}))

const mockedApi = vi.mocked(api)
const forbiddenFetch = vi.fn(() => { throw new Error('商品详情组件测试不得访问真实后端') })
const imageCases = [
  ['WM-BALANCE-STONES', 'balance-stones'],
  ['WM-RAINBOW-ARCH', 'rainbow-arch'],
  ['WM-RING-TOSS', 'ring-toss'],
  ['WM-TEAM-BOARD', 'team-board'],
  ['WM-FOREST-KIT', 'forest-kit'],
  ['WM-SKIP-ROPE', 'skip-rope'],
] as const
let records: Map<string, PublicProduct>
let wrapper: VueWrapper | undefined
let router: Router
let applicationErrors: unknown[]

function responseFor(path: string): ApiEnvelope<PublicProduct> {
  const match = /^\/products\/([^/?]+)$/.exec(path)
  const product = match && records.get(match[1]!)
  if (!product) throw new Error(`未预期的接口：${path}`)
  return { data: product }
}
function product(sku: string) { return fixtures.products.find(item => item.sku === sku)! }
function deferred<T>() {
  let resolve!: (value: T) => void
  let reject!: (reason: Error) => void
  const promise = new Promise<T>((yes, no) => { resolve = yes; reject = no })
  return { promise, resolve, reject }
}
async function settle() { await flushPromises(); await flushPromises() }
async function render(id = fixtures.products[0]!.id) {
  const pinia = createPinia()
  setActivePinia(pinia)
  useSessionStore().replace({ ...fixtures.actors.user })
  router = createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/products/:id', component: { template: '<div />' } },
      { path: '/:pathMatch(.*)*', component: { template: '<div />' } },
    ],
  })
  await router.push(`/products/${id}`)
  // Mount the real page once: param changes must update this same component instance.
  wrapper = mount(ProductDetailPage, {
    attachTo: document.body,
    global: { plugins: [pinia, router], config: { errorHandler: error => { applicationErrors.push(error) } } },
  })
  await router.isReady()
  await settle()
  return wrapper
}
function detailsCalls() { return mockedApi.mock.calls.filter(([path]) => path.startsWith('/products/')) }

beforeEach(() => {
  applicationErrors = []
  records = new Map(fixtures.products.map(item => [item.id, { ...item }]))
  mockedApi.mockReset()
  mockedApi.mockImplementation(async path => responseFor(path))
  forbiddenFetch.mockClear()
  sessionStorage.clear()
  document.title = 'WEMOVE'
  vi.stubGlobal('fetch', forbiddenFetch)
})
afterEach(async () => {
  wrapper?.unmount()
  wrapper = undefined
  await settle()
  document.body.innerHTML = ''
  sessionStorage.clear()
  expect(forbiddenFetch).not.toHaveBeenCalled()
  expect(applicationErrors).toEqual([])
  vi.unstubAllGlobals()
})

describe('商品详情的对应示意图与真实使用正文', () => {
  it.each(imageCases)('%s 复用对应主图、专属玩法图和三个步骤', async (sku, filename) => {
    const expected = product(sku)
    const page = await render(expected.id)
    expect(page.get('.product-intro h1').text()).toBe(expected.name)
    expect(page.get('.product-main-image').attributes('src')).toBe(`/assets/products/${filename}.png`)
    expect(page.get('.product-main-image').attributes('alt')).toContain(expected.name)
    expect(page.get('.product-photo-name').text()).toBe(expected.name)
    expect(page.findComponent(ProductArtwork).exists()).toBe(false)
    expect(page.get('.product-guide-image').attributes('src')).toBe(`/assets/products/guides/${filename}-guide.png`)
    expect(page.get('.product-guide-image').attributes('alt')).toBeTruthy()
    const steps = page.findAll('.product-guide-steps li')
    expect(steps).toHaveLength(3)
    expect(new Set(steps.map(step => step.text())).size).toBe(3)
    for (const step of steps) {
      expect(step.get('h4').text()).not.toBe('')
      expect(step.get('p').text()).not.toBe('')
    }
    expect(page.text()).toContain(expected.instructions)
    expect(page.get('.product-safety').text()).toContain(expected.safetyNotes)
    expect(detailsCalls()).toHaveLength(1)
  })

  it('未知 SKU 即使名称与已知商品一致，也只展示后端正文而不猜测图解', async () => {
    const source = fixtures.products[0]!
    const backendInstructions = '后端专属操作说明：先检查固定件，再在指定区域试用。'
    const backendDescription = '后端专属介绍：这是一款尚未配置本地图解的新商品。'
    records.set(source.id, { ...source, sku: 'WM-NEW-UNMAPPED', instructions: backendInstructions, description: backendDescription })
    const page = await render(source.id)
    expect(page.find('.product-main-image').exists()).toBe(false)
    expect(page.findComponent(ProductArtwork).exists()).toBe(true)
    expect(page.find('.product-guide-image').exists()).toBe(false)
    expect(page.findAll('.product-guide-steps li')).toHaveLength(0)
    expect(page.get('.product-guide').text()).toContain(backendInstructions)
    expect(page.text()).toContain(backendDescription)
    expect(page.text()).toContain('WM-NEW-UNMAPPED')
    expect(detailsCalls()).toHaveLength(1)
  })

  it('主图加载失败回退手绘占位，玩法图失败仍保留三个步骤和后端说明', async () => {
    const source = fixtures.products[0]!
    const page = await render(source.id)
    const stepsBefore = page.findAll('.product-guide-steps li').map(step => step.text())
    const requestsBefore = mockedApi.mock.calls.length
    await page.get('.product-main-image').trigger('error')
    await settle()
    expect(page.find('.product-main-image').exists()).toBe(false)
    expect(page.findComponent(ProductArtwork).exists()).toBe(true)
    expect(page.get('.product-intro h1').text()).toBe(source.name)
    expect(page.find('.product-guide-image').exists()).toBe(true)
    await page.get('.product-guide-image').trigger('error')
    await settle()
    expect(page.find('.product-guide-image').exists()).toBe(false)
    expect(page.findAll('.product-guide-steps li').map(step => step.text())).toEqual(stepsBefore)
    expect(page.get('.product-guide').text()).toContain(source.instructions)
    expect(mockedApi.mock.calls).toHaveLength(requestsBefore)
  })
})

describe('详情路由复用与请求竞态', () => {
  it('切换 ID 重新取数、重置数量和图片失败状态，不复用上一个商品', async () => {
    const first = fixtures.products[0]!
    const second = fixtures.products[1]!
    const pending = deferred<ApiEnvelope<PublicProduct>>()
    mockedApi.mockImplementation(async path => path === `/products/${second.id}` ? pending.promise : responseFor(path))
    const page = await render(first.id)
    await page.get('.purchase-panel input[type="number"]').setValue('7')
    await page.get('.product-main-image').trigger('error')
    await page.get('.product-guide-image').trigger('error')
    await router.push(`/products/${second.id}`)
    await settle()
    expect(page.text()).toContain('正在加载商品详情')
    expect(page.find('.product-intro').exists()).toBe(false)
    expect(page.find('.product-guide').exists()).toBe(false)
    pending.resolve({ data: second })
    await settle()
    expect(page.get('.product-intro h1').text()).toBe(second.name)
    expect((page.get('.purchase-panel input[type="number"]').element as HTMLInputElement).value).toBe('1')
    expect(page.get('.product-main-image').attributes('src')).toBe('/assets/products/rainbow-arch.png')
    expect(page.get('.product-guide-image').attributes('src')).toBe('/assets/products/guides/rainbow-arch-guide.png')
    expect(page.findAll('.product-guide-steps li')).toHaveLength(3)
    expect(detailsCalls().map(([path]) => path)).toEqual([`/products/${first.id}`, `/products/${second.id}`])
    expect(document.title).toBe(`${second.name} · WEMOVE`)
  })

  it('较旧 ID 成功响应晚到，不覆盖新商品、图解或浏览器标题', async () => {
    const olderProduct = fixtures.products[1]!
    const newerProduct = fixtures.products[2]!
    const older = deferred<ApiEnvelope<PublicProduct>>()
    const newer = deferred<ApiEnvelope<PublicProduct>>()
    mockedApi.mockImplementation(async path => {
      if (path === `/products/${olderProduct.id}`) return older.promise
      if (path === `/products/${newerProduct.id}`) return newer.promise
      return responseFor(path)
    })
    const page = await render()
    await router.push(`/products/${olderProduct.id}`)
    await settle()
    await router.push(`/products/${newerProduct.id}`)
    await settle()
    newer.resolve({ data: newerProduct })
    await settle()
    expect(page.get('.product-intro h1').text()).toBe(newerProduct.name)
    older.resolve({ data: olderProduct })
    await settle()
    expect(page.get('.product-intro h1').text()).toBe(newerProduct.name)
    expect(page.get('.product-main-image').attributes('src')).toBe('/assets/products/ring-toss.png')
    expect(page.get('.product-guide-image').attributes('src')).toBe('/assets/products/guides/ring-toss-guide.png')
    expect(document.title).toBe(`${newerProduct.name} · WEMOVE`)
    expect(page.find('.state-panel[role="alert"]').exists()).toBe(false)
  })

  it('旧 ID 请求失败不终止最新请求的加载状态，也不展示过时错误', async () => {
    const olderProduct = fixtures.products[1]!
    const newerProduct = fixtures.products[2]!
    const older = deferred<ApiEnvelope<PublicProduct>>()
    const newer = deferred<ApiEnvelope<PublicProduct>>()
    mockedApi.mockImplementation(async path => {
      if (path === `/products/${olderProduct.id}`) return older.promise
      if (path === `/products/${newerProduct.id}`) return newer.promise
      return responseFor(path)
    })
    const page = await render()
    await router.push(`/products/${olderProduct.id}`)
    await settle()
    await router.push(`/products/${newerProduct.id}`)
    await settle()
    older.reject(new Error('旧商品读取失败'))
    await settle()
    expect(page.text()).toContain('正在加载商品详情')
    expect(page.find('.product-intro').exists()).toBe(false)
    expect(page.find('.state-panel[role="alert"]').exists()).toBe(false)
    newer.resolve({ data: newerProduct })
    await settle()
    expect(page.get('.product-intro h1').text()).toBe(newerProduct.name)
    expect(page.text()).not.toContain('旧商品读取失败')
    expect(page.find('.product-guide-image').exists()).toBe(true)
  })
})

describe('下架商品的内容与购买限制', () => {
  it.each([false, true])('UNLISTED 不显示完整使用信息，并阻止购买（purchasable=%s）', async (purchasable) => {
    const source = fixtures.products[0]!
    const restricted = {
      ...source, status: 'UNLISTED' as const, purchasable,
      description: '下架后不应展示的详细介绍', instructions: '下架后不应展示的详细步骤',
      material: '下架后不应展示的详细材质', safetyNotes: '下架后不应展示的安全正文',
    }
    records.set(source.id, restricted)
    const page = await render(source.id)
    expect(page.get('.product-intro h1').text()).toBe(source.name)
    expect(page.text()).toContain('该商品已下架')
    expect(page.find('.product-guide').exists()).toBe(false)
    expect(page.find('.product-specifications').exists()).toBe(false)
    expect(page.find('.product-safety').exists()).toBe(false)
    for (const privateDetail of [restricted.description, restricted.instructions, restricted.material, restricted.safetyNotes]) {
      expect(page.text()).not.toContain(privateDetail)
    }
    const buyButton = page.get('.purchase-panel .primary-button')
    expect(buyButton.attributes('disabled')).toBeDefined()
    await buyButton.trigger('click')
    await settle()
    expect(mockedApi.mock.calls.filter(([, options]) => options?.method === 'POST')).toHaveLength(0)
    expect(router.currentRoute.value.path).toBe(`/products/${source.id}`)
  })
})
