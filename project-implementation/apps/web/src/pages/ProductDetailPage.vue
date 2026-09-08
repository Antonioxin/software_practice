<script setup lang="ts">
import { computed, onUnmounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useSessionStore } from '../stores/session'
import { useCommandRecovery } from '../features/commerce/commandRecovery'
import { useCartStore } from '../features/commerce/cartStore'
import SketchIcon from '../components/SketchIcon.vue'
import PublicShell from '../components/PublicShell.vue'
import ProductArtwork from '../components/ProductArtwork.vue'
import PolaroidMotion from '../components/PolaroidMotion.vue'
import { formatAgeRange, formatCny } from '../features/catalog/presentation'
import { productIllustrationSrc } from '../features/catalog/productImages'
import { productUsageGuide } from '../features/catalog/productGuides'
import { api, ApiProblem } from '../services/http'
import type { PublicProduct } from '../types'

const session = useSessionStore()
const cart = useCartStore()
const addCommand = useCommandRecovery(() => session.actor?.id, 'addCartItem')
const route = useRoute()
const router = useRouter()
const product = ref<PublicProduct | null>(null)
const loading = ref(true)
const error = ref('')
const missing = ref(false)
const quantity = ref(1)
const canPurchase = computed(() => product.value?.status === 'PUBLISHED' && product.value.purchasable)
const validQuantity = computed(() => Number.isInteger(quantity.value) && quantity.value >= 1 && quantity.value <= 99)
const mainImageFailed = ref(false)
const guideImageFailed = ref(false)
const mainImage = computed(() => product.value && !mainImageFailed.value ? productIllustrationSrc(product.value.sku) : null)
const guide = computed(() => product.value?.status === 'PUBLISHED' ? productUsageGuide(product.value.sku) : null)
let latestRequest = 0

async function load() {
  const request = ++latestRequest
  const id = route.params.id
  loading.value = true
  error.value = ''
  missing.value = false
  product.value = null
  quantity.value = 1
  mainImageFailed.value = false
  guideImageFailed.value = false
  document.title = '商品详情 · WEMOVE'
  try {
    const result = await api<PublicProduct>(`/products/${id}`)
    if (request !== latestRequest) return
    product.value = result.data
    document.title = `${product.value.name} · WEMOVE`
  } catch (cause) {
    if (request !== latestRequest) return
    if (cause instanceof ApiProblem && cause.problem.status === 404) missing.value = true
    else error.value = cause instanceof ApiProblem ? cause.problem.detail : '商品详情暂时无法加载。'
  } finally {
    if (request === latestRequest) loading.value = false
  }
}

function back() {
  router.push({ path: '/products', query: route.query })
}

async function addToCart() {
  if (!session.actor) { await router.push({ path: '/login', query: { redirect: route.fullPath } }); return }
  if (session.isAdmin) return
  try {
    if (addCommand.pending.value) await addCommand.retry()
    else if (product.value && canPurchase.value && validQuantity.value) await addCommand.send('/cart/items', { productId: product.value.id, quantity: quantity.value })
    else return
    cart.clear(); ElMessage.success('已加入购物车'); await router.push('/cart')
  } catch (e) { ElMessage.error((e as Error).message) }
}

const playLabels: Record<string, string> = { BALANCE: '平衡能力', COORDINATION: '协调训练', THROWING: '投掷与瞄准', TEAM_PLAY: '团队游戏', OUTDOOR_EXPLORATION: '户外探索' }
const sceneLabels: Record<string, string> = { INDOOR: '室内', OUTDOOR: '户外', BOTH: '室内与户外' }

watch(() => route.params.id, load, { immediate: true })
onUnmounted(() => { latestRequest++ })
</script>

