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

export type ProductStatus = 'DRAFT' | 'PUBLISHED' | 'UNLISTED'
export type PlayType = 'BALANCE' | 'COORDINATION' | 'THROWING' | 'TEAM_PLAY' | 'OUTDOOR_EXPLORATION'
export type ProductScene = 'INDOOR' | 'OUTDOOR' | 'BOTH'

export interface Category {
  id: string
  name: string
  description?: string | null
  sortOrder: number
  enabled: boolean
  version: number
}

export interface ProductCard {
  id: string
  sku: string
  name: string
  summary: string
  category: Category
  ageMin: number
  ageMax?: number | null
  playType: PlayType
  scene: ProductScene
  mainImageId: string
  retailUnitPriceFen: number
  currency: 'CNY'
  inStock: boolean
  stockStatus: 'IN_STOCK' | 'OUT_OF_STOCK'
}

export interface PublicProduct extends ProductCard {
  description?: string | null
  material?: string | null
  dimensions?: string | null
  packageContents?: string | null
  instructions?: string | null
  safetyNotes?: string | null
  imageIds: string[]
  status: 'PUBLISHED' | 'UNLISTED'
  purchasable: boolean
  availabilityMessage: string
  updatedAt: string
}

export interface AdminProduct {
  id: string
  sku?: string | null
  name?: string | null
  summary?: string | null
  description?: string | null
  categoryId?: string | null
  categoryName?: string | null
  ageMin?: number | null
  ageMax?: number | null
  playType?: PlayType | null
  scene?: ProductScene | null
  material?: string | null
  dimensions?: string | null
  packageContents?: string | null
  instructions?: string | null
  safetyNotes?: string | null
  mainImageId?: string | null
  imageIds: string[]
  retailUnitPriceFen?: number | null
  dealerEnabled: boolean
  dealerReferenceUnitPriceFen?: number | null
  minInquiryQuantity?: number | null
  leadTimeText?: string | null
  status: ProductStatus
  displayOrder: number
  stock: number
  stockVersion: number
  version: number
  createdAt: string
  updatedAt: string
}

export interface ProductOptions {
  playTypes: Array<{ value: PlayType; label: string }>
  scenes: Array<{ value: ProductScene; label: string }>
}
