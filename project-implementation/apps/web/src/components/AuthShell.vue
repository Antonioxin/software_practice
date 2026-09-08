<script setup lang="ts">
import BrandMark from './BrandMark.vue'
import SketchIcon from './SketchIcon.vue'
import SiteFooter from './SiteFooter.vue'
defineProps<{ eyebrow?: string; title: string; description?: string; handwrittenStory?: boolean }>()
</script>
<template>
  <div class="auth-page wm-auth">
    <header class="wm-header"><BrandMark /><nav class="wm-auth-nav" aria-label="账户导航"><RouterLink to="/products"><SketchIcon name="arrow-left" />返回商店</RouterLink><RouterLink to="/login">登录</RouterLink><RouterLink to="/register">注册</RouterLink></nav></header>
    <main id="main-content" class="auth-main">
      <section class="story-panel" aria-label="WEMOVE 品牌介绍">
        <p class="wm-eyebrow">WELCOME TO WEMOVE</p>
        <div class="wm-auth-story">
          <p v-if="handwrittenStory" class="wm-auth-display wm-auth-display--handwritten">Good days<br />start with<br /><span>a little play.</span></p>
          <p v-else class="wm-auth-display">GOOD DAYS<br />START WITH<br /><span>A LITTLE PLAY.</span></p>
          <p>一点好奇，一点行动。<br />发现生活里更多好玩的可能。</p>
        </div>
        <div class="wm-auth-icons" aria-hidden="true"><span class="tile-blue"><SketchIcon name="gift" :size="58" /></span><span class="tile-yellow"><SketchIcon name="heart" :size="58" /></span><span class="tile-mint"><SketchIcon name="home" :size="58" /></span></div>
        <p class="hand-note">Make room for a little joy.</p>
      </section>
      <section class="form-panel glass-panel"><div class="form-wrap"><p v-if="eyebrow" class="eyebrow">{{ eyebrow }}</p><h1>{{ title }}</h1><p v-if="description" class="lede">{{ description }}</p><slot /></div></section>
    </main>
    <SiteFooter />
  </div>
</template>

<style scoped>
.wm-auth-display--handwritten { font: 400 clamp(68px, 6.7vw, 100px)/1.02 var(--font-hand); letter-spacing: -1.5px; }
@media (max-width: 760px) {
  .wm-auth-display--handwritten { font-size: 64px; letter-spacing: -1px; }
}
</style>
