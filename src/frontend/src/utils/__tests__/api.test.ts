import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest'
import {
  TOKEN_EXPIRED_EVENT,
  getCsrfToken,
  getAuthHeaders,
  getAuthHeadersWithCsrf,
  fetchWithAuth,
  fetchJsonWithAuth,
} from '../api'

// ─── module mocks ────────────────────────────────────────────────────────────

const loadAuthMock = vi.fn()
const isTokenExpiredMock = vi.fn()
const clearAuthMock = vi.fn()

vi.mock('../auth', () => ({
  loadAuth: () => loadAuthMock(),
  isTokenExpired: (token: string) => isTokenExpiredMock(token),
  clearAuth: () => clearAuthMock(),
}))

// js-cookie mock
const cookieGetMock = vi.fn()
vi.mock('js-cookie', () => ({
  default: {
    get: (key: string) => cookieGetMock(key),
  },
}))

// ─── setup helpers ───────────────────────────────────────────────────────────

function mockNoAuth() {
  loadAuthMock.mockReturnValue({ accessToken: null, user: null })
  isTokenExpiredMock.mockReturnValue(false)
}

function mockWithAuth(token = 'valid-token') {
  loadAuthMock.mockReturnValue({ accessToken: token, user: null })
  isTokenExpiredMock.mockReturnValue(false)
}

function mockExpiredToken(token = 'expired-token') {
  loadAuthMock.mockReturnValue({ accessToken: token, user: null })
  isTokenExpiredMock.mockReturnValue(true)
}

// ─── TOKEN_EXPIRED_EVENT ─────────────────────────────────────────────────────

describe('TOKEN_EXPIRED_EVENT', () => {
  it('is the expected event name string', () => {
    expect(TOKEN_EXPIRED_EVENT).toBe('auth:token-expired')
  })
})

// ─── getCsrfToken ────────────────────────────────────────────────────────────

describe('getCsrfToken', () => {
  afterEach(() => { cookieGetMock.mockReset() })

  it('returns the XSRF-TOKEN cookie value when present', () => {
    cookieGetMock.mockReturnValue('csrf-abc')
    expect(getCsrfToken()).toBe('csrf-abc')
    expect(cookieGetMock).toHaveBeenCalledWith('XSRF-TOKEN')
  })

  it('returns null when cookie is not set', () => {
    cookieGetMock.mockReturnValue(undefined)
    expect(getCsrfToken()).toBeNull()
  })
})

// ─── getAuthHeaders ──────────────────────────────────────────────────────────

describe('getAuthHeaders', () => {
  afterEach(() => { loadAuthMock.mockReset() })

  it('returns empty object when no token is stored', () => {
    mockNoAuth()
    const headers = getAuthHeaders() as Record<string, string>
    expect(headers['Authorization']).toBeUndefined()
  })

  it('returns Authorization header when token is stored', () => {
    mockWithAuth('my-token')
    const headers = getAuthHeaders() as Record<string, string>
    expect(headers['Authorization']).toBe('Bearer my-token')
  })
})

// ─── getAuthHeadersWithCsrf ──────────────────────────────────────────────────

describe('getAuthHeadersWithCsrf', () => {
  afterEach(() => {
    loadAuthMock.mockReset()
    cookieGetMock.mockReset()
  })

  it('includes both Authorization and X-XSRF-TOKEN headers', () => {
    mockWithAuth('my-token')
    cookieGetMock.mockReturnValue('csrf-xyz')

    const headers = getAuthHeadersWithCsrf() as Record<string, string>
    expect(headers['Authorization']).toBe('Bearer my-token')
    expect(headers['X-XSRF-TOKEN']).toBe('csrf-xyz')
  })

  it('skips X-XSRF-TOKEN when cookie is absent', () => {
    mockWithAuth('my-token')
    cookieGetMock.mockReturnValue(undefined)

    const headers = getAuthHeadersWithCsrf() as Record<string, string>
    expect(headers['Authorization']).toBe('Bearer my-token')
    expect(headers['X-XSRF-TOKEN']).toBeUndefined()
  })
})

// ─── fetchWithAuth ────────────────────────────────────────────────────────────

