import { api } from '../../services/http'
import type { AuditView, DashboardSnapshot, PageMeta, TicketDetail, TicketSummary } from './types'

// 后端分页信封：items 在 envelope.data（数组），PageMeta 在 envelope.meta。
export interface TicketPage {
  items: TicketSummary[]
  meta: PageMeta
}

export interface AuditPage {
  items: AuditView[]
  meta: PageMeta
}

const emptyMeta: PageMeta = { page: 1, pageSize: 20, totalItems: 0, totalPages: 0 }

export const readTickets = async (query: string, admin = false): Promise<TicketPage> => {
  const envelope = await api<TicketSummary[]>(`${admin ? '/admin' : ''}/tickets?${query}`)
  return { items: envelope.data, meta: envelope.meta ?? emptyMeta }
}

export const readTicket = async (id: string, admin = false) =>
  (await api<TicketDetail>(`${admin ? '/admin' : ''}/tickets/${id}`)).data

export const readDashboard = async (query: string) =>
  (await api<DashboardSnapshot>(`/admin/dashboard?${query}`)).data

export const readAuditLogs = async (query: string): Promise<AuditPage> => {
  const envelope = await api<AuditView[]>(`/admin/audit-logs?${query}`)
  return { items: envelope.data, meta: envelope.meta ?? emptyMeta }
}

export const readAuditLog = async (id: string) =>
  (await api<AuditView>(`/admin/audit-logs/${id}`)).data

// 写操作统一由页面经 useCommandRecovery 发送（带 Idempotency-Key + expectedVersion），
// 这里只保留路径常量，避免绕过恢复机制。
export const ticketPaths = {
  create: '/tickets',
  messages: (id: string) => `/tickets/${id}/messages`,
  userClose: (id: string) => `/tickets/${id}/close`,
  adminStart: (id: string) => `/admin/tickets/${id}/start`,
  adminReply: (id: string) => `/admin/tickets/${id}/replies`,
  adminNote: (id: string) => `/admin/tickets/${id}/internal-notes`,
  adminClose: (id: string) => `/admin/tickets/${id}/close`,
}
