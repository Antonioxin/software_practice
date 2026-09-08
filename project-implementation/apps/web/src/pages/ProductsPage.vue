<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import SketchIcon from '../components/SketchIcon.vue'
import PublicShell from '../components/PublicShell.vue'
import PolaroidMotion from '../components/PolaroidMotion.vue'
import { buildCatalogQuery, formatAgeRange, formatCny } from '../features/catalog/presentation'
import { productIllustrationSrc } from '../features/catalog/productImages'
import { api, ApiProblem } from '../services/http'
import type { Category, PageMeta, ProductCard, ProductOptions } from '../types'

const route = useRoute()
const router = useRouter()
const products = ref<ProductCard[]>([])
const failedIllustrations = ref(new Set<string>())
const categories = ref<Category[]>([])
const options = ref<ProductOptions>({ playTypes: [], scenes: [] })
const meta = ref<PageMeta>({ page: 1, pageSize: 12, totalItems: 0, totalPages: 0 })
const loading = ref(true)
const error = ref('')
const referenceLoading = ref(false)
const referenceError = ref('')
const filters = reactive({ keyword: '', categoryId: '', age: '', playType: '', scene: '', sort: 'recommended' })
const collectionTitle = computed(() => {
  const selected = typeof route.query.categoryId === 'string' ? route.query.categoryId : ''
  return categories.value.find(item => item.id === selected)?.name ?? (selected ? '筛选结果' : '全部商品')
})
const activeFilterCount = computed(() => ['keyword', 'age', 'playType', 'scene'].filter(key => route.query[key]).length)
const tileColors = ['#b5d3e8', '#f2efe8', '#dfaa80', '#b8ccb1', '#dfbec2', '#bcbcd7']
let latestRequest = 0

function fromQuery() {
  filters.keyword = typeof route.query.keyword === 'string' ? route.query.keyword : ''
  filters.categoryId = typeof route.query.categoryId === 'string' ? route.query.categoryId : ''
  filters.age = typeof route.query.age === 'string' ? route.query.age : ''
  filters.playType = typeof route.query.playType === 'string' ? route.query.playType : ''
  filters.scene = typeof route.query.scene === 'string' ? route.query.scene : ''
  filters.sort = typeof route.query.sort === 'string' ? route.query.sort : 'recommended'
}

async function loadReferenceData() {
  referenceLoading.value = true
  referenceError.value = ''
  const [categoryResult, optionResult] = await Promise.allSettled([
    api<Category[]>('/categories'), api<ProductOptions>('/product-options'),
  ])
  if (categoryResult.status === 'fulfilled') categories.value = categoryResult.value.data
  if (optionResult.status === 'fulfilled') options.value = optionResult.value.data
  if (categoryResult.status === 'rejected' || optionResult.status === 'rejected') {
    referenceError.value = '部分筛选选项暂时无法加载。'
  }
  referenceLoading.value = false
}

async function loadProducts() {
  const request = ++latestRequest
  loading.value = true
  error.value = ''
  fromQuery()
  const page = typeof route.query.page === 'string' ? Number(route.query.page) : 1
  const params = buildCatalogQuery(filters, Number.isInteger(page) && page > 0 ? page : 1)
  try {
    const result = await api<ProductCard[]>(`/products?${params}`)
    if (request !== latestRequest) return
    products.value = result.data
    meta.value = result.meta ?? { page: 1, pageSize: 12, totalItems: result.data.length, totalPages: result.data.length ? 1 : 0 }
  } catch (cause) {
    if (request !== latestRequest) return
    error.value = cause instanceof ApiProblem ? cause.problem.detail : '商品暂时无法加载，请稍后重试。'
  } finally {
    if (request === latestRequest) loading.value = false
  }
}

function applyQuery(query: Record<string, string>) {
  // Keep explicit preview context when changing only business filters.
  const next = { ...query }
  for (const key of ['preview', 'role', 'state']) {
    const value = route.query[key]
    if (typeof value === 'string') next[key] = value
  }
  const target = { path: route.path, query: next }
  if (router.resolve(target).fullPath === route.fullPath) void loadProducts()
  else void router.push(target)
}

