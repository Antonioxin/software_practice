<script setup lang="ts">
import SketchIcon from '../../components/SketchIcon.vue'
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import PublicShell from '../../components/PublicShell.vue'
import ProductArtwork from '../../components/ProductArtwork.vue'
import { ApiProblem } from '../../services/http'
import { previewCart } from '../../features/commerce/api'
import { useCommandRecovery } from '../../features/commerce/commandRecovery'
import { useSessionStore } from '../../stores/session'
import { useCartStore } from '../../features/commerce/cartStore'
import { formatCny } from '../../features/catalog/presentation'
import type { Address, Order, Preview } from '../../features/commerce/types'

const session = useSessionStore()
const cart = useCartStore()
const router = useRouter()
const command = useCommandRecovery(() => session.actor?.id, 'createOrder')
const preview = ref<Preview | null>(null)
const error = ref('')
const loading = ref(false)
const remark = ref('')
const now = ref(Date.now())
const formRef = ref<HTMLFormElement | null>(null)
const errorSummary = ref<HTMLElement | null>(null)
const fieldErrors = ref<Record<string, string>>({})
let expiryTimer: number | undefined

const address = reactive<Address>({
  recipient: '',
  phone: '',
  countryOrRegion: '中国',
  region: '',
  city: '',
  addressLine: '',
})

const previewExpired = computed(() => Boolean(preview.value && new Date(preview.value.expiresAt).getTime() <= now.value))

function fieldErrorId(field: string) {
  return `checkout-error-${field}`
}

function previewExpiryText() {
  return preview.value ? new Date(preview.value.expiresAt).toLocaleString('zh-CN') : ''
}

function validateAddress() {
  const next: Record<string, string> = {}
  if (address.recipient.trim().length < 2) next.recipient = '请填写至少2个字符的收件人。'
  const phone = address.phone.replace(/[\s\-()]/g, '')
  if (!/^\+?[0-9]{6,20}$/.test(phone)) next.phone = '请输入6—20位联系电话。'
  if (address.countryOrRegion.trim().length < 2) next.countryOrRegion = '请填写国家或地区。'
  if (address.region.trim() && address.region.trim().length < 2) next.region = '省 / 州至少需要2个字符。'
  if (address.city.trim().length < 2) next.city = '请填写至少2个字符的城市。'
  if (address.addressLine.trim().length < 5) next.addressLine = '详细地址至少需要5个字符。'
  fieldErrors.value = next
  const firstInvalid = Object.keys(next)[0]
  if (firstInvalid) {
    error.value = '请检查标记的地址字段后再创建订单。'
    void nextTick(() => {
      formRef.value?.querySelector<HTMLElement>(`[data-field="${firstInvalid}"]`)?.focus()
    })
    return false
  }
  return true
}

function focusErrorSummary() {
  void nextTick(() => errorSummary.value?.focus())
}

async function load() {
  loading.value = true
  error.value = ''
  try {
    preview.value = await previewCart()
  } catch (cause) {
    error.value = (cause as Error).message
    focusErrorSummary()
  } finally {
    loading.value = false
  }
}

async function submit(retry = false) {
  error.value = ''
  if (!retry && (!preview.value || previewExpired.value)) {
    error.value = '当前金额预览已过期，请重新预览整车后再创建订单。'
    focusErrorSummary()
    return
  }
  if (!retry && !validateAddress()) return
  try {
    const order = retry
      ? await command.retry<Order>()
      : await command.send<Order>('/orders', {
          previewToken: preview.value!.previewToken,
          cartVersion: preview.value!.cartVersion,
          shippingAddress: address,
          remark: remark.value,
        })
    cart.clear()
    await router.push('/account/orders/' + order.id)
  } catch (cause) {
    if (cause instanceof ApiProblem) {
      error.value = [cause.problem.detail, ...(cause.problem.errors?.map((item) => item.message) ?? [])].join(' ')
      for (const item of cause.problem.errors ?? []) {
        const field = item.field.replace(/^shippingAddress\./, '')
        if (field in address) fieldErrors.value[field] = item.message
      }
    } else {
      error.value = (cause as Error).message
    }
    if (
      cause instanceof ApiProblem &&
      ['CART_CHANGED', 'PRICE_CHANGED', 'CHECKOUT_PREVIEW_EXPIRED', 'PRODUCT_UNAVAILABLE', 'INSUFFICIENT_STOCK'].includes(
        cause.problem.code,
      )
    ) {
      preview.value = null
    }
    focusErrorSummary()
  }
}

