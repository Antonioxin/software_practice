<script setup lang="ts">
import BrandMark from './BrandMark.vue'
import { useSessionStore } from '../stores/session'
const session = useSessionStore()
</script>
<template>
  <footer class="wm-footer">
    <div><BrandMark /><p class="hand-note">A little play, every day.</p></div>
    <p>把运动变成日常的小乐趣。<small>© 2026 WEMOVE · 课程模拟系统</small></p>
    <nav aria-label="页脚导航">
      <RouterLink to="/about">关于 WEMOVE</RouterLink><RouterLink to="/faq">常见问题</RouterLink><RouterLink to="/downloads">下载中心</RouterLink>
      <RouterLink to="/terms">使用说明</RouterLink><RouterLink to="/privacy">隐私说明</RouterLink><RouterLink :to="session.isAdmin ? '/admin/users' : '/account/profile'">我的账户</RouterLink>
    </nav>
  </footer>
</template>

<style scoped>
.wm-footer nav { display: flex; flex-wrap: wrap; justify-content: flex-end; gap: 12px 20px; max-width: 360px; }
@media (max-width: 760px) { .wm-footer nav { justify-content: center; } }
</style>
