import type { RouteRecordRaw } from 'vue-router'

export interface CatalogRouteRegistration {
  routeId: string
  path: string
  layout: 'public' | 'admin'
  requiredCapability?: string
  loadPage: () => Promise<unknown>
}

export const catalogRouteRegistrations: CatalogRouteRegistration[] = [
  { routeId: 'catalog-products', path: '/products', layout: 'public', loadPage: () => import('../../pages/ProductsPage.vue') },
  { routeId: 'catalog-product-detail', path: '/products/:id', layout: 'public', loadPage: () => import('../../pages/ProductDetailPage.vue') },
  { routeId: 'catalog-admin-products', path: '/admin/products', layout: 'admin', requiredCapability: 'ADMIN_CATALOG_READ', loadPage: () => import('../../pages/AdminProductsPage.vue') },
  { routeId: 'catalog-admin-product-create', path: '/admin/products/new', layout: 'admin', requiredCapability: 'ADMIN_CATALOG_WRITE', loadPage: () => import('../../pages/AdminProductEditorPage.vue') },
  { routeId: 'catalog-admin-product-edit', path: '/admin/products/:id', layout: 'admin', requiredCapability: 'ADMIN_CATALOG_WRITE', loadPage: () => import('../../pages/AdminProductEditorPage.vue') },
  { routeId: 'catalog-admin-categories', path: '/admin/categories', layout: 'admin', requiredCapability: 'ADMIN_CATALOG_WRITE', loadPage: () => import('../../pages/AdminCategoriesPage.vue') },
]

export const catalogRoutes: RouteRecordRaw[] = catalogRouteRegistrations.map((route) => ({
  name: route.routeId,
  path: route.path,
  component: route.loadPage,
  meta: { layout: route.layout, capability: route.requiredCapability },
}))
