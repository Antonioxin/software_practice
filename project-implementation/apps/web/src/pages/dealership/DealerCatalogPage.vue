<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'
import SiteShell from '../../components/SiteShell.vue'
import { createInquiry, getDealerCatalog } from '../../features/dealership/api'
import type { DealerProduct } from '../../features/dealership/types'
import { formatCny } from '../../features/catalog/presentation'
import '../../features/dealership/style.css'

const router = useRouter(); const products = ref<DealerProduct[]>([]); const quantities = reactive<Record<string, number>>({})
const expectedDeliveryDate = ref(''); const deliveryNotes = ref(''); const purpose = ref(''); const remark = ref('')
const loading = ref(true); const busy = ref(false); const error = ref('')
async function load() { try { products.value = await getDealerCatalog(); products.value.forEach((p) => { quantities[p.id] = 0 }) } catch (cause) { error.value = (cause as Error).message } finally { loading.value = false } }
async function submit() {
  const items = products.value.filter((p) => quantities[p.id] > 0).map((p) => ({ productId: p.id, quantity: quantities[p.id] }))
  if (!items.length) { error.value = '请至少选择一个商品并填写询价数量。'; return }
  busy.value = true; error.value = ''
  try {
    const result = await createInquiry({ items, expectedDeliveryDate: expectedDeliveryDate.value || null, deliveryNotes: deliveryNotes.value || null, purpose: purpose.value || null, remark: remark.value || null })
    ElMessage.success('询价已提交'); await router.push(`/account/inquiries/${result.id}`)
  } catch (cause) { error.value = (cause as Error).message } finally { busy.value = false }
}
onMounted(load)
</script>

<template>
  <SiteShell title="经销专属目录" eyebrow="DEALER / CATALOG">
    <section class="dealer-callout"><strong>统一经销参考价</strong><span>目录只对当前有效合作开放；价格为参考单价，实际合作以管理员回复为准。</span></section>
    <p v-if="error" class="error-summary" role="alert">{{ error }}</p>
    <div v-if="loading" class="dealer-empty">正在校验资格并读取目录…</div>
    <form v-else class="dealer-catalog-layout" @submit.prevent="submit">
      <div class="dealer-product-grid">
        <article v-for="product in products" :key="product.id" class="dealer-product-card">
          <p>{{ product.sku }}</p><h2>{{ product.name }}</h2>
          <strong>{{ formatCny(product.referenceUnitPriceFen) }}</strong><small>{{ product.priceNotice }}</small>
          <dl><div><dt>起询量</dt><dd>{{ product.minInquiryQuantity }} 件</dd></div><div><dt>现有库存</dt><dd>{{ product.availableQuantity }} 件</dd></div><div><dt>参考交期</dt><dd>{{ product.leadTimeText }}</dd></div></dl>
          <label>本次询价数量<input v-model.number="quantities[product.id]" type="number" min="0" max="9999" :placeholder="`至少 ${product.minInquiryQuantity} 件`" /></label>
        </article>
      </div>
      <aside class="paper-section dealer-inquiry-compose">
        <h2>询价需求</h2><p>数量可以超过当前库存，提交不会预占库存或生成订单。</p>
        <label>期望交付日期（可选）<input v-model="expectedDeliveryDate" type="date" /></label>
        <label>交付说明（可选）<textarea v-model="deliveryNotes" maxlength="2000"></textarea></label>
        <label>用途（可选）<textarea v-model="purpose" maxlength="2000"></textarea></label>
        <label>备注（可选）<textarea v-model="remark" maxlength="2000"></textarea></label>
        <button class="primary-button" type="submit" :disabled="busy">{{ busy ? '提交中…' : '提交询价' }}</button>
      </aside>
    </form>
  </SiteShell>
</template>
