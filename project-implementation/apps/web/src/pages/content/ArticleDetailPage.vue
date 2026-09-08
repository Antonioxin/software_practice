<script setup lang="ts">
import SketchIcon from '../../components/SketchIcon.vue'
import { computed, onBeforeUnmount, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import PublicShell from '../../components/PublicShell.vue'
import ContentState from '../../features/content/ContentState.vue'
import ContentBody from '../../features/content/ContentBody'
import { inlineImages } from '../../features/content/bodyMarkup'
import { readArticle, readProducts } from '../../features/content/api'
import { mediaUrl, messageOf } from '../../features/content/presentation'
import { formatDate, type Article, type ProductChoice } from '../../features/content/types'
import '../../features/content/style.css'
const route = useRoute(), article = ref<Article | null>(null), products = ref<ProductChoice[]>([])
const loading = ref(true), error = ref(''); let request = 0
const bodyImages = computed(() => new Set(inlineImages(article.value?.body ?? '').map(image => image.src)))
const supplementary = computed(() => article.value?.mediaIds.filter(id => !bodyImages.value.has(mediaUrl(id))) ?? [])
const cover = computed(() => supplementary.value[0] ? mediaUrl(supplementary.value[0]) : null)
const gallery = computed(() => supplementary.value.slice(1))
const readingMinutes = computed(() => {
  const doc = new DOMParser().parseFromString(article.value?.body ?? '', 'text/html')
  return Math.max(1, Math.ceil((doc.body.textContent ?? '').length / 350))
})
const existingDescription = document.querySelector<HTMLMetaElement>('meta[name="description"]')
const previousDescription = existingDescription?.content
let ownMeta: HTMLMetaElement | null = null
async function load() {
  const current = ++request; loading.value = true; error.value = ''; article.value = null
  try {
    const item = await readArticle(String(route.params.id)); if (current !== request) return
    article.value = item; document.title = `${item.title} · WEMOVE`
    const meta = existingDescription ?? ownMeta ?? document.createElement('meta'); meta.name = 'description'; meta.content = item.pageDescription || item.summary
    if (!meta.parentNode) { document.head.append(meta); ownMeta = meta }
    products.value = []
    if (item.productIds.length) { try { const choices = await readProducts(); if (current === request) products.value = choices.filter(product => item.productIds.includes(product.id)) } catch { /* Article remains readable when catalog is unavailable. */ } }
  } catch (cause) { if (current === request) error.value = messageOf(cause) } finally { if (current === request) loading.value = false }
}
watch(() => route.params.id, load, { immediate: true })
onBeforeUnmount(() => { request++; ownMeta?.remove(); if (existingDescription && previousDescription !== undefined) existingDescription.content = previousDescription })
</script>
<template><PublicShell><section class="content-public content-reading"><RouterLink class="content-text-link" to="/articles"><SketchIcon name="arrow-left" :size="22" />返回玩法灵感</RouterLink><ContentState :loading="loading" :error="error" @retry="load"><article v-if="article" class="content-story"><header><p class="content-eyebrow">PLAY NOTES / 玩法手记</p><div class="content-story-meta"><span>{{ article.category }}</span><span>{{ formatDate(article.publishedAt) }}</span><span><SketchIcon name="timer" :size="18" />约 {{ readingMinutes }} 分钟阅读</span></div><h1>{{ article.title }}</h1><p class="content-story-summary">{{ article.summary }}</p></header><img v-if="cover" class="content-story-cover" :src="cover" :alt="article.title" /><ContentBody :body="article.body" /><div v-if="gallery.length" class="content-story-gallery"><img v-for="id in gallery" :key="id" :src="mediaUrl(id)" alt="文章配图" loading="lazy" /></div><aside v-if="article.productIds.length" class="content-callout"><div><p class="content-eyebrow">KEEP PLAYING</p><h2>把这份灵感，带到户外</h2></div><div class="content-links"><RouterLink v-for="product in products" :key="product.id" :to="`/products/${product.id}`">{{ product.name }} ↗</RouterLink><RouterLink v-if="!products.length" to="/products">探索商品 ↗</RouterLink></div></aside></article></ContentState></section></PublicShell></template>
