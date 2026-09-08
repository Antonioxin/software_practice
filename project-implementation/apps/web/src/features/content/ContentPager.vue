<script setup lang="ts">
import SketchIcon from '../../components/SketchIcon.vue'
import type { PageMeta } from '../../types'
defineProps<{ meta: PageMeta; loading?: boolean }>()
defineEmits<{ change: [page: number] }>()
</script>
<template><nav v-if="meta.totalPages > 1" class="pagination content-pagination" aria-label="内容分页"><button type="button" :disabled="loading || meta.page <= 1" @click="$emit('change', meta.page - 1)"><SketchIcon name="arrow-left" :size="18" />上一页</button><span>第 {{ meta.page }} / {{ meta.totalPages }} 页 · {{ meta.totalItems }} 条</span><button type="button" :disabled="loading || meta.page >= meta.totalPages" @click="$emit('change', meta.page + 1)">下一页<SketchIcon name="arrow-left" :size="18" class="content-next-icon" /></button></nav></template>
