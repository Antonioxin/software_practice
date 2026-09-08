<script setup lang="ts">
import SketchIcon from '../../components/SketchIcon.vue'
defineProps<{ eyebrow: string; title: string; subtitle: string; note: string; symbol?: string; icon?: string }>()
</script>
<template>
  <section class="content-hero">
    <div><p class="content-eyebrow">{{ eyebrow }}</p><h1>{{ title }}</h1><p class="content-hero-subtitle">{{ subtitle }}</p><p class="content-hero-note">{{ note }}</p></div>
    <div class="content-hero-sketch" aria-hidden="true"><SketchIcon :name="icon || 'heart'" :size="155" /><small>A little play,<br />every day.</small></div>
  </section>
</template>
