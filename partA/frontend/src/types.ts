export type AccountStatus = 'ACTIVE' | 'DISABLED'
export type BaseRole = 'USER' | 'ADMIN'

export interface Actor {
  id: string
  email: string
  nickname: string
  phone: string | null
  baseRole: BaseRole
  accountStatus: AccountStatus
  derivedIdentity: 'USER' | 'DEALER'
  capabilities: string[]
  version: number
}

export interface UserSummary extends Omit<Actor, 'capabilities'> {
  createdAt: string
  updatedAt: string
}

export interface StatusHistory {
  action: 'DISABLE' | 'RESTORE'
  previousStatus: AccountStatus
  newStatus: AccountStatus
  reason: string
  createdAt: string
}

export interface UserDetail {
  account: UserSummary
  statusHistory: StatusHistory[]
}

export interface PageMeta {
  page: number
  pageSize: number
  totalItems: number
  totalPages: number
}

export interface ApiEnvelope<T> {
  data: T
  meta?: PageMeta
}

export interface FieldProblem {
  field: string
  code: string
  message: string
}

export interface ProblemDetails {
  type: string
  title: string
  status: number
  detail: string
  code: string
  requestId?: string
  errors?: FieldProblem[]
}
