import { useState } from 'react'
import type { FormEvent } from 'react'
import { createEvent } from '../api/events'
import type { EventRecord } from '../types/event'

export function EventForm({ onCreated }: { onCreated: (event: EventRecord) => void }) {
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState('')

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const form = event.currentTarget
    const fields = new FormData(form)
    setSaving(true)
    setError('')
    try {
      const created = await createEvent({
        name: String(fields.get('name')).trim(),
        description: String(fields.get('description')).trim(),
        eventDate: String(fields.get('eventDate')),
        venue: String(fields.get('venue')).trim(),
        plannedBudget: Number(fields.get('plannedBudget')),
      })
      form.reset()
      onCreated(created)
    } catch (failure) {
      setError(failure instanceof Error ? failure.message : 'Unable to create event.')
    } finally {
      setSaving(false)
    }
  }

  return (
    <section className="panel">
      <h2>Create event</h2>
      <form onSubmit={submit}>
        <fieldset disabled={saving}>
          <label>Event name<input name="name" required maxLength={150} /></label>
          <label>Description<textarea name="description" maxLength={2000} rows={3} /></label>
          <label>Date<input name="eventDate" type="date" required /></label>
          <label>Venue<input name="venue" required maxLength={255} /></label>
          <label>Planned budget (LKR)<input name="plannedBudget" type="number" min="0" max="9999999999999.99" step="0.01" required /></label>
          <button type="submit">{saving ? 'Saving…' : 'Create event'}</button>
        </fieldset>
        {error && <p role="alert" className="error">{error}</p>}
      </form>
    </section>
  )
}
