export interface ExpenseRecord {
  id: number
  description: string
  category: string
  amount: number
  expenseDate: string
}

export interface EventExpenses {
  eventId: number
  plannedBudget: number
  totalSpent: number
  remainingBudget: number
  expenses: ExpenseRecord[]
}

export interface CreateExpenseRequest {
  description: string
  category: string
  amount: number
  expenseDate: string
}