function search() {
  const query: Record<string, string> = {}
  for (const [key, value] of Object.entries(filters)) {
    const normalized = String(value).trim()
    if (normalized) query[key] = normalized
  }
  applyQuery(query)
}
function selectCategory(id: string) {
  filters.categoryId = id
  search()
}
function clearFilters() {
  Object.assign(filters, { keyword: '', categoryId: '', age: '', playType: '', scene: '', sort: 'recommended' })
  applyQuery({})
}
function pageTo(page: number) {
  void router.push({ query: { ...route.query, page: String(page) } })
}
function tileColor(product: ProductCard) {
  const index = [...product.sku].reduce((sum, character) => sum + character.charCodeAt(0), 0)
  return tileColors[index % tileColors.length]
}
function illustrationSrc(product: ProductCard) {
  const src = productIllustrationSrc(product.sku)
  return src && !failedIllustrations.value.has(src) ? src : null
}
function illustrationFailed(sku: string) {
  const src = productIllustrationSrc(sku)
  if (src) failedIllustrations.value.add(src)
}

onMounted(loadReferenceData)
watch(() => route.query, loadProducts, { immediate: true })
</script>

<template>
  <PublicShell>
    <section class="shop-layout" aria-labelledby="shop-title">
      <aside class="shop-directory">
        <div class="shop-directory-inner">
          <header class="shop-heading">
            <p>WEMOVE / THE PLAY SHOP</p>
            <h1 id="shop-title">探索商品</h1>
            <span>把每一次移动，<br />变成一次发现。</span>
          </header>

          <nav class="shop-categories" aria-label="商品分类">
            <p class="shop-section-label">按分类探索</p>
            <button type="button" :aria-pressed="!filters.categoryId" @click="selectCategory('')"><span>全部商品</span><span aria-hidden="true">↗</span></button>
            <button v-for="category in categories" :key="category.id" type="button" :aria-pressed="filters.categoryId === category.id" @click="selectCategory(category.id)"><span>{{ category.name }}</span><span aria-hidden="true">↗</span></button>
            <p v-if="referenceLoading" class="shop-reference-note" role="status">正在读取分类与选项…</p>
          </nav>

          <form class="shop-filter-form" aria-label="商品筛选" @submit.prevent="search">
            <label class="shop-search">
              <span class="sr-only">搜索商品名称或 SKU</span>
              <input v-model="filters.keyword" maxlength="100" placeholder="搜索名称或 SKU" />
              <button type="submit" aria-label="搜索商品"><SketchIcon name="search" :size="23" /></button>
            </label>
            <details class="shop-filter-details">
              <summary><span>更多筛选 <small v-if="activeFilterCount">{{ activeFilterCount }}</small></span><SketchIcon name="filter" :size="23" /></summary>
              <div class="shop-filter-fields">
                <label><span>适用年龄</span><input v-model="filters.age" type="number" min="0" max="18" placeholder="例如 6" /></label>
                <label><span>玩法</span><select v-model="filters.playType"><option value="">全部玩法</option><option v-for="item in options.playTypes" :key="item.value" :value="item.value">{{ item.label }}</option></select></label>
                <label><span>使用场景</span><select v-model="filters.scene"><option value="">全部场景</option><option v-for="item in options.scenes" :key="item.value" :value="item.value">{{ item.label }}</option></select></label>
                <button class="shop-apply" type="submit">应用筛选 <span aria-hidden="true">↗</span></button>
                <p>室内或户外筛选，均包含两种场景皆可的商品。</p>
              </div>
            </details>
            <button class="shop-clear" type="button" @click="clearFilters">清除全部筛选</button>
          </form>
          <p v-if="referenceError" class="shop-reference-error" role="alert">{{ referenceError }}<button type="button" :disabled="referenceLoading" @click="loadReferenceData">重新读取选项</button></p>
        </div>
      </aside>

      <div class="shop-collection">
        <header class="shop-toolbar">
          <div><h2>{{ collectionTitle }}</h2><span v-if="!loading && !error" aria-live="polite">{{ meta.totalItems }} 件商品</span></div>
          <label><span>排序</span><select v-model="filters.sort" @change="search"><option value="recommended">推荐顺序</option><option value="priceAsc">价格从低到高</option><option value="priceDesc">价格从高到低</option></select></label>
        </header>
        <div v-if="loading" class="shop-state" role="status"><SketchIcon name="package" :size="66" /><h2>正在整理商品…</h2></div>
        <div v-else-if="error" class="shop-state" role="alert"><SketchIcon name="help" :size="66" /><h2>暂时无法取得商品</h2><p>{{ error }}</p><button class="shop-state-action" type="button" @click="loadProducts">重新加载</button></div>
        <div v-else-if="!products.length" class="shop-state"><SketchIcon name="search" :size="66" /><h2>未找到符合条件的商品</h2><p>试试减少筛选条件，或返回全部商品。</p><button class="shop-state-action" type="button" @click="clearFilters">查看全部商品</button></div>
        <div v-else class="shop-grid" aria-label="商品列表">
          <RouterLink v-for="(product, index) in products" :key="product.id" class="shop-tile" :aria-label="`${product.name}，${formatCny(product.retailUnitPriceFen)}，查看商品详情`" :to="{ path: `/products/${product.id}`, query: route.query }">
            <PolaroidMotion>
              <div class="shop-polaroid">
                <div class="shop-photo-window" :style="{ backgroundColor: tileColor(product) }">
                  <img v-if="illustrationSrc(product)" class="shop-product-image" :src="illustrationSrc(product) ?? undefined" :alt="`${product.name}的 AI 生成商品示意图（非实拍）`" :loading="index < 2 ? 'eager' : 'lazy'" decoding="async" @error="illustrationFailed(product.sku)" />
                  <div v-else class="shop-photo-placeholder" aria-hidden="true"><SketchIcon name="package" :size="128" /><span>商品示意</span></div>
                </div>
                <img class="shop-polaroid-paper" src="/assets/frames/polaroid-product.png" width="1149" height="1369" alt="" aria-hidden="true" draggable="false" />
                <h3 class="shop-name-label" :title="product.name"><span>{{ product.name }}</span></h3>
              </div>
            </PolaroidMotion>
            <div class="shop-product-info">
              <p v-if="illustrationSrc(product)" class="shop-image-note">AI 商品示意 · 非实拍</p>
              <div class="shop-tile-top"><span>{{ product.category.name }}</span><span>{{ formatAgeRange(product.ageMin, product.ageMax) }}</span></div>
              <p class="shop-product-summary">{{ product.summary }}</p>
              <div class="shop-tile-bottom"><div><span :class="{ 'shop-out-of-stock': !product.inStock }">{{ product.inStock ? '有货' : '暂时缺货' }}</span><span class="shop-sku">SKU {{ product.sku }}</span></div><strong>{{ formatCny(product.retailUnitPriceFen) }}</strong></div>
            </div>
          </RouterLink>
        </div>
        <nav v-if="!loading && !error && meta.totalPages > 1" class="shop-pagination" aria-label="商品分页">
          <button type="button" :disabled="meta.page <= 1" @click="pageTo(meta.page - 1)">← 上一页</button>
          <span>第 {{ meta.page }} / {{ meta.totalPages }} 页</span>
          <button type="button" :disabled="meta.page >= meta.totalPages" @click="pageTo(meta.page + 1)">下一页 →</button>
        </nav>
      </div>
    </section>
  </PublicShell>