onMounted(() => {
  expiryTimer = window.setInterval(() => { now.value = Date.now() }, 1000)
  if (!command.pending.value) void load()
})

onBeforeUnmount(() => {
  if (expiryTimer) window.clearInterval(expiryTimer)
})
</script>

<template>
  <PublicShell>
    <section class="commerce-page commerce-checkout-page" aria-labelledby="checkout-title">
      <header class="commerce-page-head">
        <div>
          <p class="commerce-eyebrow">CHECKOUT / SIMULATED</p>
          <h1 id="checkout-title"><SketchIcon name="wallet" :size="44" />确认订单</h1>
          <p class="commerce-lede">确认地址与本次成交快照。下单不占用库存，模拟付款时才会再次检查库存。</p>
        </div>
        <div class="commerce-head-meta">
          <span class="commerce-count">购物车 → 确认订单 → 订单结果</span>
          <RouterLink class="commerce-back-link" to="/cart">← 返回购物车</RouterLink>
        </div>
      </header>

      <p v-if="error" ref="errorSummary" class="error-summary commerce-error" role="alert" tabindex="-1">{{ error }}</p>

      <div v-if="command.pending.value" class="commerce-notice commerce-notice--pending commerce-recovery" role="status">
        <strong>有待确认的建单请求</strong>
        <p>地址、金额预览与原请求键已固定。不要重新填写并提交新订单，请先恢复原请求或查询订单结果。</p>
        <div class="commerce-button-row">
          <button class="primary-button compact" type="button" :disabled="command.busy.value" @click="submit(true)">
            {{ command.busy.value ? '正在确认…' : '使用原请求重试' }}
          </button>
          <RouterLink class="secondary-button" to="/account/orders">查询我的订单</RouterLink>
        </div>
      </div>

      <template v-else>
        <div class="commerce-button-row commerce-button-row--end" style="margin-bottom: 18px">
          <span class="commerce-filter-help">预览有效期15分钟；地址填写会保留在本页。</span>
          <button class="secondary-button" type="button" :disabled="loading" @click="load">
            {{ loading ? '正在预览…' : '重新预览整车' }}
          </button>
        </div>

        <form v-if="preview" ref="formRef" class="checkout-grid" novalidate @submit.prevent="submit()">
          <section class="checkout-card" aria-labelledby="shipping-title">
            <header>
              <div><p>01 / DELIVERY</p><h2 id="shipping-title">收货地址</h2></div>
              <span>保存为订单快照</span>
            </header>

            <div class="checkout-form-fields">
              <label class="commerce-field" :class="{ 'commerce-field--invalid': fieldErrors.recipient }">
                <span>收件人</span>
                <input
                  class="commerce-field__control"
                  data-field="recipient"
                  v-model="address.recipient"
                  autocomplete="name"
                  maxlength="50"
                  :aria-invalid="!!fieldErrors.recipient"
                  :aria-describedby="fieldErrors.recipient ? fieldErrorId('recipient') : undefined"
                  :disabled="command.busy.value"
                />
                <small v-if="fieldErrors.recipient" :id="fieldErrorId('recipient')" class="commerce-field-error">{{ fieldErrors.recipient }}</small>
              </label>
              <label class="commerce-field" :class="{ 'commerce-field--invalid': fieldErrors.phone }">
                <span>联系电话</span>
                <input
                  class="commerce-field__control"
                  data-field="phone"
                  v-model="address.phone"
                  type="tel"
                  autocomplete="tel"
                  maxlength="24"
                  :aria-invalid="!!fieldErrors.phone"
                  :aria-describedby="fieldErrors.phone ? fieldErrorId('phone') : undefined"
                  :disabled="command.busy.value"
                />
                <small v-if="fieldErrors.phone" :id="fieldErrorId('phone')" class="commerce-field-error">{{ fieldErrors.phone }}</small>
              </label>
              <label class="commerce-field" :class="{ 'commerce-field--invalid': fieldErrors.countryOrRegion }">
                <span>国家或地区</span>
                <input
                  class="commerce-field__control"
                  data-field="countryOrRegion"
                  v-model="address.countryOrRegion"
                  autocomplete="country-name"
                  maxlength="100"
                  :aria-invalid="!!fieldErrors.countryOrRegion"
                  :aria-describedby="fieldErrors.countryOrRegion ? fieldErrorId('countryOrRegion') : undefined"
                  :disabled="command.busy.value"
                />
                <small v-if="fieldErrors.countryOrRegion" :id="fieldErrorId('countryOrRegion')" class="commerce-field-error">{{ fieldErrors.countryOrRegion }}</small>
              </label>
              <label class="commerce-field" :class="{ 'commerce-field--invalid': fieldErrors.region }">
                <span>省 / 州 <em>选填</em></span>
                <input
                  class="commerce-field__control"
                  data-field="region"
                  v-model="address.region"
                  autocomplete="address-level1"
                  maxlength="100"
                  :aria-invalid="!!fieldErrors.region"
                  :aria-describedby="fieldErrors.region ? fieldErrorId('region') : undefined"
                  :disabled="command.busy.value"
                />
                <small v-if="fieldErrors.region" :id="fieldErrorId('region')" class="commerce-field-error">{{ fieldErrors.region }}</small>
              </label>
              <label class="commerce-field" :class="{ 'commerce-field--invalid': fieldErrors.city }">
                <span>城市</span>
                <input
                  class="commerce-field__control"
                  data-field="city"
                  v-model="address.city"
                  autocomplete="address-level2"
                  maxlength="100"
                  :aria-invalid="!!fieldErrors.city"
                  :aria-describedby="fieldErrors.city ? fieldErrorId('city') : undefined"
                  :disabled="command.busy.value"
                />
                <small v-if="fieldErrors.city" :id="fieldErrorId('city')" class="commerce-field-error">{{ fieldErrors.city }}</small>
              </label>
              <label class="commerce-field" :class="{ 'commerce-field--invalid': fieldErrors.addressLine }">
                <span>详细地址</span>
                <input
                  class="commerce-field__control"
                  data-field="addressLine"
                  v-model="address.addressLine"
                  autocomplete="street-address"
                  maxlength="200"
                  :aria-invalid="!!fieldErrors.addressLine"
                  :aria-describedby="fieldErrors.addressLine ? fieldErrorId('addressLine') : undefined"
                  :disabled="command.busy.value"
                />
                <small v-if="fieldErrors.addressLine" :id="fieldErrorId('addressLine')" class="commerce-field-error">{{ fieldErrors.addressLine }}</small>
              </label>
              <label class="commerce-field commerce-field--span-2">
                <span>订单备注 <em>选填</em></span>
                <textarea class="commerce-field__control" v-model="remark" maxlength="2000" :disabled="command.busy.value" />
              </label>
            </div>

            <div class="commerce-submit-bar">
              <p>提交后会创建一笔待付款模拟订单；价格和地址按右侧预览固定。</p>
              <button class="primary-button" type="submit" :disabled="command.busy.value || previewExpired">
                {{ command.busy.value ? '正在创建…' : previewExpired ? '预览已过期' : '确认快照并创建订单' }}
              </button>
            </div>
          </section>

          <aside class="checkout-card checkout-card--summary" aria-labelledby="checkout-summary-title">
            <header>
              <div><p>02 / SNAPSHOT</p><h2 id="checkout-summary-title">商品与金额</h2></div>
              <span>{{ preview.items.length }} 个商品行</span>
            </header>
            <div class="commerce-summary-items">
              <article v-for="item in preview.items" :key="item.productId" class="commerce-summary-item">
                <ProductArtwork :name="item.name" :sku="item.sku" compact />
                <div><strong>{{ item.name }}</strong><span>{{ item.sku }} · 数量 {{ item.quantity }}</span></div>
                <strong>{{ formatCny(item.subtotalFen) }}</strong>
              </article>
            </div>
            <dl class="commerce-summary-breakdown">
              <div><dt>商品金额</dt><dd>{{ formatCny(preview.subtotalFen) }}</dd></div>
              <div><dt>运费</dt><dd>{{ formatCny(preview.shippingFen) }}</dd></div>
              <div><dt>额外税费</dt><dd>{{ formatCny(preview.taxFen) }}</dd></div>
              <div><dt>优惠</dt><dd>−{{ formatCny(preview.discountFen) }}</dd></div>
            </dl>
            <div class="commerce-summary-total commerce-summary-total--paper"><span>订单合计</span><strong>{{ formatCny(preview.totalFen) }}</strong></div>
            <p class="commerce-expiry" :class="{ 'commerce-expiry--expired': previewExpired }" role="status">
              {{ previewExpired ? '本次预览已过期，请重新预览以获取可信金额。' : `预览有效至 ${previewExpiryText()}` }}
            </p>
          </aside>
        </form>
      </template>

      <RouterLink v-if="!command.pending.value" class="commerce-back-link" to="/cart">← 返回购物车</RouterLink>
    </section>
  </PublicShell>
</template>
