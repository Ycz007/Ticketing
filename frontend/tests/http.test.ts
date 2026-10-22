import test from 'node:test'
import assert from 'node:assert/strict'
import { api, ApiError } from '../src/api/http.ts'

test('JSON写请求先获取CSRF，并携带令牌与同源凭据', async () => {
  const original = globalThis.fetch
  const calls: { url: string; init?: RequestInit }[] = []
  globalThis.fetch = async (input, init) => {
    calls.push({ url: String(input), init })
    if (String(input).endsWith('/auth/csrf')) return Response.json({ headerName: 'X-XSRF-TOKEN', token: 'token-123' })
    return Response.json({ id: '10' })
  }
  try {
    assert.deepEqual(await api.json('/tickets', { category: 'USAGE_QUESTION' }), { id: '10' })
    assert.equal(calls[0]?.url, '/api/auth/csrf')
    assert.equal(calls[1]?.init?.credentials, 'same-origin')
    assert.equal(new Headers(calls[1]?.init?.headers).get('X-XSRF-TOKEN'), 'token-123')
    assert.equal(calls[1]?.init?.body, '{"category":"USAGE_QUESTION"}')
  } finally { globalThis.fetch = original }
})
test('保留后端409冲突供页面提示刷新', async () => {
  const original = globalThis.fetch
  globalThis.fetch = async () => Response.json({ message: '工单已更新' }, { status: 409 })
  try { await assert.rejects(api.get('/tickets/10'), (error: unknown) => error instanceof ApiError && error.status === 409) }
  finally { globalThis.fetch = original }
})
test('204已读响应不进行JSON解析', async () => {
  const original = globalThis.fetch
  globalThis.fetch = async () => new Response(null, { status: 204 })
  try { assert.equal(await api.get('/empty'), undefined) }
  finally { globalThis.fetch = original }
})
