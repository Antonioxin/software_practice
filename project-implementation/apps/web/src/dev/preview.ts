import type { ApiEnvelope } from '../types'
import { ApiProblem } from '../services/http'
import { fixtures } from './fixtures'

export type PreviewRole = 'guest' | 'user' | 'admin'
export type PreviewState = 'normal' | 'empty' | 'error'
export interface PreviewContext { role: PreviewRole; state: PreviewState }

export function previewContext(location: Pick<Location, 'pathname' | 'search'>): PreviewContext {
  const query = new URLSearchParams(location.search)
  const defaultRole = location.pathname.startsWith('/admin/') ? 'admin'
    : location.pathname.startsWith('/account/') || ['/cart', '/checkout'].includes(location.pathname) ? 'user' : 'guest'
  const role = query.get('role')
  const state = query.get('state')
  return {
    role: role === 'guest' || role === 'user' || role === 'admin' ? role : defaultRole,
    state: state === 'empty' || state === 'error' ? state : 'normal',
  }
}

// Capture once: Vue navigation may replace search filters without keeping preview=1.
export const activePreview = previewContext(window.location)

function problem(status: number, code: string, detail: string): never {
  throw new ApiProblem({ type: 'about:blank', title: 'Development preview', status, code, detail })
}

function requireRole(context: PreviewContext, role: 'user' | 'admin') {
  if (context.role !== role) problem(403, 'PREVIEW_ROLE_REQUIRED', `此示例需要${role === 'admin' ? '管理员' : '普通用户'}身份，请使用底部预览工具切换。`)
}

function clone<T>(data: T): T { return structuredClone(data) }
function includes(value: string | null | undefined, filter: string | null) {
  return !filter || (value ?? '').toLocaleLowerCase().includes(filter.toLocaleLowerCase())
}
function pageValues(query: URLSearchParams, defaultSize: number) {
  const number = (key: string, fallback: number) => {
    const parsed = Number(query.get(key))
    return Number.isInteger(parsed) && parsed > 0 ? parsed : fallback
  }
  return { page: number('page', 1), pageSize: Math.min(number('pageSize', defaultSize), 100) }
}
function paginated<T>(items: T[], query: URLSearchParams, defaultSize: number) {
  const { page, pageSize } = pageValues(query, defaultSize)
  return {
    data: items.slice((page - 1) * pageSize, page * pageSize),
    meta: { page, pageSize, totalItems: items.length, totalPages: Math.ceil(items.length / pageSize) },
  }
}

