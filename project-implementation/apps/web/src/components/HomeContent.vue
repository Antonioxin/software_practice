<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { api, ApiProblem, isDevelopmentPreview } from '../services/http'
import ProductThumbnail from './ProductThumbnail.vue'
import SketchIcon from './SketchIcon.vue'

interface HomeContent {
  banners: Array<{ id: string; title: string; imageId: string; buttonText: string; targetUrl: string }>
  recommendedProducts: Array<{ id: string; sku: string; name: string; mainImageId?: string | null; retailUnitPriceFen: number; inStock: boolean }>
  featuredArticles: Array<{ id: string; title: string; summary: string; category: string }>
}
const content = ref<HomeContent | null>(null)
const loading = ref(true)
const error = ref('')
const failedImages = ref(new Set<string>())
const links = [
  { to: '/articles', icon: 'heart', title: '留一点时间，给好玩', text: '简单的玩法灵感，让每天都有新发现。', label: '翻开玩法手记' },
  { to: '/faq', icon: 'help', title: '每个小问题，都有答案', text: '从挑选到收纳，把常见疑问一次说清。', label: '查看常见问题' },
  { to: '/downloads', icon: 'orders', title: '把实用的资料带走', text: '查找产品说明、玩法指南与合作资料。', label: '前往下载中心' },
]
function bannerImage(id: string) {
  if (import.meta.env.DEV && isDevelopmentPreview) {
    if (id === 'e1000000-0000-4000-8000-000000000001') return '/assets/products/balance-stones.png'
    if (id === 'e1000000-0000-4000-8000-000000000002') return '/assets/products/ring-toss.png'
  }
  return `/api/v1/media/${encodeURIComponent(id)}/content`
}
function safeTarget(value: string) {
  // Belt-and-braces for administrator-authored links; never bind script or network-relative URLs.
  return /^(\/(?!\/)|https?:\/\/)/i.test(value) && !/[\\\u0000-\u0020]/.test(value) ? value : '/articles'
}
async function load() {
  loading.value = true
  error.value = ''
  try { content.value = (await api<HomeContent>('/home')).data }
  catch (cause) { content.value = null; error.value = cause instanceof ApiProblem ? cause.message : '首页内容暂时无法加载。' }
  finally { loading.value = false }
}
onMounted(load)
</script>

<template>
  <div class="home-content">
    <p v-if="loading" class="home-content-status" role="status">正在整理最新的玩法与推荐…</p>
    <p v-else-if="error" class="home-content-status" role="alert">{{ error }} <button type="button" @click="load">重新加载</button></p>
    <section v-if="content?.banners.length" class="home-banners" aria-label="本期精选">
      <a v-for="banner in content.banners" :key="banner.id" class="home-banner" :href="safeTarget(banner.targetUrl)">
        <div><p class="wm-eyebrow">A LITTLE INSPIRATION</p><h2>{{ banner.title }}</h2><span>{{ banner.buttonText }} ↗</span></div>
        <img v-if="!failedImages.has(banner.imageId)" :src="bannerImage(banner.imageId)" :alt="banner.title" loading="lazy" @error="failedImages.add(banner.imageId)" />
        <SketchIcon v-else name="heart" :size="90" />
      </a>
    </section>
    <section v-if="content?.recommendedProducts.length" aria-labelledby="home-recommend-title">
      <header class="home-content-heading"><div><p class="wm-eyebrow">PICK A LITTLE JOY</p><h2 id="home-recommend-title">从一件好物，开始动起来</h2></div><RouterLink to="/products">探索全部商品 ↗</RouterLink></header>
      <div class="home-recommendations">
        <RouterLink v-for="product in content.recommendedProducts" :key="product.id" :to="`/products/${product.id}`" class="home-recommendation">
          <ProductThumbnail :sku="product.sku" :name="product.name" :main-image-id="product.mainImageId" />
          <h3>{{ product.name }}</h3><p>¥ {{ (product.retailUnitPriceFen / 100).toFixed(2) }} <span v-if="!product.inStock">暂时缺货</span></p>
        </RouterLink>
      </div>
    </section>
    <section v-if="content?.featuredArticles.length" aria-labelledby="home-articles-title">
      <header class="home-content-heading"><div><p class="wm-eyebrow">NOTES ON PLAY</p><h2 id="home-articles-title">一起玩，也一起发现</h2></div><RouterLink to="/articles">全部玩法手记 ↗</RouterLink></header>
      <div class="home-article-list">
        <RouterLink v-for="(article, index) in content.featuredArticles" :key="article.id" :to="`/articles/${article.id}`">
          <span class="home-article-number">0{{ index + 1 }}</span><div><small>{{ article.category || '玩法手记' }}</small><h3>{{ article.title }}</h3><p>{{ article.summary }}</p></div><span aria-hidden="true">↗</span>
        </RouterLink>
      </div>
    </section>
    <nav class="home-content-links" aria-label="内容与帮助">
      <RouterLink v-for="link in links" :key="link.to" :to="link.to"><SketchIcon :name="link.icon" :size="35" /><h3>{{ link.title }}</h3><p>{{ link.text }}</p><span>{{ link.label }} ↗</span></RouterLink>
    </nav>
  </div>
