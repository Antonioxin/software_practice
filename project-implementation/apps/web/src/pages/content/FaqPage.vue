<script setup lang="ts">
import SketchIcon from '../../components/SketchIcon.vue'
import { onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import PublicShell from '../../components/PublicShell.vue'
import ContentHero from '../../features/content/ContentHero.vue'
import ContentState from '../../features/content/ContentState.vue'
import ContentPager from '../../features/content/ContentPager.vue'
import ContentBody from '../../features/content/ContentBody'
import { readFaqs, readProducts } from '../../features/content/api'
import { messageOf } from '../../features/content/presentation'
import type { Faq, PageResult, ProductChoice } from '../../features/content/types'
import '../../features/content/style.css'
const route = useRoute()
const result = ref<PageResult<Faq> | null>(null), products = ref<ProductChoice[]>([])
const loading = ref(true), error = ref(''), choicesError = ref(''), keyword = ref(''), category = ref(''), productId = ref(typeof route.query.productId === 'string' ? route.query.productId : '')
let request = 0
async function load(page = 1) { const current = ++request; loading.value = true; error.value = ''; try { const data = await readFaqs({ keyword: keyword.value, category: category.value, productId: productId.value, page, pageSize: 20 }); if (current === request) result.value = data } catch (cause) { if (current === request) error.value = messageOf(cause) } finally { if (current === request) loading.value = false } }
onMounted(async () => { void load(); try { products.value = await readProducts() } catch { choicesError.value = '商品筛选暂时不可用，仍可按关键词和分类查找。' } })
</script>
<template><PublicShell><ContentHero eyebrow="A LITTLE HELP" title="Good questions." subtitle="关于玩耍，你可能想知道" note="从第一次开箱，到下一场户外游戏，在这里找到一点小帮助。" icon="help" /><section class="content-public"><form class="content-filters" @submit.prevent="load(1)"><label>问题关键词<input v-model="keyword" type="search" class="wm-search-input" placeholder="比如：清洁、安装、保养" maxlength="100" /></label><label>问题分类<input v-model="category" placeholder="例如：使用与保养" maxlength="50" /></label><label>关联商品<select v-model="productId"><option value="">所有商品</option><option v-for="product in products" :key="product.id" :value="product.id">{{ product.name }}</option></select></label><button class="primary-button" type="submit">查找答案</button></form><p v-if="choicesError" class="content-note">{{ choicesError }}</p><ContentState :loading="loading" :error="error" :empty="!result?.items.length" empty-title="暂时没有找到这个问题" empty-note="可以换个关键词，或通过下方入口联系我们。" @retry="load()"><div class="content-faq-list"><details v-for="(faq, index) in result?.items" :key="faq.id"><summary><span class="content-faq-number">{{ String(index + 1 + ((result?.meta.page || 1) - 1) * 20).padStart(2, '0') }}</span><span><small>{{ faq.category }}</small>{{ faq.question }}</span><span class="content-faq-toggle" aria-hidden="true"><SketchIcon name="plus" :size="28" class="content-faq-expand" /><SketchIcon name="minus" :size="28" class="content-faq-collapse" /></span></summary><div class="content-faq-answer"><ContentBody :body="faq.answer" /><div v-if="faq.productIds.length" class="content-links"><RouterLink v-for="product in products.filter(p => faq.productIds.includes(p.id))" :key="product.id" :to="`/products/${product.id}`">{{ product.name }} ↗</RouterLink></div></div></details></div><ContentPager v-if="result" :meta="result.meta" @change="load" /></ContentState><aside class="content-callout"><div><p class="content-eyebrow">WE'RE HERE</p><h2>还有一些小疑问？</h2><p>告诉我们，一起找到答案。</p></div><RouterLink to="/contact" class="primary-button"><SketchIcon name="chat" :size="22" />联系 WEMOVE ↗</RouterLink></aside></section></PublicShell></template>
