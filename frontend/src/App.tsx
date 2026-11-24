import { useEffect, useRef, useState } from 'react'
import { getEvent, listEvents } from './api/events'
import { EventForm } from './components/EventForm'
import { ExpensePanel } from './components/ExpensePanel'
import type { EventRecord } from './types/event'

const currency = new Intl.NumberFormat('en-LK', { style: 'currency', currency: 'LKR' })

function App() {
  const [events, setEvents] = useState<EventRecord[]>([])
  const [selected, setSelected] = useState<EventRecord | null>(null)
  const [loading, setLoading] = useState(true)
  const [detailsLoading, setDetailsLoading] = useState(false)
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')
  const selectionRequest = useRef(0)

  useEffect(() => {
    let active = true
    listEvents()
      .then((result) => { if (active) setEvents(result) })
      .catch((failure: Error) => { if (active) setError(failure.message) })
      .finally(() => { if (active) setLoading(false) })
    return () => { active = false }
  }, [])

  async function selectEvent(id: number) {
    const request = ++selectionRequest.current
    setDetailsLoading(true)
    setError('')
    try {
      const event = await getEvent(id)
      if (request === selectionRequest.current) setSelected(event)
    } catch (failure) {
      if (request === selectionRequest.current) {
        setError(failure instanceof Error ? failure.message : 'Unable to load event.')
      }
    } finally {
      if (request === selectionRequest.current) setDetailsLoading(false)
    }
  }

  function onCreated(event: EventRecord) {
    selectionRequest.current++
    setDetailsLoading(false)
    setEvents((previous) => [...previous, event].sort(
      (a, b) => b.eventDate.localeCompare(a.eventDate) || b.id - a.id,
    ))
    setSelected(event)
    setError('')
    setNotice('Event created successfully.')
  }

  return (
    <main>
      <header><p className="chapter">IEEE Computer Society Â· SLIIT</p><h1>IEEE Event Finance</h1></header>
      <div className="layout">
        <EventForm onCreated={onCreated} />
        <section className="panel">
          <h2>Events</h2>
          {loading && <p role="status">Loading eventsâ€¦</p>}
          {error && <p role="alert" className="error">{error} Reload the page to try again.</p>}
          {notice && <p role="status">{notice}</p>}
          {!loading && !error && events.length === 0 && <p>Create your first event to get started.</p>}
          <ul className="events">
            {events.map((event) => (
              <li key={event.id}>
                <button className="event" onClick={() => selectEvent(event.id)} aria-pressed={selected?.id === event.id}>
                  <strong>{event.name}</strong>
                  <span>{event.eventDate} Â· {event.venue}</span>
                  <span>{currency.format(event.plannedBudget)}</span>
                </button>
              </li>
            ))}
          </ul>
          {detailsLoading && <p role="status">Loading event detailsâ€¦</p>}
          {selected && !detailsLoading && (
            <article className="details" aria-label="Event details">
              <h2>{selected.name}</h2>
              <dl>
                <dt>Date</dt><dd>{selected.eventDate}</dd>
                <dt>Venue</dt><dd>{selected.venue}</dd>
                <dt>Planned budget</dt><dd>{currency.format(selected.plannedBudget)}</dd>
              </dl>
              <p className="description">{selected.description || 'No description provided.'}</p>
              <ExpensePanel key={selected.id} eventId={selected.id} />
            </article>
          )}
        </section>
      </div>
    </main>
  )
}

export default App
