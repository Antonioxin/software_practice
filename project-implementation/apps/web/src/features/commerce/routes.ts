import type { RouteRecordRaw } from 'vue-router'
export const commerceRoutes: RouteRecordRaw[] = [
  {
    path: '/cart',
    component: () => import('../../pages/commerce/CartPage.vue'),
    meta: { title: '购物车', capability: 'CART_READ' },
  },
  {
    path: '/checkout',
    component: () => import('../../pages/commerce/CheckoutPage.vue'),
    meta: { title: '确认订单', capability: 'ORDERS_WRITE' },
  },
  {
    path: '/account/orders',
    component: () => import('../../pages/commerce/OrdersPage.vue'),
    meta: { title: '我的订单', capability: 'ORDERS_READ' },
  },
  {
    path: '/account/orders/:id',
    component: () => import('../../pages/commerce/OrderDetailPage.vue'),
    meta: { title: '订单详情', capability: 'ORDERS_READ' },
  },
  {
    path: '/admin/orders',
    component: () => import('../../pages/commerce/AdminOrdersPage.vue'),
    meta: { title: '订单管理', capability: 'ADMIN_ORDERS_READ' },
  },
  {
    path: '/admin/orders/:id',
    component: () => import('../../pages/commerce/AdminOrderDetailPage.vue'),
    meta: { title: '订单详情', capability: 'ADMIN_ORDERS_READ' },
  },
]
