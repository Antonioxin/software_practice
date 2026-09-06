<script setup lang="ts">
import { onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import PublicShell from '../components/PublicShell.vue'
import ProductArtwork from '../components/ProductArtwork.vue'
import { buildCatalogQuery, formatAgeRange, formatCny } from '../features/catalog/presentation'
import { api, ApiProblem } from '../services/http'
import type { Category, PageMeta, ProductCard, ProductOptions } from '../types'

const route = useRoute()
const router = useRouter()
const products = ref<ProductCard[]>([])
const categories = ref<Category[]>([])
const options = ref<ProductOptions>({ playTypes: [], scenes: [] })
const meta = ref<PageMeta>({ page: 1, pageSize: 12, totalItems: 0, totalPages: 0 })
const loading = ref(true)
const error = ref('')
const filters = reactive({ keyword: '', categoryId: '', age: '', playType: '', scene: '', sort: 'recommended' })

function fromQuery() {
  filters.keyword = typeof route.query.keyword === 'string' ? route.query.keyword : ''
  filters.categoryId = typeof route.query.categoryId === 'string' ? route.query.categoryId : ''
  filters.age = typeof route.query.age === 'string' ? route.query.age : ''
  filters.playType = typeof route.query.playType === 'string' ? route.query.playType : ''
  filters.scene = typeof route.query.scene === 'string' ? route.query.scene : ''
  filters.sort = typeof route.query.sort === 'string' ? route.query.sort : 'recommended'
}

async function loadReferenceData() {
  try {
    const [categoryResult, optionResult] = await Promise.all([
      api<Category[]>('/categories'), api<ProductOptions>('/product-options'),
    ])
    categories.value = categoryResult.data
    options.value = optionResult.data
  } catch { /* list request below still provides a useful retry state */ }
}

async function loadProducts() {
  loading.value = true
  error.value = ''
  fromQuery()
  const page = typeof route.query.page === 'string' ? Number(route.query.page) : 1
  const params = buildCatalogQuery(filters, Number.isInteger(page) && page > 0 ? page : 1)
  try {
    const result = await api<ProductCard[]>(`/products?${params}`)
    products.value = result.data
    if (result.meta) meta.value = result.meta
  } catch (cause) {
    error.value = cause instanceof ApiProblem ? cause.problem.detail : '商品暂时无法加载，请稍后重试。'
  } finally { loading.value = false }
}

function search() {
  const query: Record<string, string> = {}
  for (const [key, value] of Object.entries(filters)) if (value) query[key] = value
  router.push({ query })
}

function clearFilters() {
  Object.assign(filters, { keyword: '', categoryId: '', age: '', playType: '', scene: '', sort: 'recommended' })
  router.push({ query: {} })
}

function pageTo(page: number) {
  router.push({ query: { ...route.query, page: String(page) } })
}

function ageLabel(product: ProductCard) { return formatAgeRange(product.ageMin, product.ageMax) }

onMounted(loadReferenceData)
watch(() => route.query, loadProducts, { immediate: true })
</script>

<template>
  <PublicShell>
    <section class="catalog-hero">
      <div><p>MOVE / PLAY / GROW</p><h1>把每一次移动<br />变成一次发现</h1></div>
      <p>为家庭、学校与活动空间设计的运动游戏。按年龄、玩法和场景，找到刚刚好的挑战。</p>
      <span aria-hidden="true">18</span>
    </section>

    <section class="catalog-browser">
      <form class="catalog-filters" @submit.prevent="search">
        <header><p>PRODUCT FINDER</p><h2>筛选商品</h2><button type="button" @click="clearFilters">清除全部</button></header>
        <label class="catalog-search"><span class="sr-only">搜索商品名称或 SKU</span><input v-model="filters.keyword" maxlength="100" placeholder="搜索商品名称或 SKU" /><button type="submit">搜索</button></label>
        <div class="filter-stack">
          <label><span>分类</span><select v-model="filters.categoryId"><option value="">全部分类</option><option v-for="item in categories" :key="item.id" :value="item.id">{{ item.name }}</option></select></label>
          <label><span>适用年龄</span><input v-model="filters.age" type="number" min="0" max="18" placeholder="例如 6" /></label>
          <label><span>玩法</span><select v-model="filters.playType"><option value="">全部玩法</option><option v-for="item in options.playTypes" :key="item.value" :value="item.value">{{ item.label }}</option></select></label>
          <label><span>使用场景</span><select v-model="filters.scene"><option value="">全部场景</option><option v-for="item in options.scenes" :key="item.value" :value="item.value">{{ item.label }}</option></select></label>
          <button class="primary-button" type="submit">应用筛选 <span>→</span></button>
        </div>
        <aside><strong>场景筛选说明</strong><p>“室内”与“户外”会同时包含两种场景皆可的商品。</p></aside>
      </form>

      <div class="catalog-results">
        <header class="results-toolbar">
          <div><p>CURATED FOR MOTION</p><h2>全部商品 <span>{{ meta.totalItems }}</span></h2></div>
          <label>排序<select v-model="filters.sort" @change="search"><option value="recommended">推荐顺序</option><option value="priceAsc">价格从低到高</option><option value="priceDesc">价格从高到低</option></select></label>
        </header>
        <div v-if="loading" class="state-panel" aria-live="polite"><span class="loader"></span><p>正在整理商品…</p></div>
        <div v-else-if="error" class="state-panel" role="alert"><h2>暂时无法取得商品</h2><p>{{ error }}</p><button class="secondary-button" type="button" @click="loadProducts">重新加载</button></div>
        <div v-else-if="!products.length" class="state-panel"><h2>未找到符合条件的商品</h2><p>试试减少筛选条件，或返回全部商品。</p><button class="secondary-button" type="button" @click="clearFilters">查看全部商品</button></div>
        <div v-else class="product-grid">
          <RouterLink v-for="product in products" :key="product.id" class="product-card" :to="{ path: `/products/${product.id}`, query: route.query }">
            <ProductArtwork :name="product.name" :sku="product.sku" compact />
            <div class="product-card-copy">
              <p>{{ product.category.name }} · {{ ageLabel(product) }}</p>
              <h3>{{ product.name }}</h3>
              <span>{{ product.summary }}</span>
              <footer><strong>{{ formatCny(product.retailUnitPriceFen) }}</strong><em :class="{ empty: !product.inStock }">{{ product.inStock ? '有货' : '暂时缺货' }}</em></footer>
            </div>
          </RouterLink>
        </div>
        <nav v-if="meta.totalPages > 1" class="catalog-pagination" aria-label="商品分页">
          <button type="button" :disabled="meta.page <= 1" @click="pageTo(meta.page - 1)">← 上一页</button>
          <span>第 {{ meta.page }} / {{ meta.totalPages }} 页</span>
          <button type="button" :disabled="meta.page >= meta.totalPages" @click="pageTo(meta.page + 1)">下一页 →</button>
        </nav>
      </div>
    </section>
  </PublicShell>
</template>
