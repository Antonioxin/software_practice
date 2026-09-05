import type { RouteRecordRaw } from 'vue-router'

export interface IdentityRouteRegistration {
  routeId: string
  path: string
  layout: 'public' | 'account' | 'admin'
  requiredCapability?: string
  loadPage: () => Promise<unknown>
}

export const identityRouteRegistrations: IdentityRouteRegistration[] = [
  { routeId: 'identity-login', path: '/login', layout: 'public', loadPage: () => import('../../pages/LoginPage.vue') },
  { routeId: 'identity-register', path: '/register', layout: 'public', loadPage: () => import('../../pages/RegisterPage.vue') },
  { routeId: 'identity-profile', path: '/account/profile', layout: 'account', requiredCapability: 'ACCOUNT_PROFILE_READ', loadPage: () => import('../../pages/ProfilePage.vue') },
  { routeId: 'identity-admin-users', path: '/admin/users', layout: 'admin', requiredCapability: 'ADMIN_USERS_READ', loadPage: () => import('../../pages/AdminUsersPage.vue') },
  { routeId: 'identity-admin-user-detail', path: '/admin/users/:id', layout: 'admin', requiredCapability: 'ADMIN_USERS_READ', loadPage: () => import('../../pages/AdminUserDetailPage.vue') },
]

export const identityRoutes: RouteRecordRaw[] = identityRouteRegistrations.map((route) => ({
  name: route.routeId,
  path: route.path,
  component: route.loadPage,
  meta: { layout: route.layout, capability: route.requiredCapability },
}))
