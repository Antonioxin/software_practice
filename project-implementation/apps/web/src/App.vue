<script setup lang="ts">
import { defineAsyncComponent, onBeforeUnmount, onMounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import { createPageEntrance } from './features/motion/pageEntrance'
import { isDevelopmentPreview } from './services/http'

const route = useRoute()
let entrance: ReturnType<typeof createPageEntrance> | undefined
onMounted(() => { entrance = createPageEntrance(document.body) })
// Keep component/form state intact; only replay visuals on a different page.
watch(() => route.path, () => entrance?.refresh(true), { flush: 'post' })
onBeforeUnmount(() => entrance?.destroy())

const PreviewToolbar = import.meta.env.DEV && isDevelopmentPreview
  ? defineAsyncComponent(() => import('./dev/PreviewToolbar.vue')) : null
</script>

<template>
  <RouterView />
  <component :is="PreviewToolbar" v-if="PreviewToolbar" />
</template>
