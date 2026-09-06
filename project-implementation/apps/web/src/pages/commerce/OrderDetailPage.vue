<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElDialog } from 'element-plus'
import PublicShell from '../../components/PublicShell.vue'
import SiteShell from '../../components/SiteShell.vue'
import ProductArtwork from '../../components/ProductArtwork.vue'
import { useSessionStore } from '../../stores/session'
import { readOrder } from '../../features/commerce/api'
import { useCommandRecovery } from '../../features/commerce/commandRecovery'
import { statusLabels, type Detail } from '../../features/commerce/types'
import { formatCny } from '../../features/catalog/presentation'

const props = withDefaults(defineProps<{ admin?: boolean }>(), { admin: false })
const route = useRoute()
const session = useSessionStore()
const order = ref<Detail | null>(null)
const error = ref('')
const loading = ref(true)
const selected = ref('')
const outcome = ref('SUCCESS')
const reason = ref('')
const logisticsName = ref('模拟物流')
const trackingNumber = ref('')
const actionDialogOpen = ref(false)
const actionError = ref('')
const command = useCommandRecovery(
  () => session.actor?.id,
  (props.admin ? 'adminOrder:' : 'ownOrder:') + route.params.id,
)

const actionLabels: Record<string, string> = {
  MOCK_PAYMENT: '模拟付款',
  CANCEL: '取消整单',
  MOCK_SHIPMENT: '模拟整单发货',
  CONFIRM_RECEIPT: '确认本人收货',
}

const nextSteps: Record<string, string> = {
  PENDING_PAYMENT: '下一步：完成一次模拟付款。失败会保留待付款状态，成功后进入待发货。',
  PAID: '下一步：管理员确认模拟发货，之后收货人才能确认收到整单商品。',
  SHIPPED: '下一步：物流送达后由本人确认收货，订单才会进入已完成。',
  COMPLETED: '订单已完成，所有付款、物流和状态历史均保留供核对。',
  CANCELLED: '订单已取消，已付款订单的模拟退款和库存返还记录在交易历史中。',
}

const actionTitle = computed(() => (selected.value ? `确认${actionLabels[selected.value] ?? '操作'}` : '确认订单操作'))

function statusLabel(value: string) {
  return statusLabels[value] ?? value
}

function statusClass(value: string) {
  return `commerce-status--${value.toLowerCase().replace(/_/g, '-')}`
}

function formatDate(value: string) {
  return new Date(value).toLocaleString('zh-CN', { dateStyle: 'medium', timeStyle: 'short' })
}

function addressText() {
  if (!order.value) return ''
  const address = order.value.shippingAddress
  return [address.countryOrRegion, address.region, address.city, address.addressLine].filter(Boolean).join(' · ')
}

function actionClass(action: string) {
  if (action === 'CANCEL') return 'danger-button'
  if (action === 'MOCK_PAYMENT' || action === 'MOCK_SHIPMENT') return 'primary-button'
  return 'secondary-button'
}

async function load() {
  loading.value = true
  error.value = ''
  try {
    order.value = await readOrder(String(route.params.id), props.admin)
  } catch (cause) {
    error.value = (cause as Error).message
  } finally {
    loading.value = false
  }
}

function openAction(action: string) {
  selected.value = action
  actionError.value = ''
  actionDialogOpen.value = true
}

function closeAction() {
  if (!command.busy.value) actionDialogOpen.value = false
}

function validateAction() {
  if (selected.value === 'CANCEL' && reason.value.trim().length < 2) {
    actionError.value = '请填写至少2个字符的取消原因。'
    return false
  }
  if (selected.value === 'MOCK_SHIPMENT') {
    if (logisticsName.value.trim().length < 2) {
      actionError.value = '请填写模拟物流名称。'
      return false
    }
    if (!/^[A-Za-z0-9-]{3,50}$/.test(trackingNumber.value.trim())) {
      actionError.value = '运单号需为3—50位字母、数字或连字符。'
      return false
    }
  }
  return true
}

async function submit(retry = false) {
  if (!order.value) return
  if (!retry && !validateAction()) return
  error.value = ''
  actionError.value = ''
  try {
    if (retry) {
      await command.retry<Detail>()
    } else {
      const paths: Record<string, string> = {
        MOCK_PAYMENT: 'mock-payments',
        CANCEL: 'cancel',
        MOCK_SHIPMENT: 'mock-shipment',
        CONFIRM_RECEIPT: 'confirm-receipt',
      }
      const body: Record<string, unknown> = { expectedVersion: order.value.version }
      if (selected.value === 'MOCK_PAYMENT') body.outcome = outcome.value
      if (selected.value === 'CANCEL') body.reason = reason.value.trim()
      if (selected.value === 'MOCK_SHIPMENT') {
        body.logisticsName = logisticsName.value.trim()
        body.trackingNumber = trackingNumber.value.trim()
      }
      await command.send<Detail>(
        `${props.admin ? '/admin' : ''}/orders/${order.value.id}/${paths[selected.value]}`,
        body,
      )
    }
    selected.value = ''
    actionDialogOpen.value = false
    await load()
  } catch (cause) {
    error.value = (cause as Error).message
    actionDialogOpen.value = false
    if (!command.pending.value) {
      selected.value = ''
      await load()
    }
  }
}

