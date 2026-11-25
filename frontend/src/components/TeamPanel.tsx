import { useEffect, useState } from 'react'
import type { FormEvent } from 'react'
import { request } from '../api/events'
import { useAuth } from './AuthContext'
import type { ChapterRole, ChapterUser } from '../types/auth'
import { roleLabels } from '../types/auth'

export function TeamPanel() {
  const { isAdmin, user } = useAuth()
  const [users, setUsers] = useState<ChapterUser[]>()
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')
  const [busy, setBusy] = useState(false)
  const [revision, setRevision] = useState(0)
  useEffect(() => {
    let active = true
    if (isAdmin) request<ChapterUser[]>('/api/users').then((result) => { if (active) { setUsers(result); setError('') } })
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
      }) })
      form.reset(); setRevision((value) => value + 1); setNotice('Account created. Share its credentials privately with the member.')
    } catch (failure) { setError(failure instanceof Error ? failure.message : 'Unable to create account.') }
    finally { setBusy(false) }
  }
  async function update(account: ChapterUser, role: ChapterRole, enabled: boolean) {
    setBusy(true); setError(''); setNotice('')
    try {
      const result = await request<ChapterUser>(`/api/users/${account.id}`, { method: 'PUT', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ role, enabled, accountAdmin: role === 'COMMITTEE_MEMBER' && account.accountAdmin }) })
      setUsers((previous) => previous?.map((item) => item.id === result.id ? result : item)); setNotice('Account access updated.')
      if (account.id === user.id) window.dispatchEvent(new Event('auth-refresh'))
    } catch (failure) { setError(failure instanceof Error ? failure.message : 'Unable to update access.') }
    finally { setBusy(false) }
  }
  if (!isAdmin) return <p role="alert">Only an account administrator can manage chapter accounts.</p>
  return <>
    <div className="info-strip">Committee members manage events and budgets. Finance and Logistics Team Leads are limited to their assigned events and permitted records.</div>
    {error && <p role="alert" className="error">{error}</p>}{notice && <p role="status" className="info-strip">{notice}</p>}
    <section className="panel table-panel"><h2>Chapter accounts</h2><div className="table-scroll"><table><thead><tr><th>Person</th><th>Email</th><th>Role</th><th>Status</th><th>Access</th></tr></thead><tbody>
      {users?.map((account) => <tr key={account.id}><td>{account.displayName}{account.id === user.id ? ' (you)' : ''}</td><td>{account.email}</td>
        <td><select aria-label={`Role for ${account.email}`} disabled={busy} value={account.role} onChange={(event) => void update(account, event.target.value as ChapterRole, account.enabled)}>{Object.entries(roleLabels).map(([value, label]) => <option key={value} value={value}>{label}</option>)}</select></td>
        <td>{account.enabled ? 'Active' : 'Disabled'}</td><td><button className="secondary" disabled={busy} onClick={() => void update(account, account.role, !account.enabled)}>{account.enabled ? 'Disable' : 'Enable'}</button></td></tr>)}
    </tbody></table></div>{!users && <p className="empty-state">Loading accounts...</p>}</section>
    <section className="panel budget-form"><h2>Create chapter account</h2><form onSubmit={create}><fieldset disabled={busy}><div className="form-grid">
      <label>Full name<input name="displayName" required maxLength={150} autoComplete="off" /></label>
      <label>Email address<input name="email" type="email" required maxLength={254} autoComplete="off" /></label>
      <label>Initial password<input name="password" type="password" required minLength={12} maxLength={72} autoComplete="new-password" /></label>
      <label>Role<select name="role" defaultValue="VOLUNTEER">{Object.entries(roleLabels).map(([value, label]) => <option key={value} value={value}>{label}</option>)}</select></label>
    </div><p className="field-help">Use at least 12 characters (up to 72 UTF-8 bytes). Email invitations and password recovery are not available yet.</p><div className="actions form-actions"><button type="submit">{busy ? 'Saving...' : 'Create account'}</button></div></fieldset></form></section>
  </>
}
