import { useAuth } from './AuthContext'
import { useEffect, useState } from 'react'
import type { FormEvent } from 'react'
import { createIncome, getIncomes } from '../api/incomes'
import type { EventIncomes } from '../types/income'

const currency = new Intl.NumberFormat('en-LK', { style: 'currency', currency: 'LKR' })

export function IncomePanel({ eventId, revision }: { eventId: number; revision: number }) {
  const { canManageFinance } = useAuth()
  const [data, setData] = useState<EventIncomes | null>(null)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')
  const [refresh, setRefresh] = useState(0)

  useEffect(() => {
    let active = true
    getIncomes(eventId)
      .then((result) => { if (active) { setData(result); setError('') } })
      .catch((failure: Error) => { if (active) setError(failure.message) })
    return () => { active = false }
  }, [eventId, refresh, revision])

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const form = event.currentTarget
    const fields = new FormData(form)
    setSaving(true)
    setError('')
    setNotice('')
    try {
      await createIncome(eventId, {
        description: String(fields.get('description')).trim(),
        source: String(fields.get('source')).trim(),
        amount: Number(fields.get('amount')),
        incomeDate: String(fields.get('incomeDate')),
      })
      form.reset()
      setNotice('Income saved.')
      setRefresh((value) => value + 1)
    } catch (failure) {
      setError(failure instanceof Error ? failure.message : 'Unable to save income.')
    } finally {
      setSaving(false)
    }
  }

  return (
    <section className="expense-panel" aria-label="Event income">
      <h3>Income</h3>
      {error && <div role="alert" className="error"><p>{error}</p><button type="button" onClick={() => setRefresh((value) => value + 1)}>Refresh income</button></div>}
      {notice && <p role="status">{notice}</p>}
      {!data && !error && <p role="status">Loading income...</p>}
      {data && <>
        <dl>
          <dt>Total income</dt><dd>{currency.format(data.totalIncome)}</dd>
          <dt>Total expenses</dt><dd>{currency.format(data.totalExpenses)}</dd>
          <dt>Event balance</dt><dd className={data.balance < 0 ? 'error' : ''}>{currency.format(data.balance)}</dd>
        </dl>
        {data.incomes.length === 0 && <p>No income recorded for this event.</p>}
        <ul className="expense-list">
          {data.incomes.map((income) => (
            <li key={income.id}>
              <strong>{income.description}</strong>
              <span>{income.source} &middot; {income.incomeDate}</span>
              <span>{currency.format(income.amount)}</span>
            </li>
          ))}
        </ul>
      </>}
      {canManageFinance && <><h3>Add income</h3>
      <form onSubmit={submit}>
        <fieldset disabled={saving || !data}>
          <label>Income description<input name="description" required maxLength={500} /></label>
          <label>Source<input name="source" required maxLength={150} list="income-categories" /></label>
          <datalist id="income-categories">
            <option value="Sponsorship" /><option value="Registration fees" /><option value="Donation" /><option value="Other" />
          </datalist>
          <label>Amount (LKR)<input name="amount" type="number" min="0.01" max="9999999999999.99" step="0.01" required /></label>
          <label>Income date<input name="incomeDate" type="date" required /></label>
          <button type="submit">{saving ? 'Saving...' : 'Add income'}</button>
        </fieldset>
      </form></>}
    </section>
  )
}
