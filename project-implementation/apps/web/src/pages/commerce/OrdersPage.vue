<script setup lang="ts">
import SketchIcon from '../../components/SketchIcon.vue'
import { onMounted, ref } from 'vue'
import PublicShell from '../../components/PublicShell.vue'
import SiteShell from '../../components/SiteShell.vue'
import ProductThumbnail from '../../components/ProductThumbnail.vue'
import { readOrders } from '../../features/commerce/api'
import { statusLabels, type OrderPage } from '../../features/commerce/types'
import { formatCny } from '../../features/catalog/presentation'

const props = withDefaults(defineProps<{ admin?: boolean }>(), { admin: false })
const result = ref<OrderPage | null>(null)
const error = ref('')
const loading = ref(true)
const status = ref('')
const page = ref(1)
const start = ref('')
const end = ref('')

function statusLabel(value: string) {
  return statusLabels[value] ?? value
}

function statusClass(value: string) {
  return `commerce-status--${value.toLowerCase().replace(/_/g, '-')}`
}

function formatDate(value: string) {
  return new Date(value).toLocaleString('zh-CN', { dateStyle: 'medium', timeStyle: 'short' })
}

async function load() {
  loading.value = true
  error.value = ''
  try {
    const query = new URLSearchParams({ page: String(page.value), pageSize: '20' })
    if (status.value) query.set('status', status.value)
    if (props.admin && start.value) query.set('start', new Date(start.value).toISOString())
    if (props.admin && end.value) query.set('end', new Date(end.value).toISOString())
    result.value = await readOrders(query.toString(), props.admin)
  } catch (cause) {
    error.value = (cause as Error).message
  } finally {
    loading.value = false
  }
}

function filterOrders() {
  page.value = 1
  void load()
}

function resetFilters() {
  status.value = ''
  start.value = ''
  end.value = ''
  filterOrders()
}

function changePage(delta: number) {
  page.value += delta
  void load()
}

onMounted(() => void load())
</script>

<template>
  <component :is="props.admin ? SiteShell : PublicShell" :title="props.admin ? '订单管理' : undefined" :admin="props.admin" eyebrow="ORDERS / SIMULATED">
    <section
      :class="['commerce-content commerce-list-page', { 'commerce-page': !props.admin, 'commerce-admin-list-page': props.admin }]"
      aria-labelledby="orders-title"
    >
      <header v-if="!props.admin" class="commerce-page-head">
        <div>
          <p class="commerce-eyebrow">ORDERS / SIMULATED</p>
          <h1 id="orders-title"><SketchIcon name="orders" :size="44" />我的订单</h1>
          <p class="commerce-lede">查看订单中的商品、数量与配送进度。</p>
        </div>
        <RouterLink class="commerce-back-link" to="/products">← 返回商品目录</RouterLink>
      </header>

      <p v-else class="commerce-intro">按现有订单摘要筛选；不在列表页额外拉取买家或商品详情。</p>

      <form
        class="commerce-filter-panel"
        :class="{ 'commerce-filter-panel--admin': props.admin }"
        @submit.prevent="filterOrders"
      >
        <div v-if="props.admin" class="commerce-filter-grid commerce-filter-grid--admin">
          <label class="commerce-filter-field">订单状态<select v-model="status" class="commerce-filter-control"><option value="">全部状态</option><option v-for="(label, value) in statusLabels" :key="value" :value="value">{{ label }}</option></select></label>
          <label class="commerce-filter-field">创建时间起点<input v-model="start" class="commerce-filter-control" type="datetime-local" /></label>
          <label class="commerce-filter-field">创建时间终点（不含）<input v-model="end" class="commerce-filter-control" type="datetime-local" /></label>
        </div>
        <label v-else class="commerce-filter-field">订单状态<select v-model="status" class="commerce-filter-control"><option value="">全部状态</option><option v-for="(label, value) in statusLabels" :key="value" :value="value">{{ label }}</option></select></label>
        <div class="commerce-filter-actions">
          <button class="primary-button compact" type="submit">查询</button>
          <button v-if="props.admin" class="secondary-button" type="button" @click="resetFilters">重置</button>
        </div>
      </form>

      <p v-if="props.admin" class="commerce-filter-help">起点包含，终点不含；金额、状态和订单号均来自当前列表接口。</p>
      <p v-if="error" class="error-summary commerce-error" role="alert">{{ error }}</p>

      <div v-if="loading" class="commerce-state-panel" aria-live="polite"><p>正在读取订单…</p></div>
      <div v-else-if="error" class="commerce-state-panel" role="alert"><h2>订单读取失败</h2><p>{{ error }}</p><button class="secondary-button" type="button" @click="load">重试</button></div>
      <template v-else-if="result">
        <div class="commerce-results-meta"><strong>{{ result.total }} 笔订单</strong><span>第 {{ result.page }} / {{ Math.max(1, Math.ceil(result.total / result.pageSize)) }} 页</span></div>

        <div v-if="!result.items.length" class="commerce-state-panel"><h2>暂无符合条件的订单</h2><p>可以调整筛选条件，或先从商品目录创建一笔模拟订单。</p></div>

        <div v-else-if="!props.admin" class="commerce-order-list">
          <article v-for="order in result.items" :key="order.id" class="commerce-order-row">
            <header class="order-card-header">
              <div class="commerce-order-main">
                <RouterLink class="commerce-order-number" :to="`/account/orders/${order.id}`">{{ order.orderNumber }}</RouterLink>
                <p class="commerce-order-date">{{ formatDate(order.createdAt) }}</p>
              </div>
              <span class="commerce-status" :class="statusClass(order.status)">{{ statusLabel(order.status) }}</span>
            </header>
            <div class="order-card-body">
              <div class="order-products">
                <div v-for="item in (order.items ?? []).slice(0, 2)" :key="item.productId" class="order-product">
                  <ProductThumbnail :sku="item.sku" :name="item.name" />
                  <div class="order-product-copy"><strong>{{ item.name }}</strong><span>{{ item.sku }} · 数量 {{ item.quantity }}</span></div>
                </div>
                <p v-if="!order.items?.length" class="order-more-products">商品信息暂未提供，可进入详情查看。</p>
                <p v-else-if="order.items.length > 2" class="order-more-products">另有 {{ order.items.length - 2 }} 种商品，可在详情中查看全部。</p>
              </div>
              <div class="order-card-summary">
                <p v-if="order.items?.length">共 {{ order.items.reduce((total, item) => total + item.quantity, 0) }} 件商品</p>
                <div class="order-card-amount"><span>订单合计</span><strong class="commerce-order-total">{{ formatCny(order.totalFen) }}</strong></div>
                <RouterLink class="secondary-button" :to="`/account/orders/${order.id}`">查看详情</RouterLink>
              </div>
            </div>
          </article>
        </div>

        <div v-else class="table-wrap commerce-admin-table">
          <table>
            <thead><tr><th>订单号</th><th>创建时间</th><th>状态</th><th>金额</th><th>操作</th></tr></thead>
            <tbody>
              <tr v-for="order in result.items" :key="order.id">
                <td><RouterLink class="commerce-order-number" :to="`/admin/orders/${order.id}`">{{ order.orderNumber }}</RouterLink></td>
                <td>{{ formatDate(order.createdAt) }}</td>
                <td><span class="commerce-status" :class="statusClass(order.status)">{{ statusLabel(order.status) }}</span></td>
                <td><strong class="commerce-order-total">{{ formatCny(order.totalFen) }}</strong></td>
                <td><RouterLink class="secondary-button" :to="`/admin/orders/${order.id}`">查看</RouterLink></td>
              </tr>
            </tbody>
          </table>
        </div>

        <nav v-if="result.total > result.pageSize" class="commerce-pagination" aria-label="订单分页">
          <button class="secondary-button" type="button" :disabled="page === 1 || loading" @click="changePage(-1)">上一页</button>
          <span>第 {{ page }} 页 · 共 {{ result.total }} 单</span>
          <button class="secondary-button" type="button" :disabled="page * result.pageSize >= result.total || loading" @click="changePage(1)">下一页</button>
        </nav>
      </template>
    </section>
  </component>
