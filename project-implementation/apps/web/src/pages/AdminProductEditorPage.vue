<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import SiteShell from '../components/SiteShell.vue'
import ProductArtwork from '../components/ProductArtwork.vue'
import { api, ApiProblem, newIdempotencyKey } from '../services/http'
import type { AdminProduct, Category, ProductOptions } from '../types'

const route = useRoute()
const router = useRouter()
const id = computed(() => typeof route.params.id === 'string' ? route.params.id : '')
const creating = computed(() => !id.value)
const loading = ref(!creating.value)
const busy = ref(false)
const error = ref('')
const fields = reactive<Record<string, string>>({})
const current = ref<AdminProduct | null>(null)
const categories = ref<Category[]>([])
const options = ref<ProductOptions>({ playTypes: [], scenes: [] })
const form = reactive({
  sku: '', name: '', categoryId: '', summary: '', description: '', ageMin: null as number | null,
  ageMax: null as number | null, playType: '', scene: '', material: '', dimensions: '',
  packageContents: '', instructions: '', safetyNotes: '', mainImageId: '', imageIds: '',
  retailPrice: '', dealerEnabled: false, dealerPrice: '', minInquiryQuantity: null as number | null,
  leadTimeText: '', displayOrder: 0, initialStock: 0,
})

function fill(product: AdminProduct) {
  current.value = product
  Object.assign(form, {
    sku: product.sku ?? '', name: product.name ?? '', categoryId: product.categoryId ?? '',
    summary: product.summary ?? '', description: product.description ?? '', ageMin: product.ageMin ?? null,
    ageMax: product.ageMax ?? null, playType: product.playType ?? '', scene: product.scene ?? '',
    material: product.material ?? '', dimensions: product.dimensions ?? '', packageContents: product.packageContents ?? '',
    instructions: product.instructions ?? '', safetyNotes: product.safetyNotes ?? '', mainImageId: product.mainImageId ?? '',
    imageIds: product.imageIds.join(', '), retailPrice: product.retailUnitPriceFen == null ? '' : (product.retailUnitPriceFen / 100).toFixed(2),
    dealerEnabled: product.dealerEnabled, dealerPrice: product.dealerReferenceUnitPriceFen == null ? '' : (product.dealerReferenceUnitPriceFen / 100).toFixed(2),
    minInquiryQuantity: product.minInquiryQuantity ?? null, leadTimeText: product.leadTimeText ?? '', displayOrder: product.displayOrder,
  })
}

async function load() {
  try {
    const [categoryResult, optionResult] = await Promise.all([
      api<Category[]>('/admin/categories'), api<ProductOptions>('/product-options'),
    ])
    categories.value = categoryResult.data
    options.value = optionResult.data
    if (!creating.value) fill((await api<AdminProduct>(`/admin/products/${id.value}`)).data)
  } catch (cause) {
    error.value = cause instanceof ApiProblem ? cause.problem.detail : '商品资料加载失败。'
  } finally { loading.value = false }
}

function toFen(value: string, field: string) {
  if (!value.trim()) return null
  if (!/^\d+(\.\d{1,2})?$/.test(value.trim())) {
    fields[field] = '请输入大于 0、最多两位小数的金额。'
    return null
  }
  const fen = Math.round(Number(value) * 100)
  if (fen < 1 || fen > 99_999_999) fields[field] = '金额需在 ¥0.01—¥999999.99 之间。'
  return fen
}

function payload() {
  const retailUnitPriceFen = toFen(form.retailPrice, 'retailUnitPriceFen')
  const dealerReferenceUnitPriceFen = form.dealerEnabled ? toFen(form.dealerPrice, 'dealerReferenceUnitPriceFen') : null
  const body = {
    sku: form.sku.trim() || null, name: form.name.trim() || null, categoryId: form.categoryId || null,
    summary: form.summary.trim() || null, description: form.description.trim() || null,
    ageMin: form.ageMin, ageMax: form.ageMax, playType: form.playType || null, scene: form.scene || null,
    material: form.material.trim() || null, dimensions: form.dimensions.trim() || null,
    packageContents: form.packageContents.trim() || null, instructions: form.instructions.trim() || null,
    safetyNotes: form.safetyNotes.trim() || null, mainImageId: form.mainImageId.trim() || null,
    imageIds: form.imageIds.split(',').map((item) => item.trim()).filter(Boolean),
    retailUnitPriceFen, dealerEnabled: form.dealerEnabled,
    dealerReferenceUnitPriceFen, minInquiryQuantity: form.dealerEnabled ? form.minInquiryQuantity : null,
    leadTimeText: form.dealerEnabled ? form.leadTimeText.trim() || null : null,
    displayOrder: form.displayOrder,
  }
  return creating.value ? { ...body, initialStock: form.initialStock } : { ...body, expectedVersion: current.value?.version }
}