<template>
  <PublicShell class="product-detail-shell">
    <div class="product-detail-page">
      <div v-if="addCommand.pending.value && !session.isAdmin" class="detail-recovery" role="status"><p>有加购请求结果尚未确认，可先查询购物车或恢复原请求。</p><button type="button" :disabled="addCommand.busy.value" @click="addToCart">使用原加购请求重试</button><RouterLink to="/cart">查询购物车</RouterLink></div>
      <button class="detail-back" type="button" @click="back">← 返回商品列表与当前筛选</button>
      <div v-if="loading" class="state-panel" role="status"><span class="loader"></span><p>正在加载商品详情…</p></div>
      <div v-else-if="missing" class="state-panel"><h1>没有找到这个商品</h1><p>链接可能已经失效，或商品仍处于草稿阶段。</p><RouterLink class="secondary-button" to="/products">返回商品列表</RouterLink></div>
      <div v-else-if="error" class="state-panel" role="alert"><h1>详情暂时不可用</h1><p>{{ error }}</p><button class="secondary-button" type="button" @click="load">重新加载</button></div>
      <article v-else-if="product" class="product-detail">
        <section class="product-gallery">
          <PolaroidMotion>
            <figure class="product-polaroid">
              <div class="product-photo-window">
                <img v-if="mainImage" class="product-main-image" :src="mainImage" :alt="`${product.name}的 AI 生成商品示意图（非实拍）`" loading="eager" decoding="async" @error="mainImageFailed = true" />
                <ProductArtwork v-else :name="product.name" :sku="product.sku" />
              </div>
              <img class="product-photo-paper" src="/assets/frames/polaroid-product.png" width="1149" height="1369" alt="" aria-hidden="true" draggable="false" />
              <figcaption class="product-photo-name" :title="product.name"><span>{{ product.name }}</span></figcaption>
            </figure>
          </PolaroidMotion>
          <p class="product-image-caption">{{ mainImage ? 'AI 商品示意 · 非实拍' : '手绘商品示意' }}</p>
        </section>
        <section class="product-intro">
          <p class="detail-eyebrow">{{ product.category.name }} / {{ product.sku }}</p>
          <h1>{{ product.name }}</h1>
          <p class="product-summary">{{ product.summary }}</p>
          <p v-if="product.status === 'PUBLISHED' && product.description" class="product-description">{{ product.description }}</p>
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
            <button v-if="!session.isAdmin" class="primary-button" type="button" :disabled="addCommand.busy.value || !canPurchase || !validQuantity" @click="addToCart">
              {{ addCommand.pending.value ? '重试原加购请求' : canPurchase ? '加入购物车' : '暂不可购买' }} <SketchIcon name="cart" :size="24" />
            </button>
            <p>大宗采购可通过经销合作渠道咨询。</p>
          </div>
        </section>
        <template v-if="product.status === 'PUBLISHED'">
          <section class="product-guide" aria-labelledby="product-guide-title">
            <header class="product-section-heading"><p>LET'S PLAY</p><h2 id="product-guide-title">这样开始玩</h2></header>
            <div v-if="guide" class="product-guide-layout" :class="{ 'product-guide-layout-text': guideImageFailed }">
              <figure v-if="!guideImageFailed" class="product-guide-art">
                <img class="product-guide-image" :src="guide.imageSrc" :alt="guide.imageAlt" width="1254" height="1254" loading="lazy" decoding="async" @error="guideImageFailed = true" />
                <figcaption>玩法示意</figcaption>
              </figure>
              <div class="product-guide-copy">
                <h3>{{ guide.title }}</h3><p class="product-guide-intro">{{ guide.intro }}</p>
                <ol class="product-guide-steps">
                  <li v-for="(step, index) in guide.steps" :key="step.title"><span class="product-step-number" aria-hidden="true">{{ String(index + 1).padStart(2, '0') }}</span><div><h4>{{ step.title }}</h4><p>{{ step.body }}</p></div></li>
                </ol>
              </div>
            </div>
            <div v-if="product.instructions" class="product-use-notes" :class="{ 'product-use-notes-only': !guide }"><h3 v-if="guide">使用说明</h3><p>{{ product.instructions }}</p></div>
            <p v-else-if="!guide" class="product-use-notes">暂无使用说明。</p>
          </section>
          <div class="product-practical-details">
            <section class="product-specifications" aria-labelledby="product-specifications-title">
              <header class="product-section-heading"><p>THE LITTLE DETAILS</p><h2 id="product-specifications-title">商品规格</h2></header>
              <dl>
                <div><dt>材质</dt><dd>{{ product.material || '—' }}</dd></div>
                <div><dt>规格与单位</dt><dd>{{ product.dimensions || '—' }}</dd></div>
                <div><dt>包装包含</dt><dd>{{ product.packageContents || '—' }}</dd></div>
              </dl>
            </section>
            <aside v-if="product.safetyNotes" class="product-safety" aria-labelledby="product-safety-title"><SketchIcon name="help" :size="38" /><span>PLAY WITH CARE</span><h2 id="product-safety-title">安全提示</h2><p>{{ product.safetyNotes }}</p></aside>
          </div>
        </template>
      </article>
    </div>
  </PublicShell>
