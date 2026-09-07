export type ApplicationStatus = 'PENDING' | 'APPROVED' | 'REJECTED'
export type CooperationStatus = 'ACTIVE' | 'SUSPENDED'
export type InquiryStatus = 'NEW' | 'PROCESSING' | 'REPLIED' | 'CLOSED'

export interface PageResult<T> { items: T[]; page: number; pageSize: number; total: number }
export interface ApplicationVersion {
  contentVersion: number; companyName: string; businessType: string; countryOrRegion: string; city: string
  contactName: string; phone: string; cooperationEmail: string; businessChannels: string; website?: string | null
  cooperationIntent: string; publicChannelConsent: boolean; submittedAt: string
}
export interface ApplicationReview { contentVersion: number; decision: string; publicReason?: string | null; internalNote?: string | null; createdAt: string }
export interface DealerApplication {
  id: string; applicationNumber: string; userId: string; status: ApplicationStatus; currentContentVersion: number
  version: number; publicReason?: string | null; internalNote?: string | null; suspectedDuplicate: boolean
  versions: ApplicationVersion[]; reviews: ApplicationReview[]; createdAt: string; updatedAt: string
}
export interface DealerProduct {
  id: string; sku: string; name: string; retailUnitPriceFen: number; referenceUnitPriceFen: number; currency: 'CNY'
  minInquiryQuantity: number; availableQuantity: number; leadTimeText: string; priceNotice: string
}
export interface InquiryItem {
  id: string; productId: string; sku: string; name: string; referenceUnitPriceFenSnapshot: number
  minInquiryQuantitySnapshot: number; quantity: number; replyReferenceUnitPriceFen?: number | null; replyLeadTimeText?: string | null
}
export interface InquiryHistory { action: string; fromStatus?: InquiryStatus | null; toStatus: InquiryStatus; inquiryVersion: number; reason?: string | null; createdAt: string }
export interface Inquiry {
  id: string; inquiryNumber: string; companyId: string; userId: string; status: InquiryStatus
  expectedDeliveryDate?: string | null; deliveryNotes?: string | null; purpose?: string | null; remark?: string | null
  publicReply?: string | null; closeReason?: string | null; version: number; items: InquiryItem[]
  history: InquiryHistory[]; createdAt: string; updatedAt: string
}
export interface Company {
  id: string; ownerUserId: string; sourceApplicationId: string; sourcePublicConsent: boolean; companyName: string
  businessType: string; countryOrRegion: string; city: string; contactName: string; phone: string
  cooperationEmail: string; website?: string | null; cooperationStatus: CooperationStatus; internalNote?: string | null
  version: number; createdAt: string; updatedAt: string
}
export interface Channel {
  id: string; name: string; countryOrRegion: string; city: string; address: string; phone: string
  website?: string | null; companyId?: string | null; published: boolean; version: number; updatedAt: string
}

export const applicationLabels: Record<string, string> = { PENDING: '待审核', APPROVED: '已通过', REJECTED: '已驳回' }
export const inquiryLabels: Record<string, string> = { NEW: '待处理', PROCESSING: '处理中', REPLIED: '已回复', CLOSED: '已关闭' }
export const businessLabels: Record<string, string> = { RETAIL: '零售', WHOLESALE: '批发', IMPORT: '进口', EDUCATION_ACTIVITY: '教育／活动机构', OTHER: '其他' }
