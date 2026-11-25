import { useState } from 'react'
import type { FormEvent } from 'react'
import { useAuth } from './AuthContext'
import { request } from '../api/events'
import { roleLabels } from '../types/auth'
export function AccountPanel() {
  const { user } = useAuth()
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  async function change(event: FormEvent<HTMLFormElement>) {
    event.preventDefault(); const fields = new FormData(event.currentTarget); setError('')
    if (fields.get('newPassword') !== fields.get('confirmPassword')) { setError('New passwords do not match.'); return }
    setBusy(true)
    try {
      await request<void>('/api/auth/password', { method: 'PUT', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ currentPassword: fields.get('currentPassword'), newPassword: fields.get('newPassword') }) })
      window.dispatchEvent(new Event('auth-expired'))
    } catch (failure) { setError(failure instanceof Error ? failure.message : 'Unable to change password.') }
    finally { setBusy(false) }
  }
  return <section className="panel"><h2>{user.displayName}</h2><p>{user.email} · {roleLabels[user.role]}</p><h3>Change password</h3>
    {error && <p role="alert" className="error">{error}</p>}<form onSubmit={change}><fieldset disabled={busy}><div className="form-grid">
      <label>Current password<input name="currentPassword" type="password" required maxLength={72} autoComplete="current-password" /></label>
      <label>New password<input name="newPassword" type="password" required minLength={12} maxLength={72} autoComplete="new-password" /></label>
      <label>Confirm new password<input name="confirmPassword" type="password" required minLength={12} maxLength={72} autoComplete="new-password" /></label>
    </div><p className="field-help">Use at least 12 characters, up to 72 UTF-8 bytes. Changing your password ends all existing sessions. Sign in again afterward.</p><div className="actions form-actions"><button type="submit">{busy ? 'Saving...' : 'Change password'}</button></div></fieldset></form>
  </section>
}
