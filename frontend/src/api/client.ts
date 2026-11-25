import type { ChapterUser } from '../types/auth'
interface LoginResponse { accessToken: string; user: ChapterUser }
let accessToken: string | null = null
let refreshing: Promise<ChapterUser> | null = null
let generation = 0
export class ApiError extends Error {
  status: number
  constructor(message: string, status: number) { super(message); this.status = status }
}
async function fetchApi(path: string, options?: RequestInit) {
  try { return await fetch(path, { ...options, credentials: 'same-origin', cache: 'no-store' }) }
  catch { throw new ApiError('Cannot reach the server. Check that Spring Boot and Vite are running.', 0) }
}
async function parse<T>(response: Response): Promise<T> {
  if (!response.ok) {
    const problem = await response.json().catch(() => null)
    const message = problem?.errors ? Object.values(problem.errors).join('. ') : problem?.detail
    throw new ApiError(message || (response.status >= 500 ? 'Backend unavailable. Start Spring Boot on port 8080.' : 'Unable to complete the request.'), response.status)
  }
  return response.status === 204 ? undefined as T : response.json() as Promise<T>
}
async function cookieRequest<T>(path: string, options: RequestInit): Promise<T> {
  const csrf = await parse<{ headerName: string; token: string }>(await fetchApi('/api/auth/csrf'))
  const headers = new Headers(options.headers); headers.set(csrf.headerName, csrf.token)
  return parse<T>(await fetchApi(path, { ...options, headers }))
}
export function refreshUser(): Promise<ChapterUser> {
  if (!refreshing) {
    const started = generation
    const rotate = async () => {
      const result = await cookieRequest<LoginResponse>('/api/auth/refresh', { method: 'POST' })
      if (generation !== started) throw new ApiError('Sign-in changed. Please try again.', 401)
      accessToken = result.accessToken
      return result.user
    }
    // The lock also serializes cookie rotation across tabs on this origin.
    refreshing = (navigator.locks ? navigator.locks.request('chapter-jwt-refresh', rotate) : rotate())
      .catch((failure: unknown) => { if (generation === started) accessToken = null; throw failure })
      .finally(() => { refreshing = null })
  }
  return refreshing
}
export async function signIn(email: string, password: string) {
  if (refreshing) await refreshing.catch(() => undefined)
  generation++
  const result = await cookieRequest<LoginResponse>('/api/auth/login', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ email, password }) })
  accessToken = result.accessToken
  return result.user
}
export async function signOut() {
  generation++
  if (refreshing) await refreshing.catch(() => undefined)
  try { await cookieRequest<void>('/api/auth/logout', { method: 'POST' }) }
  finally { accessToken = null; window.dispatchEvent(new Event('auth-expired')) }
}
export function clearAuthentication() { generation++; accessToken = null }
export async function request<T>(path: string, options?: RequestInit): Promise<T> {
  const send = () => {
    const headers = new Headers(options?.headers)
    if (accessToken) headers.set('Authorization', `Bearer ${accessToken}`)
    return fetchApi(path, { ...options, headers })
  }
  let response = await send()
  if (response.status === 401) {
    try { await refreshUser(); response = await send() }
    catch (failure) { window.dispatchEvent(new Event('auth-expired')); throw failure }
  }
  if (response.status === 401) { clearAuthentication(); window.dispatchEvent(new Event('auth-expired')) }
  return parse<T>(response)
}
export async function restoreUser() {
  const started = generation
  const user = await (accessToken ? request<ChapterUser>('/api/auth/me') : refreshUser())
  if (generation !== started) throw new ApiError('Sign-in changed. Please sign in again.', 401)
  return user
}
