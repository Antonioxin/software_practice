export interface Line {
  productId: string
  sku: string
  name: string
  unitPriceFen: number
  quantity: number
  subtotalFen: number
  valid: boolean
  reason: string | null
  priceChanged: boolean
  previousUnitPriceFen: number
}
export interface Cart {
  cartVersion: number
  items: Line[]
  totalFen: number
  canCheckout: boolean
  currency: string
}
export interface Preview {
  previewToken: string
  cartVersion: number
  expiresAt: string
  items: Line[]
  subtotalFen: number
  shippingFen: number
  taxFen: number
  discountFen: number
  totalFen: number
}
export interface Address {
  recipient: string
  phone: string
  countryOrRegion: string
  region: string
  city: string
  addressLine: string
}
export interface Order {
  id: string
  orderNumber: string
  status: string
  version: number
  totalFen: number
  currency: string
  mode: string
  createdAt: string
}
export interface Detail extends Order {
  shippingAddress: Address
  remark: string
  items: Line[]
  subtotalFen: number
  shippingFen: number
  taxFen: number
  discountFen: number
  allowedActions: string[]
  logisticsName: string | null
  trackingNumber: string | null
  paymentAttempts: { id: string; outcome: string; amountFen: number; simulationReference: string }[]
  refunds: { id: string; amountFen: number; simulationReference: string }[]
  history: {
    action: string
    fromStatus: string | null
    toStatus: string
    version: number
    reason: string | null
    createdAt: string
  }[]
}
export interface OrderPage {
  items: Order[]
  page: number
  pageSize: number
  total: number
}
export const statusLabels: Record<string, string> = {
  PENDING_PAYMENT: '待付款',
  PAID: '待发货',
  SHIPPED: '已发货',
  COMPLETED: '已完成',
  CANCELLED: '已取消',
}
