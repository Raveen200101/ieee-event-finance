import { useState } from 'react'
import type { FormEvent } from 'react'
import { createEvent } from '../api/events'
import type { EventRecord } from '../types/event'

const currentYear = new Date().getFullYear()

export function EventForm({ onCreated, onCancel }: {
  onCreated: (event: EventRecord) => void
  onCancel: () => void
}) {
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState('')

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const fields = new FormData(event.currentTarget)
    setSaving(true)
    setError('')
    try {
      onCreated(await createEvent({
        name: String(fields.get('name')).trim(),
        description: String(fields.get('description')).trim(),
        eventDate: String(fields.get('eventDate')),
        venue: String(fields.get('venue')).trim(),
        plannedBudget: Number(fields.get('plannedBudget')),
        term: String(fields.get('term')).trim(),
        financeLead: String(fields.get('financeLead')).trim(),
        status: String(fields.get('status')),
        expectedParticipants: fields.get('expectedParticipants') === '' ? null : Number(fields.get('expectedParticipants')),
      }))
    } catch (failure) {
      setError(failure instanceof Error ? failure.message : 'Unable to create event.')
    } finally {
      setSaving(false)
    }
  }

  return (
    <form onSubmit={submit}>
      <fieldset disabled={saving}>
        <section className="panel event-setup">
          <h2>Event setup</h2>
          <div className="form-grid">
            <label>Event name *<input name="name" required maxLength={150} placeholder="e.g. CodeSprint 2026" /></label>
            <label>ExCom term<input name="term" maxLength={30} defaultValue={`${currentYear} / ${currentYear + 1}`} /></label>
            <label>Event date *<input name="eventDate" type="date" required /></label>
            <label>Venue *<input name="venue" required maxLength={255} placeholder="e.g. SLIIT Malabe" /></label>
            <label>Finance lead<input name="financeLead" maxLength={150} placeholder="Finance lead's name" /></label>
            <label>Event status<select aria-label="Event status" name="status" defaultValue="Planning"><option>Planning</option><option>In progress</option><option>Completed</option><option>Archived</option></select></label>
            <label>Planned budget (LKR) *<input name="plannedBudget" type="number" min="0" max="9999999999999.99" step="0.01" required /></label>
            <label>Expected participants<input name="expectedParticipants" type="number" min="0" max="2147483647" step="1" /></label>
            <label className="full-width">Description<textarea name="description" maxLength={2000} rows={2} placeholder="A chapter coding competition with workshops, team challenges and awards." /></label>
          </div>
          <p className="field-help">Draft? You can add expenses and income after saving this event.</p>
        </section>
        {error && <p role="alert" className="error">{error}</p>}
        <div className="actions form-actions"><button type="submit">{saving ? 'Saving...' : '+ Create event'}</button><button type="button" className="secondary" onClick={onCancel}>Cancel</button></div>
      </fieldset>
    </form>
  )
}
