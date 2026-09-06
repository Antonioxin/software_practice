<script setup lang="ts">
import { computed } from 'vue'

const props = defineProps<{ name?: string | null; sku?: string | null; compact?: boolean }>()
const palettes = [
  ['#d2a15e', '#315848', '#f2dfb8'],
  ['#c66549', '#24483c', '#e7c779'],
  ['#718d73', '#e3b66e', '#efe7d4'],
  ['#b9835b', '#486a58', '#d8c999'],
]
const palette = computed(() => {
  const seed = [...(props.sku ?? props.name ?? 'W')].reduce((total, char) => total + char.charCodeAt(0), 0)
  return palettes[seed % palettes.length]
})
const initials = computed(() => (props.name?.replace(/[（(].*$/, '').slice(0, 2) || 'WM'))
</script>

<template>
  <div class="product-artwork" :class="{ compact }" role="img" :aria-label="`${name || '商品'} 图片占位`" :style="{ '--art-a': palette[0], '--art-b': palette[1], '--art-c': palette[2] }">
    <i class="art-orbit"></i><i class="art-tile art-tile-one"></i><i class="art-tile art-tile-two"></i>
    <strong>{{ initials }}</strong><small>{{ sku || 'WEMOVE' }}</small>
  </div>
</template>
