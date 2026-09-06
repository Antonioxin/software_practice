<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import SiteShell from '../components/SiteShell.vue'
import ProductArtwork from '../components/ProductArtwork.vue'
import { api, ApiProblem, newIdempotencyKey } from '../services/http'
import type { AdminProduct, Category, PageMeta } from '../types'

const router = useRouter()
const products = ref<AdminProduct[]>([])
const categories = ref<Category[]>([])
const meta = ref<PageMeta>({ page: 1, pageSize: 20, totalItems: 0, totalPages: 0 })
const loading = ref(true)
const error = ref('')
const filters = reactive({ keyword: '', status: '', categoryId: '' })
const stockOpen = ref(false)
const stockBusy = ref(false)
const selected = ref<AdminProduct | null>(null)
const stockForm = reactive({ direction: 'INCREASE', quantity: 1, reason: '' })
const movementOpen = ref(false)
const movementLoading = ref(false)
const movements = ref<Array<{ id: string; direction: string; quantity: number; quantityBefore: number; quantityAfter: number; reason: string; sourceType: string; createdAt: string }>>([])

async function load(page = 1) {
  loading.value = true
  error.value = ''
  const params = new URLSearchParams({ page: String(page), pageSize: '20' })
  for (const [key, value] of Object.entries(filters)) if (value) params.set(key, value)
  try {
    const result = await api<AdminProduct[]>(`/admin/products?${params}`)
    products.value = result.data
    if (result.meta) meta.value = result.meta
  } catch (cause) {
    error.value = cause instanceof ApiProblem ? cause.problem.detail : '商品管理数据暂时无法加载。'
  } finally { loading.value = false }
}

async function loadCategories() {
  try { categories.value = (await api<Category[]>('/admin/categories')).data } catch { categories.value = [] }
}

async function publication(product: AdminProduct) {
  const publish = product.status !== 'PUBLISHED'
  try {
    await ElMessageBox.confirm(
      publish ? '发布后商品会立即出现在公开列表中，并可进入交易校验。' : '下架后公开列表会立即隐藏商品，旧详情链接仅保留有限信息。',
      publish ? '确认发布商品' : '确认下架商品',
      { confirmButtonText: publish ? '发布' : '下架', cancelButtonText: '返回' },
    )
    await api<AdminProduct>(`/admin/products/${product.id}/${publish ? 'publish' : 'unpublish'}`, {
      method: 'POST', headers: { 'Idempotency-Key': newIdempotencyKey() },
      body: JSON.stringify({ expectedVersion: product.version }),
    })
    ElMessage.success(publish ? '商品已发布' : '商品已下架')
    await load(meta.value.page)
  } catch (cause) {
    if (cause === 'cancel' || cause === 'close') return
    ElMessage.error(cause instanceof ApiProblem ? cause.problem.detail : '操作未完成，请稍后重试。')
  }
}

function openStock(product: AdminProduct) {
  selected.value = product
  Object.assign(stockForm, { direction: 'INCREASE', quantity: 1, reason: '' })
  stockOpen.value = true
}

async function adjustStock() {
  if (!selected.value) return
  stockBusy.value = true
  try {
    await api<AdminProduct>(`/admin/products/${selected.value.id}/stock-adjustments`, {
      method: 'POST', headers: { 'Idempotency-Key': newIdempotencyKey() }, body: JSON.stringify(stockForm),
    })
    ElMessage.success('库存已原子调整并写入流水')
    stockOpen.value = false
    await load(meta.value.page)
  } catch (cause) {
    ElMessage.error(cause instanceof ApiProblem ? cause.problem.detail : '库存调整失败。')
  } finally { stockBusy.value = false }
}

async function openMovements(product: AdminProduct) {
  selected.value = product
  movementOpen.value = true
  movementLoading.value = true
  try { movements.value = (await api<typeof movements.value>(`/admin/products/${product.id}/stock-movements?page=1&pageSize=20`)).data }
  catch (cause) { ElMessage.error(cause instanceof ApiProblem ? cause.problem.detail : '库存流水加载失败。') }
  finally { movementLoading.value = false }
}

function statusLabel(status: string) { return ({ DRAFT: '草稿', PUBLISHED: '已上架', UNLISTED: '已下架' } as Record<string, string>)[status] }
function money(fen?: number | null) { return fen == null ? '—' : `¥${(fen / 100).toFixed(2)}` }
function time(value: string) { return new Intl.DateTimeFormat('zh-CN', { dateStyle: 'medium', timeStyle: 'short', timeZone: 'Asia/Shanghai' }).format(new Date(value)) }

onMounted(() => { load(); loadCategories() })
</script>

