<script setup lang="ts">
import SketchIcon from '../../components/SketchIcon.vue'
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElDialog } from 'element-plus'
import SiteShell from '../../components/SiteShell.vue'
import { useSessionStore } from '../../stores/session'
import { useCommandRecovery } from '../../features/commerce/commandRecovery'
import { readTicket, ticketPaths } from '../../features/support/api'
import {
  formatDateTime,
  historyActionLabel,
  ticketStatusClass,
  ticketStatusLabel,
  ticketTypeLabel,
} from '../../features/support/presentation'
import { ticketStatusLabels, type TicketDetail } from '../../features/support/types'
import '../../features/support/style.css'

const props = withDefaults(defineProps<{ admin?: boolean }>(), { admin: false })
const route = useRoute()
const session = useSessionStore()
const ticket = ref<TicketDetail | null>(null)
const loading = ref(true)
const error = ref('')
const command = useCommandRecovery(
  () => session.actor?.id,
  (props.admin ? 'adminTicket:' : 'ticket:') + route.params.id,
)

const selected = ref('')
const content = ref('')
const closeReason = ref('')
const actionError = ref('')
const dialogOpen = ref(false)

const actionLabels: Record<string, string> = props.admin
  ? { START: '开始处理', REPLY: '公开回复', NOTE: '内部备注', CLOSE: '关闭工单' }
  : { FOLLOW_UP: '补充说明', CLOSE: '关闭咨询' }

const actionCopy: Record<string, string> = props.admin
  ? {
      START: '工单将从“待处理”进入“处理中”；该动作会写入状态历史与审计。',
      REPLY: '回复会立即对用户可见，并把工单置为“已回复”。内容要求 1—2000 字符。',
      NOTE: '内部备注仅管理员可见，不改变工单状态与版本，也不出现在用户页面。',
      CLOSE: '关闭后工单全只读；请填写 2—500 字符的关闭原因供用户查看。',
    }
  : {
      FOLLOW_UP: '补充内容会立即发送给客服；若工单已回复，将回到“处理中”。内容要求 1—2000 字符。',
      CLOSE: '关闭后不可继续补充；可填写关闭原因（留空则记录“用户确认关闭”）。',
    }

const dialogTitle = computed(() => (selected.value ? actionLabels[selected.value] : '工单操作'))

const orderPath = computed(() => (props.admin ? '/admin/orders/' : '/account/orders/'))

async function load() {
  loading.value = true
  error.value = ''
  try {
    ticket.value = await readTicket(String(route.params.id), props.admin)
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : '工单详情暂时无法加载。'
  } finally {
    loading.value = false
  }
}

function openAction(action: string) {
  selected.value = action
  content.value = ''
  closeReason.value = ''
  actionError.value = ''
  dialogOpen.value = true
}

function closeDialog() {
  if (!command.busy.value) dialogOpen.value = false
}

function validateAction(): boolean {
  if (selected.value === 'REPLY' || selected.value === 'FOLLOW_UP') {
    const length = content.value.trim().length
    if (length < 1 || length > 2000) {
      actionError.value = '内容需为 1—2000 个字符。'
      return false
    }
  }
  if (selected.value === 'NOTE' && content.value.trim().length > 2000) {
    actionError.value = '备注不能超过 2000 个字符。'
    return false
  }
  if (selected.value === 'CLOSE') {
    const length = closeReason.value.trim().length
    const min = props.admin ? 2 : 0
    if (length < min || length > 500) {
      actionError.value = props.admin ? '关闭原因需为 2—500 个字符。' : '关闭原因不能超过 500 个字符。'
      return false
    }
  }
  return true
}

function actionBody(): Record<string, unknown> {
  if (!ticket.value) return {}
  const base: Record<string, unknown> = { expectedVersion: ticket.value.version }
  if (selected.value === 'REPLY' || selected.value === 'FOLLOW_UP') base.content = content.value.trim()
  if (selected.value === 'NOTE') return { content: content.value.trim() }
  if (selected.value === 'CLOSE') base.reason = closeReason.value.trim() || null
  return base
}

function actionPath(): string {
  if (!ticket.value) return ''
  const id = ticket.value.id
  switch (selected.value) {
    case 'FOLLOW_UP': return ticketPaths.messages(id)
    case 'REPLY': return ticketPaths.adminReply(id)
    case 'NOTE': return ticketPaths.adminNote(id)
    case 'START': return ticketPaths.adminStart(id)
    case 'CLOSE': return props.admin ? ticketPaths.adminClose(id) : ticketPaths.userClose(id)
    default: return ''
  }
}

