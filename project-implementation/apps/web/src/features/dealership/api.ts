import { api, newIdempotencyKey } from '../../services/http'
import type { Channel, Company, DealerApplication, DealerProduct, Inquiry, PageResult } from './types'

export const getChannels = async (query = '') => (await api<PageResult<Channel>>(`/channels${query ? `?${query}` : ''}`)).data
export const getApplications = async (admin = false, query = '') => (await api<PageResult<DealerApplication>>(`${admin ? '/admin' : ''}/dealer-applications${query ? `?${query}` : ''}`)).data
export const getApplication = async (id: string, admin = false) => (await api<DealerApplication>(`${admin ? '/admin' : ''}/dealer-applications/${id}`)).data
export const createApplication = async (body: unknown) => (await api<DealerApplication>('/dealer-applications', { method: 'POST', headers: { 'Idempotency-Key': newIdempotencyKey() }, body: JSON.stringify(body) })).data
export const resubmitApplication = async (id: string, body: unknown) => (await api<DealerApplication>(`/dealer-applications/${id}/resubmit`, { method: 'POST', headers: { 'Idempotency-Key': newIdempotencyKey() }, body: JSON.stringify(body) })).data
export const reviewApplication = async (id: string, body: unknown) => (await api<DealerApplication>(`/admin/dealer-applications/${id}/review`, { method: 'POST', headers: { 'Idempotency-Key': newIdempotencyKey() }, body: JSON.stringify(body) })).data
export const getDealerCatalog = async () => (await api<DealerProduct[]>('/dealer/catalog')).data
export const createInquiry = async (body: unknown) => (await api<Inquiry>('/inquiries', { method: 'POST', headers: { 'Idempotency-Key': newIdempotencyKey() }, body: JSON.stringify(body) })).data
export const getInquiries = async (admin = false, query = '') => (await api<PageResult<Inquiry>>(`${admin ? '/admin' : ''}/inquiries${query ? `?${query}` : ''}`)).data
export const getInquiry = async (id: string, admin = false) => (await api<Inquiry>(`${admin ? '/admin' : ''}/inquiries/${id}`)).data
export const inquiryCommand = async (id: string, command: 'start' | 'replies' | 'close', body: unknown, admin = false) => (await api<Inquiry>(`${admin ? '/admin' : ''}/inquiries/${id}/${command}`, { method: 'POST', headers: { 'Idempotency-Key': newIdempotencyKey() }, body: JSON.stringify(body) })).data
export const getCompanies = async (query = '') => (await api<PageResult<Company>>(`/admin/companies${query ? `?${query}` : ''}`)).data
export const companyStatus = async (company: Company, restore: boolean, reason: string) => (await api<Company>(`/admin/companies/${company.id}/${restore ? 'restore' : 'suspend'}`, { method: 'POST', headers: { 'Idempotency-Key': newIdempotencyKey() }, body: JSON.stringify({ expectedVersion: company.version, reason }) })).data
export const getAdminChannels = async (query = '') => (await api<PageResult<Channel>>(`/admin/channels${query ? `?${query}` : ''}`)).data
export const createChannel = async (body: unknown) => (await api<Channel>('/admin/channels', { method: 'POST', headers: { 'Idempotency-Key': newIdempotencyKey() }, body: JSON.stringify(body) })).data
export const updateChannel = async (id: string, body: unknown) => (await api<Channel>(`/admin/channels/${id}`, { method: 'PATCH', body: JSON.stringify(body) })).data
export const channelStatus = async (channel: Channel, publish: boolean, reason: string) => (await api<Channel>(`/admin/channels/${channel.id}/${publish ? 'publish' : 'unpublish'}`, { method: 'POST', headers: { 'Idempotency-Key': newIdempotencyKey() }, body: JSON.stringify({ expectedVersion: channel.version, reason }) })).data