onMounted(() => void load())
</script>

<template>
  <component :is="props.admin ? SiteShell : PublicShell" :title="props.admin ? '订单详情' : undefined" :admin="props.admin" eyebrow="ORDER / SIMULATED">
    <section
      :class="['commerce-content commerce-detail-page', { 'commerce-page': !props.admin }]"
      :aria-labelledby="props.admin ? 'order-detail-title' : 'own-order-title'"
    >
      <h1 v-if="!props.admin" id="own-order-title" class="sr-only">订单详情</h1>
      <RouterLink class="commerce-back-link" :to="props.admin ? '/admin/orders' : '/account/orders'">← 返回订单列表</RouterLink>
      <p v-if="error" class="error-summary commerce-error" role="alert">{{ error }}</p>

      <div v-if="loading" class="commerce-state-panel" aria-live="polite"><p>正在读取订单详情…</p></div>
      <div v-else-if="!order" class="commerce-state-panel" role="alert"><h2>无法打开订单</h2><p>请刷新页面，或从订单列表重新进入。</p><button class="secondary-button" type="button" @click="load">重试</button></div>

      <template v-else>
        <header class="commerce-detail-lead">
          <div>
            <p class="commerce-eyebrow">ORDER / SIMULATED</p>
            <span class="commerce-status" :class="statusClass(order.status)">{{ statusLabel(order.status) }}</span>
            <h2 id="order-detail-title">{{ order.orderNumber }}</h2>
            <p>创建于 {{ formatDate(order.createdAt) }} · 订单版本 {{ order.version }}</p>
          </div>
          <div class="commerce-detail-total"><span>订单合计</span><strong>{{ formatCny(order.totalFen) }}</strong></div>
        </header>

        <div v-if="command.pending.value" class="commerce-notice commerce-notice--pending commerce-recovery commerce-recovery--detail" role="status">
          <strong>上次操作结果尚未确认</strong>
          <p>原请求键已保留；请重试原请求或先刷新订单核对结果，不要创建同一业务的新操作。</p>
          <div class="commerce-button-row">
            <button class="primary-button compact" type="button" :disabled="command.busy.value" @click="submit(true)">{{ command.busy.value ? '正在确认…' : '原请求重试' }}</button>
            <button class="secondary-button" type="button" :disabled="command.busy.value" @click="load">刷新订单</button>
          </div>
        </div>

        <div class="commerce-detail-layout">
          <div class="commerce-detail-main">
            <section class="commerce-detail-section" aria-labelledby="snapshot-title">
              <header><div><p>01 / SNAPSHOT</p><h2 id="snapshot-title">商品快照</h2></div><span>成交价格与数量已固定</span></header>
              <div class="commerce-detail-items">
                <article v-for="item in order.items" :key="item.productId" class="commerce-detail-item">
                  <ProductArtwork :name="item.name" :sku="item.sku" compact />
                  <div><strong>{{ item.name }}</strong><span>{{ item.sku }} · 数量 {{ item.quantity }} · 单价 {{ formatCny(item.unitPriceFen) }}</span></div>
                  <strong>{{ formatCny(item.subtotalFen) }}</strong>
                </article>
              </div>
            </section>

            <section class="commerce-detail-section" aria-labelledby="shipping-snapshot-title">
              <header><div><p>02 / DELIVERY</p><h2 id="shipping-snapshot-title">地址与物流</h2></div><span>订单创建时保存的收货快照</span></header>
              <dl class="commerce-kv-grid">
                <div class="commerce-kv"><dt>收件人</dt><dd>{{ order.shippingAddress.recipient }}</dd></div>
                <div class="commerce-kv"><dt>联系电话</dt><dd>{{ order.shippingAddress.phone }}</dd></div>
                <div class="commerce-kv commerce-kv--wide"><dt>收货地址</dt><dd>{{ addressText() }}</dd></div>
                <div class="commerce-kv commerce-kv--wide"><dt>订单备注</dt><dd>{{ order.remark || '无备注' }}</dd></div>
                <div class="commerce-kv commerce-kv--wide"><dt>物流</dt><dd>{{ order.trackingNumber ? `${order.logisticsName || '模拟物流'} · ${order.trackingNumber}` : '尚未生成物流信息' }}</dd></div>
              </dl>
            </section>

            <section class="commerce-detail-section" aria-labelledby="payments-title">
              <header><div><p>03 / TRANSACTIONS</p><h2 id="payments-title">付款与退款</h2></div><span>模拟流水号仅用于课程核对</span></header>
              <p v-if="!order.paymentAttempts.length && !order.refunds.length" class="commerce-empty-events">尚无付款或退款记录。</p>
              <ul v-else class="commerce-events">
                <li v-for="payment in order.paymentAttempts" :key="payment.id" class="commerce-event">
                  <span>付款尝试</span>
                  <div><strong>{{ payment.outcome === 'SUCCESS' ? '模拟付款成功' : '模拟付款失败' }} · {{ formatCny(payment.amountFen) }}</strong><span class="commerce-event-reference">{{ payment.simulationReference }}</span></div>
                </li>
                <li v-for="refund in order.refunds" :key="refund.id" class="commerce-event">
                  <span>退款</span>
                  <div><strong>模拟退款成功 · {{ formatCny(refund.amountFen) }}</strong><span class="commerce-event-reference">{{ refund.simulationReference }}</span></div>
                </li>
              </ul>
            </section>

            <section class="commerce-detail-section" aria-labelledby="history-title">
              <header><div><p>04 / TIMELINE</p><h2 id="history-title">订单时间线</h2></div><span>{{ order.history.length }} 个状态节点</span></header>
              <ol class="commerce-events">
                <li v-for="history in order.history" :key="history.version" class="commerce-event">
                  <time>{{ formatDate(history.createdAt) }}</time>
                  <div><strong>{{ history.fromStatus ? `${statusLabel(history.fromStatus)} → ` : '' }}{{ statusLabel(history.toStatus) }}</strong><p>{{ history.reason || '状态已更新' }}</p></div>
                </li>
              </ol>
            </section>
          </div>

          <aside class="commerce-detail-aside">
            <section class="commerce-amount-card" aria-labelledby="amount-title">
              <header><p>ORDER TOTAL</p><h2 id="amount-title">状态与金额</h2></header>
              <dl>
                <div><dt>商品金额</dt><dd>{{ formatCny(order.subtotalFen) }}</dd></div>
                <div><dt>运费</dt><dd>{{ formatCny(order.shippingFen) }}</dd></div>
                <div><dt>额外税费</dt><dd>{{ formatCny(order.taxFen) }}</dd></div>
                <div><dt>优惠</dt><dd>−{{ formatCny(order.discountFen) }}</dd></div>
              </dl>
              <div class="commerce-summary-total"><span>订单合计</span><strong>{{ formatCny(order.totalFen) }}</strong></div>
              <p class="commerce-next-step">{{ nextSteps[order.status] ?? '请以当前订单状态和可用操作为准。' }}</p>
              <div v-if="order.allowedActions.length" class="commerce-button-row">
                <button
                  v-for="action in order.allowedActions"
                  :key="action"
                  :class="actionClass(action)"
                  type="button"
                  @click="openAction(action)"
                >{{ actionLabels[action] ?? action }}</button>
              </div>
            </section>
          </aside>
        </div>

        <ElDialog v-model="actionDialogOpen" class="commerce-action-dialog" :title="actionTitle" width="min(520px, 92vw)" :close-on-click-modal="false">
          <form @submit.prevent="submit()">
            <p class="commerce-dialog-copy">
              {{ selected === 'CANCEL' ? '取消后无法恢复；已付款订单会整单返还库存并生成一次模拟退款。' : selected === 'MOCK_PAYMENT' ? '付款结果仅用于模拟订单状态与库存校验。' : selected === 'MOCK_SHIPMENT' ? '发货后订单将进入已发货，取消操作会受到状态限制。' : '请确认你已收到本订单中的全部商品。' }}
            </p>
            <p v-if="actionError" class="error-summary" role="alert">{{ actionError }}</p>
            <label v-if="selected === 'MOCK_PAYMENT'" class="commerce-field">
              <span>模拟结果</span>
              <select v-model="outcome" class="commerce-field__control"><option value="SUCCESS">成功</option><option value="FAILURE">失败（保留待付款状态）</option></select>
            </label>
            <label v-if="selected === 'CANCEL'" class="commerce-field">
              <span>取消原因</span>
              <textarea v-model="reason" class="commerce-field__control" minlength="2" maxlength="500" />
            </label>
            <template v-if="selected === 'MOCK_SHIPMENT'">
              <label class="commerce-field"><span>模拟物流名称</span><input v-model="logisticsName" class="commerce-field__control" minlength="2" maxlength="50" /></label>
              <label class="commerce-field"><span>模拟运单号</span><input v-model="trackingNumber" class="commerce-field__control" pattern="[A-Za-z0-9-]{3,50}" /></label>
            </template>
          </form>
          <template #footer>
            <button class="secondary-button" type="button" :disabled="command.busy.value" @click="closeAction">返回</button>
            <button :class="actionClass(selected)" type="button" :disabled="command.busy.value" @click="submit()">{{ command.busy.value ? '正在提交…' : '确认执行' }}</button>
          </template>
        </ElDialog>
      </template>
    </section>
  </component>
</template>
