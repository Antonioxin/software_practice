<script setup lang="ts">
import SketchIcon from '../../components/SketchIcon.vue'
import { computed, onMounted, ref } from 'vue'
import SiteShell from '../../components/SiteShell.vue'
import { readDashboard } from '../../features/support/api'
import {
  formatFenAmount,
  formatShanghai,
  shanghaiDateRange,
  shanghaiTodayRange,
} from '../../features/support/presentation'
import type { DashboardSnapshot } from '../../features/support/types'
import '../../features/support/style.css'

const snapshot = ref<DashboardSnapshot | null>(null)
const loading = ref(true)
const error = ref('')
const today = shanghaiTodayRange().start.slice(0, 10)
const range = ref({ start: today, end: today })

const startIso = computed(() => shanghaiDateRange(range.value.start)?.start ?? null)
const endIso = computed(() => shanghaiDateRange(range.value.end)?.end ?? null)
const rangeValid = computed(() => {
  if (!startIso.value || !endIso.value) return false
  return startIso.value < endIso.value
})

interface DashboardCard {
  key: string
  label: string
  value: string
  hint: string
  link: string | null
  money?: boolean
}

const cards = computed<DashboardCard[]>(() => {
  if (!snapshot.value) return []
  const s = snapshot.value
  return [
    {
      key: 'publishedProductCount', label: '在售商品', value: String(s.publishedProductCount),
      hint: '已发布状态的商品目录总量', link: s.publishedProductCount > 0 ? '/admin/products' : null,
    },
    {
      key: 'activeUserCount', label: '活跃用户', value: String(s.activeUserCount),
      hint: '启用状态的全部账户（含管理员）', link: s.activeUserCount > 0 ? '/admin/users' : null,
    },
    {
      key: 'pendingApplicationCount', label: '待审合作申请', value: String(s.pendingApplicationCount),
      hint: '经销合作域（D）接入前固定为 0', link: null,
    },
    {
      key: 'pendingInquiryCount', label: '待处理询价', value: String(s.pendingInquiryCount),
      hint: '经销询价域（D）接入前固定为 0', link: null,
    },
    {
      key: 'pendingTicketCount', label: '待处理工单', value: String(s.pendingTicketCount),
      hint: '待处理与处理中状态的工单', link: s.pendingTicketCount > 0 ? '/admin/tickets' : null,
    },
    {
      key: 'pendingShipmentCount', label: '待发货订单', value: String(s.pendingShipmentCount),
      hint: '已付款待发货的当前订单量', link: s.pendingShipmentCount > 0 ? '/admin/orders' : null,
    },
    {
      key: 'createdOrderCount', label: '区间创建订单', value: String(s.createdOrderCount),
      hint: `所选区间 [${range.value.start}, ${range.value.end}) 内创建`, link: null,
    },
    {
      key: 'netPaidFen', label: '区间净收款', value: formatFenAmount(s.netPaidFen),
      hint: '成功付款减模拟退款（整数分转元）', link: null, money: true,
    },
  ]
})

async function load() {
  if (!rangeValid.value) {
    error.value = '请选择有效的日期区间（起点需早于终点，终点当天含全天）。'
    return
  }
  loading.value = true
  error.value = ''
  try {
    const query = new URLSearchParams({ start: startIso.value!, end: endIso.value! })
    snapshot.value = await readDashboard(query.toString())
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : '总览数据暂时无法加载。'
  } finally {
    loading.value = false
  }
}

onMounted(() => void load())
</script>

<template>
  <SiteShell title="运营总览" admin eyebrow="OPERATIONS / DASHBOARD">
    <p class="support-page-intro">
      前 6 项指标为读取时点的当前总量，后 2 项按所选日期区间（上海时区，起点包含、终点当日全天包含）统计；
      全部指标来自同一次只读数据库快照，共同读取时点见下方标注。
    </p>

    <form class="dashboard-range-form" @submit.prevent="load">
      <div class="field"><label for="dashboard-start">区间起点（上海）</label>
        <input id="dashboard-start" v-model="range.start" type="date" :max="today" />
      </div>
      <div class="field"><label for="dashboard-end">区间终点（上海，含当日）</label>
        <input id="dashboard-end" v-model="range.end" type="date" />
      </div>
      <div class="filter-actions">
        <button class="primary-button compact" type="submit" :disabled="loading"><SketchIcon name="search" :size="22" />{{ loading ? '读取中…' : '查看指标' }}</button>
        <button class="secondary-button" type="button" @click="range = { start: today, end: today }; load()"><SketchIcon name="return" :size="22" />回到今天</button>
      </div>
    </form>

    <p v-if="error" class="error-summary" role="alert" style="margin-bottom: 20px">{{ error }}</p>

    <div v-if="loading" class="state-panel" aria-live="polite"><span class="loader"></span><p>正在读取运营指标…</p></div>

    <template v-else-if="snapshot">
      <div class="dashboard-grid">
        <div v-for="card in cards" :key="card.key" :class="['dashboard-card', { 'dashboard-card--money': card.money }]">
          <span>{{ card.label }}</span>
          <strong>{{ card.value }}</strong>
          <small>{{ card.hint }}</small>
          <RouterLink v-if="card.link" :to="card.link">查看明细 →</RouterLink>
        </div>
      </div>
      <p class="dashboard-footnote">
        数据读取时点（Asia/Shanghai）：{{ formatShanghai(snapshot.asOf) }} ·
        区间口径 [{{ range.start }} 00:00, {{ range.end }} 次日 00:00) 上海时间 ·
        净收款为字符串整数分原样转换，与交易模块流水一致。
      </p>
    </template>
  </SiteShell>
</template>
