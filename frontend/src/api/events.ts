import type { CreateEventRequest, EventRecord } from '../types/event'

async function request<T>(path: string, options?: RequestInit): Promise<T> {
  const response = await fetch(path, options)
  if (!response.ok) {
    const problem = await response.json().catch(() => null)
    const details = problem?.errors
      ? Object.values(problem.errors).join('. ')
      : problem?.detail
    throw new Error(details || 'Unable to complete the request. Please try again.')
  }
  return response.json() as Promise<T>
}

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
