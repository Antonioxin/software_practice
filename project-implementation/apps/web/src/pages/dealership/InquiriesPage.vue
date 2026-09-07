<script setup lang="ts">
import { onMounted, ref } from 'vue'
import SiteShell from '../../components/SiteShell.vue'
import { getInquiries } from '../../features/dealership/api'
import { inquiryLabels, type Inquiry } from '../../features/dealership/types'
import '../../features/dealership/style.css'

const props = withDefaults(defineProps<{ admin?: boolean }>(), { admin: false })
const inquiries = ref<Inquiry[]>([]); const status = ref(''); const loading = ref(true); const error = ref('')
async function load() {
  loading.value = true; error.value = ''
  try { inquiries.value = (await getInquiries(props.admin, status.value ? `status=${status.value}` : '')).items }
  catch (cause) { error.value = (cause as Error).message } finally { loading.value = false }
}
onMounted(load)
</script>

<template>
  <SiteShell :title="admin ? '询价管理' : '我的询价'" :admin="admin" :eyebrow="admin ? 'OPERATIONS / INQUIRIES' : 'DEALER / INQUIRIES'">
    <section class="paper-section">
      <div class="dealer-toolbar"><select v-model="status" @change="load"><option value="">全部状态</option><option v-for="(label, value) in inquiryLabels" :key="value" :value="value">{{ label }}</option></select><RouterLink v-if="!admin" class="primary-button" to="/dealer/catalog">新建询价</RouterLink></div>
      <p v-if="error" class="error-summary" role="alert">{{ error }}</p>
      <div v-if="loading" class="dealer-empty">正在读取询价…</div>
      <div v-else-if="!inquiries.length" class="dealer-empty"><h2>暂无询价记录</h2><p>{{ admin ? '当前没有符合条件的询价。' : '从经销目录选择商品后，可以提交第一笔询价。' }}</p></div>
      <div v-else class="dealer-list">
        <article v-for="inquiry in inquiries" :key="inquiry.id">
          <div><p>{{ inquiry.inquiryNumber }}</p><h2>{{ inquiry.items.map((item) => item.name).join('、') }}</h2><span>{{ new Date(inquiry.createdAt).toLocaleString('zh-CN') }} · {{ inquiry.items.length }} 项商品</span></div>
          <span class="dealer-status" :data-status="inquiry.status">{{ inquiryLabels[inquiry.status] }}</span>
          <RouterLink class="secondary-button" :to="`${admin ? '/admin' : '/account'}/inquiries/${inquiry.id}`">查看详情</RouterLink>
        </article>
      </div>
    </section>
  </SiteShell>
</template>
