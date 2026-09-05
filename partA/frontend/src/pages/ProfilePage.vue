<script setup lang="ts">
import { reactive, ref, watchEffect } from 'vue'
import { ElMessage } from 'element-plus'
import SiteShell from '../components/SiteShell.vue'
import StatusBadge from '../components/StatusBadge.vue'
import { api, ApiProblem } from '../services/http'
import { useSessionStore } from '../stores/session'
import type { Actor } from '../types'

const session = useSessionStore()
const form = reactive({ nickname: '', phone: '' })
const fields = reactive<Record<string, string>>({})
const busy = ref(false)
const error = ref('')
watchEffect(() => {
  if (session.actor) { form.nickname = session.actor.nickname; form.phone = session.actor.phone ?? '' }
})

async function save() {
  busy.value = true
  error.value = ''
  Object.keys(fields).forEach((key) => delete fields[key])
  try {
    const actor = (await api<Actor>('/account/profile', { method: 'PATCH', body: JSON.stringify(form) })).data
    session.replace(actor)
    ElMessage.success('个人资料已保存')
  } catch (cause) {
    if (cause instanceof ApiProblem) {
      error.value = cause.problem.detail
      cause.problem.errors?.forEach((item) => { fields[item.field] = item.message })
    } else error.value = '保存结果暂未确认，请刷新后核对。'
  } finally { busy.value = false }
}
</script>

<template>
  <SiteShell title="个人资料">
    <div class="profile-layout">
      <section class="profile-summary">
        <div class="monogram" aria-hidden="true">{{ session.actor?.nickname.slice(0, 1) }}</div>
        <p class="summary-label">账户身份</p>
        <h2>{{ session.actor?.nickname }}</h2>
        <p>{{ session.actor?.email }}</p>
        <StatusBadge v-if="session.actor" :status="session.actor.accountStatus" />
        <dl>
          <div><dt>基础角色</dt><dd>{{ session.actor?.baseRole === 'USER' ? '普通用户' : '管理员' }}</dd></div>
          <div><dt>派生身份</dt><dd>{{ session.actor?.derivedIdentity === 'DEALER' ? '有效经销商' : '普通账户' }}</dd></div>
          <div><dt>资料版本</dt><dd>v{{ session.actor?.version }}</dd></div>
        </dl>
      </section>

      <section class="paper-section profile-form-section">
        <div class="section-heading"><div><p>CONTACT PROFILE</p><h2>必要联系信息</h2></div><span>邮箱在本版本中不可修改</span></div>
        <form class="identity-form" @submit.prevent="save">
          <div v-if="error" class="error-summary" role="alert"><strong>未能保存</strong><span>{{ error }}</span></div>
          <div class="field"><label for="profile-email">登录邮箱</label><input id="profile-email" :value="session.actor?.email" disabled /></div>
          <div class="field"><label for="profile-nickname">昵称</label><input id="profile-nickname" v-model="form.nickname" maxlength="30" required /><span v-if="fields.nickname" class="field-error">{{ fields.nickname }}</span></div>
          <div class="field"><label for="profile-phone">联系电话（可选）</label><input id="profile-phone" v-model="form.phone" inputmode="tel" placeholder="+86 138 0000 0000" /><span v-if="fields.phone" class="field-error">{{ fields.phone }}</span></div>
          <div class="form-actions"><button class="primary-button compact" type="submit" :disabled="busy">{{ busy ? '保存中…' : '保存修改' }}</button></div>
        </form>
      </section>
    </div>

    <section class="business-links">
      <header><p>MY RECORDS</p><h2>本人业务记录</h2><span>入口已按团队契约预留，由对应成员模块接入真实数据。</span></header>
      <div class="link-grid">
        <div><strong>零售订单</strong><span>C · commerce</span><em>待接入</em></div>
        <div><strong>我的咨询</strong><span>F · support</span><em>待接入</em></div>
        <div><strong>经销申请</strong><span>D · dealership</span><em>待接入</em></div>
        <div><strong>经销询价</strong><span>D · dealership</span><em>待接入</em></div>
      </div>
    </section>
  </SiteShell>
</template>
