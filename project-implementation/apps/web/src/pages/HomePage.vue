<script setup lang="ts">
import PublicShell from '../components/PublicShell.vue'
import SketchIcon from '../components/SketchIcon.vue'
import HeroHandwriting from '../components/HeroHandwriting.vue'
import { isDevelopmentPreview } from '../services/http'
const showPreviewLink = import.meta.env.DEV && !isDevelopmentPreview
</script>

<template>
  <PublicShell>
    <div class="home-page">
      <section class="home-hero" aria-labelledby="home-title">
        <p class="wm-eyebrow">WEMOVE SPORTS / EVERYDAY PLAY</p>
        <HeroHandwriting />
        <RouterLink class="home-explore" to="/products">探索全部商品 <SketchIcon name="arrow-left" class="arrow-forward" :size="23" /></RouterLink>
      </section>
      <p v-if="showPreviewLink" class="home-dev-link"><a href="/products?preview=1">打开只读界面预览</a><span>无需启动后端，查看全部已实现页面。</span></p>
    </div>
  </PublicShell>
</template>

<style scoped>
.home-page { width: min(1280px, calc(100% - 64px)); margin: auto; }
.home-hero { display: flex; flex-direction: column; align-items: center; justify-content: center; min-height: min(780px, calc(100svh - var(--header-height))); padding: 58px 0 68px; text-align: center; }
.home-hero > .wm-eyebrow { margin: 0 0 25px; color: #69716b; font-size: 14px; letter-spacing: 2px; }
.home-explore { display: inline-flex; align-items: center; justify-content: center; gap: 18px; min-height: 46px; margin-top: 28px; padding: 7px 2px; border-bottom: 1px solid #9eaca2; color: var(--green-dark); font-size: 15px; text-decoration: none; }
.home-explore:hover { color: var(--ink); border-color: var(--ink); }
.home-dev-link { display: flex; justify-content: flex-end; gap: 14px; flex-wrap: wrap; margin: 0 0 24px; color: var(--muted); font-size: 12px; }
@media (max-width: 760px) {
  .home-page { width: calc(100% - 36px); }
  .home-hero { min-height: min(720px, calc(100svh - var(--header-height))); padding: 64px 0 58px; }
  .home-hero > .wm-eyebrow { margin-bottom: 30px; font-size: 12px; letter-spacing: 1.6px; }
  .home-dev-link { justify-content: center; line-height: 1.8; }
}
</style>
