import type { RouteRecordRaw } from 'vue-router'

// capability 说明（占位）：工单/审计暂无专属能力码，按“不改 A 的能力码表”的决定，
// 用户页复用全 USER 恒含的 ORDERS_READ、管理页复用 ADMIN_ORDERS_READ 作为前端导航门面；
// 真正的授权由后端角色检查（/admin/** hasRole(ADMIN)、用户侧归属校验）完成。
// 待 A 下发 TICKET_READ / ADMIN_TICKET_READ / ADMIN_AUDIT_READ 等能力码后在此替换。
export const supportRoutes: RouteRecordRaw[] = [
  {
    path: '/contact',
    component: () => import('../../pages/support/TicketSubmitPage.vue'),
    meta: { title: '联系客服', capability: 'ORDERS_READ' },
  },
  {
    path: '/account/tickets',
    component: () => import('../../pages/support/TicketsListPage.vue'),
    meta: { title: '我的咨询', capability: 'ORDERS_READ' },
  },
  {
    path: '/account/tickets/:id',
    component: () => import('../../pages/support/TicketDetailPage.vue'),
    meta: { title: '咨询详情', capability: 'ORDERS_READ' },
  },
  {
    path: '/admin/tickets',
    component: () => import('../../pages/support/AdminTicketsPage.vue'),
    meta: { title: '工单管理', capability: 'ADMIN_ORDERS_READ' },
  },
  {
    path: '/admin/tickets/:id',
    component: () => import('../../pages/support/AdminTicketDetailPage.vue'),
    meta: { title: '工单详情', capability: 'ADMIN_ORDERS_READ' },
  },
  {
    path: '/admin',
    component: () => import('../../pages/support/AdminOverviewPage.vue'),
    meta: { title: '运营总览', capability: 'ADMIN_ORDERS_READ' },
  },
  {
    path: '/admin/audit-logs',
    component: () => import('../../pages/support/AdminAuditLogsPage.vue'),
    meta: { title: '审计日志', capability: 'ADMIN_ORDERS_READ' },
  },
]
