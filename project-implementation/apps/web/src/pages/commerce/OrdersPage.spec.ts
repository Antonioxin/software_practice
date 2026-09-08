import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount, type VueWrapper } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { createMemoryHistory, createRouter } from 'vue-router'
import OrdersPage from './OrdersPage.vue'
import { readOrders } from '../../features/commerce/api'
import { fixtures } from '../../dev/fixtures'
import { useSessionStore } from '../../stores/session'
import type { Order } from '../../features/commerce/types'

vi.mock('../../features/commerce/api', () => ({ readOrders: vi.fn() }))
const mockedReadOrders = vi.mocked(readOrders)
let wrapper: VueWrapper | undefined

function summary(overrides: Partial<Order> = {}): Order {
  return { ...fixtures.orderSummaries[0]!, ...overrides }
}

async function render(orders: Order[], admin = false) {
  mockedReadOrders.mockResolvedValue({ items: orders, page: 1, pageSize: 20, total: orders.length })
  const pinia = createPinia()
  setActivePinia(pinia)
  useSessionStore().replace({ ...fixtures.actors[admin ? 'admin' : 'user'] })
  const router = createRouter({ history: createMemoryHistory(), routes: [{ path: '/:pathMatch(.*)*', component: { template: '<div />' } }] })
  await router.push(admin ? '/admin/orders' : '/account/orders')
  wrapper = mount(OrdersPage, { props: { admin }, global: { plugins: [pinia, router] } })
  await flushPromises()
  return wrapper
}

beforeEach(() => { mockedReadOrders.mockReset() })
afterEach(() => { wrapper?.unmount(); wrapper = undefined })

describe('订单列表中的商品摘要', () => {
  it('保留订单快照，仅在显示时去除名称的示例后缀，不额外读取商品或订单详情', async () => {
    const order = summary({ items: [
      { productId: 'saved-1', name: '下单时的平衡石名称 · 示例', sku: 'WM-BALANCE-STONES', quantity: 2 },
      { productId: 'saved-2', name: '下单时的拱桥名称', sku: 'WM-RAINBOW-ARCH', quantity: 3 },
    ] })
    const page = await render([order])
    const row = page.get('.commerce-order-row')
    expect(row.get('.order-product h2').text()).toBe('下单时的平衡石名称')
    expect(row.get('.order-product img').attributes('alt')).not.toContain('示例')
    expect(order.items[0]!.name).toBe('下单时的平衡石名称 · 示例')
    expect(row.get('.order-product [aria-label="数量 2"]').text()).toMatch(/×\s*2/)
    expect(row.get('.order-product [aria-label="数量 3"]').text()).toMatch(/×\s*3/)
    expect(row.text()).toContain('共 5 件商品')
    expect(row.findAll('.order-product img').map(image => image.attributes('src'))).toEqual([
      '/assets/products/balance-stones.png', '/assets/products/rainbow-arch.png',
    ])
    expect(row.get('.secondary-button').attributes('href')).toBe(`/account/orders/${order.id}`)
    expect(mockedReadOrders).toHaveBeenCalledExactlyOnceWith('page=1&pageSize=20', false)
  })

  it('以商品名称作为卡片内的标题，订单号保留为次要信息', async () => {
    const order = summary({
      orderNumber: 'WM-SECONDARY-ORDER-NUMBER',
      totalFen: 77700,
      items: [{ productId: 'balance', name: '平衡石套装', sku: 'WM-BALANCE-STONES', quantity: 3 }],
    })
    const page = await render([order])
    const row = page.get('.commerce-order-row')
    const header = row.get('header')
    const headings = row.findAll('h1, h2, h3, h4, h5, h6')

    expect(header.text()).not.toContain(order.orderNumber)
    expect(header.text()).toContain('待付款')
    expect(headings.map(heading => heading.text())).toContain('平衡石套装')
    expect(headings.every(heading => !heading.text().includes(order.orderNumber))).toBe(true)
    expect(row.text()).toContain(order.orderNumber)
    expect(row.get('.commerce-order-total').text()).toContain('777.00')
    expect(row.get('.secondary-button').attributes('href')).toBe(`/account/orders/${order.id}`)
  })

  it('多商品订单只展开两种，仍统计全部商品数量并提示其余商品', async () => {
    const items = [1, 2, 3].map(index => ({ productId: String(index), sku: 'WM-FOREST-KIT', name: `商品 ${index}`, quantity: index }))
    const page = await render([summary({ items })])
    expect(page.findAll('.order-product')).toHaveLength(2)
    expect(page.get('.order-more-products').text()).toContain('另有 1 种商品')
    expect(page.text()).toContain('共 6 件商品')
  })

  it('未知SKU仍显示历史名称和数量，不借用其他商品图片', async () => {
    const page = await render([summary({ items: [{ productId: 'old', name: '旧款商品', sku: 'ARCHIVED-SKU', quantity: 1 }] })])
    expect(page.get('.order-product').text()).toContain('旧款商品')
    expect(page.get('.order-product [aria-label="数量 1"]').text()).toMatch(/×\s*1/)
    expect(page.find('.order-product .product-thumbnail__image').exists()).toBe(false)
    expect(page.get('.product-thumbnail__placeholder').attributes('aria-label')).toBe('旧款商品，暂无商品图片')
  })

  it('筛选状态重新读取摘要并保持详情入口', async () => {
    const page = await render([summary()])
    mockedReadOrders.mockResolvedValue({ items: [], page: 1, pageSize: 20, total: 0 })
    await page.get('.commerce-filter-control').setValue('PAID')
    await page.get('.commerce-filter-panel').trigger('submit')
    await flushPromises()
    expect(mockedReadOrders).toHaveBeenLastCalledWith('page=1&pageSize=20&status=PAID', false)
    expect(page.findAll('.commerce-order-row')).toHaveLength(0)
    expect(page.text()).toContain('暂无符合条件的订单')
  })

  it('管理端保留订单表格，使用管理员列表接口', async () => {
    const page = await render([summary()], true)
    expect(page.find('.commerce-admin-table').exists()).toBe(true)
    expect(page.find('.order-products').exists()).toBe(false)
    expect(mockedReadOrders).toHaveBeenCalledExactlyOnceWith('page=1&pageSize=20', true)
  })
})
