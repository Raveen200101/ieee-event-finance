import { request } from './client'
import type { ChapterRole } from '../types/auth'

export interface EventMember {
  id: number
  displayName: string
  role: ChapterRole
  enabled: boolean
  assigned: boolean
}

export const getEventMembers = (eventId: number) => request<EventMember[]>(`/api/events/${eventId}/members/options`)
export const setEventMember = (eventId: number, userId: number, assigned: boolean) =>
  request<void>(`/api/events/${eventId}/members/${userId}`, { method: assigned ? 'PUT' : 'DELETE' })
export const setPurchaseOwner = (eventId: number, groupId: number, userId?: number) =>
  request<void>(`/api/events/${eventId}/purchase-groups/${groupId}/owner`, userId === undefined
    ? { method: 'DELETE' }
    : { method: 'PUT', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ userId }) })
