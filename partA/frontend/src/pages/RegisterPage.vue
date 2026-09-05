<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import AuthShell from '../components/AuthShell.vue'
import { api, ApiProblem } from '../services/http'
import type { Actor } from '../types'

interface Policy { adultStatement: string; termsVersion: string; termsPath: string; privacyVersion: string; privacyPath: string }
const policy = ref<Policy | null>(null)
const busy = ref(false)
const error = ref('')
const fields = reactive<Record<string, string>>({})
const form = reactive({ email: '', nickname: '', password: '', confirmPassword: '', adultConfirmed: false, termsAccepted: false, privacyAccepted: false })
const router = useRouter()

onMounted(async () => {
  try { policy.value = (await api<Policy>('/auth/registration-policy')).data }
  catch { error.value = '暂时无法读取注册说明，请稍后重试。' }
})

async function submit() {
  if (!policy.value) return
  busy.value = true
  error.value = ''
  Object.keys(fields).forEach((key) => delete fields[key])
  try {
    await api<Actor>('/auth/register', { method: 'POST', body: JSON.stringify({
      ...form, termsVersion: policy.value.termsVersion, privacyVersion: policy.value.privacyVersion,
    }) })
    await router.push({ path: '/login', query: { registered: '1' } })
  } catch (cause) {
    if (cause instanceof ApiProblem) {
      error.value = cause.problem.detail
      cause.problem.errors?.forEach((item) => { fields[item.field] = item.message })
    } else error.value = '无法连接服务，请稍后重试。'
  } finally { busy.value = false }
}
</script>

<template>
  <AuthShell eyebrow="ADULT REGISTRATION" title="创建账户" description="用于成年消费、采购与合作申请，不创建儿童档案。">
    <form class="identity-form register-form" novalidate @submit.prevent="submit">
      <div v-if="error" class="error-summary" role="alert"><strong>未能完成注册</strong><span>{{ error }}</span></div>
      <div class="field-pair">
        <div class="field"><label for="nickname">昵称</label><input id="nickname" v-model="form.nickname" autocomplete="name" required maxlength="30" placeholder="2—30 个字符" /><span v-if="fields.nickname" class="field-error">{{ fields.nickname }}</span></div>
        <div class="field"><label for="email">电子邮箱</label><input id="email" v-model="form.email" type="email" autocomplete="username" required maxlength="254" placeholder="name@company.com" /><span v-if="fields.email" class="field-error">{{ fields.email }}</span></div>
      </div>
      <div class="field-pair">
        <div class="field"><label for="password">密码</label><input id="password" v-model="form.password" type="password" autocomplete="new-password" required placeholder="8—64 位，含字母和数字" /><span v-if="fields.password" class="field-error">{{ fields.password }}</span></div>
        <div class="field"><label for="confirm-password">确认密码</label><input id="confirm-password" v-model="form.confirmPassword" type="password" autocomplete="new-password" required placeholder="再次输入密码" /><span v-if="fields.confirmPassword" class="field-error">{{ fields.confirmPassword }}</span></div>
      </div>
      <div v-if="policy" class="consent-box">
        <label><input v-model="form.adultConfirmed" type="checkbox" /> <span>{{ policy.adultStatement }}</span></label>
        <details><summary>查看本期使用与隐私说明</summary><p>账户仅收集登录、联系和完成业务所需信息；不收集儿童姓名、生日、照片、学校或位置。模拟交易不发生真实支付或物流。</p></details>
        <label><input v-model="form.termsAccepted" type="checkbox" /> <span>我已阅读并同意使用说明（{{ policy.termsVersion }}）</span></label>
        <label><input v-model="form.privacyAccepted" type="checkbox" /> <span>我已阅读并同意隐私说明（{{ policy.privacyVersion }}）</span></label>
      </div>
      <button class="primary-button" type="submit" :disabled="busy || !policy">{{ busy ? '正在创建…' : '创建普通用户账户' }}<span aria-hidden="true">→</span></button>
      <p class="form-switch">已有账户？<RouterLink to="/login">返回登录</RouterLink></p>
    </form>
  </AuthShell>
</template>
