<script setup lang="ts">
import SketchIcon from '../../components/SketchIcon.vue'
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import SiteShell from '../../components/SiteShell.vue'
import { api, ApiProblem } from '../../services/http'
import { useSessionStore } from '../../stores/session'
import { useCommandRecovery } from '../../features/commerce/commandRecovery'
import { readOrders } from '../../features/commerce/api'
import { ticketPaths } from '../../features/support/api'
import { ticketTypeLabels, type TicketDetail, type TicketType } from '../../features/support/types'
import '../../features/support/style.css'
import type { Order } from '../../features/commerce/types'
import type { ProductCard } from '../../types'

const router = useRouter()
const session = useSessionStore()
const command = useCommandRecovery(() => session.actor?.id, 'support.createTicket')

const typeHints: Record<TicketType, string> = {
  GENERAL: '账户、订单之外的一般问题：网站使用、活动咨询、建议反馈等。',
  PRODUCT: '针对某一件商品的售前疑问；提交后请留意公开回复。',
  AFTER_SALES: '已完成订单的售后问题（退换、破损、缺失）；需选择本人订单。',
}

const form = reactive({
  type: 'GENERAL' as TicketType,
  subject: '',
  body: '',
  phone: '',
  productId: '',
  orderId: '',
})
const fields = reactive<Record<string, string>>({})
const error = ref('')
const products = ref<ProductCard[]>([])
const orders = ref<Order[]>([])
const productsLoaded = ref(false)
const ordersLoaded = ref(false)

const needProduct = computed(() => form.type === 'PRODUCT')
const needOrder = computed(() => form.type === 'AFTER_SALES')

watch(() => form.type, (type) => {
  if (type !== 'PRODUCT') form.productId = ''
  if (type !== 'AFTER_SALES') form.orderId = ''
  if (type === 'PRODUCT') void loadProducts()
  if (type === 'AFTER_SALES') void loadOrders()
})

async function loadProducts() {
  if (productsLoaded.value) return
  try {
    const result = await api<ProductCard[]>('/products?page=1&pageSize=50')
    products.value = result.data
    productsLoaded.value = true
  } catch { /* 下拉加载失败时保留空列表，提交前仍需选择 */ }
}

async function loadOrders() {
  if (ordersLoaded.value) return
  try {
    const result = await readOrders('page=1&pageSize=20')
    orders.value = result.items
    ordersLoaded.value = true
  } catch { /* 同上 */ }
}

function validate() {
  Object.keys(fields).forEach((key) => delete fields[key])
  if (form.subject.trim().length < 2) { fields.subject = '主题至少 2 个字符。'; return false }
  if (form.body.trim().length < 10) { fields.body = '问题描述至少 10 个字符。'; return false }
  if (needProduct.value && !form.productId) { fields.productId = '请选择要咨询的商品。'; return false }
  if (needOrder.value && !form.orderId) { fields.orderId = '请选择关联的本人订单。'; return false }
  return true
}

async function submit(retry = false) {
  if (!retry && !validate()) return
  error.value = ''
  try {
    const created = retry
      ? await command.retry<TicketDetail>()
      : await command.send<TicketDetail>(ticketPaths.create, {
          type: form.type,
          subject: form.subject.trim(),
          body: form.body.trim(),
          phone: form.phone.trim() || null,
          productId: needProduct.value ? form.productId : null,
          orderId: needOrder.value ? form.orderId : null,
        })
    ElMessage.success('咨询已提交，可在我的咨询中跟踪处理进度')
    await router.push(`/account/tickets/${created.id}`)
  } catch (cause) {
    if (cause instanceof ApiProblem) {
      error.value = cause.problem.detail
      cause.problem.errors?.forEach((item) => { fields[item.field] = item.message })
    } else {
      error.value = (cause as Error).message
    }
  }
}

onMounted(() => void loadOrders())
</script>