</template>

<style scoped>
.product-detail-shell { overflow-x: clip; }
.product-detail-page { width: min(1180px, calc(100% - 64px)); margin: 0 auto; padding: 34px 0 84px; }
.detail-back { margin-bottom: 28px; min-height: 44px; padding: 8px 0; color: var(--muted); font-size: 14px; font-weight: 400; text-align: left; }
.detail-recovery { display: flex; align-items: center; flex-wrap: wrap; gap: 12px 22px; margin-bottom: 20px; padding: 18px 22px; border: 1px solid var(--line); background: #fffefa; font-size: 14px; line-height: 1.8; }
.detail-recovery p { flex-basis: 100%; margin: 0; }
.detail-recovery button { min-height: 40px; border: 0; padding: 6px 0; color: var(--green); background: transparent; text-decoration: underline; text-underline-offset: 4px; }
.detail-recovery a { color: var(--green); text-underline-offset: 4px; }
.product-detail { display: grid; grid-template-columns: minmax(0, 1fr) minmax(0, .93fr); align-items: start; gap: 76px 60px; }
.product-gallery, .product-intro { min-width: 0; }
.product-gallery { width: 100%; max-width: 560px; justify-self: center; container-type: inline-size; }
.product-polaroid { position: relative; isolation: isolate; width: 100%; aspect-ratio: 1149 / 1369; margin: 0; }
.product-photo-window { position: absolute; left: 11.9234%; top: 9.1308%; width: 76.5013%; height: 66.7641%; overflow: hidden; background: #efece2; }
.product-main-image { display: block; width: 100%; height: 100%; object-fit: cover; object-position: center; }
.product-photo-window :deep(.product-artwork) { width: 100%; height: 100%; min-height: 0; border-radius: 0; padding: 20px; gap: 14px; }
.product-photo-window :deep(.sketch-product-icon) { width: 35%; max-width: 128px; height: auto; }
.product-photo-window :deep(.product-artwork strong) { font-size: clamp(14px, 4cqi, 20px); }
.product-photo-window :deep(.product-artwork small) { font-size: clamp(10px, 2.5cqi, 12px); overflow-wrap: anywhere; }
.product-photo-paper { position: absolute; z-index: 1; inset: 0; width: 100%; height: 100%; pointer-events: none; }
.product-photo-name { position: absolute; z-index: 2; left: 13.0548%; top: 78.8897%; width: 74.4125%; height: 13.0022%; display: flex; align-items: center; justify-content: center; font: 400 30px/1.3 var(--font-hand); font-size: clamp(18px, 6cqi, 34px); text-align: center; }
.product-photo-name > span { min-width: 0; overflow-wrap: anywhere; display: -webkit-box; -webkit-box-orient: vertical; -webkit-line-clamp: 2; overflow: hidden; }
.product-image-caption { margin: 10px 12% 0; color: var(--muted); font-size: 12px; line-height: 1.7; }
.product-intro { padding-top: 30px; }
.detail-eyebrow { margin: 0 0 19px; color: var(--muted); font: 500 14px/1.7 var(--font-display); letter-spacing: .6px; overflow-wrap: anywhere; }
.product-intro h1 { margin: 0; font: 500 clamp(30px, 3vw, 42px)/1.45 var(--font-body); letter-spacing: -.5px; overflow-wrap: anywhere; }
.product-summary { margin: 22px 0 14px; color: var(--ink); font-size: 18px; line-height: 1.9; overflow-wrap: anywhere; }
.product-description { margin: 0 0 28px; color: var(--muted); font-size: 15px; line-height: 1.95; white-space: pre-line; overflow-wrap: anywhere; }
.product-keyfacts { grid-template-columns: 1fr 1fr; margin: 30px 0; border-color: var(--line); }
.product-keyfacts div { min-width: 0; padding: 15px 14px 15px 0; border-color: var(--line); }
.product-keyfacts dt { margin-bottom: 7px; color: var(--muted); font-size: 13px; }
.product-keyfacts dd { font-size: 15px; font-weight: 400; line-height: 1.7; overflow-wrap: anywhere; }
.purchase-panel { grid-template-columns: minmax(0, 1fr) 82px; gap: 22px 24px; padding: 26px; border-radius: 4px; }
.purchase-panel > div { min-width: 0; gap: 6px; }
.purchase-panel > div span { font-size: 13px; letter-spacing: .5px; }
.purchase-panel > div strong { font-size: 38px; line-height: 1.2; }
.purchase-panel > div small { font-size: 12px; line-height: 1.7; }
.purchase-panel label { align-content: start; gap: 10px; font-size: 13px; }
.purchase-panel input { min-width: 0; min-height: 45px; border-radius: 3px; font-size: 17px; }
.purchase-panel .primary-button { display: flex; align-items: center; justify-content: space-between; min-height: 52px; gap: 16px; padding: 12px 18px; border-radius: 3px; font-size: 16px; }
.purchase-panel > p { font-size: 12px; line-height: 1.7; }
.product-guide { grid-column: 1 / -1; min-width: 0; padding-top: 40px; border-top: 1px solid var(--line); }
.product-section-heading { margin-bottom: 32px; }
.product-section-heading > p { margin: 0 0 5px; color: var(--green); font: 400 24px/1.2 var(--font-hand); letter-spacing: .5px; }
.product-section-heading h2 { margin: 0; font: 500 30px/1.6 var(--font-body); }
.product-guide-layout { display: grid; grid-template-columns: minmax(0, 1fr) minmax(0, 1.05fr); align-items: center; gap: 58px; }
.product-guide-art { min-width: 0; margin: 0; }
.product-guide-image { display: block; width: 100%; height: auto; aspect-ratio: 1; object-fit: contain; }
.product-guide-art figcaption { margin: 12px 0 0; color: var(--muted); font-size: 12px; text-align: center; }
.product-guide-copy { min-width: 0; }
.product-guide-copy > h3 { margin: 0 0 12px; font-size: 25px; font-weight: 500; line-height: 1.6; overflow-wrap: anywhere; }
.product-guide-intro { margin: 0 0 28px; color: var(--muted); font-size: 16px; line-height: 1.9; overflow-wrap: anywhere; }
.product-guide-steps { display: grid; gap: 24px; margin: 0; padding: 0; list-style: none; }
.product-guide-steps li { display: grid; grid-template-columns: 42px minmax(0, 1fr); gap: 14px; align-items: start; }
.product-step-number { color: var(--green); font: 400 34px/1.35 var(--font-hand); }
.product-guide-steps h4 { margin: 4px 0 6px; font-size: 18px; font-weight: 500; line-height: 1.7; }
.product-guide-steps p { margin: 0; color: var(--muted); font-size: 15px; line-height: 1.9; overflow-wrap: anywhere; }
.product-guide-layout-text { grid-template-columns: minmax(0, 1fr); max-width: 760px; }
.product-use-notes { margin: 35px 0 0; padding-top: 23px; border-top: 1px solid var(--line); color: var(--muted); font-size: 15px; line-height: 1.9; }
.product-use-notes h3 { margin: 0 0 9px; color: var(--ink); font-size: 16px; font-weight: 500; }
.product-use-notes p { margin: 0; white-space: pre-line; overflow-wrap: anywhere; }
.product-use-notes-only { max-width: 850px; margin: 0; padding: 0; border: 0; }
.product-practical-details { grid-column: 1 / -1; display: grid; grid-template-columns: minmax(0, 1.2fr) minmax(0, 1fr); align-items: start; gap: 70px; padding-top: 40px; border-top: 1px solid var(--line); }
.product-specifications { min-width: 0; }
.product-specifications .product-section-heading { margin-bottom: 24px; }
.product-specifications dl { margin: 0; }
.product-specifications dl > div { display: grid; grid-template-columns: 96px minmax(0, 1fr); gap: 20px; padding: 16px 0; border-bottom: 1px solid var(--line); font-size: 15px; line-height: 1.9; }
.product-specifications dt { color: var(--muted); }
.product-specifications dd { margin: 0; white-space: pre-line; overflow-wrap: anywhere; }
.product-safety { min-width: 0; padding: 29px 32px 32px; border-radius: 3px; background: #ebe6d7; }
.product-safety > .sketch-icon { display: block; margin-bottom: 18px; }
.product-safety > span { color: #70634e; font: 400 22px/1.4 var(--font-hand); }
.product-safety h2 { margin: 6px 0 15px; font-size: 24px; font-weight: 500; line-height: 1.5; }
.product-safety p { margin: 0; color: #655e50; font-size: 15px; line-height: 1.95; white-space: pre-line; overflow-wrap: anywhere; }
@media (max-width: 1050px) {
  .product-detail { gap: 60px 34px; }
  .product-intro { padding-top: 14px; }
  .product-summary { font-size: 17px; }
  .purchase-panel { padding: 22px; gap: 18px; }
  .product-guide-layout { gap: 34px; }
  .product-practical-details { gap: 40px; }
}
@media (max-width: 800px) {
  .product-detail-page { width: calc(100% - 40px); padding: 24px 0 60px; }
  .detail-back { margin-bottom: 14px; }
  .product-detail { grid-template-columns: minmax(0, 1fr); gap: 48px; }
  .product-gallery { max-width: 510px; }
  .product-intro { padding-top: 0; }
  .product-intro h1 { font-size: 34px; }
  .product-guide { padding-top: 30px; }
  .product-guide-layout { grid-template-columns: minmax(0, 1fr); gap: 30px; }
  .product-guide-art { width: 100%; max-width: 500px; justify-self: center; }
  .product-practical-details { grid-template-columns: minmax(0, 1fr); gap: 34px; padding-top: 30px; }
}
@media (max-width: 480px) {
  .product-detail-page { width: calc(100% - 28px); padding-top: 18px; }
  .detail-back { font-size: 13px; }
  .product-detail { gap: 36px; }
  .product-intro h1 { font-size: 29px; }
  .product-summary { margin-top: 16px; font-size: 16px; }
  .product-description { font-size: 14px; }
  .product-keyfacts { margin: 25px 0; }
  .product-keyfacts dd { font-size: 14px; }
  .purchase-panel { grid-template-columns: minmax(0, 1fr) 72px; padding: 20px; gap: 20px 14px; }
  .purchase-panel > div strong { font-size: 33px; }
  .product-section-heading { margin-bottom: 25px; }
  .product-section-heading h2 { font-size: 27px; }
  .product-guide-copy > h3 { font-size: 23px; }
  .product-guide-intro { font-size: 15px; }
  .product-guide-steps li { grid-template-columns: 35px minmax(0, 1fr); gap: 10px; }
  .product-guide-steps h4 { font-size: 17px; }
  .product-specifications dl > div { grid-template-columns: 80px minmax(0, 1fr); gap: 12px; font-size: 14px; }
  .product-safety { padding: 25px; }
}
</style>
