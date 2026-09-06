<script setup lang="ts">
import PublicShell from '../components/PublicShell.vue'
import SketchIcon from '../components/SketchIcon.vue'
</script>
<template><PublicShell><section class="wm-not-found"><SketchIcon name="help" :size="110" /><p class="wm-eyebrow">404 / A LITTLE DETOUR</p><h1>这个角落，还没有内容。</h1><p>链接可能已变更，回到首页继续探索吧。</p><RouterLink class="wm-button wm-button-dark" to="/">返回首页<SketchIcon name="home" /></RouterLink></section></PublicShell></template>
