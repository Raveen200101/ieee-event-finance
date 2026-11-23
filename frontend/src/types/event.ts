export interface EventRecord {
  id: number
  name: string
  description: string
  eventDate: string
  venue: string
  plannedBudget: number
}

export interface CreateEventRequest {
  name: string
  description: string
  eventDate: string
  venue: string
  plannedBudget: number
}
