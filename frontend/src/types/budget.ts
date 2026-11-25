export interface BudgetCategory {
  id: number | null
  category: string
  owner: string | null
  plannedAmount: number
  actualAmount: number
  remainingAmount: number
}

export interface EventBudget {
  eventId: number
  plannedBudget: number
  totalAllocated: number
  unallocatedBudget: number
  totalActual: number
  categories: BudgetCategory[]
}

export interface BudgetInput {
  category: string
  owner: string
  plannedAmount: number
}
