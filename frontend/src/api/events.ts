import type { CreateEventRequest, EventRecord } from '../types/event'

import { request } from './client'
export { request } from './client'

export function listEvents() {
  return request<EventRecord[]>('/api/events')
}

export function getEvent(id: number) {
  return request<EventRecord>(`/api/events/${id}`)
}

export function createEvent(event: CreateEventRequest) {
  return request<EventRecord>('/api/events', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(event),
  })
}
