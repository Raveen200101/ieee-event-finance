import { useAuth } from './AuthContext'
import type { EventRecord } from '../types/event'
import type { EventExpenses } from '../types/expense'
import type { EventIncomes } from '../types/income'
import { useEffect, useState } from 'react'
import { getBudget } from '../api/budgets'
import type { EventBudget } from '../types/budget'
import { BudgetTable } from './BudgetPanel'

const money = new Intl.NumberFormat('en-LK', { style: 'currency', currency: 'LKR', maximumFractionDigits: 2 })

export function EventOverview({ event, expenses, income }: {
  event: EventRecord
  expenses?: EventExpenses
  income?: EventIncomes
}) {
  const { isChair, canManageFinance, canManagePurchases } = useAuth()
  const [budget, setBudget] = useState<EventBudget>()
  const [budgetError, setBudgetError] = useState('')
  useEffect(() => {
    let active = true
    if (!isChair) return () => { active = false }
    getBudget(event.id).then((result) => { if (active) setBudget(result) })
      .catch((failure: Error) => { if (active) setBudgetError(failure.message) })
    return () => { active = false }
  }, [event.id, isChair])
  return <>
    {!isChair && canManageFinance && <p className="info-strip">These totals include only your own records for this assigned event.</p>}
    <div className="info-strip">{event.name} &nbsp; · &nbsp; {event.eventDate} &nbsp; · &nbsp; {event.venue}{event.financeLead && <> &nbsp; · &nbsp; Finance Lead: {event.financeLead}</>}</div>
    {canManageFinance && <div className="metric-grid">
      <div className="metric"><span>Planned budget</span><strong>{event.plannedBudget === undefined ? '—' : money.format(event.plannedBudget)}</strong><small>Event spending target</small></div>
      <div className="metric"><span>Actual expenses</span><strong>{expenses ? money.format(expenses.totalSpent) : '—'}</strong><small>Recorded expenses</small></div>
      <div className="metric"><span>Bank transfers</span><strong>—</strong><small>Transfer tracking is pending</small></div>
      <div className="metric"><span>Funds raised</span><strong>{income ? money.format(income.totalIncome) : '—'}</strong><small>Total recorded income</small></div>
    </div>}
    {budgetError && <p role="alert" className="error">Category budgets could not be loaded. {budgetError}</p>}
    {isChair && !budget && !budgetError && <p role="status">Loading category budgets...</p>}
    {budget && <BudgetTable budget={budget} />}
    {canManageFinance && <p className="balance-line">{isChair ? 'Event balance' : 'Your records balance'} <strong className={income && income.balance < 0 ? 'error' : ''}>{income ? money.format(income.balance) : '—'}</strong><span>Income minus expenses</span></p>}
    <div className="actions">{isChair && <a className="button" href={`#/events/${event.id}/budget`}>Manage planned budget →</a>}{canManageFinance && <a className="button secondary" href={`#/events/${event.id}/expenses`}>Review expenses →</a>}{canManagePurchases && <a className="button secondary" href={`#/events/${event.id}/purchases`}>Compare quotations →</a>}<button className="secondary" disabled title="Document storage is pending">Browse documents →</button></div>
  </>
}
