<script setup lang="ts">
import SketchIcon from '../../components/SketchIcon.vue'
import { computed, onMounted, ref } from 'vue'
import PublicShell from '../../components/PublicShell.vue'
import ProductArtwork from '../../components/ProductArtwork.vue'
import { useCartStore } from '../../features/commerce/cartStore'
import { formatCny } from '../../features/catalog/presentation'
import { api } from '../../services/http'

const store = useCartStore()
const error = ref('')
const busy = ref(false)
const itemCount = computed(() => store.cart?.items.reduce((total, item) => total + item.quantity, 0) ?? 0)

async function load() {
  error.value = ''
  try {
    await store.load()
  } catch (cause) {
    error.value = (cause as Error).message
  }
}

async function change(id: string | null, quantity?: number) {
  if (!store.cart || busy.value) return
  const version = store.cart.cartVersion
  busy.value = true
  error.value = ''
  try {
    if (quantity !== undefined) {
      await api(`/cart/items/${id}`, {
        method: 'PATCH',
        body: JSON.stringify({ quantity, cartVersion: version }),
      })
    } else {
      await api(`/cart/items${id ? '/' + id : ''}?cartVersion=${version}`, { method: 'DELETE' })
    }
  } catch (cause) {
    error.value = (cause as Error).message + ' 已重新读取购物车，请核对后再操作。'
  } finally {
    await load()
    busy.value = false
  }
}

onMounted(load)
</script>

<template>
  <PublicShell>
    <section class="commerce-page commerce-cart-page" aria-labelledby="cart-title">
      <header class="commerce-page-head">
        <div>
          <p class="commerce-eyebrow">MY WEMOVE / CART</p>
          <h1 id="cart-title"><SketchIcon name="cart" :size="44" />购物车</h1>
          <p class="commerce-lede">整车结算，保留零售价格快照。运费、额外税费与优惠均为 ¥0.00。</p>
        </div>
        <div class="commerce-head-meta">
          <span class="commerce-count">{{ itemCount }} 件商品 · {{ store.cart?.items.length ?? 0 }} 个商品行</span>
          <button class="secondary-button" type="button" :disabled="busy" @click="load">
            {{ busy ? '正在更新…' : '刷新购物车' }}
          </button>
        </div>
      </header>

      <p v-if="error" class="error-summary commerce-error" role="alert">{{ error }}</p>

      <template v-if="store.cart">
        <div v-if="!store.cart.items.length" class="commerce-empty">
          <span class="commerce-empty-mark" aria-hidden="true"><SketchIcon name="cart" :size="52" /></span>
          <h2>购物车还是空的</h2>
          <p>从一件适合全家参与的商品开始，回到商品目录继续挑选。</p>
          <RouterLink class="primary-button compact" to="/products">继续选购 <span aria-hidden="true">→</span></RouterLink>
        </div>

        <div v-else class="commerce-cart-layout">
          <section class="commerce-cart-list" aria-labelledby="cart-items-title">
            <header class="commerce-list-header">
              <div>
                <p>ORDER BUILD</p>
                <h2 id="cart-items-title">商品清单</h2>
              </div>
              <span aria-live="polite">{{ busy ? '数量保存中' : '价格以结算预览为准' }}</span>
            </header>

            <div class="commerce-cart-columns" aria-hidden="true">
              <span>商品</span><span>数量</span><span>单价</span><span>小计</span><span></span>
            </div>

            <article
              v-for="item in store.cart.items"
              :key="item.productId"
              class="commerce-cart-row"
              :class="{ 'commerce-cart-row--invalid': !item.valid }"
            >
              <div class="commerce-item-art">
                <ProductArtwork :name="item.name" :sku="item.sku" compact />
              </div>
              <div class="commerce-item-main">
                <RouterLink class="commerce-item-name" :to="'/products/' + item.productId">{{ item.name }}</RouterLink>
                <p class="commerce-item-meta">{{ item.sku }} · 零售价 {{ formatCny(item.unitPriceFen) }}</p>
                <p v-if="!item.valid && item.reason" class="commerce-inline-alert" role="status">{{ item.reason }}</p>
                <p v-if="item.priceChanged" class="commerce-inline-alert commerce-inline-alert--price" role="status">
                  价格已变更：{{ formatCny(item.previousUnitPriceFen) }} → {{ formatCny(item.unitPriceFen) }}；请重新预览确认。
                </p>
              </div>
              <div class="commerce-item-field">
                <label :for="`cart-quantity-${item.productId}`">
                  数量
                  <input
                    :id="`cart-quantity-${item.productId}`"
                    class="commerce-quantity-input"
                    type="number"
                    min="1"
                    max="99"
                    :value="item.quantity"
                    :disabled="busy"
                    :aria-label="`修改 ${item.name} 数量`"
                    @change="change(item.productId, Number(($event.target as HTMLInputElement).value))"
                  />
                </label>
              </div>
              <div class="commerce-item-price"><span>单价</span><strong>{{ formatCny(item.unitPriceFen) }}</strong></div>
              <div class="commerce-item-subtotal"><span>小计</span><strong>{{ formatCny(item.subtotalFen) }}</strong></div>
              <button class="commerce-remove-button" type="button" :disabled="busy" @click="change(item.productId)" :aria-label="'移除' + item.name"><SketchIcon name="trash" :size="25" /></button>
            </article>
          </section>

          <aside class="commerce-summary commerce-summary--sticky" aria-labelledby="cart-summary-title">
            <header>
              <p>ORDER SUMMARY</p>
              <h2 id="cart-summary-title">金额摘要</h2>
            </header>
            <dl>
              <div><dt>商品金额</dt><dd>{{ formatCny(store.cart.totalFen) }}</dd></div>
              <div><dt>运费</dt><dd>¥0.00</dd></div>
              <div><dt>额外税费</dt><dd>¥0.00</dd></div>
              <div><dt>优惠</dt><dd>−¥0.00</dd></div>
            </dl>
            <div class="commerce-summary-total"><span>预计合计</span><strong>{{ formatCny(store.cart.totalFen) }}</strong></div>
            <RouterLink
              v-if="store.cart.canCheckout && !busy"
              class="primary-button"
              to="/checkout"
            >前往结算 <span aria-hidden="true">→</span></RouterLink>
            <p v-else class="commerce-summary-note">请先修正不可售商品或数量，确认后才能继续结算。</p>
            <button class="secondary-button" type="button" :disabled="busy" @click="change(null)">清空购物车</button>
            <p class="commerce-summary-note">结算时会再次核对购物车版本、价格与商品可售状态。</p>
          </aside>
        </div>
      </template>
    </section>
  </PublicShell>
</template>
