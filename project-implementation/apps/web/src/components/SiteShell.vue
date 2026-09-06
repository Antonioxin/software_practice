<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useSessionStore } from '../stores/session'
import BrandMark from './BrandMark.vue'
import SketchIcon from './SketchIcon.vue'
import SiteFooter from './SiteFooter.vue'
defineProps<{ title: string; eyebrow?: string; admin?: boolean }>()
const mobileOpen = ref(false)
const error = ref('')
const session = useSessionStore()
const router = useRouter()
const route = useRoute()
const links = computed(() => session.isAdmin ? [
  { to: '/admin/products', label: '商品管理', icon: 'package' },
  { to: '/admin/categories', label: '分类管理', icon: 'grid' },
  { to: '/admin/orders', label: '订单管理', icon: 'orders' },
  { to: '/admin/users', label: '用户管理', icon: 'user' },
] : [
  { to: '/account/profile', label: '个人资料', icon: 'user' },
  { to: '/account/orders', label: '我的订单', icon: 'orders' },
  { to: '/cart', label: '购物车', icon: 'cart' },
])
const currentIcon = computed(() => links.value.find(item => route.path.startsWith(item.to))?.icon ?? 'grid')
watch(() => route.fullPath, () => { mobileOpen.value = false })
async function signOut() {
  try { await session.logout(); await router.replace('/login') }
  catch { error.value = '退出结果暂未确认，请重试。' }
}
</script>
<template>
  <div :class="['site-root', 'wm-portal', { 'wm-admin': admin }]">
    <header class="wm-header glass-panel">
      <BrandMark /><span class="wm-portal-label">{{ session.isAdmin ? 'WORKSPACE / 管理工作台' : 'MY SPACE / 个人中心' }}</span>
      <div class="wm-header-actions">
        <RouterLink class="wm-account-button" to="/products" aria-label="返回商店"><SketchIcon name="arrow-left" /><span>返回商店</span></RouterLink>
        <span class="wm-persona"><SketchIcon name="user" />{{ session.actor?.nickname }}</span>
        <button class="wm-signout" type="button" @click="signOut">退出</button>
        <button class="wm-menu-button wm-icon-button" type="button" aria-controls="workspace-navigation" :aria-expanded="mobileOpen" aria-label="展开工作台导航" @click="mobileOpen = !mobileOpen"><SketchIcon :name="mobileOpen ? 'close' : 'menu'" /></button>
      </div>
    </header>
    <div class="wm-workspace">
      <aside id="workspace-navigation" :class="['wm-sidebar', { 'is-open': mobileOpen }]">
        <p class="wm-eyebrow">{{ session.isAdmin ? 'MANAGE & ORGANIZE' : 'A SPACE FOR YOU' }}</p>
        <nav aria-label="工作台导航"><RouterLink v-for="link in links" :key="link.to" :to="link.to"><SketchIcon :name="link.icon" />{{ link.label }}</RouterLink></nav>
        <div class="wm-sidebar-note"><SketchIcon name="heart" :size="38" /><span class="hand-note">Keep it simple.</span><p>让每一件小事，井井有条。</p></div>
        <button class="wm-sidebar-signout" type="button" @click="signOut"><SketchIcon name="return" />退出登录</button>
      </aside>
      <main id="main-content" :class="['portal-main', { 'admin-main': admin }]">
        <p v-if="error" class="error-summary" role="alert">{{ error }}</p>
        <header class="page-heading"><div><p>{{ eyebrow ?? (admin ? 'WORKSPACE / WEMOVE' : 'MY WEMOVE') }}</p><h1>{{ title }}</h1></div><span class="wm-heading-icon"><SketchIcon :name="currentIcon" :size="44" /></span></header>
        <slot />
      </main>
    </div>
    <SiteFooter />
  </div>
</template>
