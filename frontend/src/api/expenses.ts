import { request } from './events'
import type { CreateExpenseRequest, EventExpenses, ExpenseRecord } from '../types/expense'

export function getExpenses(eventId: number) {
  return request<EventExpenses>(`/api/events/${eventId}/expenses`)
}

export function createExpense(eventId: number, expense: CreateExpenseRequest) {
  return request<ExpenseRecord>(`/api/events/${eventId}/expenses`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(expense),
  })
}
