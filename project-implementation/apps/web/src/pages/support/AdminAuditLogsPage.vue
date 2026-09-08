<script setup lang="ts">
import SketchIcon from '../../components/SketchIcon.vue'
import { onMounted, reactive, ref } from 'vue'
import { ElDialog } from 'element-plus'
import SiteShell from '../../components/SiteShell.vue'
import { readAuditLogs, type AuditPage } from '../../features/support/api'
import { buildAuditQuery, formatShanghai } from '../../features/support/presentation'
import type { AuditView } from '../../features/support/types'
import '../../features/support/style.css'

const result = ref<AuditPage | null>(null)
const page = ref(1)
const loading = ref(true)
const error = ref('')
const filters = reactive({ actorId: '', action: '', objectType: '', from: '', to: '' })
const detailOpen = ref(false)
const selected = ref<AuditView | null>(null)

function shortId(id: string | null): string {
  if (!id) return '—'
  return id.length > 13 ? `${id.slice(0, 13)}…` : id
}

async function load(target = 1) {
  loading.value = true
  error.value = ''
  page.value = target
  try {
    const query = buildAuditQuery(filters, target, 20)
    result.value = await readAuditLogs(query.toString())
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : '审计日志暂时无法加载。'
  } finally {
    loading.value = false
  }
}

function resetFilters() {
  Object.assign(filters, { actorId: '', action: '', objectType: '', from: '', to: '' })
  void load(1)
}

function openDetail(record: AuditView) {
  selected.value = record
  detailOpen.value = true
}

onMounted(() => void load())
</script>

<template>
  <SiteShell title="审计日志" admin eyebrow="OPERATIONS / AUDIT">
    <section class="admin-stats" aria-label="列表摘要">
      <div><span>当前结果</span><strong>{{ result?.meta.totalItems ?? '—' }}</strong><small>条记录</small></div>
      <p>审计为只读追踪：不提供修改或删除入口，查询动作本身也不写入审计。时间均按上海时区显示。</p>
    </section>

    <section class="paper-section admin-filter">
      <form @submit.prevent="load(1)">
        <div class="field"><label for="audit-filter-actor">操作者 ID</label>
          <input class="wm-search-input" id="audit-filter-actor" v-model="filters.actorId" placeholder="用户 UUID" />
        </div>
        <div class="field"><label for="audit-filter-action">动作</label>
          <input class="wm-search-input" id="audit-filter-action" v-model="filters.action" placeholder="如 TICKET_CLOSED" />
        </div>
        <div class="field"><label for="audit-filter-object">对象类型</label>
          <input class="wm-search-input" id="audit-filter-object" v-model="filters.objectType" placeholder="如 TICKET / ORDER" />
        </div>
        <div class="field"><label for="audit-filter-from">发生时间起点（上海）</label>
          <input id="audit-filter-from" v-model="filters.from" type="datetime-local" />
        </div>
        <div class="field"><label for="audit-filter-to">发生时间终点（不含）</label>
          <input id="audit-filter-to" v-model="filters.to" type="datetime-local" />
        </div>
        <div class="filter-actions">
          <button class="primary-button compact" type="submit">查询</button>
          <button class="secondary-button" type="button" @click="resetFilters"><SketchIcon name="return" :size="22" />重置</button>
        </div>
      </form>
    </section>

    <section aria-live="polite">
      <div v-if="loading" class="state-panel"><span class="loader"></span><p>正在读取审计日志…</p></div>
      <div v-else-if="error" class="state-panel error"><h2>加载失败</h2><p>{{ error }}</p><button class="secondary-button" type="button" @click="load(page)">重试</button></div>
      <div v-else-if="result && !result.items.length" class="state-panel"><h2>没有匹配的审计记录</h2><p>请调整筛选条件；审计记录随各模块关键操作自动产生。</p></div>
      <template v-else-if="result">
        <div class="table-wrap">
          <table>
            <thead><tr><th>发生时间</th><th>动作</th><th>操作者</th><th>对象</th><th>结果</th><th><span class="sr-only">操作</span></th></tr></thead>
            <tbody>
              <tr v-for="record in result.items" :key="record.id">
                <td>{{ formatShanghai(record.occurredAt) }}</td>
                <td><strong style="font-size: 12px">{{ record.action }}</strong></td>
                <td :title="record.actorId ?? ''">{{ shortId(record.actorId) }}</td>
                <td><strong style="font-size: 12px">{{ record.objectType }}</strong><span :title="record.objectId">{{ shortId(record.objectId) }}</span></td>
                <td>{{ record.result === 'SUCCESS' ? '成功' : record.result }}</td>
                <td><button class="row-link" type="button" @click="openDetail(record)">查看详情 <span>↗</span></button></td>
              </tr>
            </tbody>
          </table>
        </div>
        <div class="pagination">
          <button type="button" :disabled="page <= 1 || loading" @click="load(page - 1)">上一页</button>
          <span>第 {{ result.meta.page }} / {{ Math.max(result.meta.totalPages, 1) }} 页 · 共 {{ result.meta.totalItems }} 条</span>
          <button type="button" :disabled="page >= result.meta.totalPages || loading" @click="load(page + 1)">下一页</button>
        </div>
      </template>
    </section>

    <ElDialog v-model="detailOpen" title="审计记录详情" width="min(560px, 92vw)">
      <dl v-if="selected" class="audit-detail-facts">
        <div><dt>记录 ID</dt><dd>{{ selected.id }}</dd></div>
        <div><dt>动作</dt><dd>{{ selected.action }}</dd></div>
        <div><dt>操作者 ID</dt><dd>{{ selected.actorId ?? '—' }}</dd></div>
        <div><dt>对象类型</dt><dd>{{ selected.objectType }}</dd></div>
        <div><dt>对象 ID</dt><dd>{{ selected.objectId }}</dd></div>
        <div><dt>结果</dt><dd>{{ selected.result }}</dd></div>
        <div><dt>发生时间（上海）</dt><dd>{{ formatShanghai(selected.occurredAt) }}</dd></div>
        <div><dt>请求 ID</dt><dd>{{ selected.requestId ?? '—' }}</dd></div>
        <div><dt>变更摘要</dt><dd>{{ selected.changeSummary ?? '—' }}</dd></div>
        <div><dt>原因</dt><dd>{{ selected.reason ?? '—' }}</dd></div>
      </dl>
      <template #footer>
        <button class="secondary-button" type="button" @click="detailOpen = false">关闭</button>
      </template>
    </ElDialog>
  </SiteShell>
</template>
