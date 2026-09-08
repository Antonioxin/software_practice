<script setup lang="ts">
import SketchIcon from '../../components/SketchIcon.vue'
import { computed } from 'vue'
import type { ProductChoice } from './types'
const model = defineModel<string[]>({ required: true })
const props = withDefaults(defineProps<{ products: ProductChoice[]; label?: string; max?: number }>(), { max: 20 })
const missing = computed(() => model.value.filter(id => !props.products.some(product => product.id === id)))
</script>
<template><fieldset class="content-choice-field"><legend><SketchIcon name="package" :size="20" />{{ label || '关联商品（可多选）' }}</legend><p v-if="!products.length" class="content-note">暂无可选择的商品；已有关联仍可取消。</p><div v-if="products.length || missing.length" class="content-choice-grid"><label v-for="product in products" :key="product.id"><input v-model="model" type="checkbox" :value="product.id" :disabled="!model.includes(product.id) && (model.length >= max || (!!product.status && product.status !== 'PUBLISHED'))" /><span>{{ product.name }}<small v-if="product.status && product.status !== 'PUBLISHED'"> · {{ product.status === 'DRAFT' ? '草稿' : product.status === 'UNLISTED' ? '已下架' : product.status }}</small></span></label><label v-for="id in missing" :key="id"><input v-model="model" type="checkbox" :value="id" /><span>已选商品（当前不可访问）<small> · {{ id }}</small></span></label></div><small>已选择 {{ model.length }} 项；公开页面仅展示仍可访问的商品。</small></fieldset></template>