<template>
  <SiteShell title="商品与库存" eyebrow="OPERATIONS / CATALOG" admin>
    <section class="admin-stats catalog-admin-stats"><div><span>商品总数</span><strong>{{ meta.totalItems }}</strong><small>条记录</small></div><p>商品信息、两类价格与库存分别维护；普通编辑不会覆盖库存余额。</p><button class="light-button" type="button" @click="router.push('/admin/products/new')">＋ 新建商品</button></section>
    <section class="paper-section admin-filter catalog-admin-filter">
      <form @submit.prevent="load(1)">
        <label class="field"><span>名称或 SKU</span><input v-model="filters.keyword" placeholder="搜索商品" /></label>
        <label class="field"><span>状态</span><select v-model="filters.status"><option value="">全部状态</option><option value="DRAFT">草稿</option><option value="PUBLISHED">已上架</option><option value="UNLISTED">已下架</option></select></label>
        <label class="field"><span>分类</span><select v-model="filters.categoryId"><option value="">全部分类</option><option v-for="category in categories" :key="category.id" :value="category.id">{{ category.name }}</option></select></label>
        <div class="filter-actions"><button class="primary-button compact" type="submit">查询</button><button class="secondary-button" type="button" @click="Object.assign(filters, { keyword: '', status: '', categoryId: '' }); load(1)">重置</button></div>
      </form>
    </section>
    <div v-if="loading" class="state-panel"><span class="loader"></span><p>正在读取商品…</p></div>
    <div v-else-if="error" class="state-panel" role="alert"><h2>加载失败</h2><p>{{ error }}</p><button class="secondary-button" @click="load(meta.page)">重试</button></div>
    <div v-else-if="!products.length" class="state-panel"><h2>没有匹配的商品</h2><p>可以调整条件，或创建一条草稿。</p></div>
    <div v-else class="table-wrap catalog-admin-table">
      <table><thead><tr><th>商品</th><th>状态</th><th>价格</th><th>库存</th><th>经销</th><th>更新时间</th><th>操作</th></tr></thead>
        <tbody><tr v-for="product in products" :key="product.id">
          <td><div class="admin-product-cell"><ProductArtwork :name="product.name" :sku="product.sku" compact /><div><strong>{{ product.name || '未命名草稿' }}</strong><span>{{ product.sku || 'SKU 待补充' }} · {{ product.categoryName || '未分类' }}</span></div></div></td>
          <td><span class="catalog-status" :class="product.status.toLowerCase()">{{ statusLabel(product.status) }}</span></td>
          <td>{{ money(product.retailUnitPriceFen) }}</td>
          <td><button class="stock-number" type="button" @click="openMovements(product)"><strong>{{ product.stock }}</strong><span>查看流水</span></button></td>
          <td>{{ product.dealerEnabled ? money(product.dealerReferenceUnitPriceFen) : '未启用' }}</td>
          <td>{{ time(product.updatedAt) }}</td>
          <td><div class="row-actions"><button type="button" @click="router.push(`/admin/products/${product.id}`)">编辑</button><button type="button" @click="openStock(product)">调库存</button><button type="button" @click="publication(product)">{{ product.status === 'PUBLISHED' ? '下架' : '发布' }}</button></div></td>
        </tr></tbody>
      </table>
    </div>
    <nav v-if="meta.totalPages > 1" class="pagination"><button :disabled="meta.page <= 1" @click="load(meta.page - 1)">上一页</button><span>第 {{ meta.page }} / {{ meta.totalPages }} 页</span><button :disabled="meta.page >= meta.totalPages" @click="load(meta.page + 1)">下一页</button></nav>

    <el-dialog v-model="stockOpen" title="调整库存" width="min(520px, 92vw)">
      <p v-if="selected" class="dialog-copy">{{ selected.name || '未命名商品' }} 当前库存 <strong>{{ selected.stock }}</strong>。提交的是增减量，不会用旧页面值覆盖余额。</p>
      <form class="identity-form" @submit.prevent="adjustStock">
        <div class="field-pair"><label class="field"><span>方向</span><select v-model="stockForm.direction"><option value="INCREASE">增加</option><option value="DECREASE">减少</option></select></label><label class="field"><span>数量</span><input v-model.number="stockForm.quantity" type="number" min="1" required /></label></div>
        <label class="field"><span>调整原因</span><textarea v-model="stockForm.reason" minlength="2" maxlength="500" rows="3" required placeholder="例如：测试入库盘点调整"></textarea></label>
      </form>
      <template #footer><button class="secondary-button" @click="stockOpen = false">取消</button><button class="primary-button" :disabled="stockBusy || stockForm.quantity < 1 || stockForm.reason.trim().length < 2" @click="adjustStock">{{ stockBusy ? '正在提交…' : '确认调整' }}</button></template>
    </el-dialog>

    <el-dialog v-model="movementOpen" title="库存流水" width="min(760px, 94vw)">
      <div v-if="movementLoading" class="state-panel compact-state"><span class="loader"></span></div>
      <div v-else-if="!movements.length" class="state-panel compact-state"><p>暂无流水</p></div>
      <div v-else class="movement-list"><article v-for="item in movements" :key="item.id"><strong :class="item.direction === 'INCREASE' ? 'positive' : 'negative'">{{ item.direction === 'INCREASE' ? '+' : '−' }}{{ item.quantity }}</strong><div><p>{{ item.reason }}</p><span>{{ item.quantityBefore }} → {{ item.quantityAfter }} · {{ item.sourceType }}</span></div><time>{{ time(item.createdAt) }}</time></article></div>
    </el-dialog>
  </SiteShell>
</template>
