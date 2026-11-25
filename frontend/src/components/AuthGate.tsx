import { useEffect, useState } from 'react'
import type { FormEvent } from 'react'
import App from '../App'
import { ApiError, clearAuthentication, restoreUser, signIn, signOut } from '../api/client'
import type { ChapterUser } from '../types/auth'
import { AuthContext } from './AuthContext'

export function AuthGate() {
  const [user, setUser] = useState<ChapterUser | null>(null)
  const [loading, setLoading] = useState(true)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  const [revision, setRevision] = useState(0)
  useEffect(() => {
    let active = true
    restoreUser().then((account) => { if (active) { setUser(account); setError('') } })
      .catch((failure: Error) => { if (active) { setUser(null); if (!(failure instanceof ApiError && failure.status === 401)) setError(failure.message) } })
      .finally(() => { if (active) setLoading(false) })
    const expired = () => { clearAuthentication(); setUser(null); setError('Your session ended. Please sign in again.') }
    const refresh = () => setRevision((value) => value + 1)
    window.addEventListener('auth-expired', expired)
    window.addEventListener('auth-refresh', refresh)
    window.addEventListener('focus', refresh)
    return () => { active = false; window.removeEventListener('auth-expired', expired); window.removeEventListener('auth-refresh', refresh); window.removeEventListener('focus', refresh) }
  }, [revision])

  async function login(event: FormEvent<HTMLFormElement>) {
    event.preventDefault(); setBusy(true); setError('')
    const form = new FormData(event.currentTarget)
    try {
      setUser(await signIn(String(form.get('email')).trim(), String(form.get('password'))))
    } catch (failure) { setError(failure instanceof Error ? failure.message : 'Unable to sign in.') }
    finally { setBusy(false) }
  }
  async function logout() {
    await signOut()
    setUser(null); setError(''); window.location.hash = '/events'
  }
  if (loading) return <main className="login-page"><p role="status">Checking your session...</p></main>
  if (user) return <AuthContext.Provider value={{ user, logout }}><App key={`${user.id}:${user.role}:${user.accountAdmin}`} /></AuthContext.Provider>
  return <main className="login-page"><section className="panel login-card">
    <p className="brand-eyebrow">IEEE / COMPUTER SOCIETY</p><h1>Chapter Finance</h1><p>Sign in to your chapter workspace.</p>
    {error && <p role="alert" className="error">{error}</p>}
    <form onSubmit={login}><fieldset disabled={busy}>
      <label>Email address<input name="email" type="email" autoComplete="username" required maxLength={254} /></label>
      <label>Password<input name="password" type="password" autoComplete="current-password" required maxLength={72} /></label>
      <button type="submit">{busy ? 'Signing in...' : 'Sign in'}</button>
    </fieldset></form><p className="field-help">Accounts are created by your chapter administrator. Contact the Chair if you need access.</p>
  </section></main>
}
