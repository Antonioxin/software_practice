<script setup lang="ts">
import SketchIcon from '../../components/SketchIcon.vue'
defineProps<{ loading: boolean; error?: string; empty?: boolean; emptyTitle?: string; emptyNote?: string }>()
defineEmits<{ retry: [] }>()
</script>
<template>
  <div v-if="loading" class="content-state" role="status"><span class="loader"></span><p>正在整理内容，请稍候…</p></div>
  <div v-else-if="error" class="content-state" role="alert"><SketchIcon name="help" :size="48" /><h2>这次没有加载成功</h2><p>{{ error }}</p><button class="secondary-button" type="button" @click="$emit('retry')"><SketchIcon name="return" :size="18" />重新加载</button></div>
  <div v-else-if="empty" class="content-state"><SketchIcon name="package" :size="56" /><h2>{{ emptyTitle || '还没有匹配的内容' }}</h2><p>{{ emptyNote || '试试调整筛选条件，或稍后再来看看。' }}</p></div>
  <slot v-else />
</template>