function actionClass(action: string): string {
  if (action === 'CLOSE') return 'danger-button'
  if (action === 'START' || action === 'REPLY') return 'primary-button'
  return 'secondary-button'
}

async function submit(retry = false) {
  if (!ticket.value) return
  if (!retry && !validateAction()) return
  error.value = ''
  actionError.value = ''
  try {
    ticket.value = retry
      ? await command.retry<TicketDetail>()
      : await command.send<TicketDetail>(actionPath(), actionBody())
    selected.value = ''
    dialogOpen.value = false
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : '操作未完成。'
    dialogOpen.value = false
    if (!command.pending.value) {
      selected.value = ''
      await load()
    }
  }
}

onMounted(() => void load())
</script>

<template>
  <SiteShell :title="props.admin ? '工单详情' : '咨询详情'" :admin="props.admin" eyebrow="SUPPORT / TICKET">
    <RouterLink class="back-link" :to="props.admin ? '/admin/tickets' : '/account/tickets'">← 返回{{ props.admin ? '工单管理' : '我的咨询' }}</RouterLink>
    <p v-if="error" class="error-summary" role="alert" style="margin-bottom: 20px">{{ error }}</p>

    <div v-if="loading" class="state-panel" aria-live="polite"><span class="loader"></span><p>正在读取工单详情…</p></div>
    <div v-else-if="!ticket" class="state-panel" role="alert"><h2>无法打开工单</h2><p>工单可能不存在，或您没有权限查看；请从列表重新进入。</p><button class="secondary-button" type="button" @click="load">重试</button></div>

    <template v-else>
      <header class="support-detail-lead">
        <div>
          <span class="ticket-status" :class="ticketStatusClass(ticket.status)">{{ ticketStatusLabel(ticket.status) }}</span>
          <h2>{{ ticket.subject }}</h2>
          <p>{{ ticket.ticketNumber }} · {{ ticketTypeLabel(ticket.type) }} · 创建于 {{ formatDateTime(ticket.createdAt) }} · 版本 v{{ ticket.version }}</p>
        </div>
      </header>

      <div v-if="command.pending.value" class="error-summary" role="status" style="margin-bottom: 22px">
        <strong>上次操作结果尚未确认</strong>
        <span>原请求键已保留；请重试原请求或刷新工单核对结果，不要创建同一业务的新操作。</span>
        <div class="support-actions" style="margin-top: 10px">
          <button class="primary-button compact" type="button" :disabled="command.busy.value" @click="submit(true)">{{ command.busy.value ? '正在确认…' : '原请求重试' }}</button>
          <button class="secondary-button" type="button" :disabled="command.busy.value" @click="load">刷新工单</button>
        </div>
      </div>

      <div class="support-detail-layout">
        <div class="support-detail-main">
          <section class="paper-section">
            <div class="section-heading"><div><p>01 / CONVERSATION</p><h2 class="wm-icon-label"><SketchIcon name="chat" :size="28" /> 公开对话</h2></div><span>{{ ticket.messages.length }} 条往来消息</span></div>
            <ul class="support-message-list">
              <li class="support-message support-message--body">
                <header><strong>用户提问</strong><time>{{ formatDateTime(ticket.createdAt) }}</time></header>
                <p>{{ ticket.body }}</p>
              </li>
              <li v-for="message in ticket.messages" :key="message.id" :class="['support-message', message.kind === 'USER_FOLLOWUP' ? 'support-message--followup' : 'support-message--reply']">
                <header>
                  <strong>{{ message.kind === 'USER_FOLLOWUP' ? '用户补充' : '客服回复' }}</strong>
                  <time>{{ formatDateTime(message.createdAt) }}</time>
                </header>
                <p>{{ message.content }}</p>
              </li>
            </ul>
          </section>

          <section v-if="props.admin" class="paper-section">
            <div class="section-heading"><div><p>02 / INTERNAL</p><h2>内部备注</h2></div><span>仅管理员可见</span></div>
            <p v-if="!ticket.internalNotes.length" class="support-empty">尚无内部备注；可通过“内部备注”记录处理线索，该内容不会改变工单状态。</p>
            <ul v-else class="support-note-list">
              <li v-for="note in ticket.internalNotes" :key="note.id" class="support-note">
                <header><span>管理员备注</span><time>{{ formatDateTime(note.createdAt) }}</time></header>
                <p>{{ note.content }}</p>
              </li>
            </ul>
          </section>

          <section v-if="props.admin" class="paper-section">
            <div class="section-heading"><div><p>03 / TIMELINE</p><h2 class="wm-icon-label"><SketchIcon name="history" :size="28" /> 状态时间线</h2></div><span>{{ ticket.history.length }} 个状态节点</span></div>
            <div class="history">
              <ol>
                <li v-for="item in ticket.history" :key="item.id">
                  <i aria-hidden="true"></i>
                  <div>
                    <strong>{{ historyActionLabel(item.action) }}</strong>
                    <span>{{ item.fromStatus ? `${ticketStatusLabels[item.fromStatus as keyof typeof ticketStatusLabels] ?? item.fromStatus} → ` : '' }}{{ ticketStatusLabels[item.toStatus as keyof typeof ticketStatusLabels] ?? item.toStatus }} · v{{ item.ticketVersion }}</span>
                    <time>{{ formatDateTime(item.createdAt) }}</time>
                    <p v-if="item.reason">{{ item.reason }}</p>
                  </div>
                </li>
              </ol>
            </div>
          </section>
        </div>

        <aside class="support-detail-aside">
          <section class="paper-section">
            <div class="section-heading"><div><p>TICKET FACTS</p><h2>工单信息</h2></div></div>
            <dl class="support-facts">
              <div><dt>类型</dt><dd>{{ ticketTypeLabel(ticket.type) }}</dd></div>
              <div><dt>状态</dt><dd>{{ ticketStatusLabel(ticket.status) }}</dd></div>
              <div v-if="ticket.product"><dt>咨询商品</dt><dd><RouterLink :to="`/products/${ticket.product.id}`">{{ ticket.product.name ?? ticket.product.id }}</RouterLink>（{{ ticket.product.sku ?? '无 SKU' }}）</dd></div>
              <div v-if="ticket.order"><dt>关联订单</dt><dd><RouterLink :to="orderPath + ticket.order.id">{{ ticket.order.orderNumber }}</RouterLink> · {{ ticket.order.status }}</dd></div>
              <div v-if="props.admin && ticket.phone"><dt>联系电话</dt><dd>{{ ticket.phone }}</dd></div>
              <div><dt>创建时间</dt><dd>{{ formatDateTime(ticket.createdAt) }}</dd></div>
              <div><dt>最近更新</dt><dd>{{ formatDateTime(ticket.updatedAt) }}</dd></div>
              <template v-if="ticket.status === 'CLOSED'">
                <div><dt>关闭时间</dt><dd>{{ formatDateTime(ticket.closedAt) }}</dd></div>
                <div><dt>关闭原因</dt><dd>{{ ticket.closeReason ?? '—' }}</dd></div>
              </template>
            </dl>
          </section>

          <section class="paper-section">
            <div class="section-heading"><div><p>ACTIONS</p><h2>可用操作</h2></div><span>{{ ticket.allowedActions.length ? '按当前状态提供' : '已关闭，全只读' }}</span></div>
            <div v-if="ticket.allowedActions.length" class="support-actions">
              <button
                v-for="action in ticket.allowedActions"
                :key="action"
                :class="actionClass(action)"
                type="button"
                @click="openAction(action)"
              >{{ actionLabels[action] ?? action }}</button>
            </div>
            <p v-else class="support-empty">工单已关闭，公开对话与状态历史仍可查看，但不再接受任何修改。</p>
          </section>
        </aside>
      </div>

      <ElDialog v-model="dialogOpen" :title="dialogTitle" width="min(560px, 92vw)" :close-on-click-modal="false">
        <form class="support-dialog-form" @submit.prevent="submit()">
          <p class="dialog-copy">{{ actionCopy[selected] }}</p>
          <p v-if="actionError" class="error-summary" role="alert">{{ actionError }}</p>
          <div v-if="selected === 'REPLY' || selected === 'FOLLOW_UP' || selected === 'NOTE'" class="field">
            <label for="ticket-action-content">{{ selected === 'NOTE' ? '备注内容' : '回复内容' }}</label>
            <textarea id="ticket-action-content" v-model="content" rows="5" :maxlength="selected === 'NOTE' ? 2000 : 2000" />
          </div>
          <div v-if="selected === 'CLOSE'" class="field">
            <label for="ticket-action-reason">关闭原因{{ props.admin ? '' : '（可选）' }}</label>
            <textarea id="ticket-action-reason" v-model="closeReason" rows="3" maxlength="500" :required="props.admin" />
          </div>
          <p v-if="selected === 'START'" class="support-empty">确认后工单状态将变为“处理中”。</p>
        </form>
        <template #footer>
          <button class="secondary-button" type="button" :disabled="command.busy.value" @click="closeDialog">返回</button>
          <button :class="actionClass(selected)" type="button" :disabled="command.busy.value" @click="submit()">{{ command.busy.value ? '正在提交…' : '确认执行' }}</button>
        </template>
      </ElDialog>
    </template>
  </SiteShell>
</template>
