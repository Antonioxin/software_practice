import { createRouter, createWebHistory } from 'vue-router'
import { identityRoutes } from './features/identity/routes'
import { useSessionStore } from './stores/session'

export const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', redirect: '/login' },
    ...identityRoutes,
    { path: '/:pathMatch(.*)*', component: () => import('./pages/NotFoundPage.vue') },
  ],
  scrollBehavior: () => ({ top: 0 }),
})

router.beforeEach(async (to) => {
  const session = useSessionStore()
  await session.load()
  const capability = to.meta.capability as string | undefined
  if (capability && !session.actor) return { path: '/login', query: { redirect: to.fullPath } }
  if (capability && !session.actor?.capabilities.includes(capability)) return { path: '/account/profile' }
  if ((to.path === '/login' || to.path === '/register') && session.actor) {
    return session.isAdmin ? '/admin/users' : '/account/profile'
  }
})
