import { createRouter, createWebHistory } from 'vue-router'
import { identityRoutes } from './features/identity/routes'
import { commerceRoutes } from './features/commerce/routes'
import { catalogRoutes } from './features/catalog/routes'
import { useSessionStore } from './stores/session'

export const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', component: () => import('./pages/HomePage.vue'), meta: { title: '首页' } },
    ...catalogRoutes,
    ...commerceRoutes,
    ...identityRoutes,
    { path: '/:pathMatch(.*)*', component: () => import('./pages/NotFoundPage.vue'), meta: { title: '页面未找到' } },
  ],
  scrollBehavior: () => ({ top: 0 }),
})

router.beforeEach(async (to) => {
  const session = useSessionStore()
  await session.load()
  const capability = to.meta.capability as string | undefined
  if (capability && !session.actor) return { path: '/login', query: { redirect: to.fullPath } }
  if (capability && !session.actor?.capabilities.includes(capability)) return { path: '/' }
  if ((to.path === '/login' || to.path === '/register') && session.actor) {
    return session.isAdmin ? '/admin/users' : '/account/profile'
  }
})

router.afterEach((to) => { if (to.meta.title) document.title = `${to.meta.title} · WEMOVE` })
