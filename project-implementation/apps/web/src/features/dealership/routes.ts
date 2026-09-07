import type { RouteRecordRaw } from 'vue-router'

export const dealershipRoutes: RouteRecordRaw[] = [
  { path: '/channels', component: () => import('../../pages/dealership/ChannelsPage.vue'), meta: { title: '购买渠道' } },
  { path: '/account/dealer-application', component: () => import('../../pages/dealership/DealerApplicationPage.vue'), meta: { title: '经销合作申请', capability: 'DEALER_APPLICATION_READ' } },
  { path: '/dealer/catalog', component: () => import('../../pages/dealership/DealerCatalogPage.vue'), meta: { title: '经销目录', capability: 'DEALER_CATALOG_READ' } },
  { path: '/account/inquiries', component: () => import('../../pages/dealership/InquiriesPage.vue'), meta: { title: '我的询价', capability: 'INQUIRIES_READ' } },
  { path: '/account/inquiries/:id', component: () => import('../../pages/dealership/InquiryDetailPage.vue'), meta: { title: '询价详情', capability: 'INQUIRIES_READ' } },
  { path: '/admin/dealer-applications', component: () => import('../../pages/dealership/AdminApplicationsPage.vue'), meta: { title: '合作申请审核', capability: 'ADMIN_DEALERSHIP_READ' } },
  { path: '/admin/dealer-applications/:id', component: () => import('../../pages/dealership/AdminApplicationDetailPage.vue'), meta: { title: '申请审核', capability: 'ADMIN_DEALERSHIP_WRITE' } },
  { path: '/admin/inquiries', component: () => import('../../pages/dealership/InquiriesPage.vue'), props: { admin: true }, meta: { title: '询价管理', capability: 'ADMIN_DEALERSHIP_READ' } },
  { path: '/admin/inquiries/:id', component: () => import('../../pages/dealership/InquiryDetailPage.vue'), props: { admin: true }, meta: { title: '询价处理', capability: 'ADMIN_DEALERSHIP_WRITE' } },
  { path: '/admin/companies', component: () => import('../../pages/dealership/AdminCompaniesPage.vue'), meta: { title: '合作企业', capability: 'ADMIN_DEALERSHIP_READ' } },
  { path: '/admin/channels', component: () => import('../../pages/dealership/AdminChannelsPage.vue'), meta: { title: '渠道管理', capability: 'ADMIN_DEALERSHIP_WRITE' } },
]
