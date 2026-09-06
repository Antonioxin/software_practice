<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useSessionStore } from '../stores/session'

const menuOpen = ref(false)
const session = useSessionStore()
const router = useRouter()

async function signOut() {
  await session.logout()
  await router.replace('/products')
}
</script>

<template>
  <div class="catalog-site">
    <a class="skip-link" href="#main-content">跳到主要内容</a>
    <header class="catalog-header">
      <div class="catalog-header-inner">
        <RouterLink class="brand" to="/products">
          <img src="/brand-logo.png" alt="WEMOVE SPORTS" />
          <span>WeMove</span>
        </RouterLink>
        <nav class="catalog-nav" aria-label="主导航">
          <RouterLink to="/products">全部商品</RouterLink>
          <span>玩法指南</span>
          <span>购买渠道</span>
        </nav>
        <div class="catalog-account">
          <RouterLink v-if="!session.actor" to="/login">登录</RouterLink>
          <RouterLink v-else :to="session.isAdmin ? '/admin/products' : '/account/profile'">
            {{ session.isAdmin ? '管理后台' : session.actor.nickname }}
          </RouterLink>
          <button v-if="session.actor" type="button" @click="signOut">退出</button>
          <button class="catalog-menu" type="button" :aria-expanded="menuOpen" aria-label="展开导航" @click="menuOpen = !menuOpen">
            <span></span><span></span>
          </button>
        </div>
      </div>
      <nav v-if="menuOpen" class="catalog-mobile-nav" aria-label="移动端导航">
        <RouterLink to="/products" @click="menuOpen = false">全部商品</RouterLink>
        <RouterLink :to="session.actor ? '/account/profile' : '/login'" @click="menuOpen = false">
          {{ session.actor ? '个人中心' : '登录账户' }}
        </RouterLink>
      </nav>
    </header>
    <main id="main-content"><slot /></main>
    <footer class="catalog-footer">
      <div><strong>WE MOVE, TOGETHER.</strong><span>把运动变成全家都愿意参与的游戏。</span></div>
      <nav><RouterLink to="/products">商品</RouterLink><span>使用说明</span><span>隐私说明</span></nav>
    </footer>
  </div>
</template>
