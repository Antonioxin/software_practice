<script setup lang="ts">
import BrandMark from './BrandMark.vue'
import { useSessionStore } from '../stores/session'
const session = useSessionStore()
</script>
<template>
  <footer class="wm-footer">
    <div><BrandMark /><p class="hand-note">A little play, every day.</p></div>
    <p>把运动变成日常的小乐趣。<small>© 2026 WEMOVE · 课程模拟系统</small></p>
    <nav aria-label="页脚导航"><RouterLink to="/products">探索商品</RouterLink><RouterLink :to="session.isAdmin ? '/admin/users' : '/account/profile'">我的账户</RouterLink></nav>
  </footer>
</template>
