<script setup lang="ts">
withDefaults(defineProps<{ name: string; size?: number; label?: string }>(), { size: 24, label: '' })
</script>
<template>
  <img class="sketch-icon" :src="`/assets/sketch/${name}.webp`" :width="size" :height="size" :alt="label" :aria-hidden="label ? undefined : true" draggable="false" />
</template>
<style scoped>
.sketch-icon { display: inline-block; flex-shrink: 0; object-fit: contain; vertical-align: middle; }
</style>
