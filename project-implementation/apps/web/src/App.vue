<script setup lang="ts">
import { defineAsyncComponent } from 'vue'
import { isDevelopmentPreview } from './services/http'

const PreviewToolbar = import.meta.env.DEV && isDevelopmentPreview
  ? defineAsyncComponent(() => import('./dev/PreviewToolbar.vue')) : null
</script>

<template>
  <RouterView />
  <component :is="PreviewToolbar" v-if="PreviewToolbar" />
</template>
