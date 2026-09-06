import { api } from '../../services/http'
import type { Cart, Detail, OrderPage, Preview } from './types'
export const readCart = async () => (await api<Cart>('/cart')).data
export const previewCart = async () => (await api<Preview>('/checkout-previews', { method: 'POST' })).data
export const readOrder = async (id: string, admin = false) =>
  (await api<Detail>(`${admin ? '/admin' : ''}/orders/${id}`)).data
export const readOrders = async (query: string, admin = false) =>
  (await api<OrderPage>(`${admin ? '/admin' : ''}/orders?${query}`)).data