/** A closed set of local responses. This function never performs fetch or falls back to the API. */
export function handlePreviewRequest<T>(
  path: string,
  init: RequestInit = {},
  context: PreviewContext = activePreview,
): ApiEnvelope<T> {
  const method = (init.method ?? 'GET').toUpperCase()
  const url = new URL(path, 'https://preview.wemove.invalid')
  if (!path.startsWith('/') || path.startsWith('//') || url.origin !== 'https://preview.wemove.invalid') {
    problem(404, 'PREVIEW_ENDPOINT_MISSING', '此接口没有只读展示样例。')
  }
  const endpoint = url.pathname
  const query = url.searchParams
  const trial = method === 'POST' && endpoint === '/checkout-previews'
  if (method !== 'GET' && !trial) {
    problem(403, 'PREVIEW_READ_ONLY', '当前为只读界面预览，未发送请求或保存数据。请退出预览后连接测试后端。')
  }

  // Keep the selected identity available when exercising empty/error page states.
  if (endpoint === '/auth/me' && method === 'GET') {
    if (context.role === 'guest') problem(401, 'AUTH_REQUIRED', '当前为游客预览。')
    return { data: clone(fixtures.actors[context.role]) as T }
  }
  if (endpoint.startsWith('/admin/')) requireRole(context, 'admin')
  if (endpoint === '/cart' || endpoint === '/checkout-previews' || endpoint === '/orders' || endpoint.startsWith('/orders/')) {
    requireRole(context, 'user')
  }

  const empty = context.state === 'empty'
  const envelope = <D>(data: D): ApiEnvelope<T> => {
    if (context.state === 'error') problem(503, 'PREVIEW_SERVICE_UNAVAILABLE', '示例：服务暂时不可用。可切换为「正常」状态查看完整界面。')
    return { data: clone(data) as unknown as T }
  }
  const list = <D>(items: D[], defaultSize: number): ApiEnvelope<T> => {
    const page = paginated(empty ? [] : items, query, defaultSize)
    return { ...envelope(page.data), meta: page.meta }
  }
  const detail = <D>(item: D | undefined): ApiEnvelope<T> => {
    if (context.state === 'error') return envelope(item)
    if (empty || !item) problem(404, 'PREVIEW_RECORD_MISSING', '示例：未找到此记录，请从列表选择其他内容。')
    return envelope(item)
  }

  if (endpoint === '/auth/registration-policy') return envelope(fixtures.registrationPolicy)
  if (endpoint === '/categories' || endpoint === '/admin/categories') return envelope(empty ? [] : fixtures.categories)
  if (endpoint === '/product-options') return envelope(fixtures.productOptions)
  if (endpoint === '/admin/users') {
    return list(fixtures.users.filter(user => includes(user.email, query.get('email')) && includes(user.nickname, query.get('nickname'))
      && (!query.get('baseRole') || user.baseRole === query.get('baseRole'))
      && (!query.get('status') || user.accountStatus === query.get('status'))), 20)
  }
  const userMatch = endpoint.match(/^\/admin\/users\/([^/]+)$/)
  if (userMatch) return detail(fixtures.userDetails[userMatch[1]!])

  if (endpoint === '/products') {
    const age = query.get('age')
    const products = fixtures.products.filter(product =>
      (includes(product.name, query.get('keyword')) || includes(product.sku, query.get('keyword')))
      && (!query.get('categoryId') || product.category.id === query.get('categoryId'))
      && (age === null || age === '' || (product.ageMin <= Number(age) && (product.ageMax == null || product.ageMax >= Number(age))))
      && (!query.get('playType') || product.playType === query.get('playType'))
      && (!query.get('scene') || product.scene === query.get('scene') || product.scene === 'BOTH'),
    )
    if (query.get('sort') === 'priceAsc') products.sort((a, b) => a.retailUnitPriceFen - b.retailUnitPriceFen)
    if (query.get('sort') === 'priceDesc') products.sort((a, b) => b.retailUnitPriceFen - a.retailUnitPriceFen)
    return list(products, 12)
  }
  if (endpoint === '/admin/products') {
    return list(fixtures.adminProducts.filter(product =>
      (includes(product.name, query.get('keyword')) || includes(product.sku, query.get('keyword')))
      && (!query.get('categoryId') || product.categoryId === query.get('categoryId'))
      && (!query.get('status') || product.status === query.get('status'))), 20)
  }
  const productMatch = endpoint.match(/^\/(admin\/)?products\/([^/]+)$/)
  if (productMatch) return detail((productMatch[1] ? fixtures.adminProducts : fixtures.products).find(product => product.id === productMatch[2]))
  const movementMatch = endpoint.match(/^\/admin\/products\/([^/]+)\/stock-movements$/)
  if (movementMatch) {
    if (!fixtures.stockMovements[movementMatch[1]!]) problem(404, 'PREVIEW_RECORD_MISSING', '此示例商品不存在。')
    return list(fixtures.stockMovements[movementMatch[1]!]!, 20)
  }

  if (endpoint === '/cart') return envelope(empty ? { ...fixtures.cart, items: [], totalFen: 0, canCheckout: false } : fixtures.cart)
  if (trial) {
    if (context.state === 'error') return envelope(null)
    if (empty) problem(409, 'CART_EMPTY', '示例购物车为空，请切换为正常状态查看结算界面。')
    return envelope({ ...fixtures.checkoutPreview, expiresAt: new Date(Date.now() + 15 * 60 * 1000).toISOString() })
  }
  if (endpoint === '/orders' || endpoint === '/admin/orders') {
    const items = fixtures.orderSummaries.filter(order => (!query.get('status') || order.status === query.get('status'))
      && (!query.get('start') || Date.parse(order.createdAt) >= Date.parse(query.get('start')!))
      && (!query.get('end') || Date.parse(order.createdAt) < Date.parse(query.get('end')!)))
    const page = paginated(empty ? [] : items, query, 20)
    return envelope({ items: page.data, page: page.meta.page, pageSize: page.meta.pageSize, total: page.meta.totalItems })
  }
  const orderMatch = endpoint.match(/^\/(admin\/)?orders\/([^/]+)$/)
  if (orderMatch) {
    const order = fixtures.orders.find(item => item.id === orderMatch[2])
    return detail(order && { ...order, allowedActions: orderMatch[1] ? fixtures.adminOrderAllowedActions[order.id] ?? [] : order.allowedActions })
  }
  problem(404, 'PREVIEW_ENDPOINT_MISSING', '此接口没有只读展示样例；没有向后端发送请求。')
}

export const previewScenes: Array<{ label: string; path: string; role: PreviewRole }> = [
  { label: '商品目录', path: '/products', role: 'guest' },
  { label: '商品详情', path: `/products/${fixtures.products[0]!.id}`, role: 'user' },
  { label: '登录', path: '/login', role: 'guest' },
  { label: '注册', path: '/register', role: 'guest' },
  { label: '个人资料', path: '/account/profile', role: 'user' },
  { label: '购物车', path: '/cart', role: 'user' },
  { label: '结算', path: '/checkout', role: 'user' },
  { label: '我的订单', path: '/account/orders', role: 'user' },
  { label: '订单详情', path: `/account/orders/${fixtures.orders[0]!.id}`, role: 'user' },
  { label: '用户管理', path: '/admin/users', role: 'admin' },
  { label: '账户详情', path: `/admin/users/${fixtures.users[1]!.id}`, role: 'admin' },
  { label: '商品管理', path: '/admin/products', role: 'admin' },
  { label: '新建商品', path: '/admin/products/new', role: 'admin' },
  { label: '编辑商品', path: `/admin/products/${fixtures.adminProducts[0]!.id}`, role: 'admin' },
  { label: '分类管理', path: '/admin/categories', role: 'admin' },
  { label: '订单管理', path: '/admin/orders', role: 'admin' },
  { label: '管理订单详情', path: `/admin/orders/${fixtures.orders.find(order => order.status === 'PAID')!.id}`, role: 'admin' },
]
