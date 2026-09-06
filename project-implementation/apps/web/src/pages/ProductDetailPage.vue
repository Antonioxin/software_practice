<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import PublicShell from '../components/PublicShell.vue'
import ProductArtwork from '../components/ProductArtwork.vue'
import { formatAgeRange, formatCny } from '../features/catalog/presentation'
import { api, ApiProblem } from '../services/http'
import type { PublicProduct } from '../types'

const route = useRoute()
const router = useRouter()
const product = ref<PublicProduct | null>(null)
const loading = ref(true)
const error = ref('')
const missing = ref(false)
const quantity = ref(1)

async function load() {
  loading.value = true
  error.value = ''
  missing.value = false
  try {
    product.value = (await api<PublicProduct>(`/products/${route.params.id}`)).data
    document.title = `${product.value.name} · WEMOVE`
  } catch (cause) {
    if (cause instanceof ApiProblem && cause.problem.status === 404) missing.value = true
    else error.value = cause instanceof ApiProblem ? cause.problem.detail : '商品详情暂时无法加载。'
  } finally { loading.value = false }
}

function back() {
  router.push({ path: '/products', query: route.query })
}

function addToCart() {
  ElMessage.info('购物车由角色 C 模块接入；当前商品与数量已经准备好。')
}

const playLabels: Record<string, string> = { BALANCE: '平衡能力', COORDINATION: '协调训练', THROWING: '投掷与瞄准', TEAM_PLAY: '团队游戏', OUTDOOR_EXPLORATION: '户外探索' }
const sceneLabels: Record<string, string> = { INDOOR: '室内', OUTDOOR: '户外', BOTH: '室内与户外' }

onMounted(load)
</script>

<template>
  <PublicShell>
    <div class="product-detail-page">
      <button class="detail-back" type="button" @click="back">← 返回商品列表与当前筛选</button>
      <div v-if="loading" class="state-panel"><span class="loader"></span><p>正在加载商品详情…</p></div>
      <div v-else-if="missing" class="state-panel"><h1>没有找到这个商品</h1><p>链接可能已经失效，或商品仍处于草稿阶段。</p><RouterLink class="secondary-button" to="/products">返回商品列表</RouterLink></div>
      <div v-else-if="error" class="state-panel" role="alert"><h1>详情暂时不可用</h1><p>{{ error }}</p><button class="secondary-button" type="button" @click="load">重新加载</button></div>
      <article v-else-if="product" class="product-detail">
        <section class="product-gallery">
          <ProductArtwork :name="product.name" :sku="product.sku" />
          <div class="gallery-caption"><span>IMAGE REFERENCE</span><strong>{{ product.mainImageId || '等待媒体模块提供图片' }}</strong></div>
        </section>
        <section class="product-intro">
          <p class="detail-eyebrow">{{ product.category.name }} / {{ product.sku }}</p>
          <h1>{{ product.name }}</h1>
          <p class="product-summary">{{ product.summary }}</p>
          <div v-if="product.status === 'UNLISTED'" class="unlisted-notice" role="status"><strong>该商品已下架</strong><span>旧链接继续保留基本信息，但不能购买。</span></div>
          <dl class="product-keyfacts">
            <div><dt>建议年龄</dt><dd>{{ formatAgeRange(product.ageMin, product.ageMax) }}</dd></div>
            <div><dt>玩法方向</dt><dd>{{ playLabels[product.playType] }}</dd></div>
            <div><dt>使用场景</dt><dd>{{ sceneLabels[product.scene] }}</dd></div>
            <div><dt>库存状态</dt><dd :class="{ unavailable: !product.inStock }">{{ product.availabilityMessage }}</dd></div>
          </dl>
          <div class="purchase-panel">
            <div><span>零售价格</span><strong>{{ formatCny(product.retailUnitPriceFen) }}</strong><small>含税规则以本期模拟流程为准，运费 ¥0.00</small></div>
            <label>数量<input v-model.number="quantity" type="number" min="1" max="99" /></label>
            <button class="primary-button" type="button" :disabled="!product.purchasable || quantity < 1 || quantity > 99 || !Number.isInteger(quantity)" @click="addToCart">
              {{ product.purchasable ? '加入购物车' : '暂不可购买' }} <span>→</span>
            </button>
            <p>经销参考价仅在经销商专属目录显示，不会从此公开接口返回。</p>
          </div>
        </section>
        <section v-if="product.status === 'PUBLISHED'" class="product-story">
          <div><p>WHY IT MOVES</p><h2>游戏说明</h2><p>{{ product.description || product.instructions }}</p></div>
          <dl>
            <div><dt>材质</dt><dd>{{ product.material }}</dd></div>
            <div><dt>规格与单位</dt><dd>{{ product.dimensions }}</dd></div>
            <div><dt>包装包含</dt><dd>{{ product.packageContents }}</dd></div>
            <div><dt>怎么玩</dt><dd>{{ product.instructions }}</dd></div>
          </dl>
          <aside><span>SAFETY FIRST</span><h3>安全提示</h3><p>{{ product.safetyNotes }}</p></aside>
        </section>
      </article>
    </div>
  </PublicShell>
</template>
