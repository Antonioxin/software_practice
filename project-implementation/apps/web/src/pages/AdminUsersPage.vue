<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import SiteShell from '../components/SiteShell.vue'
import StatusBadge from '../components/StatusBadge.vue'
import { api, ApiProblem } from '../services/http'
import type { PageMeta, UserSummary } from '../types'

const users = ref<UserSummary[]>([])
const meta = ref<PageMeta>({ page: 1, pageSize: 20, totalItems: 0, totalPages: 0 })
const loading = ref(true)
const error = ref('')
const filters = reactive({ email: '', nickname: '', baseRole: '', status: '' })
const router = useRouter()

async function load(page = 1) {
  loading.value = true
  error.value = ''
  const params = new URLSearchParams({ page: String(page), pageSize: String(meta.value.pageSize) })
  Object.entries(filters).forEach(([key, value]) => { if (value) params.set(key, value) })
  try {
    const result = await api<UserSummary[]>(`/admin/users?${params}`)
    users.value = result.data
    if (result.meta) meta.value = result.meta
  } catch (cause) {
    error.value = cause instanceof ApiProblem ? cause.problem.detail : '无法读取用户列表。'
  } finally { loading.value = false }
}

function reset() { Object.assign(filters, { email: '', nickname: '', baseRole: '', status: '' }); void load(1) }
onMounted(() => load())
</script>

<template>
  <SiteShell title="用户账户" eyebrow="ADMIN / IDENTITY" admin>
    <section class="admin-stats" aria-label="列表摘要">
      <div><span>当前结果</span><strong>{{ meta.totalItems }}</strong><small>个账户</small></div>
      <p>用户状态与业务历史分离保留。停用会立即撤销会话，不删除订单、工单或合作记录。</p>
    </section>

    <section class="paper-section admin-filter">
      <form @submit.prevent="load(1)">
        <div class="field"><label for="filter-email">邮箱</label><input id="filter-email" v-model="filters.email" placeholder="搜索邮箱" /></div>
        <div class="field"><label for="filter-nickname">昵称</label><input id="filter-nickname" v-model="filters.nickname" placeholder="搜索昵称" /></div>
        <div class="field"><label for="filter-role">基础角色</label><select id="filter-role" v-model="filters.baseRole"><option value="">全部</option><option value="USER">普通用户</option><option value="ADMIN">管理员</option></select></div>
        <div class="field"><label for="filter-status">账户状态</label><select id="filter-status" v-model="filters.status"><option value="">全部</option><option value="ACTIVE">已启用</option><option value="DISABLED">已停用</option></select></div>
        <div class="filter-actions"><button class="primary-button compact" type="submit">查询</button><button class="secondary-button" type="button" @click="reset">重置</button></div>
      </form>
    </section>

    <section class="users-section" aria-live="polite">
      <div v-if="loading" class="state-panel"><span class="loader"></span><p>正在读取账户…</p></div>
      <div v-else-if="error" class="state-panel error"><h2>加载失败</h2><p>{{ error }}</p><button class="secondary-button" @click="load(meta.page)">重试</button></div>
      <div v-else-if="!users.length" class="state-panel"><h2>没有匹配账户</h2><p>请调整筛选条件。</p></div>
      <template v-else>
        <div class="table-wrap">
          <table>
            <thead><tr><th>账户</th><th>身份</th><th>状态</th><th>创建时间</th><th><span class="sr-only">操作</span></th></tr></thead>
            <tbody><tr v-for="user in users" :key="user.id">
              <td><strong>{{ user.nickname }}</strong><span>{{ user.email }}</span></td>
              <td>{{ user.baseRole === 'ADMIN' ? '管理员' : (user.derivedIdentity === 'DEALER' ? '有效经销商' : '普通用户') }}</td>
              <td><StatusBadge :status="user.accountStatus" /></td>
              <td>{{ new Date(user.createdAt).toLocaleString('zh-CN') }}</td>
              <td><button class="row-link" type="button" @click="router.push(`/admin/users/${user.id}`)">查看详情 <span>↗</span></button></td>
            </tr></tbody>
          </table>
        </div>
        <div class="pagination"><button :disabled="meta.page <= 1" @click="load(meta.page - 1)">上一页</button><span>第 {{ meta.page }} / {{ Math.max(meta.totalPages, 1) }} 页</span><button :disabled="meta.page >= meta.totalPages" @click="load(meta.page + 1)">下一页</button></div>
      </template>
    </section>
  </SiteShell>
</template>
