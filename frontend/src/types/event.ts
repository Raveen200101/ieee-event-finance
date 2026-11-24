export interface EventRecord {
  id: number
  name: string
  description: string
  eventDate: string
  venue: string
  plannedBudget: number
  term?: string | null
  financeLead?: string | null
  status?: string
  expectedParticipants?: number | null
}

export interface CreateEventRequest {
  name: string
  description: string
  eventDate: string
  venue: string
  plannedBudget: number
  term?: string | null
  financeLead?: string | null
  status?: string
  expectedParticipants?: number | null
}
