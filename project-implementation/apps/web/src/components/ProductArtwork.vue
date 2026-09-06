<script setup lang="ts">
import { computed } from 'vue'
import SketchIcon from './SketchIcon.vue'
const props = defineProps<{ name?: string | null; sku?: string | null; compact?: boolean }>()
const backgrounds = ['#dce8f5', '#f1dc87', '#d8e6d9', '#eed2bd']
const background = computed(() => backgrounds[[...(props.sku ?? props.name ?? 'W')].reduce((sum, ch) => sum + ch.charCodeAt(0), 0) % backgrounds.length])
</script>
<template>
  <div class="product-artwork sketch-product" :class="{ compact }" role="img" :aria-label="`${name || '商品'}，手绘占位图，非产品实拍`" :style="{ background }">
    <SketchIcon name="package" class="sketch-product-icon" :size="128" />
    <strong>{{ name || 'WEMOVE' }}</strong><small>{{ sku || 'PRODUCT' }} · 商品示意</small>
  </div>
</template>
