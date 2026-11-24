import { useEffect, useState } from 'react'
import type { FormEvent } from 'react'
import { createExpense, getExpenses } from '../api/expenses'
import type { EventExpenses } from '../types/expense'

const currency = new Intl.NumberFormat('en-LK', { style: 'currency', currency: 'LKR' })

export function ExpensePanel({ eventId }: { eventId: number }) {
  const [data, setData] = useState<EventExpenses | null>(null)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')
  const [refresh, setRefresh] = useState(0)

  useEffect(() => {
    let active = true
    getExpenses(eventId)
      .then((result) => { if (active) { setData(result); setError('') } })
      .catch((failure: Error) => { if (active) setError(failure.message) })
    return () => { active = false }
  }, [eventId, refresh])

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const form = event.currentTarget
    const fields = new FormData(form)
    setSaving(true)
    setError('')
    setNotice('')
    try {
      await createExpense(eventId, {
        description: String(fields.get('description')).trim(),
        category: String(fields.get('category')).trim(),
        amount: Number(fields.get('amount')),
        expenseDate: String(fields.get('expenseDate')),
      })
      form.reset()
      setNotice('Expense saved.')
      setData(await getExpenses(eventId))
    } catch (failure) {
      setError(failure instanceof Error ? failure.message : 'Unable to save expense.')
    } finally {
      setSaving(false)
    }
  }

  return (
    <section className="expense-panel" aria-label="Event expenses">
      <h3>Expenses</h3>
      {error && <div role="alert" className="error"><p>{error}</p><button type="button" onClick={() => setRefresh((value) => value + 1)}>Refresh expenses</button></div>}
      {notice && <p role="status">{notice}</p>}
      {!data && !error && <p role="status">Loading expenses…</p>}
      {data && <>
        <dl>
          <dt>Total spent</dt><dd>{currency.format(data.totalSpent)}</dd>
          <dt>{data.remainingBudget < 0 ? 'Over budget by' : 'Remaining budget'}</dt>
          <dd className={data.remainingBudget < 0 ? 'error' : ''}>{currency.format(Math.abs(data.remainingBudget))}</dd>
        </dl>
        {data.expenses.length === 0 && <p>No expenses recorded for this event.</p>}
        <ul className="expense-list">
          {data.expenses.map((expense) => (
            <li key={expense.id}>
              <strong>{expense.description}</strong>
              <span>{expense.category} · {expense.expenseDate}</span>
              <span>{currency.format(expense.amount)}</span>
            </li>
          ))}
        </ul>
      </>}
      <h3>Add expense</h3>
      <form onSubmit={submit}>
        <fieldset disabled={saving || !data}>
          <label>Expense description<input name="description" required maxLength={500} /></label>
          <label>Category<input name="category" required maxLength={50} list="expense-categories" /></label>
          <datalist id="expense-categories">
            <option value="Food" /><option value="Transport" /><option value="Venue" />
            <option value="Printing" /><option value="Other" />
          </datalist>
          <label>Amount (LKR)<input name="amount" type="number" min="0.01" max="9999999999999.99" step="0.01" required /></label>
          <label>Expense date<input name="expenseDate" type="date" required /></label>
          <button type="submit">{saving ? 'Saving…' : 'Add expense'}</button>
        </fieldset>
      </form>
    </section>
  )
}
