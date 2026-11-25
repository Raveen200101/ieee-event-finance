import { useEffect, useState } from 'react'
import type { FormEvent } from 'react'
import { listEvents, request } from '../api/events'
import type { EventRecord } from '../types/event'
import { useAuth } from './AuthContext'
import type { ChapterAccount, ChapterRole, ChapterUser } from '../types/auth'
import { roleLabels } from '../types/auth'

export function TeamPanel() {
  const { isAdmin, user } = useAuth()
  const [users, setUsers] = useState<ChapterAccount[]>()
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')
  const [busy, setBusy] = useState(false)
  const [revision, setRevision] = useState(0)
  const [events, setEvents] = useState<EventRecord[]>()
  const [eventsError, setEventsError] = useState('')
  const [createRole, setCreateRole] = useState<ChapterRole>('VOLUNTEER')
  const [eventIds, setEventIds] = useState<number[]>([])
  useEffect(() => {
    let active = true
    if (isAdmin) listEvents().then((result) => { if (active) { setEvents(result); setEventsError('') } })
      .catch((failure: Error) => { if (active) setEventsError(failure.message) })
    return () => { active = false }
  }, [isAdmin, revision])
  useEffect(() => {
    let active = true
    if (isAdmin) request<ChapterAccount[]>('/api/users').then((result) => { if (active) { setUsers(result); setError('') } })
      .catch((failure: Error) => { if (active) setError(failure.message) })
    return () => { active = false }
  }, [isAdmin, revision])
  async function create(event: FormEvent<HTMLFormElement>) {
    event.preventDefault(); const form = event.currentTarget; const fields = new FormData(form)
    setBusy(true); setError(''); setNotice('')
    try {
      await request('/api/users', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({
        email: String(fields.get('email')).trim(), displayName: String(fields.get('displayName')).trim(),
        password: String(fields.get('password')), role: String(fields.get('role')),
        eventIds: createRole === 'COMMITTEE_MEMBER' ? [] : eventIds,
      }) })
      form.reset(); setCreateRole('VOLUNTEER'); setEventIds([]); setRevision((value) => value + 1); setNotice('Account created and selected event assignments saved. Share its credentials privately with the member.')
    } catch (failure) { setError(failure instanceof Error ? failure.message : 'Unable to create account.') }
    finally { setBusy(false) }
  }
  async function update(account: ChapterUser, role: ChapterRole, enabled: boolean) {
    setBusy(true); setError(''); setNotice('')
    try {
      const result = await request<ChapterUser>(`/api/users/${account.id}`, { method: 'PUT', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ role, enabled, accountAdmin: role === 'COMMITTEE_MEMBER' && account.accountAdmin }) })
      setUsers((previous) => previous?.map((item) => item.id === result.id ? { ...item, ...result } : item)); setNotice('Account access updated.')
      if (account.id === user.id) window.dispatchEvent(new Event('auth-refresh'))
    } catch (failure) { setError(failure instanceof Error ? failure.message : 'Unable to update access.') }
    finally { setBusy(false) }
  }
  if (!isAdmin) return <p role="alert">Only an account administrator can manage chapter accounts.</p>
  return <>
    <div className="info-strip">Committee members manage events and budgets. Finance and Logistics Team Leads are limited to their assigned events and permitted records.</div>
    {error && <p role="alert" className="error">{error}</p>}{notice && <p role="status" className="info-strip">{notice}</p>}
    <section className="panel table-panel"><div className="page-heading"><h2>Chapter accounts</h2><button className="secondary" disabled={busy} onClick={() => setRevision((value) => value + 1)}>Reload accounts</button></div><div className="table-scroll"><table><thead><tr><th>Person</th><th>Email</th><th>Role</th><th>Current events</th><th>Status</th><th>Access</th></tr></thead><tbody>
      {users?.map((account) => <tr key={account.id}><td>{account.displayName}{account.id === user.id ? ' (you)' : ''}</td><td>{account.email}</td>
        <td><select aria-label={`Role for ${account.email}`} disabled={busy} value={account.role} onChange={(event) => void update(account, event.target.value as ChapterRole, account.enabled)}>{Object.entries(roleLabels).map(([value, label]) => <option key={value} value={value}>{label}</option>)}</select></td>
        <td>{account.role === 'COMMITTEE_MEMBER' ? <span className="field-help">All events (committee access)</span> : account.currentEvents?.length ? <ul className="account-events">{account.currentEvents.map((event) => <li key={event.id}><a href={`#/events/${event.id}`}>{event.name}</a><small>{event.status}</small></li>)}</ul> : <span className="field-help">No current events</span>}</td>
        <td>{account.enabled ? 'Active' : 'Disabled'}</td><td><button className="secondary" disabled={busy} onClick={() => void update(account, account.role, !account.enabled)}>{account.enabled ? 'Disable' : 'Enable'}</button></td></tr>)}
    </tbody></table></div>{!users && <p className="empty-state">Loading accounts...</p>}</section>
    <section className="panel budget-form"><h2>Create chapter account</h2><form onSubmit={create}><fieldset disabled={busy}><div className="form-grid">
      <label>Full name<input name="displayName" required maxLength={150} autoComplete="off" /></label>
      <label>Email address<input name="email" type="email" required maxLength={254} autoComplete="off" /></label>
      <label>Initial password<input name="password" type="password" required minLength={12} maxLength={72} autoComplete="new-password" /></label>
      <label>Role<select name="role" value={createRole} onChange={(event) => { setCreateRole(event.target.value as ChapterRole); setEventIds([]) }}>{Object.entries(roleLabels).map(([value, label]) => <option key={value} value={value}>{label}</option>)}</select></label>
    </div>
    <fieldset className="event-choices"><legend>Assigned events (optional)</legend>
      {createRole === 'COMMITTEE_MEMBER' ? <p>Committee members automatically have access to all events.</p> : <>
        {eventsError && <p role="alert" className="error">Events could not be loaded: {eventsError} <button type="button" className="secondary" onClick={() => setRevision((value) => value + 1)}>Reload events</button></p>}
        {!events && !eventsError && <p role="status">Loading events...</p>}
        {events?.map((event) => <label className="event-choice" key={event.id}><input type="checkbox" checked={eventIds.includes(event.id)} onChange={(change) => setEventIds((previous) => change.target.checked ? [...previous, event.id] : previous.filter((id) => id !== event.id))} /><span>{event.name} · {event.eventDate}</span></label>)}
        {events?.length === 0 && <p>No events yet. You can assign this account after an event is created.</p>}
        <p className="field-help">Select any events this person should access immediately. Leave empty to create an account without event assignments. Assigned members appear in each selected event’s Event team page. Logistics purchase groups are assigned separately under Purchase groups.</p>
      </>}
    </fieldset>
    <p className="field-help">Use at least 12 characters (up to 72 UTF-8 bytes). Email invitations and password recovery are not available yet.</p><div className="actions form-actions"><button type="submit">{busy ? 'Saving...' : 'Create account'}</button></div></fieldset></form></section>
  </>
}
