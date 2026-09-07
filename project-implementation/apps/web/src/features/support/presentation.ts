import { historyActionLabels, ticketStatusLabels, ticketTypeLabels, type TicketStatus, type TicketType } from './types'

export function ticketTypeLabel(value: string): string {
  return ticketTypeLabels[value as TicketType] ?? value
}

export function ticketStatusLabel(value: string): string {
  return ticketStatusLabels[value as TicketStatus] ?? value
}

export function historyActionLabel(value: string): string {
  return historyActionLabels[value] ?? value
}

export function ticketStatusClass(value: string): string {
  return `ticket-status--${value.toLowerCase().replace(/_/g, '-')}`
}

/** datetime-local 输入（浏览器本地时区）转 ISO 即时值；空白或无效返回 null。 */
export function localDatetimeToIso(value: string): string | null {
  const trimmed = value.trim()
  if (!trimmed) return null
  const date = new Date(trimmed)
  return Number.isNaN(date.getTime()) ? null : date.toISOString()
}

export interface TicketListFilters {
  type: string
  status: string
  from: string
  to: string
}

/** 列表查询串：仅保留有意义的筛选，时间转为 ISO，起点包含、终点不含。 */
export function buildTicketQuery(filters: TicketListFilters, page = 1, pageSize = 20): URLSearchParams {
  const params = new URLSearchParams()
  if (filters.type.trim()) params.set('type', filters.type.trim())
  if (filters.status.trim()) params.set('status', filters.status.trim())
  const from = localDatetimeToIso(filters.from)
  const to = localDatetimeToIso(filters.to)
  if (from) params.set('from', from)
  if (to) params.set('to', to)
  params.set('page', String(page))
  params.set('pageSize', String(pageSize))
  return params
}

export interface AuditListFilters {
  actorId: string
  action: string
  objectType: string
  from: string
  to: string
}

export function buildAuditQuery(filters: AuditListFilters, page = 1, pageSize = 20): URLSearchParams {
  const params = new URLSearchParams()
  const actorId = filters.actorId.trim()
  if (actorId) params.set('actorId', actorId)
  if (filters.action.trim()) params.set('action', filters.action.trim())
  if (filters.objectType.trim()) params.set('objectType', filters.objectType.trim())
  const from = localDatetimeToIso(filters.from)
  const to = localDatetimeToIso(filters.to)
  if (from) params.set('from', from)
  if (to) params.set('to', to)
  params.set('page', String(page))
  params.set('pageSize', String(pageSize))
  return params
}

/** 净成交金额：整数分字符串 → 人民币显示；空或非整数返回 —。 */
export function formatFenAmount(fen: string | null | undefined): string {
  if (fen == null || fen.trim() === '') return '—'
  const value = Number(fen)
  if (!Number.isInteger(value)) return '—'
  return `¥${(value / 100).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`
}

/** ISO 即时值 → 上海时区完整显示。 */
export function formatShanghai(value: string | null | undefined): string {
  if (!value) return '—'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return '—'
  return date.toLocaleString('zh-CN', { timeZone: 'Asia/Shanghai', dateStyle: 'medium', timeStyle: 'medium' })
}

export function formatDateTime(value: string | null | undefined): string {
  if (!value) return '—'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return '—'
  return date.toLocaleString('zh-CN', { dateStyle: 'medium', timeStyle: 'short' })
}

/** 上海日期（固定 UTC+8）→ 当日 [00:00, 次日 00:00) 的 ISO 即时值；非法输入返回 null。 */
export function shanghaiDateRange(date: string): { start: string; end: string } | null {
  const match = /^(\d{4})-(\d{2})-(\d{2})$/.exec(date.trim())
  if (!match) return null
  const startMs = Date.UTC(Number(match[1]), Number(match[2]) - 1, Number(match[3])) - 8 * 3_600_000
  return { start: new Date(startMs).toISOString(), end: new Date(startMs + 86_400_000).toISOString() }
}

/** 上海“今天”的 [start, end) 区间。 */
export function shanghaiTodayRange(now = new Date()): { start: string; end: string } {
  const shifted = new Date(now.getTime() + 8 * 3_600_000)
  const today = `${shifted.getUTCFullYear()}-${String(shifted.getUTCMonth() + 1).padStart(2, '0')}-${String(shifted.getUTCDate()).padStart(2, '0')}`
  return shanghaiDateRange(today) ?? { start: now.toISOString(), end: new Date(now.getTime() + 86_400_000).toISOString() }
}

export function isActionAllowed(allowedActions: string[], action: string): boolean {
  return allowedActions.includes(action)
}