</template>

<style scoped>
.home-content { padding: 0 0 60px; }
.home-content > section { margin-bottom: 64px; }
.home-content-status { color: var(--muted); font-size: 13px; text-align: center; margin: 0 0 30px; }
.home-content-status button { border: 0; background: transparent; color: var(--green-dark); text-decoration: underline; padding: 8px; }
.home-banners { display: grid; grid-template-columns: repeat(auto-fit, minmax(300px, 1fr)); gap: 24px; }
.home-banner { display: flex; justify-content: space-between; align-items: center; gap: 20px; min-height: 245px; border-radius: 24px; background: #e8eee5; padding: 32px; color: var(--ink); text-decoration: none; overflow: hidden; }
.home-banner:nth-child(even) { background: #f2ead9; }
.home-banner > div { min-width: 0; }
.home-banner h2 { font-size: 25px; line-height: 1.6; margin: 14px 0 20px; }
.home-banner span { font-size: 14px; }
.home-banner img { width: 42%; max-height: 210px; object-fit: contain; border-radius: 14px; }
.home-content-heading { display: flex; align-items: end; justify-content: space-between; gap: 20px; margin-bottom: 25px; }
.home-content-heading p { margin: 0 0 10px; }
.home-content-heading h2 { margin: 0; font-size: 29px; line-height: 1.5; }
.home-content-heading > a { flex-shrink: 0; color: var(--muted); font-size: 14px; text-underline-offset: 6px; }
.home-recommendations { display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 24px; }
.home-recommendation { text-decoration: none; min-width: 0; max-width: 380px; }
.home-recommendation h3 { font-size: 18px; margin: 18px 0 9px; }
.home-recommendation p { font-size: 15px; margin: 0; }
.home-recommendation p span { margin-left: 14px; color: var(--muted); font-size: 12px; }
.home-article-list > a { display: flex; align-items: center; gap: 28px; border-top: 1px solid var(--line); padding: 25px 0; text-decoration: none; }
.home-article-list > a > div { flex: 1; min-width: 0; }
.home-article-list small { color: var(--muted); }
.home-article-list h3 { margin: 8px 0; font-size: 22px; }
.home-article-list p { color: var(--muted); font-size: 14px; line-height: 1.8; margin: 0; }
.home-article-number { font: 40px var(--font-hand); color: #91a18f; }
.home-content-links { display: grid; grid-template-columns: repeat(3, 1fr); gap: 34px; padding-top: 40px; border-top: 1px solid var(--line); }
.home-content-links > a { text-decoration: none; }
.home-content-links h3 { font-size: 19px; line-height: 1.7; margin: 16px 0 8px; }
.home-content-links p { color: var(--muted); font-size: 14px; line-height: 1.8; }
.home-content-links span { display: inline-block; margin-top: 7px; font-size: 14px; color: var(--green-dark); }
@media (max-width: 760px) {
  .home-content > section { margin-bottom: 42px; }
  .home-content-heading { align-items: start; flex-direction: column; gap: 12px; }
  .home-content-heading h2 { font-size: 25px; }
  .home-content-links { grid-template-columns: 1fr; gap: 30px; }
  .home-banner { padding: 24px; min-height: 210px; }
  .home-banner h2 { font-size: 22px; }
  .home-banners { grid-template-columns: 1fr; }
  .home-recommendations { grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 16px; }
  .home-recommendation h3 { font-size: 15px; }
  .home-article-list > a { gap: 16px; }
  .home-article-list h3 { font-size: 19px; }
}
</style>
