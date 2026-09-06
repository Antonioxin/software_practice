import { afterAll, afterEach, beforeAll, beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount, type VueWrapper } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { defineComponent, nextTick } from 'vue'
import { RouterView, type Router } from 'vue-router'
import type { PreviewState } from './preview'

let router: Router
let preview: typeof import('./preview')
let sample: typeof import('./fixtures')['fixtures']
let wrapper: VueWrapper | undefined
let applicationErrors: unknown[] = []
const network = vi.fn(() => { throw new Error('场景渲染不得访问真实后端') })
const rejectHandler = (event: PromiseRejectionEvent) => { applicationErrors.push(event.reason) }
const Root = defineComponent({ components: { RouterView }, template: '<RouterView />' })

beforeAll(async () => {
  // Configure the real HTTP module before importing any application/store modules.
  vi.stubEnv('DEV', true)
  window.history.replaceState(null, '', '/products?preview=1&role=guest')
  vi.stubGlobal('fetch', network)
  // jsdom cannot scroll; retain the real router's scroll behavior without pretending to test layout.
  vi.stubGlobal('scrollTo', vi.fn())
  vi.spyOn(console, 'error').mockImplementation((...errors: unknown[]) => { applicationErrors.push(errors) })
  preview = await import('./preview')
  sample = (await import('./fixtures')).fixtures
  router = (await import('../router')).router
  window.addEventListener('unhandledrejection', rejectHandler)
})

beforeEach(() => {
  applicationErrors = []
  network.mockClear()
  sessionStorage.clear()
})

afterEach(async () => {
  wrapper?.unmount()
  wrapper = undefined
  await flushPromises()
  document.body.innerHTML = ''
  expect(network).not.toHaveBeenCalled()
  expect(applicationErrors).toEqual([])
})

afterAll(() => {
  window.removeEventListener('unhandledrejection', rejectHandler)
  vi.restoreAllMocks()
  vi.unstubAllGlobals()
  vi.unstubAllEnvs()
})

async function renderScene(label: string, state: PreviewState = 'normal') {
  const scene = [...preview.previewScenes,
    { label: '首页', path: '/', role: 'guest' as const },
    { label: '404', path: '/missing-preview-page', role: 'guest' as const },
  ].find(item => item.label === label)!
  expect(scene, `场景注册缺失：${label}`).toBeDefined()
  preview.activePreview.role = scene.role
  preview.activePreview.state = state
  const pinia = createPinia()
  setActivePinia(pinia)
  await router.replace(scene.path)
  wrapper = mount(Root, {
    attachTo: document.body,
    global: {
      plugins: [pinia, router],
      config: { errorHandler: (error) => { applicationErrors.push(error) } },
    },
  })
  await router.isReady()
  // onMounted may load categories/options and only then load product detail.
  for (let iteration = 0; iteration < 4; iteration++) {
    await flushPromises()
    await nextTick()
  }
  expect(router.currentRoute.value.path).toBe(scene.path)
  expect(wrapper.find('main').exists()).toBe(true)
  return wrapper
}

const normalCases = [
  ['首页', 'Life is better'],
  ['404', '这个角落，还没有内容。'],
  ['商品目录', '探索商品'],
  ['商品详情', '平衡石 · 示例'],
  ['登录', '欢迎回来'],
  ['注册', '创建账户'],
  ['个人资料', '个人资料'],
  ['购物车', '购物车'],
  ['结算', '确认订单'],
  ['我的订单', '我的订单'],
  ['订单详情', 'WM-SAMPLE-20260905-001'],
  ['用户管理', '用户账户'],
  ['账户详情', '账户详情'],
  ['商品管理', '商品与库存'],
  ['新建商品', '新建商品草稿'],
  ['编辑商品', '编辑商品'],
  ['分类管理', '商品分类'],
  ['订单管理', '订单管理'],
  ['管理订单详情', '订单详情'],
] as const

