import { useEffect, useState } from 'react'
import { getEvent, listEvents } from './api/events'
import { getExpenses } from './api/expenses'
import { getIncomes } from './api/incomes'
import { AppShell } from './components/AppShell'
import { EventForm } from './components/EventForm'
import { EventOverview } from './components/EventOverview'
import { ExpensePanel } from './components/ExpensePanel'
import { IncomePanel } from './components/IncomePanel'
import type { EventRecord } from './types/event'
import type { EventExpenses } from './types/expense'
import type { EventIncomes } from './types/income'

const currentYear = new Date().getFullYear()

function App() {
  const [route, setRoute] = useState(window.location.hash || '#/events')
  const [events, setEvents] = useState<EventRecord[]>([])
  const [selected, setSelected] = useState<EventRecord | null>(null)
  const [expenses, setExpenses] = useState<Record<number, EventExpenses>>({})
  const [income, setIncome] = useState<EventIncomes>()
  const [loading, setLoading] = useState(true)
  const [detailsLoading, setDetailsLoading] = useState(/^#\/events\/\d+/.test(window.location.hash))
  const [error, setError] = useState('')
  const [financeError, setFinanceError] = useState('')
  const [notice, setNotice] = useState('')
  const [search, setSearch] = useState('')
  const [status, setStatus] = useState('')
  const [term, setTerm] = useState('')
  const [revision, setRevision] = useState(0)
  const segments = route.replace(/^#\//, '').split('/')
  const eventId = /^\d+$/.test(segments[1] || '') ? Number(segments[1]) : undefined
  const page = segments[1] === 'new' ? 'new' : eventId ? (segments[2] || 'event') : 'events'
  const displayTerm = term || `${currentYear} / ${currentYear + 1}`

  useEffect(() => {
    const navigate = () => {
      setRoute(window.location.hash || '#/events')
      setSelected(null)
      setIncome(undefined)
      setError('')
      setFinanceError('')
      setDetailsLoading(/^#\/events\/\d+/.test(window.location.hash))
    }
    window.addEventListener('hashchange', navigate)
    return () => window.removeEventListener('hashchange', navigate)
  }, [])

  useEffect(() => {
    let active = true
    listEvents()
      .then((result) => { if (active) setEvents(result) })
      .catch((failure: Error) => { if (active) setError(failure.message) })
      .finally(() => { if (active) setLoading(false) })
    return () => { active = false }
  }, [])

  useEffect(() => {
    let active = true
    Promise.all(events.map((event) => getExpenses(event.id)))
      .then((results) => {
        if (active) {
          setExpenses(Object.fromEntries(results.map((result) => [result.eventId, result])))
          setFinanceError('')
        }
      })
      .catch((failure: Error) => { if (active) setFinanceError(failure.message) })
    return () => { active = false }
  }, [events, revision])

  useEffect(() => {
    let active = true
    if (!eventId) return () => { active = false }
    getEvent(eventId)
      .then((result) => { if (active) setSelected(result) })
      .catch((failure: Error) => { if (active) setError(failure.message) })
      .finally(() => { if (active) setDetailsLoading(false) })
    return () => { active = false }
  }, [eventId])

  useEffect(() => {
    let active = true
    if (!eventId) return () => { active = false }
    getIncomes(eventId)
      .then((result) => { if (active) setIncome(result) })
      .catch((failure: Error) => { if (active) setFinanceError(failure.message) })
    return () => { active = false }
  }, [eventId, revision])

  function onCreated(event: EventRecord) {
    setEvents((previous) => [...previous, event].sort((a, b) => b.eventDate.localeCompare(a.eventDate) || b.id - a.id))
    window.location.hash = `/events/${event.id}`
    setNotice('Event created successfully.')
  }

  const filtered = events.filter((event) =>
    [event.name, event.venue, event.description].join(' ').toLowerCase().includes(search.toLowerCase())
    && (!status || (event.status || 'Planning') === status)
    && (!term || event.term === term),
  )
  const title = page === 'new' ? 'Create event' : page === 'events' ? 'Events'
    : page === 'expenses' ? 'Itemized expenses' : page === 'income' ? 'Fundraising' : 'Event overview'

  return <AppShell events={events} selected={selected} page={page} term={displayTerm}>
    <p className="breadcrumb">{selected ? `EVENTS / ${selected.name.toUpperCase()} / ${title.toUpperCase()}` : `CHAPTER / ${title.toUpperCase()}`}</p>
    <div className="page-heading">
      <div><h1>{title}</h1><p>{page === 'events' ? 'Plan and manage chapter events in one place.' : page === 'new' ? 'Save the event details and accountable team.' : 'A clear view of your event’s financial position.'}</p></div>
      {page === 'events' ? <a className="button" href="#/events/new">+ Create event</a>
        : page === 'new' ? <a className="button secondary" href="#/events">← Events</a>
        : <button className="secondary" disabled title="Excel reporting is pending">View report →</button>}
    </div>
    {error && <p role="alert" className="error">{error} Reload the page to try again.</p>}
    {notice && <p role="status" className="info-strip">{notice}</p>}
    {page === 'events' && <>
      <div className="info-strip">{term ? `ExCom term: ${term}` : 'All chapter events'} · All amounts in LKR</div>
      <div className="filters">
        <label>Search events<input type="search" value={search} onChange={(event) => setSearch(event.target.value)} placeholder="Search by event name or venue..." /></label>
        <label>Status<select aria-label="Status" value={status} onChange={(event) => setStatus(event.target.value)}><option value="">All statuses</option>{['Planning', 'In progress', 'Completed', 'Archived'].map((value) => <option key={value}>{value}</option>)}</select></label>
        <label>Term<select aria-label="Term" value={term} onChange={(event) => setTerm(event.target.value)}><option value="">All terms</option>{Array.from(new Set(events.map((event) => event.term).filter((value): value is string => !!value))).sort().map((value) => <option key={value}>{value}</option>)}</select></label>
      </div>
      {financeError && <p role="alert" className="error">Expense totals could not be loaded. {financeError}</p>}
      <section className="panel table-panel">
        <h2>All events</h2>
        <div className="table-scroll"><table><thead><tr><th>Event</th><th>Event date</th><th>Planned budget</th><th>Recorded actual</th><th>Status</th></tr></thead><tbody>
          {filtered.map((event) => <tr key={event.id}><td><a className="event-link" href={`#/events/${event.id}`}>{event.name}</a></td><td>{new Date(event.eventDate + 'T00:00:00').toLocaleDateString('en-GB', { day: '2-digit', month: 'short', year: 'numeric' })}</td><td>{event.plannedBudget.toLocaleString('en-LK', { minimumFractionDigits: 2 })}</td><td>{expenses[event.id] ? expenses[event.id].totalSpent.toLocaleString('en-LK', { minimumFractionDigits: 2 }) : '—'}</td><td><a className="status-link" href={`#/events/${event.id}`}>{event.status || 'Planning'}{event.status === 'In progress' ? ' →' : ''}</a></td></tr>)}
        </tbody></table></div>
        {loading && <p role="status" className="empty-state">Loading events...</p>}
        {!loading && !filtered.length && <p className="empty-state">{events.length ? 'No events match these filters.' : 'Create your first chapter event to get started.'}</p>}
        <div className="table-footer">{filtered.length} records · All amounts in LKR</div>
      </section>
    </>}
    {page === 'new' && <EventForm onCreated={onCreated} onCancel={() => { window.location.hash = '/events' }} />}
    {eventId && detailsLoading && <p role="status">Loading event details...</p>}
    {selected && <>
      {financeError && <p role="alert" className="error">Financial data could not be loaded. {financeError}</p>}
      {page === 'event' && <EventOverview event={selected} expenses={expenses[selected.id]} income={income} />}
      {page === 'expenses' && <section className="panel"><ExpensePanel key={selected.id} eventId={selected.id} onSaved={() => setRevision((value) => value + 1)} /></section>}
      {page === 'income' && <section className="panel"><IncomePanel key={selected.id} eventId={selected.id} revision={revision} /></section>}
    </>}
  </AppShell>
}

export default App
