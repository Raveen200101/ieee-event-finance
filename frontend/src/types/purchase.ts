export interface QuotationInput {
  supplier: string
  quantity: number
  unitPrice: number
  notes: string
}
export interface Quotation extends QuotationInput {
  id: number
  total: number
  selected: boolean
}
export interface PurchaseGroup {
  id: number
  name: string
  ownerId: number | null
  notes: string
  budgetCategoryId: number
  category: string
  categoryBudget: number
  categorySelectedTotal: number
  categoryRemaining: number
  quotations: Quotation[]
}
