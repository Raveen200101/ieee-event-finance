import test from 'node:test'
import assert from 'node:assert/strict'

const user = { id: 1, email: 'test@example.invalid', displayName: 'Test user', role: 'COMMITTEE_MEMBER', enabled: true, accountAdmin: true }
const json = (body, status = 200) => new Response(JSON.stringify(body), { status, headers: { 'Content-Type': 'application/json' } })
let instance = 0
async function client(handler) {
  globalThis.window = new EventTarget()
  Object.defineProperty(globalThis, 'navigator', { configurable: true, value: { locks: { request: (_name, callback) => callback() } } })
  globalThis.fetch = handler
  return import(`../src/api/client.ts?test=${++instance}`)
}

test('login uses JSON and CSRF; protected writes send a bearer without cookie-auth CSRF requests', async () => {
  const calls = []
  const api = await client(async (path, options) => {
    calls.push(path)
    if (path === '/api/auth/csrf') return json({ headerName: 'X-CSRF-TOKEN', token: 'csrf-test' })
    if (path === '/api/auth/login') {
      assert.equal(new Headers(options.headers).get('Authorization'), null)
      assert.equal(new Headers(options.headers).get('X-CSRF-TOKEN'), 'csrf-test')
      assert.deepEqual(JSON.parse(options.body), { email: user.email, password: 'test-password' })
      return json({ user, accessToken: 'access-one' })
    }
    assert.equal(new Headers(options.headers).get('Authorization'), 'Bearer access-one')
    assert.equal(options.credentials, 'same-origin')
    return json({ saved: true })
  })
  assert.deepEqual(await api.signIn(user.email, 'test-password'), user)
  calls.length = 0
  assert.deepEqual(await api.request('/api/events', { method: 'POST', body: '{}' }), { saved: true })
  assert.deepEqual(calls, ['/api/events'])
})

test('parallel restoration shares one refresh and never attaches a bearer to it', async () => {
  let refreshes = 0
  const api = await client(async (path, options) => {
    if (path === '/api/auth/csrf') return json({ headerName: 'X-CSRF-TOKEN', token: 'csrf-test' })
    assert.equal(path, '/api/auth/refresh')
    assert.equal(new Headers(options.headers).get('Authorization'), null)
    refreshes++
    await new Promise(resolve => setTimeout(resolve, 5))
    return json({ user, accessToken: 'restored-access' })
  })
  assert.deepEqual(await Promise.all([api.restoreUser(), api.restoreUser()]), [user, user])
  assert.equal(refreshes, 1)
})

test('a 401 refreshes once and retries the protected request with the replacement token', async () => {
  let protectedCalls = 0
  let refreshes = 0
  const api = await client(async (path, options) => {
    if (path === '/api/auth/csrf') return json({ headerName: 'X-CSRF-TOKEN', token: 'csrf-test' })
    if (path === '/api/auth/login') return json({ user, accessToken: 'old-access' })
    if (path === '/api/auth/refresh') { refreshes++; return json({ user, accessToken: 'new-access' }) }
    protectedCalls++
    if (protectedCalls === 1) return json({ detail: 'Expired token' }, 401)
    assert.equal(new Headers(options.headers).get('Authorization'), 'Bearer new-access')
    return json([1])
  })
  await api.signIn(user.email, 'test-password')
  assert.deepEqual(await api.request('/api/events'), [1])
  assert.equal(protectedCalls, 2)
  assert.equal(refreshes, 1)
})

test('403 authorization errors do not refresh or retry', async () => {
  const calls = []
  const api = await client(async path => { calls.push(path); return json({ detail: 'Access denied' }, 403) })
  await assert.rejects(api.request('/api/users'), error => error.status === 403)
  assert.deepEqual(calls, ['/api/users'])
})

test('logout uses cookie CSRF, clears memory, and a failed refresh cannot loop', async () => {
  let protectedCalls = 0
  let refreshes = 0
  const api = await client(async (path, options) => {
    if (path === '/api/auth/csrf') return json({ headerName: 'X-CSRF-TOKEN', token: 'csrf-test' })
    if (path === '/api/auth/login') return json({ user, accessToken: 'old-access' })
    if (path === '/api/auth/logout') {
      assert.equal(new Headers(options.headers).get('Authorization'), null)
      assert.equal(new Headers(options.headers).get('X-CSRF-TOKEN'), 'csrf-test')
      return new Response(null, { status: 204 })
    }
    if (path === '/api/auth/refresh') { refreshes++; return json({ detail: 'Sign in again' }, 401) }
    protectedCalls++
    assert.equal(new Headers(options.headers).get('Authorization'), null)
    return json({ detail: 'Sign in again' }, 401)
  })
  await api.signIn(user.email, 'test-password')
  await api.signOut()
  await assert.rejects(api.request('/api/events'), error => error.status === 401)
  assert.equal(protectedCalls, 1)
  assert.equal(refreshes, 1)
})
