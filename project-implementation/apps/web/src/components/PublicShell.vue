<script setup lang="ts">
import { ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useSessionStore } from '../stores/session'
import BrandMark from './BrandMark.vue'
import SketchIcon from './SketchIcon.vue'
import QuickLinksMenu from './QuickLinksMenu.vue'
import SiteFooter from './SiteFooter.vue'
const menuOpen = ref(false)
const error = ref('')
const session = useSessionStore()
const route = useRoute()
const router = useRouter()
watch(() => route.fullPath, () => { menuOpen.value = false })
async function signOut() {
  try { await session.logout(); await router.replace('/login') }
  catch { error.value = '退出结果暂未确认，请重试。' }
}
</script>
<template>
  <div class="catalog-site">
    <header class="wm-header glass-panel">
      <BrandMark />
      <nav class="wm-navigation" aria-label="主导航">
        <RouterLink to="/" exact-active-class="is-active"><SketchIcon name="home" />首页</RouterLink>
        <RouterLink to="/products" active-class="is-active"><SketchIcon name="grid" />探索商品</RouterLink>
        <RouterLink v-if="!session.isAdmin" to="/account/orders" active-class="is-active"><SketchIcon name="orders" />我的订单</RouterLink>
        <RouterLink v-else to="/admin/products" active-class="is-active"><SketchIcon name="filter" />管理后台</RouterLink>
      </nav>
      <div class="wm-header-actions">
        <QuickLinksMenu />
        <RouterLink class="wm-account-button" :aria-label="session.actor ? '我的账户' : '登录或注册'" :to="session.actor ? (session.isAdmin ? '/admin/users' : '/account/profile') : '/login'">
          <SketchIcon name="user" /><span>{{ session.actor ? session.actor.nickname : '登录 / 注册' }}</span>
        </RouterLink>
        <button v-if="session.actor" class="wm-signout" type="button" @click="signOut">退出</button>
        <button class="wm-menu-button wm-icon-button" type="button" aria-controls="public-mobile-menu" :aria-expanded="menuOpen" :aria-label="menuOpen ? '关闭导航' : '展开导航'" @click="menuOpen = !menuOpen"><SketchIcon :name="menuOpen ? 'close' : 'menu'" /></button>
      </div>
      <nav v-if="menuOpen" id="public-mobile-menu" class="wm-mobile-menu" aria-label="移动端导航">
        <RouterLink to="/">首页</RouterLink><RouterLink to="/products">探索商品</RouterLink>
        <RouterLink :to="session.isAdmin ? '/admin/orders' : '/account/orders'">{{ session.isAdmin ? '订单管理' : '我的订单' }}</RouterLink>
        <RouterLink v-if="session.isAdmin" to="/admin/products">商品管理</RouterLink>
        <RouterLink :to="session.actor ? (session.isAdmin ? '/admin/users' : '/account/profile') : '/login'">{{ session.actor ? '账户信息' : '登录 / 注册' }}</RouterLink>
        <button v-if="session.actor" type="button" @click="signOut">退出登录</button>
      </nav>
    </header>
    <p v-if="error" class="wm-shell-error" role="alert">{{ error }}</p>
    <main id="main-content"><slot /></main>
    <SiteFooter />
  </div>
</template>

<style scoped>
.wm-header-actions { min-width: 0; }
/* Keep the shortcut, account and mobile navigation controls on screen at 320 px. */
@media (max-width: 380px) {
  .wm-header :deep(.wm-brand > span) { display: none; }
  .wm-header :deep(.wm-brand) { gap: 0; }
}
</style>
