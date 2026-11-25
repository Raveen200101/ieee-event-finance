export interface IncomeRecord {
  id: number
  description: string
  source: string
  amount: number
  incomeDate: string
}

export interface EventIncomes {
  eventId: number
  totalIncome: number
  totalExpenses: number
  balance: number
  incomes: IncomeRecord[]
}

export interface CreateIncomeRequest {
  description: string
  source: string
  amount: number
  incomeDate: string
}