describe('fetchWithAuth', () => {
  beforeEach(() => {
    global.fetch = vi.fn()
    cookieGetMock.mockReturnValue(null)
  })

  afterEach(() => {
    loadAuthMock.mockReset()
    isTokenExpiredMock.mockReset()
    clearAuthMock.mockReset()
    cookieGetMock.mockReset()
    vi.restoreAllMocks()
  })

  it('makes a GET request without auth header when no token', async () => {
    mockNoAuth()
    ;(global.fetch as any).mockResolvedValueOnce({ status: 200, ok: true })

    await fetchWithAuth('/api/test', { method: 'GET' })

    const [, options] = (global.fetch as any).mock.calls[0]
    expect(options.headers['Authorization']).toBeUndefined()
  })

  it('adds Authorization header when token is present', async () => {
    mockWithAuth('tok-abc')
    ;(global.fetch as any).mockResolvedValueOnce({ status: 200, ok: true })

    await fetchWithAuth('/api/test', { method: 'GET' })

    const [, options] = (global.fetch as any).mock.calls[0]
    expect(options.headers['Authorization']).toBe('Bearer tok-abc')
  })

  it('throws immediately and dispatches TOKEN_EXPIRED_EVENT when token is expired', async () => {
    mockExpiredToken()

    const listener = vi.fn()
    window.addEventListener(TOKEN_EXPIRED_EVENT, listener)

    await expect(fetchWithAuth('/api/test')).rejects.toThrow('Token expired')

    expect(clearAuthMock).toHaveBeenCalled()
    expect(global.fetch).not.toHaveBeenCalled()
    expect(listener).toHaveBeenCalled()

    window.removeEventListener(TOKEN_EXPIRED_EVENT, listener)
  })

  it('adds X-XSRF-TOKEN header for POST requests when CSRF cookie is present', async () => {
    mockWithAuth('tok')
    cookieGetMock.mockReturnValue('csrf-token')
    ;(global.fetch as any).mockResolvedValueOnce({ status: 200, ok: true })

    await fetchWithAuth('/api/test', { method: 'POST' })

    const [, options] = (global.fetch as any).mock.calls[0]
    expect(options.headers['X-XSRF-TOKEN']).toBe('csrf-token')
  })

  it('adds X-XSRF-TOKEN for PUT requests', async () => {
    mockWithAuth('tok')
    cookieGetMock.mockReturnValue('csrf-put')
    ;(global.fetch as any).mockResolvedValueOnce({ status: 200, ok: true })

    await fetchWithAuth('/api/test', { method: 'PUT' })

    const [, options] = (global.fetch as any).mock.calls[0]
    expect(options.headers['X-XSRF-TOKEN']).toBe('csrf-put')
  })

  it('adds X-XSRF-TOKEN for DELETE requests', async () => {
    mockWithAuth('tok')
    cookieGetMock.mockReturnValue('csrf-del')
    ;(global.fetch as any).mockResolvedValueOnce({ status: 200, ok: true })

    await fetchWithAuth('/api/test', { method: 'DELETE' })

    const [, options] = (global.fetch as any).mock.calls[0]
    expect(options.headers['X-XSRF-TOKEN']).toBe('csrf-del')
  })

  it('does NOT add X-XSRF-TOKEN for GET requests', async () => {
    mockWithAuth('tok')
    cookieGetMock.mockReturnValue('csrf-token')
    ;(global.fetch as any).mockResolvedValueOnce({ status: 200, ok: true })

    await fetchWithAuth('/api/test', { method: 'GET' })

    const [, options] = (global.fetch as any).mock.calls[0]
    expect(options.headers['X-XSRF-TOKEN']).toBeUndefined()
  })

  it('always includes credentials: include', async () => {
    mockNoAuth()
    ;(global.fetch as any).mockResolvedValueOnce({ status: 200, ok: true })

    await fetchWithAuth('/api/test')

    const [, options] = (global.fetch as any).mock.calls[0]
    expect(options.credentials).toBe('include')
  })

  it('clears auth and dispatches TOKEN_EXPIRED_EVENT on 401 response', async () => {
    mockWithAuth('tok')
    ;(global.fetch as any).mockResolvedValueOnce({ status: 401, ok: false })

    const listener = vi.fn()
    window.addEventListener(TOKEN_EXPIRED_EVENT, listener)

    const response = await fetchWithAuth('/api/test')

    expect(clearAuthMock).toHaveBeenCalled()
    expect(listener).toHaveBeenCalled()
    expect(response.status).toBe(401)

    window.removeEventListener(TOKEN_EXPIRED_EVENT, listener)
  })

  it('does NOT dispatch event on non-401 error responses', async () => {
    mockWithAuth('tok')
    ;(global.fetch as any).mockResolvedValueOnce({ status: 403, ok: false })

    const listener = vi.fn()
    window.addEventListener(TOKEN_EXPIRED_EVENT, listener)

    await fetchWithAuth('/api/test')

    expect(listener).not.toHaveBeenCalled()
    expect(clearAuthMock).not.toHaveBeenCalled()

    window.removeEventListener(TOKEN_EXPIRED_EVENT, listener)
  })
})

// ─── fetchJsonWithAuth ───────────────────────────────────────────────────────

describe('fetchJsonWithAuth', () => {
  beforeEach(() => {
    global.fetch = vi.fn()
    cookieGetMock.mockReturnValue(null)
  })

  afterEach(() => {
    loadAuthMock.mockReset()
    isTokenExpiredMock.mockReset()
    clearAuthMock.mockReset()
    vi.restoreAllMocks()
  })

  it('returns parsed JSON on success', async () => {
    mockWithAuth('tok')
    const data = { hello: 'world' }
    ;(global.fetch as any).mockResolvedValueOnce({
      ok: true,
      status: 200,
      json: async () => data,
    })

    const result = await fetchJsonWithAuth('/api/data')
    expect(result).toEqual(data)
  })

  it('throws on non-ok response', async () => {
    mockWithAuth('tok')
    ;(global.fetch as any).mockResolvedValueOnce({
      ok: false,
      status: 404,
      statusText: 'Not Found',
    })

    await expect(fetchJsonWithAuth('/api/missing')).rejects.toThrow('HTTP 404: Not Found')
  })
})