<template>
  <SiteShell title="联系客服" eyebrow="SUPPORT / CONTACT">
    <p class="support-page-intro">
      提交文本咨询后，客服会在后台处理并通过公开回复与您沟通；同一咨询支持补充说明，
      问题解决后可随时关闭。紧急订单问题请优先选择“售后咨询”并关联订单。
    </p>

    <div v-if="command.pending.value" class="error-summary" role="status" style="margin-bottom: 22px">
      <strong>上次提交结果尚未确认</strong>
      <span>原请求键已保留；请重试原请求确认结果，不要重复填写新的咨询。</span>
      <div class="support-actions" style="margin-top: 10px">
        <button class="primary-button compact" type="button" :disabled="command.busy.value" @click="submit(true)">{{ command.busy.value ? '正在确认…' : '原请求重试' }}</button>
        <RouterLink class="secondary-button" to="/account/tickets">先看我的咨询</RouterLink>
      </div>
    </div>

    <div class="support-submit-layout">
      <section class="paper-section">
        <div class="section-heading"><div><p>NEW INQUIRY</p><h2 class="wm-icon-label"><SketchIcon name="ticket" :size="28" /> 提交咨询</h2></div><span>所有字段均会写入审计</span></div>
        <form @submit.prevent="submit()">
          <div v-if="error" class="error-summary" role="alert" style="margin-bottom: 20px"><strong>未能提交</strong><span>{{ error }}</span></div>

          <fieldset class="support-type-grid" aria-label="咨询类型">
            <legend class="sr-only" style="position: absolute; left: -9999px">咨询类型</legend>
            <label v-for="(hint, value) in typeHints" :key="value" class="support-type-option">
              <input v-model="form.type" :value="value" name="ticket-type" type="radio" />
              <span><strong>{{ ticketTypeLabels[value as TicketType] }}</strong><span>{{ hint }}</span></span>
            </label>
          </fieldset>

          <div class="field" style="margin-bottom: 17px">
            <label for="ticket-subject">主题</label>
            <input id="ticket-subject" v-model="form.subject" maxlength="100" minlength="2" required :aria-invalid="Boolean(fields.subject)" placeholder="用一句话概括您的问题" />
            <span v-if="fields.subject" class="field-error">{{ fields.subject }}</span>
          </div>

          <div class="field" style="margin-bottom: 17px">
            <label for="ticket-body">问题描述</label>
            <textarea id="ticket-body" v-model="form.body" maxlength="2000" minlength="10" required rows="6" :aria-invalid="Boolean(fields.body)" placeholder="请描述具体情况（至少 10 个字符），涉及订单或商品时请提供单号、SKU 等线索" />
            <span v-if="fields.body" class="field-error">{{ fields.body }}</span>
          </div>

          <div v-if="needProduct" class="field" style="margin-bottom: 17px">
            <label for="ticket-product">咨询商品</label>
            <select id="ticket-product" v-model="form.productId" :aria-invalid="Boolean(fields.productId)">
              <option value="">请选择商品（在售商品前 50 件）</option>
              <option v-for="product in products" :key="product.id" :value="product.id">{{ product.name }}（{{ product.sku }}）</option>
            </select>
            <span v-if="fields.productId" class="field-error">{{ fields.productId }}</span>
          </div>

          <div v-if="needOrder" class="field" style="margin-bottom: 17px">
            <label for="ticket-order">关联订单</label>
            <select id="ticket-order" v-model="form.orderId" :aria-invalid="Boolean(fields.orderId)">
              <option value="">请选择本人订单（最近 20 笔）</option>
              <option v-for="order in orders" :key="order.id" :value="order.id">{{ order.orderNumber }} · {{ order.status }}</option>
            </select>
            <span v-if="fields.orderId" class="field-error">{{ fields.orderId }}</span>
          </div>

          <div class="field" style="margin-bottom: 24px">
            <label for="ticket-phone">联系电话（可选）</label>
            <input id="ticket-phone" v-model="form.phone" inputmode="tel" maxlength="20" placeholder="+86 138 0000 0000" />
            <span v-if="fields.phone" class="field-error">{{ fields.phone }}</span>
          </div>

          <div class="form-actions">
            <button class="primary-button compact" type="submit" :disabled="command.busy.value">{{ command.busy.value ? '正在提交…' : '提交咨询' }}</button>
          </div>
        </form>
      </section>

      <aside class="support-aside">
        <section class="support-aside-card">
          <h3>处理流程</h3>
          <p>提交后工单进入“待处理”；管理员开始处理后转为“处理中”，公开回复后转为“已回复”。
            若您在“已回复”后继续补充，工单会回到“处理中”等待再次回复。</p>
        </section>
        <section class="support-aside-card">
          <h3>频率限制</h3>
          <p>提交与补充共用联系类写入限额（每 10 分钟 20 次），超限会暂时拒绝，请稍后再试。</p>
        </section>
        <section class="support-aside-card">
          <h3 class="wm-icon-label"><SketchIcon name="history" :size="26" /> 历史咨询</h3>
          <p>已提交的咨询可在“我的咨询”中随时查看、补充或关闭。</p>
          <div class="support-actions" style="margin-top: 14px">
            <RouterLink class="secondary-button" to="/account/tickets">进入我的咨询</RouterLink>
          </div>
        </section>
      </aside>
    </div>
  </SiteShell>
</template>
