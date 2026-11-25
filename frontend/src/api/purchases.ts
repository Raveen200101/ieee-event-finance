import { request } from './events'
import type { PurchaseGroup, QuotationInput } from '../types/purchase'

const path = (eventId: number) => `/api/events/${eventId}/purchase-groups`
export const getPurchases = (eventId: number) => request<PurchaseGroup[]>(path(eventId))
export const createPurchase = (eventId: number, input: { name: string; budgetCategoryId: number; notes: string }) =>
  request<PurchaseGroup[]>(path(eventId), { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(input) })
export const saveQuotation = (eventId: number, groupId: number, input: QuotationInput, id?: number) =>
  request<PurchaseGroup[]>(`${path(eventId)}/${groupId}/quotations${id ? '/' + id : ''}`, {
    method: id ? 'PUT' : 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(input),
  })
export const removeQuotation = (eventId: number, groupId: number, id: number) =>
  request<PurchaseGroup[]>(`${path(eventId)}/${groupId}/quotations/${id}`, { method: 'DELETE' })
export const selectQuotation = (eventId: number, groupId: number, id?: number) =>
  request<PurchaseGroup[]>(`${path(eventId)}/${groupId}/selection${id ? '/' + id : ''}`, { method: id ? 'PUT' : 'DELETE' })
