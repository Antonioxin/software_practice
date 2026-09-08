<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { isManagedImage, productImageSrc } from '../features/catalog/productImages'
import SketchIcon from './SketchIcon.vue'

const props = defineProps<{ sku: string; name: string; mainImageId?: string | null }>()
const failed = ref(false)
const source = computed(() => productImageSrc(props.sku, props.mainImageId))

watch(source, () => { failed.value = false })
</script>

<template>
  <div class="product-thumbnail">
    <img
      v-if="source && !failed"
      :key="source ?? sku"
      class="product-thumbnail__image"
      :src="source"
      :alt="isManagedImage(mainImageId) ? `${name}商品图片` : `${name}，AI 商品示意图，非产品实拍`"
      loading="lazy"
      decoding="async"
      @error="failed = true"
    />
    <div v-else class="product-thumbnail__placeholder" role="img" :aria-label="`${name}，暂无商品图片`">
      <SketchIcon name="package" :size="36" />
    </div>
  </div>
</template>

<style scoped>
.product-thumbnail {
  width: 100%;
  aspect-ratio: 1;
  overflow: hidden;
  border: 1px solid #e1e4dd;
  border-radius: 12px;
  background: #f5f3ee;
}

.product-thumbnail__image,
.product-thumbnail__placeholder {
  display: block;
  width: 100%;
  height: 100%;
}

.product-thumbnail__image { object-fit: contain; }
.product-thumbnail__placeholder { display: grid; place-items: center; }
</style>
