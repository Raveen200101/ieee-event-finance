export type ChapterRole = 'COMMITTEE_MEMBER' | 'FINANCE_TEAM_LEAD' | 'LOGISTICS_TEAM_LEAD' | 'VOLUNTEER'
export interface ChapterUser {
  id: number
  email: string
  displayName: string
  role: ChapterRole
  enabled: boolean
  accountAdmin: boolean
}
export interface ChapterAccount extends ChapterUser {
  currentEvents: { id: number; name: string; status: string }[]
}
export const roleLabels: Record<ChapterRole, string> = { COMMITTEE_MEMBER: 'Committee member', FINANCE_TEAM_LEAD: 'Finance Team Lead', LOGISTICS_TEAM_LEAD: 'Logistics Team Lead', VOLUNTEER: 'Volunteer' }
