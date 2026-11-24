import type { EventRecord } from '../types/event'
import type { EventExpenses } from '../types/expense'
import type { EventIncomes } from '../types/income'

const money = new Intl.NumberFormat('en-LK', { style: 'currency', currency: 'LKR', maximumFractionDigits: 2 })

export function EventOverview({ event, expenses, income }: {
  event: EventRecord
  expenses?: EventExpenses
  income?: EventIncomes
}) {
  const categories = new Map<string, number>()
  for (const expense of expenses?.expenses || []) {
    categories.set(expense.category, (categories.get(expense.category) || 0) + expense.amount)
  }
  return <>
    <div className="info-strip">{event.name} &nbsp; · &nbsp; {event.eventDate} &nbsp; · &nbsp; {event.venue}{event.financeLead && <> &nbsp; · &nbsp; Finance Lead: {event.financeLead}</>}</div>
    <div className="metric-grid">
      <div className="metric"><span>Planned budget</span><strong>{money.format(event.plannedBudget)}</strong><small>Event spending target</small></div>
      <div className="metric"><span>Actual expenses</span><strong>{expenses ? money.format(expenses.totalSpent) : '—'}</strong><small>Recorded expenses</small></div>
      <div className="metric"><span>Bank transfers</span><strong>—</strong><small>Transfer tracking is pending</small></div>
      <div className="metric"><span>Funds raised</span><strong>{income ? money.format(income.totalIncome) : '—'}</strong><small>Total recorded income</small></div>
    </div>
    <section className="panel table-panel">
      <h2>Budget by category</h2>
      <div className="table-scroll"><table>
        <thead><tr><th>Category</th><th>Owner</th><th>Planned (LKR)</th><th>Actual (LKR)</th><th>Progress</th></tr></thead>
        <tbody>{Array.from(categories, ([category, total]) => <tr key={category}><td>{category}</td><td>—</td><td>—</td><td>{total.toLocaleString('en-LK', { minimumFractionDigits: 2 })}</td><td>Not allocated</td></tr>)}</tbody>
      </table></div>
      {categories.size === 0 && <p className="empty-state">No expenses recorded for this event.</p>}
      <div className="table-footer">{categories.size} categories · All amounts in LKR. Category budgets and owners are pending.</div>
    </section>
    <p className="balance-line">Event balance <strong className={income && income.balance < 0 ? 'error' : ''}>{income ? money.format(income.balance) : '—'}</strong><span>Income minus expenses</span></p>
    <div className="actions"><a className="button" href={`#/events/${event.id}/expenses`}>Review expenses →</a><button className="secondary" disabled title="Quotation workflow is pending">Compare quotations →</button><button className="secondary" disabled title="Document storage is pending">Browse documents →</button></div>
  </>
}
