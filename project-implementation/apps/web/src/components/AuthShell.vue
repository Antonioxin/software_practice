<script setup lang="ts">
import BrandMark from './BrandMark.vue'
import SketchIcon from './SketchIcon.vue'
import SiteFooter from './SiteFooter.vue'
defineProps<{ eyebrow: string; title: string; description: string }>()
</script>
<template>
  <div class="auth-page wm-auth">
    <header class="wm-header glass-panel"><BrandMark /><nav class="wm-auth-nav" aria-label="账户导航"><RouterLink to="/products"><SketchIcon name="arrow-left" />返回商店</RouterLink><RouterLink to="/login">登录</RouterLink><RouterLink to="/register">注册</RouterLink></nav></header>
    <main id="main-content" class="auth-main">
      <section class="story-panel" aria-label="WEMOVE 品牌介绍">
        <p class="wm-eyebrow">WELCOME TO WEMOVE</p>
        <div class="wm-auth-story"><p class="wm-auth-display">GOOD DAYS<br />START WITH<br /><span>A LITTLE PLAY.</span></p><p>一点好奇，一点行动。<br />发现生活里更多好玩的可能。</p></div>
        <div class="wm-auth-icons" aria-hidden="true"><span class="tile-blue"><SketchIcon name="gift" :size="58" /></span><span class="tile-yellow"><SketchIcon name="heart" :size="58" /></span><span class="tile-mint"><SketchIcon name="home" :size="58" /></span></div>
        <p class="hand-note">Make room for a little joy.</p>
      </section>
      <section class="form-panel glass-panel"><div class="form-wrap"><p class="eyebrow">{{ eyebrow }}</p><h1>{{ title }}</h1><p class="lede">{{ description }}</p><slot /></div></section>
    </main>
    <SiteFooter />
  </div>
</template>
