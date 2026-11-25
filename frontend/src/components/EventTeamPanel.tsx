import { useEffect, useState } from 'react'
import { getEventMembers, setEventMember } from '../api/assignments'
import type { EventMember } from '../api/assignments'
import { roleLabels } from '../types/auth'
import { useAuth } from './AuthContext'

export function EventTeamPanel({ eventId }: { eventId: number }) {
  const { isChair } = useAuth()
  const [members, setMembers] = useState<EventMember[]>()
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')
  const [revision, setRevision] = useState(0)
  useEffect(() => {
    let active = true
    if (isChair) getEventMembers(eventId).then((result) => { if (active) { setMembers(result); setError('') } })
      .catch((failure: Error) => { if (active) setError(failure.message) })
    return () => { active = false }
  }, [eventId, isChair, revision])

  async function remove(member: EventMember) {
    if (!window.confirm(`Remove ${member.displayName} from this event? Their purchase groups will become unassigned. Existing records will be preserved.`)) return
    setBusy(true); setError(''); setNotice('')
    try {
      await setEventMember(eventId, member.id, false)
      setMembers(await getEventMembers(eventId))
      setNotice('Event access removed and purchase-group assignments cleared.')
    } catch (failure) { setError(failure instanceof Error ? failure.message : 'Unable to update assignment. Reload to check the current assignments.') }
    finally { setBusy(false) }
  }
  if (!isChair) return <p role="alert">Only committee members can manage event assignments.</p>
  const assignedMembers = members?.filter((member) => member.assigned)
  return <>
    <p className="info-strip">This team lists only members assigned to this event. Choose assigned events when creating an account in Chapter accounts. Committee members already have access to every event. Assign Logistics Team Leads to individual purchase groups under <a href={`#/events/${eventId}/purchases`}>Purchase groups</a>.</p>
    <p>Finance Team Leads can manage their own expenses and income in this event. Logistics Team Leads can work on their own quotations in assigned purchase groups. Volunteers can view event details.</p>
    {error && <p role="alert" className="error">{error}</p>}
    {notice && <p role="status" className="info-strip">{notice}</p>}
    <section className="panel table-panel"><div className="page-heading"><h2>Event team</h2><button className="secondary" disabled={busy} onClick={() => setRevision((value) => value + 1)}>Reload team</button></div>
      {!members && !error && <p role="status">Loading event team...</p>}
      {assignedMembers && <><div className="table-scroll"><table><thead><tr><th>Person</th><th>Role</th><th>Account status</th><th>Action</th></tr></thead><tbody>
        {assignedMembers.map((member) => <tr key={member.id}><td>{member.displayName}</td><td>{roleLabels[member.role]}</td><td>{member.enabled ? 'Active' : 'Disabled'}</td>
          <td><button className="secondary" disabled={busy} aria-label={`Remove ${member.displayName}`} onClick={() => void remove(member)}>Remove from event</button></td></tr>)}
      </tbody></table></div>{!assignedMembers.length && <p className="empty-state">No members are assigned to this event yet. An account administrator can select this event when creating an account in Chapter accounts.</p>}</>}
    </section>
  </>
}
