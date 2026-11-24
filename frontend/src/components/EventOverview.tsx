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
  const [budget, setBudget] = useState<EventBudget>()
  const [budgetError, setBudgetError] = useState('')
  useEffect(() => {
    let active = true
    getBudget(event.id).then((result) => { if (active) setBudget(result) })
      .catch((failure: Error) => { if (active) setBudgetError(failure.message) })
    return () => { active = false }
  }, [event.id])
  return <>
    <div className="info-strip">{event.name} &nbsp; · &nbsp; {event.eventDate} &nbsp; · &nbsp; {event.venue}{event.financeLead && <> &nbsp; · &nbsp; Finance Lead: {event.financeLead}</>}</div>
    <div className="metric-grid">
      <div className="metric"><span>Planned budget</span><strong>{money.format(event.plannedBudget)}</strong><small>Event spending target</small></div>
      <div className="metric"><span>Actual expenses</span><strong>{expenses ? money.format(expenses.totalSpent) : '—'}</strong><small>Recorded expenses</small></div>
      <div className="metric"><span>Bank transfers</span><strong>—</strong><small>Transfer tracking is pending</small></div>
      <div className="metric"><span>Funds raised</span><strong>{income ? money.format(income.totalIncome) : '—'}</strong><small>Total recorded income</small></div>
    </div>
    {budgetError && <p role="alert" className="error">Category budgets could not be loaded. {budgetError}</p>}
    {!budget && !budgetError && <p role="status">Loading category budgets...</p>}
    {budget && <BudgetTable budget={budget} />}
    <p className="balance-line">Event balance <strong className={income && income.balance < 0 ? 'error' : ''}>{income ? money.format(income.balance) : '—'}</strong><span>Income minus expenses</span></p>
    <div className="actions"><a className="button" href={`#/events/${event.id}/budget`}>Manage planned budget →</a><a className="button secondary" href={`#/events/${event.id}/expenses`}>Review expenses →</a><a className="button secondary" href={`#/events/${event.id}/purchases`}>Compare quotations →</a><button className="secondary" disabled title="Document storage is pending">Browse documents →</button></div>
  </>
}
