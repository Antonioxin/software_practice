export type TicketType = 'GENERAL' | 'PRODUCT' | 'AFTER_SALES'
export type TicketStatus = 'NEW' | 'PROCESSING' | 'REPLIED' | 'CLOSED'

export interface ProductReference {
  id: string
  sku: string | null
  name: string | null
}

export interface OrderReference {
  id: string
  orderNumber: string
  status: string
}

export interface MessageView {
  id: string
  kind: 'USER_FOLLOWUP' | 'PUBLIC_REPLY'
  content: string
  actorId: string
  createdAt: string
}

export interface NoteView {
  id: string
  content: string
  actorId: string
  createdAt: string
}

export interface HistoryView {
  id: string
  action: string
  fromStatus: string | null
  toStatus: string
  ticketVersion: number
  actorId: string
  reason: string | null
  createdAt: string
}

export interface TicketSummary {
  id: string
  ticketNumber: string
  type: TicketType
  status: TicketStatus
  subject: string
  createdAt: string
  updatedAt: string
  version: number
}

export interface TicketDetail extends TicketSummary {
  body: string
  phone: string | null
  product: ProductReference | null
  order: OrderReference | null
  closedAt: string | null
  closedBy: string | null
  closeReason: string | null
  messages: MessageView[]
  internalNotes: NoteView[]
  history: HistoryView[]
  allowedActions: string[]
}

export interface PageMeta {
  page: number
  pageSize: number
  totalItems: number
  totalPages: number
}

export interface AuditView {
  id: string
  actorId: string | null
  action: string
  objectType: string
  objectId: string
  result: string
  reason: string | null
  occurredAt: string
  requestId: string | null
  changeSummary: string | null
}

export interface DashboardSnapshot {
  publishedProductCount: number
  activeUserCount: number
  pendingApplicationCount: number
  pendingInquiryCount: number
  pendingTicketCount: number
  pendingShipmentCount: number
  createdOrderCount: number
  netPaidFen: string
  asOf: string
  start: string
  end: string
}

export const ticketTypeLabels: Record<TicketType, string> = {
  GENERAL: '一般咨询',
  PRODUCT: '产品咨询',
  AFTER_SALES: '售后咨询',
}

export const ticketStatusLabels: Record<TicketStatus, string> = {
  NEW: '待处理',
  PROCESSING: '处理中',
  REPLIED: '已回复',
  CLOSED: '已关闭',
}

export const historyActionLabels: Record<string, string> = {
  CREATED: '提交工单',
  STARTED: '开始处理',
  REPLIED: '公开回复',
  FOLLOWED_UP: '用户补充',
  NOTE_ADDED: '内部备注',
  CLOSED: '关闭工单',
}
