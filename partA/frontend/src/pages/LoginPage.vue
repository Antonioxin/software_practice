<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import AuthShell from '../components/AuthShell.vue'
import { ApiProblem } from '../services/http'
import { useSessionStore } from '../stores/session'

const form = reactive({ email: '', password: '' })
const busy = ref(false)
const error = ref('')
const fields = reactive<Record<string, string>>({})
const session = useSessionStore()
const route = useRoute()
const router = useRouter()
const registered = computed(() => route.query.registered === '1')

async function submit() {
  busy.value = true
  error.value = ''
  Object.keys(fields).forEach((key) => delete fields[key])
  try {
    const actor = await session.login(form.email, form.password)
    const requested = typeof route.query.redirect === 'string' && route.query.redirect.startsWith('/')
      ? route.query.redirect : null
    await router.replace(requested ?? (actor.baseRole === 'ADMIN' ? '/admin/users' : '/account/profile'))
  } catch (cause) {
    if (cause instanceof ApiProblem) {
      error.value = cause.problem.detail
      cause.problem.errors?.forEach((item) => { fields[item.field] = item.message })
    } else error.value = '无法连接服务，请检查后端是否已启动。'
  } finally { busy.value = false }
}
</script>

<template>
  <AuthShell eyebrow="ACCOUNT ACCESS" title="欢迎回来" description="登录后继续管理您的资料与业务记录。">
    <form class="identity-form" novalidate @submit.prevent="submit">
      <div v-if="registered" class="success-summary" role="status"><strong>账户已创建</strong><span>请使用新账户登录。</span></div>
      <div v-if="error" class="error-summary" role="alert"><strong>未能登录</strong><span>{{ error }}</span></div>
      <div class="field">
        <label for="login-email">电子邮箱</label>
        <input id="login-email" v-model="form.email" type="email" autocomplete="username" required placeholder="name@company.com" :aria-invalid="!!fields.email" />
        <span v-if="fields.email" class="field-error">{{ fields.email }}</span>
      </div>
      <div class="field">
        <label for="login-password">登录密码</label>
        <input id="login-password" v-model="form.password" type="password" autocomplete="current-password" required placeholder="输入密码" :aria-invalid="!!fields.password" />
        <span v-if="fields.password" class="field-error">{{ fields.password }}</span>
      </div>
      <button class="primary-button" type="submit" :disabled="busy">{{ busy ? '正在验证…' : '登录账户' }}<span aria-hidden="true">→</span></button>
      <p class="form-switch">还没有账户？<RouterLink to="/register">创建成人账户</RouterLink></p>
      <div class="security-note"><span aria-hidden="true">◇</span><p><strong>安全会话</strong>空闲 30 分钟后自动退出，请勿在公共设备保留登录。</p></div>
    </form>
  </AuthShell>
</template>
