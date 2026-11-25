import { createContext, useContext } from 'react'
import type { ChapterUser } from '../types/auth'
export const AuthContext = createContext<{ user: ChapterUser; logout: () => Promise<void> } | null>(null)
export function useAuth() {
  const context = useContext(AuthContext)
  if (!context) throw new Error('Sign-in context is unavailable.')
  const committee = context.user.role === 'COMMITTEE_MEMBER'
  return { ...context, isChair: committee, isAdmin: committee && context.user.accountAdmin, canManageFinance: committee || context.user.role === 'FINANCE_TEAM_LEAD', canManagePurchases: committee || context.user.role === 'LOGISTICS_TEAM_LEAD' }
}