describe('17 个业务路由及首页、404 的真实组件渲染', () => {
  it.each(normalCases)('%s：读取只读样例并显示关键内容', async (label, heading) => {
    const page = await renderScene(label)
    const headings = page.findAll('main h1, main h2').map(node => node.text()).join(' ')
    expect(headings).toContain(heading)
    expect(page.findAll('.error-summary, .field-error')).toHaveLength(0)

    if (label === '商品目录') {
      expect(page.findAll('.shop-tile')).toHaveLength(6)
      expect(page.text()).toContain(sample.products[0]!.name)
      expect(page.findAll('.shop-filter-fields select option').length).toBeGreaterThan(6)
      expect(page.findAll('nav[aria-label="商品分类"] button')).toHaveLength(sample.categories.length + 1)
      expect(page.get('nav[aria-label="商品分类"] button[aria-pressed="true"]').text()).toContain('全部商品')
    } else if (label === '商品详情') {
      expect(page.text()).toContain('加入购物车')
      expect(page.text()).toContain(sample.products[0]!.safetyNotes)
    } else if (label === '登录') {
      expect(page.get('#login-email').attributes('autocomplete')).toBe('username')
      expect(page.get('#login-password').attributes('type')).toBe('password')
    } else if (label === '注册') {
      expect(page.findAll('.consent-box input[type="checkbox"]')).toHaveLength(3)
      expect(page.text()).toContain(sample.registrationPolicy.termsVersion)
    } else if (label === '个人资料') {
      expect((page.get('#profile-nickname').element as HTMLInputElement).value).toBe(sample.actors.user.nickname)
      expect(page.text()).toContain(sample.actors.user.email)
    } else if (label === '购物车') {
      expect(page.findAll('.commerce-cart-row')).toHaveLength(sample.cart.items.length)
      expect(page.get('a[href="/checkout"]').text()).toContain('前往结算')
    } else if (label === '结算') {
      expect(page.findAll('.commerce-summary-item')).toHaveLength(sample.checkoutPreview.items.length)
      expect(page.find('[data-field="recipient"]').exists()).toBe(true)
      expect(page.find('.commerce-expiry--expired').exists()).toBe(false)
    } else if (label === '我的订单') {
      expect(page.findAll('.commerce-order-row')).toHaveLength(sample.orders.length)
    } else if (label === '订单详情') {
      expect(page.findAll('.commerce-detail-item')).toHaveLength(sample.orders[0]!.items.length)
      expect(page.text()).toContain('模拟付款')
      expect(page.text()).toContain(sample.orders[0]!.shippingAddress.recipient)
    } else if (label === '用户管理') {
      expect(page.findAll('tbody tr')).toHaveLength(sample.users.length)
      expect(page.text()).toContain('已停用')
    } else if (label === '账户详情') {
      expect(page.text()).toContain(sample.actors.user.email)
      expect(page.text()).toContain('停用账户')
    } else if (label === '商品管理') {
      expect(page.findAll('.catalog-admin-table tbody tr')).toHaveLength(sample.adminProducts.length)
      expect(page.text()).toContain('调库存')
    } else if (label === '新建商品' || label === '编辑商品') {
      expect(page.findAll('select option').length).toBeGreaterThan(sample.categories.length)
      const sku = page.get('input[placeholder="WM-EXAMPLE-001"]').element as HTMLInputElement
      expect(sku.value).toBe(label === '编辑商品' ? sample.adminProducts[0]!.sku : '')
      expect(sku.disabled).toBe(label === '编辑商品')
      expect(page.text()).toContain('保存商品资料')
    } else if (label === '分类管理') {
      expect(page.findAll('article.category-card')).toHaveLength(sample.categories.length)
      expect(page.text()).toContain(sample.categories[0]!.name)
    } else if (label === '订单管理') {
      expect(page.findAll('.commerce-admin-table tbody tr')).toHaveLength(sample.orders.length)
    } else if (label === '管理订单详情') {
      const paid = sample.orders.find(order => order.status === 'PAID')!
      expect(page.text()).toContain(paid.orderNumber)
      expect(page.text()).toContain('模拟整单发货')
      expect(page.text()).toContain(paid.paymentAttempts[0]!.simulationReference)
    }
  })
})

describe('真实页面的空数据和读取失败', () => {
  it('空商品目录显示恢复筛选入口而不显示产品卡片', async () => {
    const page = await renderScene('商品目录', 'empty')
    expect(page.text()).toContain('未找到符合条件的商品')
    expect(page.findAll('.shop-tile')).toHaveLength(0)
    expect(page.text()).toContain('查看全部商品')
  })

  it('空购物车显示选购入口并且不能前往结算', async () => {
    const page = await renderScene('购物车', 'empty')
    expect(page.text()).toContain('购物车还是空的')
    expect(page.find('a[href="/checkout"]').exists()).toBe(false)
  })

  it('用户列表失败仍保留管理员布局与重试操作', async () => {
    const page = await renderScene('用户管理', 'error')
    expect(page.get('h1').text()).toBe('用户账户')
    expect(page.text()).toContain(sample.actors.admin.nickname)
    expect(page.text()).toContain('加载失败')
    expect(page.text()).toContain('重试')
    expect(page.findAll('tbody tr')).toHaveLength(0)
  })

  it('订单详情失败显示错误反馈，不保留虚假的成交快照', async () => {
    const page = await renderScene('管理订单详情', 'error')
    expect(page.text()).toContain('无法打开订单')
    expect(page.find('[role="alert"]').exists()).toBe(true)
    expect(page.findAll('.commerce-detail-item')).toHaveLength(0)
  })
})
