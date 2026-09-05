<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useSessionStore } from '../stores/session'

defineProps<{ title: string; eyebrow?: string; admin?: boolean }>()
const mobileOpen = ref(false)
const session = useSessionStore()
const router = useRouter()

async function signOut() {
  await session.logout()
  await router.replace('/login')
}
</script>

<template>
  <div class="site-root">
    <header class="site-header">
      <div class="header-bar">
        <RouterLink class="brand" :to="session.isAdmin ? '/admin/users' : '/account/profile'">
          <img src="/brand-logo.png" alt="WEMOVE SPORTS" />
          <span>WeMove</span>
        </RouterLink>
        <nav class="desktop-nav" aria-label="主导航">
          <RouterLink v-if="!session.isAdmin" to="/account/profile">个人资料</RouterLink>
          <RouterLink v-if="session.isAdmin" to="/admin/users">用户管理</RouterLink>
          <span class="integration-link" title="由其他成员模块接入">业务中心 · 待接入</span>
        </nav>
        <div class="identity-actions">
          <span class="identity-name">{{ session.actor?.nickname }}</span>
          <button class="text-button" type="button" @click="signOut">退出</button>
          <button class="menu-button" type="button" :aria-expanded="mobileOpen" aria-label="打开导航" @click="mobileOpen = !mobileOpen">
            <span></span><span></span><span></span>
          </button>
        </div>
      </div>
      <nav v-if="mobileOpen" class="mobile-nav" aria-label="移动端导航">
        <RouterLink v-if="!session.isAdmin" to="/account/profile">个人资料</RouterLink>
        <RouterLink v-if="session.isAdmin" to="/admin/users">用户管理</RouterLink>
        <button type="button" @click="signOut">退出登录</button>
      </nav>
    </header>

    <main id="main-content" :class="['portal-main', { 'admin-main': admin }]">
      <header class="page-heading">
        <p>{{ eyebrow ?? (admin ? 'OPERATIONS / IDENTITY' : 'MY WEMOVE') }}</p>
        <h1>{{ title }}</h1>
      </header>
      <slot />
    </main>

    <footer class="site-footer"><strong>WeMove</strong><span>安全会话 · 最小权限 · 可追踪操作</span></footer>
  </div>
</template>
