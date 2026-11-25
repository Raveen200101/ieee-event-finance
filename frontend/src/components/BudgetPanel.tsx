import { useAuth } from './AuthContext'
import { useEffect, useRef, useState } from 'react'
import type { FormEvent } from 'react'
import { deleteBudget, getBudget, saveBudget } from '../api/budgets'
import type { BudgetCategory, EventBudget } from '../types/budget'

const money = (value: number) => value.toLocaleString('en-LK', { minimumFractionDigits: 2, maximumFractionDigits: 2 })

export function BudgetTable({ budget, onEdit, onDelete, busy }: {
  budget: EventBudget
  onEdit?: (category: BudgetCategory) => void
  onDelete?: (category: BudgetCategory) => void
  busy?: boolean
}) {
  return <section className="panel table-panel">
    <h2>Budget by category</h2>
    <div className="table-scroll"><table className="budget-table"><thead><tr><th>Category</th><th>Owner</th><th>Planned (LKR)</th><th>Actual (LKR)</th><th>Remaining (LKR)</th><th>Progress</th>{onEdit && <th>Actions</th>}</tr></thead>
      <tbody>{budget.categories.map((row) => <tr key={row.id ?? `unallocated-${row.category}`}>
        <td>{row.category}</td><td>{row.owner || 'Unassigned'}</td><td>{row.id === null ? '—' : money(row.plannedAmount)}</td><td>{money(row.actualAmount)}</td>
        <td className={row.remainingAmount < 0 ? 'error' : ''}>{row.id === null ? '—' : money(row.remainingAmount)}</td>
        <td className={row.id !== null && row.actualAmount > row.plannedAmount ? 'error' : ''}>{row.id === null ? 'Not allocated' : row.plannedAmount === 0 ? (row.actualAmount > 0 ? 'Over budget' : 'No spending') : `${Math.round(row.actualAmount / row.plannedAmount * 100)}% of budget`}</td>
        {onEdit && <td><div className="actions">{row.id === null ? <button className="secondary" disabled={busy} onClick={() => onEdit(row)}>Allocate</button> : <><button className="secondary" disabled={busy} onClick={() => onEdit(row)}>Edit</button><button className="secondary" disabled={busy} onClick={() => onDelete?.(row)} aria-label={`Remove ${row.category} allocation`}>Remove</button></>}</div></td>}
      </tr>)}</tbody></table></div>
    {!budget.categories.length && <p className="empty-state">No category allocations or expenses yet. Add your first category below.</p>}
    <div className="table-footer">{budget.categories.length} categories · All amounts in LKR. Expense categories match by name, ignoring case and surrounding spaces.</div>
  </section>
}

export function BudgetPanel({ eventId }: { eventId: number }) {
  const { canManageFinance } = useAuth()
  const [budget, setBudget] = useState<EventBudget>()
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')
  const [revision, setRevision] = useState(0)
  const [busy, setBusy] = useState(false)
  const [editing, setEditing] = useState<number>()
  const [category, setCategory] = useState('')
  const [owner, setOwner] = useState('')
  const [amount, setAmount] = useState('')
  const form = useRef<HTMLFormElement>(null)

  useEffect(() => {
    let active = true
    getBudget(eventId).then((result) => { if (active) { setBudget(result); setError('') } })
      .catch((failure: Error) => { if (active) setError(failure.message) })
    return () => { active = false }
  }, [eventId, revision])

  function reset() { setEditing(undefined); setCategory(''); setOwner(''); setAmount('') }
  function edit(row: BudgetCategory) {
    setEditing(row.id ?? undefined); setCategory(row.category); setOwner(row.owner || ''); setAmount(String(row.plannedAmount)); setError(''); setNotice('')
    form.current?.scrollIntoView({ behavior: 'smooth', block: 'center' })
    form.current?.querySelector<HTMLInputElement>('input[name="owner"]')?.focus()
  }
  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault(); setBusy(true); setError(''); setNotice('')
    try {
      setBudget(await saveBudget(eventId, { category: category.trim(), owner: owner.trim(), plannedAmount: Number(amount) }, editing))
      reset(); setNotice('Budget allocation saved.')
    } catch (failure) { setError(failure instanceof Error ? failure.message : 'Unable to save allocation.') }
    finally { setBusy(false) }
  }
  async function remove(row: BudgetCategory) {
    if (row.id === null || !window.confirm(`Remove the ${row.category} allocation? Existing expenses will be preserved and shown as not allocated.`)) return
    setBusy(true); setError(''); setNotice('')
    try {
      setBudget(await deleteBudget(eventId, row.id)); if (editing === row.id) reset()
      setNotice('Allocation removed. Existing expenses are preserved.')
    } catch (failure) { setError(failure instanceof Error ? failure.message : 'Unable to remove allocation.') }
    finally { setBusy(false) }
  }
  return <>
    {error && <div role="alert" className="error"><p>{error}</p><button className="secondary" disabled={busy} onClick={() => setRevision((value) => value + 1)}>Reload budget</button></div>}
    {notice && <p role="status" className="info-strip">{notice}</p>}
    {!budget && !error && <p role="status">Loading budget...</p>}
    {budget && <>
      <div className="metric-grid">
        <div className="metric"><span>Event budget</span><strong>LKR {money(budget.plannedBudget)}</strong><small>Event spending target</small></div>
        <div className="metric"><span>Allocated to categories</span><strong>LKR {money(budget.totalAllocated)}</strong><small>Total category plans</small></div>
        <div className="metric"><span>{budget.unallocatedBudget < 0 ? 'Overallocated by' : 'Unallocated budget'}</span><strong className={budget.unallocatedBudget < 0 ? 'error' : ''}>LKR {money(Math.abs(budget.unallocatedBudget))}</strong><small>Event budget minus allocations</small></div>
        <div className="metric"><span>Actual expenses</span><strong>LKR {money(budget.totalActual)}</strong><small>Includes unallocated categories</small></div>
      </div>
      {budget.unallocatedBudget < 0 && <p role="status" className="error">Category allocations exceed the event budget. Review your allocations.</p>}
      <BudgetTable budget={budget} onEdit={canManageFinance ? edit : undefined} onDelete={canManageFinance ? remove : undefined} busy={busy} />
      {canManageFinance && <section className="panel budget-form">
        <h2>{editing ? `Edit ${category} allocation` : 'Add category allocation'}</h2>
        <form ref={form} onSubmit={submit}><fieldset disabled={busy}><div className="form-grid">
          <label>Category<input name="category" required maxLength={50} value={category} disabled={!!editing} onChange={(event) => setCategory(event.target.value)} list="budget-category-suggestions" /></label>
          <datalist id="budget-category-suggestions">{budget.categories.filter((row) => row.id === null).map((row) => <option key={row.category} value={row.category} />)}</datalist>
          <label>Responsible person<input name="owner" required maxLength={150} value={owner} onChange={(event) => setOwner(event.target.value)} /></label>
          <label>Planned amount (LKR)<input name="amount" type="number" required min="0" max="9999999999999.99" step="0.01" value={amount} onChange={(event) => setAmount(event.target.value)} /></label>
        </div><p className="field-help">Use the same category name when recording expenses. Responsible people are entered as names until team membership is available. Editing preserves the category name.</p>
          <div className="actions form-actions"><button type="submit">{busy ? 'Saving...' : editing ? 'Save changes' : 'Add allocation'}</button>{(editing || category || owner || amount) && <button type="button" className="secondary" onClick={reset}>Cancel</button>}</div>
        </fieldset></form>
      </section>}
    </>}
  </>
}