</template>

<style scoped>
.shop-layout { --shop-yellow: #ffe72c; --shop-ink: #242a26; display: grid; grid-template-columns: 250px minmax(0, 1fr); width: min(1440px, calc(100% - 48px)); margin: 28px auto 54px; background: #fff; color: var(--shop-ink); }
.shop-directory { min-width: 0; background: var(--shop-yellow); }
.shop-directory-inner { position: sticky; top: calc(var(--header-height) + 20px); max-height: calc(100dvh - var(--header-height) - 40px); overflow-y: auto; padding: 35px 27px; scrollbar-width: thin; scrollbar-color: #242a2655 transparent; }
.shop-heading p { margin: 0 0 17px; font: 600 15px/1.4 var(--font-display); letter-spacing: 1.2px; }
.shop-heading h1 { margin: 0 0 16px; font: 500 36px/1.35 var(--font-body); letter-spacing: -1px; }
.shop-heading > span { display: block; font-size: 16px; line-height: 1.9; }
.shop-categories { display: grid; gap: 3px; margin: 36px 0 28px; }
.shop-section-label { margin: 0 0 12px; font-size: 14px; color: #4d4d27; }
.shop-categories button { display: flex; align-items: center; justify-content: space-between; gap: 12px; width: calc(100% + 24px); min-height: 42px; margin-inline: -12px; padding: 8px 12px; border: 0; background: transparent; color: var(--shop-ink); font-size: 17px; text-align: left; transition: background-color 160ms ease, color 160ms ease; }
.shop-categories button:hover, .shop-categories button:focus-visible { background: var(--shop-ink); color: var(--shop-yellow); }
.shop-categories button:focus-visible { outline: 2px solid var(--shop-ink); outline-offset: 3px; }
.shop-categories button > span:first-child { overflow-wrap: anywhere; }
.shop-categories button > span:last-child { flex-shrink: 0; opacity: 0; transition: opacity 160ms ease; }
.shop-categories button[aria-pressed="true"] { font-weight: 500; text-decoration: underline; text-underline-offset: 6px; text-decoration-thickness: 1px; }
.shop-categories button[aria-pressed="true"] > span:last-child, .shop-categories button:hover > span:last-child, .shop-categories button:focus-visible > span:last-child { opacity: 1; }
.shop-filter-form { border-top: 1px solid #242a2650; padding-top: 20px; }
.shop-search { display: flex; align-items: center; gap: 8px; padding: 8px 0; border-bottom: 1px solid #242a2666; }
.shop-search input { min-width: 0; width: 100%; border: 0; padding: 6px 0; background: transparent; color: var(--shop-ink); font-size: 16px; }
.shop-search input::placeholder { color: #52502e; opacity: 1; }
.shop-search button { display: grid; place-items: center; flex-shrink: 0; width: 36px; height: 36px; padding: 5px; border: 0; background: none; }
.shop-filter-details { border-bottom: 1px solid #242a2650; }
.shop-filter-details summary { display: flex; align-items: center; justify-content: space-between; min-height: 58px; gap: 12px; font-size: 16px; cursor: pointer; list-style: none; }
.shop-filter-details summary::-webkit-details-marker { display: none; }
.shop-filter-details summary small { margin-left: 6px; font: 600 15px var(--font-display); }
.shop-filter-fields { display: grid; gap: 17px; padding: 3px 0 20px; }
.shop-filter-fields label { display: grid; gap: 7px; font-size: 14px; }
.shop-filter-fields input, .shop-filter-fields select { min-width: 0; width: 100%; min-height: 42px; padding: 8px 9px; border: 1px solid #242a2655; border-radius: 0; color: var(--shop-ink); background: #ffffff66; font-size: 16px; }
.shop-apply { display: flex; align-items: center; justify-content: space-between; gap: 10px; min-height: 44px; padding: 10px 12px; border: 1px solid var(--shop-ink); background: var(--shop-ink); color: #fff; font-size: 15px; }
.shop-filter-fields p { margin: 0; font-size: 13px; line-height: 1.7; color: #52502e; }
.shop-clear { margin-top: 17px; padding: 7px 0; border: 0; color: #41442b; background: none; font-size: 14px; text-decoration: underline; text-underline-offset: 4px; }
.shop-reference-note, .shop-reference-error { margin: 12px 0 0; font-size: 14px; line-height: 1.8; }
.shop-reference-error button { display: block; border: 0; padding: 6px 0; background: transparent; color: inherit; text-decoration: underline; font-size: 14px; }
.shop-collection { min-width: 0; }
.shop-toolbar { display: flex; align-items: center; justify-content: space-between; gap: 24px; min-height: 84px; padding: 20px 30px; border-bottom: 1px solid #e4e5df; }
.shop-toolbar > div { display: flex; align-items: baseline; gap: 16px; flex-wrap: wrap; min-width: 0; }
.shop-toolbar h2 { margin: 0; font-size: 23px; font-weight: 500; overflow-wrap: anywhere; }
.shop-toolbar > div > span { font-size: 14px; color: #62685e; }
.shop-toolbar label { display: flex; align-items: center; gap: 12px; font-size: 14px; white-space: nowrap; }
.shop-toolbar select { width: 155px; min-height: 42px; border: 0; border-bottom: 1px solid #a0a79c; border-radius: 0; padding: 8px 2px; background: transparent; color: var(--shop-ink); font-size: 15px; }
.shop-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); align-items: start; gap: 48px 32px; padding: 30px 32px 48px; background: #f2f1ec; }
.shop-tile { display: flex; flex-direction: column; min-width: 0; width: 100%; max-width: 460px; justify-self: center; container-type: inline-size; color: var(--shop-ink); text-decoration: none; }
.shop-tile:focus-visible { outline: 3px solid #294c91; outline-offset: 6px; border-radius: 3px; }
.shop-polaroid { position: relative; isolation: isolate; width: 100%; aspect-ratio: 1149 / 1369; }
.shop-polaroid-paper { position: absolute; z-index: 1; inset: 0; width: 100%; height: 100%; pointer-events: none; }
/* Coordinates follow the supplied 1149 × 1369 PNG. The window extends 3px under the paper edge. */
.shop-photo-window { position: absolute; left: 11.9234%; top: 9.1308%; width: 76.5013%; height: 66.7641%; display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 12px; overflow: hidden; }
.shop-product-image { display: block; width: 100%; height: 100%; object-fit: cover; object-position: center; }
.shop-photo-placeholder { display: flex; width: 100%; height: 100%; flex-direction: column; align-items: center; justify-content: center; gap: 12px; }
.shop-photo-window .sketch-icon { width: 34%; height: auto; max-width: 128px; }
.shop-photo-placeholder > span { color: #3f493e; font-size: 12px; letter-spacing: 1px; }
.shop-name-label { position: absolute; z-index: 2; left: 13.0548%; top: 78.8897%; width: 74.4125%; height: 13.0022%; display: flex; align-items: center; justify-content: center; margin: 0; font: 400 25px/1.25 var(--font-hand); font-size: clamp(16px, 6cqi, 28px); text-align: center; }
.shop-name-label > span { min-width: 0; overflow-wrap: anywhere; display: -webkit-box; -webkit-box-orient: vertical; -webkit-line-clamp: 2; overflow: hidden; }
.shop-product-info { display: flex; flex-direction: column; flex: 1; padding: 5px 7% 0; }
.shop-image-note { margin: 0 0 10px; color: var(--muted); font-size: 12px; line-height: 1.6; }
.shop-tile-top { display: flex; align-items: start; justify-content: space-between; gap: 16px; font-size: 14px; line-height: 1.6; }
.shop-tile-top > span:first-child { min-width: 0; overflow-wrap: anywhere; }
.shop-tile-top > span:last-child { flex-shrink: 0; }
.shop-product-summary { margin: 12px 0 18px; font-size: 15px; line-height: 1.8; color: #3f493e; overflow-wrap: anywhere; }
.shop-tile-bottom { display: flex; align-items: center; justify-content: space-between; gap: 16px; margin-top: auto; padding-top: 13px; border-top: 1px solid #242a262b; }
.shop-tile-bottom > div { display: grid; gap: 5px; min-width: 0; font-size: 14px; }
.shop-sku { color: #3f493e; font: 500 13px/1.4 var(--font-display); overflow-wrap: anywhere; }
.shop-tile-bottom .shop-out-of-stock { text-decoration: underline; text-underline-offset: 4px; }
.shop-tile-bottom strong { flex-shrink: 0; font: 600 29px/1.2 var(--font-display); }
.shop-state { display: grid; justify-items: center; align-content: center; min-height: 540px; padding: 48px 25px; text-align: center; background: #f5f4ef; }
.shop-state h2 { margin: 25px 0 10px; font-size: 25px; font-weight: 500; line-height: 1.6; }
.shop-state p { max-width: 500px; margin: 0; color: var(--muted); font-size: 16px; line-height: 1.8; }
.shop-state-action { min-height: 44px; margin-top: 24px; padding: 10px 22px; border: 1px solid #252b25; border-radius: 0; background: var(--shop-yellow); color: var(--shop-ink); font-size: 15px; }
.shop-pagination { display: flex; align-items: center; justify-content: space-between; gap: 16px; padding: 26px 30px; font-size: 14px; }
.shop-pagination button { min-height: 42px; border: 0; padding: 8px 0; background: none; color: var(--shop-ink); }
@media (max-width: 1150px) {
  .shop-layout { grid-template-columns: 220px minmax(0, 1fr); }
  .shop-directory-inner { padding: 30px 22px; }
  .shop-grid { gap: 40px 24px; padding: 25px 24px 40px; }
  .shop-toolbar { gap: 16px; padding: 18px 23px; }
}
@media (max-width: 900px) {
  .shop-layout { width: calc(100% - 32px); grid-template-columns: 190px minmax(0, 1fr); }
  .shop-directory-inner { padding: 25px 18px; }
  .shop-heading h1 { font-size: 30px; }
  .shop-heading p { font-size: 13px; }
  .shop-heading > span { font-size: 14px; }
  .shop-categories button { font-size: 16px; }
  .shop-toolbar { flex-wrap: wrap; gap: 10px; }
  .shop-toolbar > div { width: 100%; justify-content: space-between; }
  .shop-toolbar label { margin-left: auto; }
  .shop-grid { grid-template-columns: minmax(0, 1fr); gap: 40px; }
  .shop-tile-top { flex-wrap: wrap; gap: 4px; font-size: 13px; }
  .shop-product-summary { font-size: 14px; }
}
@media (max-width: 700px) {
  .shop-layout { width: calc(100% - 24px); margin: 20px auto 36px; grid-template-columns: minmax(0, 1fr); }
  .shop-directory-inner { position: static; max-height: none; padding: 25px; overflow: visible; }
  .shop-heading p { margin-bottom: 9px; font-size: 14px; }
  .shop-heading h1 { margin-bottom: 0; font-size: 31px; }
  .shop-heading > span { display: none; }
  .shop-categories { display: flex; flex-wrap: wrap; gap: 8px 18px; margin: 20px 0; }
  .shop-section-label { flex-basis: 100%; margin-bottom: 0; }
  .shop-categories button { width: auto; min-height: 36px; margin-inline: 0; padding: 5px 10px; font-size: 15px; gap: 5px; }
  .shop-reference-note { flex-basis: 100%; }
  .shop-filter-form { padding-top: 10px; }
  .shop-search { padding: 3px 0; }
  .shop-filter-details summary { min-height: 50px; }
  .shop-filter-fields { grid-template-columns: 1fr 1fr; gap: 15px; }
  .shop-filter-fields p { grid-column: 1 / -1; }
  .shop-apply { align-self: end; }
  .shop-clear { margin-top: 9px; }
  .shop-toolbar { min-height: 74px; padding: 20px; }
  .shop-toolbar > div { width: auto; gap: 12px; }
  .shop-toolbar h2 { font-size: 22px; }
  .shop-toolbar label { gap: 8px; }
  .shop-toolbar select { width: 140px; }
  .shop-grid { padding: 24px 18px 36px; }
  .shop-state { min-height: 380px; padding: 38px 22px; }
  .shop-state h2 { font-size: 22px; }
  .shop-pagination { gap: 10px; padding: 20px; font-size: 13px; }
}
@media (max-width: 520px) {
  .shop-grid { gap: 36px; padding: 20px 12px 32px; }
  .shop-tile-top { font-size: 14px; }
  .shop-product-summary { font-size: 15px; }
  .shop-toolbar > div { width: 100%; }
  .shop-toolbar label { width: 100%; justify-content: space-between; margin: 0; }
}
</style>
