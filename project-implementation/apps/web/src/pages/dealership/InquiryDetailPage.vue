<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useRoute } from 'vue-router'
import SiteShell from '../../components/SiteShell.vue'
import { getInquiry, inquiryCommand } from '../../features/dealership/api'
import { inquiryLabels, type Inquiry } from '../../features/dealership/types'
import { formatCny } from '../../features/catalog/presentation'
import '../../features/dealership/style.css'

const props = withDefaults(defineProps<{ admin?: boolean }>(), { admin: false }); const route = useRoute()
const inquiry = ref<Inquiry | null>(null); const loading = ref(true); const busy = ref(false); const error = ref('')
const reply = ref(''); const closeReason = ref(''); const replyPrices = ref<Record<string, number | null>>({}); const leadTimes = ref<Record<string, string>>({})
async function load() { loading.value = true; try { inquiry.value = await getInquiry(route.params.id as string, props.admin); inquiry.value.items.forEach((item) => { replyPrices.value[item.id] = item.replyReferenceUnitPriceFen ?? null; leadTimes.value[item.id] = item.replyLeadTimeText ?? '' }) } catch (cause) { error.value = (cause as Error).message } finally { loading.value = false } }
async function command(action: 'start' | 'replies' | 'close') {
  if (!inquiry.value) return; busy.value = true; error.value = ''
  try {
    let body: unknown = { expectedVersion: inquiry.value.version }
    if (action === 'replies') body = { expectedVersion: inquiry.value.version, body: reply.value, items: inquiry.value.items.map((item) => ({ itemId: item.id, referenceUnitPriceFen: replyPrices.value[item.id] || null, leadTimeText: leadTimes.value[item.id] || null })) }
    if (action === 'close') body = { expectedVersion: inquiry.value.version, reason: closeReason.value }
    inquiry.value = await inquiryCommand(inquiry.value.id, action, body, props.admin)
    reply.value = ''; closeReason.value = ''; ElMessage.success('询价状态已更新')
  } catch (cause) { error.value = (cause as Error).message } finally { busy.value = false }
}
onMounted(load)
</script>

<template>
  <SiteShell :title="inquiry?.inquiryNumber ?? '询价详情'" :admin="admin" :eyebrow="admin ? 'OPERATIONS / INQUIRY' : 'DEALER / INQUIRY'">
    <div v-if="loading" class="dealer-empty">正在读取询价详情…</div><p v-else-if="error && !inquiry" class="error-summary">{{ error }}</p>
    <template v-else-if="inquiry">
      <section class="dealer-detail-head"><div><span class="dealer-status" :data-status="inquiry.status">{{ inquiryLabels[inquiry.status] }}</span><p>创建于 {{ new Date(inquiry.createdAt).toLocaleString('zh-CN') }}</p></div><RouterLink class="secondary-button" :to="admin ? '/admin/inquiries' : '/account/inquiries'">返回列表</RouterLink></section>
      <p v-if="error" class="error-summary" role="alert">{{ error }}</p>
      <div class="dealer-detail-grid">
        <section class="paper-section"><h2>商品与价格快照</h2><div class="dealer-line-items"><article v-for="item in inquiry.items" :key="item.id"><div><p>{{ item.sku }}</p><h3>{{ item.name }}</h3></div><span>{{ item.quantity }} 件</span><strong>{{ formatCny(item.referenceUnitPriceFenSnapshot) }} / 件</strong><small v-if="item.replyReferenceUnitPriceFen">回复价 {{ formatCny(item.replyReferenceUnitPriceFen) }} · {{ item.replyLeadTimeText || '交期待确认' }}</small></article></div></section>
        <aside class="paper-section dealer-demand"><h2>采购需求</h2><dl><div><dt>期望日期</dt><dd>{{ inquiry.expectedDeliveryDate || '未填写' }}</dd></div><div><dt>交付说明</dt><dd>{{ inquiry.deliveryNotes || '未填写' }}</dd></div><div><dt>用途</dt><dd>{{ inquiry.purpose || '未填写' }}</dd></div><div><dt>备注</dt><dd>{{ inquiry.remark || '未填写' }}</dd></div></dl><div v-if="inquiry.publicReply" class="dealer-reply"><strong>管理员回复</strong><p>{{ inquiry.publicReply }}</p></div></aside>
      </div>
      <section v-if="inquiry.status !== 'CLOSED'" class="paper-section dealer-actions-panel">
        <template v-if="admin">
          <button v-if="inquiry.status === 'NEW'" class="secondary-button" type="button" :disabled="busy" @click="command('start')">标记处理中</button>
          <div class="dealer-reply-form"><label>公开回复<textarea v-model="reply" maxlength="2000"></textarea></label><div v-for="item in inquiry.items" :key="item.id" class="dealer-reply-line"><strong>{{ item.name }}</strong><label>回复参考价（分）<input v-model.number="replyPrices[item.id]" type="number" min="1" max="99999999" /></label><label>预计交期<input v-model="leadTimes[item.id]" maxlength="500" /></label></div><button class="primary-button" type="button" :disabled="busy || !reply.trim()" @click="command('replies')">保存公开回复</button></div>
        </template>
        <div class="dealer-close"><label>关闭原因<input v-model="closeReason" maxlength="500" placeholder="请填写 2—500 个字符" /></label><button class="danger-button" type="button" :disabled="busy || closeReason.trim().length < 2" @click="command('close')">关闭询价</button></div>
      </section>
      <section class="paper-section"><h2>状态历史</h2><ol class="dealer-history"><li v-for="item in inquiry.history" :key="`${item.createdAt}-${item.action}`"><strong>{{ inquiryLabels[item.toStatus] }}</strong><span>{{ new Date(item.createdAt).toLocaleString('zh-CN') }}{{ item.reason ? ` · ${item.reason}` : '' }}</span></li></ol></section>
    </template>
  </SiteShell>
</template>
