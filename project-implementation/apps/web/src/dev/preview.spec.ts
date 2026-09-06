import { afterEach, describe, expect, it, vi } from 'vitest'
import type { Actor, ProductCard, PublicProduct } from '../types'
import type { Cart, Detail, OrderPage, Preview } from '../features/commerce/types'
import { fixtures } from './fixtures'
import { handlePreviewRequest, previewContext, previewScenes, type PreviewContext } from './preview'

const user: PreviewContext = { role: 'user', state: 'normal' }
const admin: PreviewContext = { role: 'admin', state: 'normal' }

afterEach(() => {
  window.history.replaceState(null, '', '/')
  vi.unstubAllGlobals()
  vi.unstubAllEnvs()
})

describe('开发预览的响应契约', () => {
  it('按路径选择默认身份，保留显式身份与数据状态，并覆盖 17 个业务场景', () => {
    expect(previewContext({ pathname: '/admin/products', search: '?preview=1' })).toEqual(admin)
    expect(previewContext({ pathname: '/checkout', search: '?preview=1' })).toEqual(user)
    expect(previewContext({ pathname: '/login', search: '?preview=1' })).toEqual({ role: 'guest', state: 'normal' })
    expect(previewContext({ pathname: '/products', search: '?role=admin&state=error' })).toEqual({ role: 'admin', state: 'error' })
    expect(previewScenes).toHaveLength(17)
    expect(new Set(previewScenes.map(scene => scene.path)).size).toBe(17)
  })

  it('保留目录 data + meta 与订单 data 内分页的不同接口结构', () => {
    const catalog = handlePreviewRequest<ProductCard[]>('/products?page=2&pageSize=2&sort=priceAsc', {}, user)
    expect(catalog.data).toHaveLength(2)
    expect(catalog.meta).toEqual({ page: 2, pageSize: 2, totalItems: 6, totalPages: 3 })
    expect(catalog.data[0]!.retailUnitPriceFen).toBeLessThan(catalog.data[1]!.retailUnitPriceFen)
    const orders = handlePreviewRequest<OrderPage>('/orders?page=1&pageSize=2', {}, user)
    expect(orders.meta).toBeUndefined()
    expect(orders.data).toMatchObject({ page: 1, pageSize: 2, total: 4 })
    expect(orders.data.items).toHaveLength(2)
  })

  it('公开商品不包含经销价格，筛选包含 BOTH 场景且数据不会被调用方改写', () => {
    const result = handlePreviewRequest<PublicProduct[]>('/products?scene=INDOOR', {}, user)
    expect(result.data.every(item => item.scene === 'INDOOR' || item.scene === 'BOTH')).toBe(true)
    expect(result.data[0]).not.toHaveProperty('dealerReferenceUnitPriceFen')
    expect(result.data[0]).not.toHaveProperty('stock')
    result.data[0]!.name = 'mutated'
    expect(handlePreviewRequest<PublicProduct>(`/products/${result.data[0]!.id}`, {}, user).data.name).not.toBe('mutated')
  })

  it('报价金额等于商品小计且每次报价拥有新的 15 分钟有效期', () => {
    const cart = handlePreviewRequest<Cart>('/cart', {}, user).data
    expect(cart.totalFen).toBe(cart.items.reduce((sum, item) => sum + item.quantity * item.unitPriceFen, 0))
    const before = Date.now()
    const preview = handlePreviewRequest<Preview>('/checkout-previews', { method: 'POST' }, user).data
    expect(preview.totalFen).toBe(cart.totalFen)
    expect(preview.cartVersion).toBe(cart.cartVersion)
    expect(Date.parse(preview.expiresAt)).toBeGreaterThanOrEqual(before + 15 * 60 * 1000)
    for (const order of fixtures.orders) {
      expect(order.totalFen).toBe(order.items.reduce((sum, item) => sum + item.subtotalFen, 0))
      expect(order.totalFen).toBe(order.subtotalFen + order.shippingFen + order.taxFen - order.discountFen)
    }
  })

  it('按角色保留发货和付款边界，并返回完整的订单详情集合', () => {
    const paid = fixtures.orders.find(order => order.status === 'PAID')!
    const own = handlePreviewRequest<Detail>(`/orders/${paid.id}`, {}, user).data
    const managed = handlePreviewRequest<Detail>(`/admin/orders/${paid.id}`, {}, admin).data
    expect(own.allowedActions).toEqual(['CANCEL'])
    expect(managed.allowedActions).toEqual(['CANCEL', 'MOCK_SHIPMENT'])
    expect(Array.isArray(managed.paymentAttempts)).toBe(true)
    expect(Array.isArray(managed.refunds)).toBe(true)
    expect(Array.isArray(managed.history)).toBe(true)
    expect(() => handlePreviewRequest('/admin/orders', {}, user)).toThrow(expect.objectContaining({ problem: expect.objectContaining({ status: 403 }) }))
  })

  it('空数据和读取失败均保留身份，不伪装成正常数据', () => {
    expect(handlePreviewRequest<Actor>('/auth/me', {}, { ...user, state: 'error' }).data.baseRole).toBe('USER')
    expect(handlePreviewRequest<Cart>('/cart', {}, { ...user, state: 'empty' }).data).toMatchObject({ items: [], totalFen: 0, canCheckout: false })
    expect(() => handlePreviewRequest('/orders', {}, { ...user, state: 'error' })).toThrow(expect.objectContaining({ problem: expect.objectContaining({ status: 503 }) }))
    expect(() => handlePreviewRequest(`/products/${fixtures.products[0]!.id}`, {}, { ...user, state: 'empty' })).toThrow(expect.objectContaining({ problem: expect.objectContaining({ status: 404 }) }))
  })
})