</template>

<style scoped>
.commerce-order-row { display: block; padding: 25px 0; }
.order-card-header { display: flex; align-items: center; justify-content: space-between; gap: 16px; }
.order-card-header .commerce-status { flex-shrink: 0; }
.order-card-body { display: grid; grid-template-columns: minmax(0, 1fr) 180px; align-items: center; gap: 32px; margin-top: 22px; }
.order-products { display: grid; min-width: 0; gap: 16px; }
.order-product { display: grid; grid-template-columns: 76px minmax(0, 1fr); align-items: center; gap: 17px; }
.order-product-copy { min-width: 0; }
.order-product-copy strong { display: -webkit-box; overflow: hidden; -webkit-box-orient: vertical; -webkit-line-clamp: 2; color: var(--ink); font-size: 17px; font-weight: 500; line-height: 1.6; overflow-wrap: anywhere; }
.order-product-copy span { display: block; margin-top: 6px; color: var(--muted); font-size: 12px; line-height: 1.6; overflow-wrap: anywhere; }
.order-more-products { margin: 0; color: var(--muted); font-size: 13px; line-height: 1.7; }
.order-card-summary { display: flex; flex-direction: column; align-items: flex-end; gap: 13px; padding-left: 24px; border-left: 1px solid var(--line); }
.order-card-summary > p { margin: 0; color: var(--muted); font-size: 13px; }
.order-card-amount { display: flex; flex-wrap: wrap; align-items: baseline; justify-content: flex-end; gap: 6px 10px; }
.order-card-amount > span { color: var(--muted); font-size: 12px; }
@media (max-width: 760px) {
  .order-card-header { align-items: flex-start; gap: 10px; }
  .commerce-order-number { font-size: 13px; }
  .commerce-order-date { font-size: 12px; line-height: 1.6; }
  .order-card-body { grid-template-columns: minmax(0, 1fr); gap: 22px; }
  .order-product { grid-template-columns: 64px minmax(0, 1fr); gap: 12px; }
  .order-product-copy strong { font-size: 16px; }
  .order-card-summary { flex-direction: row; flex-wrap: wrap; align-items: center; justify-content: space-between; gap: 12px; border: 0; border-top: 1px dashed var(--line); padding: 16px 0 0; }
  .order-card-summary > p { flex: 1 0 auto; white-space: nowrap; }
  .order-card-summary > .secondary-button { margin-left: auto; }
}
</style>
