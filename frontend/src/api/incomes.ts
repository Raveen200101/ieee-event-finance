import { request } from './events'
import type { CreateIncomeRequest, EventIncomes, IncomeRecord } from '../types/income'

export function getIncomes(eventId: number) {
  return request<EventIncomes>(`/api/events/${eventId}/incomes`)
}

export function createIncome(eventId: number, income: CreateIncomeRequest) {
  return request<IncomeRecord>(`/api/events/${eventId}/incomes`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(income),
  })
}