describe('开发预览的网络隔离', () => {
  it('在 CSRF 与 fetch 之前拒绝所有业务写入和未知读取，仅允许本地结算试算', async () => {
    vi.resetModules()
    vi.stubEnv('DEV', true)
    window.history.replaceState(null, '', '/checkout?preview=1&role=user')
    const fetchSpy = vi.fn()
    vi.stubGlobal('fetch', fetchSpy)
    const { api, ApiProblem } = await import('../services/http')
    await expect(api('/auth/me')).resolves.toMatchObject({ data: { baseRole: 'USER' } })
    await expect(api('/orders', { method: 'POST', body: '{}' })).rejects.toBeInstanceOf(ApiProblem)
    for (const [path, method] of [['/orders', 'POST'], ['/auth/login', 'POST'], ['/auth/register', 'POST'], ['/auth/logout', 'POST'], ['/account/profile', 'PATCH'], ['/cart/items/1', 'DELETE'], ['/admin/products', 'POST'], ['/unknown', 'PUT']]) {
      await expect(api(path!, { method })).rejects.toMatchObject({ problem: { status: 403, code: 'PREVIEW_READ_ONLY' } })
    }
    await expect(api('/unknown')).rejects.toMatchObject({ problem: { status: 404, code: 'PREVIEW_ENDPOINT_MISSING' } })
    await expect(api('/checkout-previews', { method: 'POST' })).resolves.toMatchObject({ data: { totalFen: 59700 } })
    // Catalog filtering replaces the query; it must not silently return to the real backend.
    window.history.replaceState(null, '', '/products?keyword=平衡')
    await expect(api('/products')).resolves.toHaveProperty('data')
    expect(fetchSpy).not.toHaveBeenCalled()
  })

  it('生产环境忽略 preview 参数，正常请求仍使用原始 API 管道', async () => {
    vi.resetModules()
    vi.stubEnv('DEV', false)
    window.history.replaceState(null, '', '/products?preview=1&role=admin')
    const fetchSpy = vi.fn().mockResolvedValue({ ok: true, status: 200, headers: new Headers(), json: async () => ({ data: { source: 'test-server' } }) })
    vi.stubGlobal('fetch', fetchSpy)
    const { api, isDevelopmentPreview } = await import('../services/http')
    expect(isDevelopmentPreview).toBe(false)
    await expect(api('/products')).resolves.toEqual({ data: { source: 'test-server' } })
    expect(fetchSpy).toHaveBeenCalledExactlyOnceWith('/api/v1/products', expect.objectContaining({ method: 'GET', credentials: 'include' }))
  })
})
