<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElDialog, ElMessage } from 'element-plus'
import SiteShell from '../components/SiteShell.vue'
import StatusBadge from '../components/StatusBadge.vue'
import { api, ApiProblem, newIdempotencyKey } from '../services/http'
import type { UserDetail, UserSummary } from '../types'

const detail = ref<UserDetail | null>(null)
const loading = ref(true)
const error = ref('')
const dialogOpen = ref(false)
const commandBusy = ref(false)
const reasonError = ref('')
const command = reactive({ action: 'DISABLE' as 'DISABLE' | 'RESTORE', reason: '' })
const route = useRoute()
const router = useRouter()
const target = computed(() => detail.value?.account)

async function load() {
  loading.value = true
  try { detail.value = (await api<UserDetail>(`/admin/users/${route.params.id}`)).data }
  catch (cause) { error.value = cause instanceof ApiProblem ? cause.problem.detail : '无法读取账户。' }
  finally { loading.value = false }
}

function openCommand() {
  if (!target.value) return
  command.action = target.value.accountStatus === 'ACTIVE' ? 'DISABLE' : 'RESTORE'
  command.reason = ''
  reasonError.value = ''
  dialogOpen.value = true
}

async function submitCommand() {
  if (!target.value) return
  if ([...command.reason.trim()].length < 2) { reasonError.value = '请填写至少 2 个字符的原因。'; return }
  commandBusy.value = true
  try {
    const path = command.action === 'DISABLE' ? 'disable' : 'restore'
    const user = (await api<UserSummary>(`/admin/users/${target.value.id}/${path}`, {
      method: 'POST', headers: { 'Idempotency-Key': newIdempotencyKey() },
      body: JSON.stringify({ expectedVersion: target.value.version, reason: command.reason.trim() }),
    })).data
    if (detail.value) detail.value.account = user
    dialogOpen.value = false
    ElMessage.success(command.action === 'DISABLE' ? '账户已停用，原会话已撤销' : '账户已恢复，需要重新登录')
    await load()
  } catch (cause) {
    reasonError.value = cause instanceof ApiProblem ? cause.problem.detail : '操作结果暂未确认，请刷新详情后核对。'
  } finally { commandBusy.value = false }
}

onMounted(load)
</script>

<template>
  <SiteShell title="账户详情" eyebrow="ADMIN / IDENTITY" admin>
    <button class="back-link" type="button" @click="router.push('/admin/users')">← 返回用户列表</button>
    <div v-if="loading" class="state-panel"><span class="loader"></span><p>正在读取账户…</p></div>
    <div v-else-if="error" class="state-panel error"><h2>无法访问记录</h2><p>{{ error }}</p></div>
    <template v-else-if="target">
      <section class="user-hero">
        <div class="monogram small" aria-hidden="true">{{ target.nickname.slice(0, 1) }}</div>
        <div><p>{{ target.baseRole === 'ADMIN' ? 'ADMIN ACCOUNT' : 'CUSTOMER ACCOUNT' }}</p><h2>{{ target.nickname }}</h2><span>{{ target.email }}</span></div>
        <StatusBadge :status="target.accountStatus" />
        <button v-if="target.baseRole !== 'ADMIN'" :class="target.accountStatus === 'ACTIVE' ? 'danger-button' : 'primary-button compact'" @click="openCommand">{{ target.accountStatus === 'ACTIVE' ? '停用账户' : '恢复账户' }}</button>
      </section>
      <div class="detail-grid">
        <section class="paper-section account-facts"><div class="section-heading"><div><p>ACCOUNT FACTS</p><h2>账户信息</h2></div></div><dl><div><dt>账户 ID</dt><dd>{{ target.id }}</dd></div><div><dt>基础角色</dt><dd>{{ target.baseRole }}</dd></div><div><dt>派生身份</dt><dd>{{ target.derivedIdentity }}</dd></div><div><dt>联系电话</dt><dd>{{ target.phone || '未填写' }}</dd></div><div><dt>资料版本</dt><dd>v{{ target.version }}</dd></div><div><dt>最后更新</dt><dd>{{ new Date(target.updatedAt).toLocaleString('zh-CN') }}</dd></div></dl></section>
        <section class="paper-section history"><div class="section-heading"><div><p>STATUS HISTORY</p><h2>状态历史</h2></div></div><ol v-if="detail?.statusHistory.length"><li v-for="item in detail.statusHistory" :key="item.createdAt"><i></i><div><strong>{{ item.action === 'DISABLE' ? '停用账户' : '恢复账户' }}</strong><span>{{ item.previousStatus }} → {{ item.newStatus }}</span><p>{{ item.reason }}</p><time>{{ new Date(item.createdAt).toLocaleString('zh-CN') }}</time></div></li></ol><div v-else class="empty-history">暂无状态变更记录</div></section>
      </div>
    </template>

    <ElDialog v-model="dialogOpen" :title="command.action === 'DISABLE' ? '确认停用账户' : '确认恢复账户'" width="min(520px, calc(100vw - 32px))" append-to-body>
      <p class="dialog-copy">{{ command.action === 'DISABLE' ? '停用后，该用户的全部现有会话将立即失效，但业务历史会保留。' : '恢复不会复活旧会话，也不会自动恢复经销合作状态。' }}</p>
      <div class="field"><label for="command-reason">操作原因</label><textarea id="command-reason" v-model="command.reason" maxlength="500" rows="4" placeholder="请记录可追溯的处理原因"></textarea><span v-if="reasonError" class="field-error" role="alert">{{ reasonError }}</span></div>
      <template #footer><button class="secondary-button" @click="dialogOpen = false">取消</button><button :class="command.action === 'DISABLE' ? 'danger-button' : 'primary-button compact'" :disabled="commandBusy" @click="submitCommand">{{ commandBusy ? '处理中…' : '确认提交' }}</button></template>
    </ElDialog>
  </SiteShell>
</template>
