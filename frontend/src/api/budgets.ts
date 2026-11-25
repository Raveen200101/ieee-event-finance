import { request } from './events'
import type { BudgetInput, EventBudget } from '../types/budget'

export const getBudget = (eventId: number) => request<EventBudget>(`/api/events/${eventId}/budget`)

export function saveBudget(eventId: number, input: BudgetInput, id?: number) {
  return request<EventBudget>(`/api/events/${eventId}/budget${id ? '/' + id : ''}`, {
    method: id ? 'PUT' : 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(id ? { owner: input.owner, plannedAmount: input.plannedAmount } : input),
  })
}

export const deleteBudget = (eventId: number, id: number) => request<EventBudget>(`/api/events/${eventId}/budget/${id}`, { method: 'DELETE' })
