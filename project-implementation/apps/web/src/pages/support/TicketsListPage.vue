<script setup lang="ts">
import SketchIcon from '../../components/SketchIcon.vue'
import { onMounted, reactive, ref } from 'vue'
import SiteShell from '../../components/SiteShell.vue'
import { readTickets, type TicketPage } from '../../features/support/api'
import {
  buildTicketQuery,
  formatDateTime,
  ticketStatusClass,
  ticketStatusLabel,
  ticketTypeLabel,
} from '../../features/support/presentation'
import { ticketStatusLabels, ticketTypeLabels } from '../../features/support/types'
import '../../features/support/style.css'

const props = withDefaults(defineProps<{ admin?: boolean }>(), { admin: false })

const result = ref<TicketPage | null>(null)
const page = ref(1)
const loading = ref(true)
const error = ref('')
// 用户列表接口仅支持类型/状态筛选；时间区间筛选为管理端接口能力。
const filters = reactive({ type: '', status: '', from: '', to: '' })

async function load(target = 1) {
  loading.value = true
  error.value = ''
  page.value = target
  try {
    const query = buildTicketQuery(filters, target, 20)
    if (!props.admin) {
      query.delete('from')
      query.delete('to')
    }
    result.value = await readTickets(query.toString(), props.admin)
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : '工单列表暂时无法加载。'
  } finally {
    loading.value = false
  }
}

function resetFilters() {
  Object.assign(filters, { type: '', status: '', from: '', to: '' })
  void load(1)
}

onMounted(() => void load())
</script>

<template>
  <SiteShell :title="props.admin ? '工单管理' : '我的咨询'" :admin="props.admin" eyebrow="SUPPORT / TICKETS">
    <p class="support-page-intro">
      <template v-if="props.admin">按类型、状态与创建时间筛选全部工单；进入详情后可开始处理、公开回复、记录内部备注或关闭。</template>
      <template v-else>在这里跟踪您提交的所有咨询；点击工单号查看公开对话、补充说明或关闭咨询。</template>
    </p>

    <section class="paper-section admin-filter">
      <form @submit.prevent="load(1)">
        <div class="field"><label for="ticket-filter-type">咨询类型</label>
          <select id="ticket-filter-type" v-model="filters.type">
            <option value="">全部类型</option>
            <option v-for="(label, value) in ticketTypeLabels" :key="value" :value="value">{{ label }}</option>
          </select>
        </div>
        <div class="field"><label for="ticket-filter-status">工单状态</label>
          <select id="ticket-filter-status" v-model="filters.status">
            <option value="">全部状态</option>
            <option v-for="(label, value) in ticketStatusLabels" :key="value" :value="value">{{ label }}</option>
          </select>
        </div>
        <template v-if="props.admin">
          <div class="field"><label for="ticket-filter-from">创建时间起点（上海）</label>
            <input id="ticket-filter-from" v-model="filters.from" type="datetime-local" />
          </div>
          <div class="field"><label for="ticket-filter-to">创建时间终点（不含）</label>
            <input id="ticket-filter-to" v-model="filters.to" type="datetime-local" />
          </div>
        </template>
        <div class="filter-actions">
          <button class="primary-button compact" type="submit"><SketchIcon name="search" :size="22" />查询</button>
          <button v-if="props.admin" class="secondary-button" type="button" @click="resetFilters"><SketchIcon name="return" :size="22" />重置</button>
        </div>
      </form>
    </section>
    <p v-if="props.admin" class="support-filter-help">时间筛选按上海时区输入、起点包含终点不含；结果按创建时间倒序。</p>

    <section aria-live="polite">
      <div v-if="loading" class="state-panel"><span class="loader"></span><p>正在读取工单…</p></div>
      <div v-else-if="error" class="state-panel error"><h2>加载失败</h2><p>{{ error }}</p><button class="secondary-button" type="button" @click="load(page)">重试</button></div>
      <div v-else-if="result && !result.items.length" class="state-panel">
        <h2>暂无符合条件的工单</h2>
        <p v-if="props.admin">可调整筛选条件查看其他状态的工单。</p>
        <p v-else>还没有咨询记录；遇到问题可从“联系客服”提交。</p>
        <RouterLink v-if="!props.admin" class="primary-button compact" to="/contact">提交新咨询</RouterLink>
      </div>
      <template v-else-if="result">
        <div class="table-wrap">
          <table>
            <thead><tr><th>工单号</th><th>类型</th><th>状态</th><th>主题</th><th>创建时间</th><th>最近更新</th><th><span class="sr-only">操作</span></th></tr></thead>
            <tbody>
              <tr v-for="ticket in result.items" :key="ticket.id">
                <td><strong>{{ ticket.ticketNumber }}</strong><span>v{{ ticket.version }}</span></td>
                <td>{{ ticketTypeLabel(ticket.type) }}</td>
                <td><span class="ticket-status" :class="ticketStatusClass(ticket.status)">{{ ticketStatusLabel(ticket.status) }}</span></td>
                <td style="max-width: 320px; overflow-wrap: anywhere">{{ ticket.subject }}</td>
                <td>{{ formatDateTime(ticket.createdAt) }}</td>
                <td>{{ formatDateTime(ticket.updatedAt) }}</td>
                <td>
                  <RouterLink class="row-link" :to="(props.admin ? '/admin/tickets/' : '/account/tickets/') + ticket.id">查看详情 <span>↗</span></RouterLink>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
        <div class="pagination">
          <button type="button" :disabled="page <= 1 || loading" @click="load(page - 1)">上一页</button>
          <span>第 {{ result.meta.page }} / {{ Math.max(result.meta.totalPages, 1) }} 页 · 共 {{ result.meta.totalItems }} 个工单</span>
          <button type="button" :disabled="page >= result.meta.totalPages || loading" @click="load(page + 1)">下一页</button>
        </div>
      </template>
    </section>
  </SiteShell>
</template>
