import type { RouteRecordRaw } from 'vue-router'
// Existing ADMIN_ORDERS_READ is the navigation capability shared by administrator modules.
// The backend remains authoritative and requires ROLE_ADMIN for every /admin endpoint.
export const contentRoutes: RouteRecordRaw[] = [
  { path: '/articles', component: () => import('../../pages/content/ArticlesPage.vue'), meta: { title: '玩法灵感', public: true } },
  { path: '/articles/:id', component: () => import('../../pages/content/ArticleDetailPage.vue'), meta: { title: '阅读故事', public: true } },
  { path: '/faq', component: () => import('../../pages/content/FaqPage.vue'), meta: { title: '常见问题', public: true } },
  { path: '/downloads', component: () => import('../../pages/content/DownloadsPage.vue'), meta: { title: '下载中心', public: true } },
  ...['about', 'terms', 'privacy'].map((name) => ({ path: `/${name}`, component: () => import('../../pages/content/AboutPage.vue'), meta: { title: { about: '关于我们', terms: '服务条款', privacy: '隐私政策' }[name], public: true } })),
  { path: '/admin/content', component: () => import('../../pages/content/AdminContentPage.vue'), meta: { title: '内容管理', capability: 'ADMIN_ORDERS_READ' } },
  { path: '/admin/content/settings', component: () => import('../../pages/content/AdminSettingsPage.vue'), meta: { title: '品牌与首页设置', capability: 'ADMIN_ORDERS_READ' } },
  { path: '/admin/files', component: () => import('../../pages/content/AdminFilesPage.vue'), meta: { title: '文件与媒体', capability: 'ADMIN_ORDERS_READ' } },
]
