<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import SiteShell from '../../components/SiteShell.vue'
import { createApplication, getApplications, resubmitApplication } from '../../features/dealership/api'
import { applicationLabels, businessLabels, type DealerApplication } from '../../features/dealership/types'
import { ApiProblem } from '../../services/http'
import '../../features/dealership/style.css'

const application = ref<DealerApplication | null>(null)
const loading = ref(true); const busy = ref(false); const error = ref('')
const fields = reactive<Record<string, string>>({})
const form = reactive({ companyName: '', businessType: 'RETAIL', countryOrRegion: '中国', city: '', contactName: '', phone: '', cooperationEmail: '', businessChannels: '', website: '', cooperationIntent: '', publicChannelConsent: false })

function fillCurrent(value: DealerApplication) {
  const current = value.versions.find((item) => item.contentVersion === value.currentContentVersion)
  if (current) Object.assign(form, {
    companyName: current.companyName, businessType: current.businessType,
    countryOrRegion: current.countryOrRegion, city: current.city, contactName: current.contactName,
    phone: current.phone, cooperationEmail: current.cooperationEmail,
    businessChannels: current.businessChannels, website: current.website ?? '',
    cooperationIntent: current.cooperationIntent, publicChannelConsent: current.publicChannelConsent,
  })
}
async function load() {
  loading.value = true; error.value = ''
  try { application.value = (await getApplications()).items[0] ?? null; if (application.value?.status === 'REJECTED') fillCurrent(application.value) }
  catch (cause) { error.value = (cause as Error).message } finally { loading.value = false }
}
async function submit() {
  busy.value = true; error.value = ''; Object.keys(fields).forEach((key) => delete fields[key])
  try {
    const body = { ...form, website: form.website || null }
    application.value = application.value
      ? await resubmitApplication(application.value.id, { ...body, applicationVersion: application.value.currentContentVersion })
      : await createApplication(body)
    ElMessage.success('合作申请已提交')
  } catch (cause) {
    error.value = (cause as Error).message
    if (cause instanceof ApiProblem) cause.problem.errors?.forEach((item) => { fields[item.field] = item.message })
  } finally { busy.value = false }
}
onMounted(load)
</script>

<template>
  <SiteShell title="经销合作申请" eyebrow="DEALERSHIP / APPLICATION">
    <div v-if="loading" class="dealer-empty">正在读取申请记录…</div>
    <p v-else-if="error && !application" class="error-summary" role="alert">{{ error }}</p>
    <template v-else>
      <section v-if="application && application.status !== 'REJECTED'" class="paper-section dealer-application-summary">
        <header><div><p>APPLICATION {{ application.applicationNumber }}</p><h2>{{ applicationLabels[application.status] }}</h2></div><span>内容版本 v{{ application.currentContentVersion }}</span></header>
        <p v-if="application.publicReason" class="dealer-review-note">审核说明：{{ application.publicReason }}</p>
        <dl><div v-for="(value, label) in application.versions.at(-1)" :key="label"><dt>{{ label }}</dt><dd>{{ value ?? '—' }}</dd></div></dl>
        <RouterLink v-if="application.status === 'APPROVED'" class="primary-button" to="/dealer/catalog">进入经销目录</RouterLink>
      </section>
      <section v-else class="paper-section">
        <div class="section-heading"><div><p>{{ application ? 'RESUBMIT' : 'NEW APPLICATION' }}</p><h2>{{ application ? '修订并重新提交' : '企业合作资料' }}</h2></div><span>申请资料默认不公开</span></div>
        <p v-if="application?.publicReason" class="dealer-review-note">上次驳回原因：{{ application.publicReason }}</p>
        <form class="dealer-form" @submit.prevent="submit">
          <p v-if="error" class="error-summary" role="alert">{{ error }}</p>
          <label>企业名称<input v-model="form.companyName" maxlength="100" required /><small v-if="fields.companyName">{{ fields.companyName }}</small></label>
          <label>业务类型<select v-model="form.businessType"><option v-for="(label, value) in businessLabels" :key="value" :value="value">{{ label }}</option></select></label>
          <label>国家／地区<input v-model="form.countryOrRegion" maxlength="100" required /></label>
          <label>城市<input v-model="form.city" maxlength="100" required /></label>
          <label>联系人<input v-model="form.contactName" maxlength="50" required /><small v-if="fields.contactName">{{ fields.contactName }}</small></label>
          <label>联系电话<input v-model="form.phone" maxlength="40" inputmode="tel" required /><small v-if="fields.phone">{{ fields.phone }}</small></label>
          <label>合作邮箱<input v-model="form.cooperationEmail" maxlength="254" type="email" required /><small v-if="fields.cooperationEmail">{{ fields.cooperationEmail }}</small></label>
          <label>网站（可选）<input v-model="form.website" maxlength="2048" type="url" placeholder="https://" /><small v-if="fields.website">{{ fields.website }}</small></label>
          <label class="wide">经营渠道说明<textarea v-model="form.businessChannels" maxlength="2000" required></textarea><small v-if="fields.businessChannels">{{ fields.businessChannels }}</small></label>
          <label class="wide">合作意向<textarea v-model="form.cooperationIntent" minlength="10" maxlength="2000" required></textarea><small v-if="fields.cooperationIntent">{{ fields.cooperationIntent }}</small></label>
          <label class="wide dealer-check"><input v-model="form.publicChannelConsent" type="checkbox" /><span>允许管理员在另行确认公开字段后，将企业关联到公开购买渠道</span></label>
          <div class="wide dealer-form-actions"><button class="primary-button" type="submit" :disabled="busy">{{ busy ? '提交中…' : application ? '提交修订版本' : '提交合作申请' }}</button></div>
        </form>
      </section>
    </template>
  </SiteShell>
</template>