async function save() {
  busy.value = true
  error.value = ''
  Object.keys(fields).forEach((key) => delete fields[key])
  const body = payload()
  if (Object.keys(fields).length) { busy.value = false; return }
  try {
    const result = creating.value
      ? await api<AdminProduct>('/admin/products', { method: 'POST', headers: { 'Idempotency-Key': newIdempotencyKey() }, body: JSON.stringify(body) })
      : await api<AdminProduct>(`/admin/products/${id.value}`, { method: 'PATCH', body: JSON.stringify(body) })
    fill(result.data)
    ElMessage.success(creating.value ? '商品草稿已创建' : '商品资料已保存')
    if (creating.value) await router.replace(`/admin/products/${result.data.id}`)
  } catch (cause) {
    if (cause instanceof ApiProblem) {
      error.value = cause.problem.detail
      cause.problem.errors?.forEach((item) => { fields[item.field] = item.message })
    } else error.value = '保存失败，请检查服务状态。'
  } finally { busy.value = false }
}

watch(() => form.sku, (value) => { if (!current.value?.sku) form.sku = value.toUpperCase().replace(/[^A-Z0-9-]/g, '') })
onMounted(load)
</script>

<template>
  <SiteShell :title="creating ? '新建商品草稿' : '编辑商品'" eyebrow="OPERATIONS / PRODUCT EDITOR" admin>
    <button class="back-link" type="button" @click="router.push('/admin/products')">← 返回商品与库存</button>
    <div v-if="loading" class="state-panel"><span class="loader"></span><p>正在加载编辑器…</p></div>
    <div v-else-if="error && !creating && !current" class="state-panel" role="alert"><h2>无法打开商品</h2><p>{{ error }}</p></div>
    <form v-else class="product-editor" novalidate @submit.prevent="save">
      <header class="editor-masthead">
        <ProductArtwork :name="form.name" :sku="form.sku" compact />
        <div><p>{{ current?.status === 'PUBLISHED' ? 'LIVE PRODUCT' : current?.status === 'UNLISTED' ? 'UNLISTED PRODUCT' : 'DRAFT PRODUCT' }}</p><h2>{{ form.name || '未命名商品' }}</h2><span>{{ form.sku || 'SKU 待补充' }}<template v-if="current"> · 版本 {{ current.version }}</template></span></div>
        <div class="editor-actions"><button class="secondary-button" type="button" @click="router.push('/admin/products')">取消</button><button class="primary-button" type="submit" :disabled="busy">{{ busy ? '正在保存…' : '保存资料' }}</button></div>
      </header>
      <div v-if="error" class="error-summary" role="alert"><strong>未能保存</strong><span>{{ error }}</span></div>

      <section class="paper-section editor-section">
        <header><div><p>01 / IDENTITY</p><h3>基本信息</h3></div><span>草稿可不完整，已填写值仍需合法。</span></header>
        <div class="editor-grid">
          <label class="field"><span>商品名称</span><input v-model="form.name" maxlength="100" placeholder="2—100 个字符" :aria-invalid="!!fields.name" /><small v-if="fields.name" class="field-error">{{ fields.name }}</small></label>
          <label class="field"><span>SKU</span><input v-model="form.sku" maxlength="40" placeholder="WM-EXAMPLE-001" :disabled="!!current?.sku" :aria-invalid="!!fields.sku" /><small v-if="fields.sku" class="field-error">{{ fields.sku }}</small></label>
          <label class="field"><span>分类</span><select v-model="form.categoryId" :aria-invalid="!!fields.categoryId"><option value="">请选择分类</option><option v-for="category in categories" :key="category.id" :value="category.id">{{ category.name }}{{ category.enabled ? '' : '（已停用）' }}</option></select><small v-if="fields.categoryId" class="field-error">{{ fields.categoryId }}</small></label>
          <label class="field"><span>推荐顺序</span><input v-model.number="form.displayOrder" type="number" min="0" /></label>
          <label class="field editor-span-2"><span>商品简述</span><textarea v-model="form.summary" maxlength="200" rows="2" placeholder="列表和页面首屏使用，最多 200 字符" :aria-invalid="!!fields.summary"></textarea><small v-if="fields.summary" class="field-error">{{ fields.summary }}</small></label>
          <label class="field editor-span-2"><span>详细说明</span><textarea v-model="form.description" maxlength="10000" rows="4" placeholder="介绍使用价值和设计思路"></textarea></label>
        </div>
      </section>

      <section class="paper-section editor-section">
        <header><div><p>02 / FIT & PLAY</p><h3>适用与玩法</h3></div><span>年龄只描述商品，不保存儿童档案。</span></header>
        <div class="editor-grid editor-grid-4">
          <label class="field"><span>年龄下限</span><input v-model.number="form.ageMin" type="number" min="0" max="18" :aria-invalid="!!fields.ageMin" /><small v-if="fields.ageMin" class="field-error">{{ fields.ageMin }}</small></label>
          <label class="field"><span>年龄上限（可空）</span><input v-model.number="form.ageMax" type="number" min="0" max="18" :aria-invalid="!!fields.ageMax" /><small v-if="fields.ageMax" class="field-error">{{ fields.ageMax }}</small></label>
          <label class="field"><span>玩法类型</span><select v-model="form.playType"><option value="">请选择</option><option v-for="item in options.playTypes" :key="item.value" :value="item.value">{{ item.label }}</option></select></label>
          <label class="field"><span>使用场景</span><select v-model="form.scene"><option value="">请选择</option><option v-for="item in options.scenes" :key="item.value" :value="item.value">{{ item.label }}</option></select></label>
          <label class="field editor-span-2"><span>材质</span><textarea v-model="form.material" maxlength="2000" rows="2"></textarea></label>
          <label class="field editor-span-2"><span>规格与单位</span><textarea v-model="form.dimensions" maxlength="2000" rows="2"></textarea></label>
          <label class="field editor-span-2"><span>包装包含</span><textarea v-model="form.packageContents" maxlength="2000" rows="3"></textarea></label>
          <label class="field editor-span-2"><span>玩法说明</span><textarea v-model="form.instructions" maxlength="2000" rows="3"></textarea></label>
          <label class="field editor-span-4"><span>安全提示</span><textarea v-model="form.safetyNotes" maxlength="2000" rows="3" :aria-invalid="!!fields.safetyNotes"></textarea><small v-if="fields.safetyNotes" class="field-error">{{ fields.safetyNotes }}</small></label>
        </div>
      </section>

      <section class="paper-section editor-section">
        <header><div><p>03 / MEDIA & PRICE</p><h3>图片与价格</h3></div><span>图片保存受控引用；文件内容由 E 模块负责。</span></header>
        <div class="editor-grid">
          <label class="field"><span>主图 ID</span><input v-model="form.mainImageId" maxlength="64" placeholder="asset_xxx 或测试引用" :aria-invalid="!!fields.mainImageId" /><small v-if="fields.mainImageId" class="field-error">{{ fields.mainImageId }}</small></label>
          <label class="field"><span>附图 ID（逗号分隔）</span><input v-model="form.imageIds" placeholder="asset_a, asset_b" :aria-invalid="!!fields.imageIds" /><small v-if="fields.imageIds" class="field-error">{{ fields.imageIds }}</small></label>
          <label class="field"><span>零售价（CNY 元）</span><input v-model="form.retailPrice" inputmode="decimal" placeholder="299.00" :aria-invalid="!!fields.retailUnitPriceFen" /><small v-if="fields.retailUnitPriceFen" class="field-error">{{ fields.retailUnitPriceFen }}</small></label>
          <label v-if="creating" class="field"><span>初始库存</span><input v-model.number="form.initialStock" type="number" min="0" /><small>建档后只能通过库存调整命令变更。</small></label>
        </div>
        <div class="dealer-panel">
          <label class="toggle-line"><input v-model="form.dealerEnabled" type="checkbox" /><span><strong>启用经销业务</strong><small>专属字段不会进入公开商品响应。</small></span></label>
          <div v-if="form.dealerEnabled" class="editor-grid editor-grid-3">
            <label class="field"><span>经销参考价（CNY 元）</span><input v-model="form.dealerPrice" inputmode="decimal" :aria-invalid="!!fields.dealerReferenceUnitPriceFen" /><small v-if="fields.dealerReferenceUnitPriceFen" class="field-error">{{ fields.dealerReferenceUnitPriceFen }}</small></label>
            <label class="field"><span>最小询价量</span><input v-model.number="form.minInquiryQuantity" type="number" min="1" max="9999" :aria-invalid="!!fields.minInquiryQuantity" /><small v-if="fields.minInquiryQuantity" class="field-error">{{ fields.minInquiryQuantity }}</small></label>
            <label class="field"><span>参考交期</span><input v-model="form.leadTimeText" maxlength="500" placeholder="例如 7—10 个工作日" :aria-invalid="!!fields.leadTimeText" /><small v-if="fields.leadTimeText" class="field-error">{{ fields.leadTimeText }}</small></label>
          </div>
        </div>
      </section>
      <footer class="editor-footer"><p>发布操作在商品列表执行；发布时会一次性检查完整字段、分类、价格和库存记录。</p><button class="primary-button" type="submit" :disabled="busy">{{ busy ? '正在保存…' : '保存商品资料' }}</button></footer>
    </form>
  </SiteShell>
</template>
