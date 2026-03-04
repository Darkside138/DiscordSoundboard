import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest'
import {
  isTokenExpired,
  getTokenExpirationTime,
  saveAuth,
  loadAuth,
  clearAuth,
  handleOAuthRedirect,
  validateToken,
  logout,
  refreshToken,
  fetchDefaultPermissions,
  type AuthState,
} from '../auth'

// ─── helpers ────────────────────────────────────────────────────────────────

/**
 * Build a minimal 3-part JWT whose payload is base64-encoded JSON.
 * atob/btoa are available in the jsdom environment.
 */
function makeToken(payload: Record<string, any>): string {
  const encoded = btoa(JSON.stringify(payload))
  return `eyJhbGciOiJIUzI1NiJ9.${encoded}.signature`
}

function nowSeconds(): number {
  return Math.floor(Date.now() / 1000)
}

// ─── mocks ──────────────────────────────────────────────────────────────────

vi.mock('../../config', () => ({
  API_ENDPOINTS: {
    AUTH_USER: '/api/auth/user',
    AUTH_LOGOUT: '/api/auth/logout',
    AUTH_REFRESH: '/api/auth/refresh',
    AUTH_DEFAULT_PERMISSIONS: '/api/auth/default-permissions',
    OAUTH_LOGIN: '/oauth2/authorization/discord',
  },
}))

vi.mock('../api', () => ({
  getCsrfToken: vi.fn(() => null),
}))

// ─── isTokenExpired ──────────────────────────────────────────────────────────

describe('isTokenExpired', () => {
  it('returns true for null token', () => {
    expect(isTokenExpired(null)).toBe(true)
  })

  it('returns true for non-JWT string (wrong number of parts)', () => {
    expect(isTokenExpired('notavalidtoken')).toBe(true)
  })

  it('returns true when payload has no exp claim', () => {
    const token = makeToken({ sub: 'user123' })
    expect(isTokenExpired(token)).toBe(true)
  })

  it('returns false for a token expiring far in the future', () => {
    const token = makeToken({ exp: nowSeconds() + 3600 }) // 1 hour from now
    expect(isTokenExpired(token)).toBe(false)
  })

  it('returns true for a token that already expired', () => {
    const token = makeToken({ exp: nowSeconds() - 60 }) // 60 seconds ago
    expect(isTokenExpired(token)).toBe(true)
  })

  it('returns true when token expires within the 30-second buffer', () => {
    const token = makeToken({ exp: nowSeconds() + 15 }) // 15 s from now < 30 s buffer
    expect(isTokenExpired(token)).toBe(true)
  })

  it('returns true for a token with invalid base64 payload', () => {
    expect(isTokenExpired('header.!!!invalid!!!.sig')).toBe(true)
  })
})

// ─── getTokenExpirationTime ──────────────────────────────────────────────────

describe('getTokenExpirationTime', () => {
  it('returns null for null token', () => {
    expect(getTokenExpirationTime(null)).toBeNull()
  })

  it('returns null for token without exp', () => {
    const token = makeToken({ sub: 'user123' })
    expect(getTokenExpirationTime(token)).toBeNull()
  })

  it('returns exp * 1000 for a valid token', () => {
    const exp = nowSeconds() + 3600
    const token = makeToken({ exp })
    expect(getTokenExpirationTime(token)).toBe(exp * 1000)
  })

  it('returns null for a malformed token', () => {
    expect(getTokenExpirationTime('not.valid')).toBeNull()
  })
})

// ─── saveAuth / loadAuth / clearAuth ────────────────────────────────────────

describe('saveAuth / loadAuth / clearAuth', () => {
  beforeEach(() => localStorage.clear())
  afterEach(() => localStorage.clear())

  const sampleAuth: AuthState = {
    accessToken: 'tok123',
    user: {
      id: 'user1',
      username: 'alice',
      discriminator: '0001',
      avatar: null,
      globalName: null,
    },
  }

  it('loadAuth returns empty state when nothing is stored', () => {
    const result = loadAuth()
    expect(result.accessToken).toBeNull()
    expect(result.user).toBeNull()
  })

  it('saveAuth persists auth and loadAuth retrieves it', () => {
    saveAuth(sampleAuth)
    const result = loadAuth()
    expect(result.accessToken).toBe('tok123')
    expect(result.user?.username).toBe('alice')
  })

  it('loadAuth returns empty state when stored JSON is invalid', () => {
    localStorage.setItem('discord_auth', 'not-valid-json')
    const result = loadAuth()
    expect(result.accessToken).toBeNull()
    expect(result.user).toBeNull()
  })

  it('clearAuth removes stored auth', () => {
    saveAuth(sampleAuth)
    clearAuth()
    const result = loadAuth()
    expect(result.accessToken).toBeNull()
    expect(result.user).toBeNull()
  })
})

// ─── handleOAuthRedirect ─────────────────────────────────────────────────────

describe('handleOAuthRedirect', () => {
  beforeEach(() => {
    localStorage.clear()
    global.fetch = vi.fn()
  })
  afterEach(() => {
    localStorage.clear()
    vi.restoreAllMocks()
  })

  it('saves auth state and returns it on success', async () => {
    const userData = {
      id: 'user1',
      username: 'alice',
      discriminator: '0001',
      avatar: null,
      globalName: null,
    }
    ;(global.fetch as any).mockResolvedValueOnce({
      ok: true,
      json: async () => userData,
    })

    const token = makeToken({ exp: nowSeconds() + 3600 })
    const authState = await handleOAuthRedirect(token)

    expect(authState.accessToken).toBe(token)
    expect(authState.user?.username).toBe('alice')

    // Verify saved to localStorage
    const stored = loadAuth()
    expect(stored.accessToken).toBe(token)
  })

  it('throws when user info fetch fails', async () => {
    ;(global.fetch as any).mockResolvedValueOnce({ ok: false })

    const token = makeToken({ exp: nowSeconds() + 3600 })
    await expect(handleOAuthRedirect(token)).rejects.toThrow('Failed to fetch user info')
  })

  it('prefers JWT permissions over backend response', async () => {
    const jwtPermissions = ['upload', 'play-sounds']
    const token = makeToken({
      exp: nowSeconds() + 3600,
      permissions: jwtPermissions,
      roles: ['admin'],
    })

    ;(global.fetch as any).mockResolvedValueOnce({
      ok: true,
      json: async () => ({
        id: 'u1',
        username: 'bob',
        discriminator: '0000',
        avatar: null,
        globalName: null,
        permissions: { playSounds: false }, // will be overwritten by JWT
      }),
    })

    const authState = await handleOAuthRedirect(token)
    // JWT permissions (array format) are transformed: 'upload' → upload:true, 'play-sounds' → playSounds:true
    expect(authState.user?.permissions?.upload).toBe(true)
    expect(authState.user?.permissions?.playSounds).toBe(true)
    expect(authState.user?.roles).toContain('admin')
  })
})

// ─── validateToken ───────────────────────────────────────────────────────────

describe('validateToken', () => {
  beforeEach(() => { global.fetch = vi.fn() })
  afterEach(() => { vi.restoreAllMocks() })

  it('returns user data on success', async () => {
    ;(global.fetch as any).mockResolvedValueOnce({
      ok: true,
      json: async () => ({
        id: 'u1',
        username: 'alice',
        discriminator: '0000',
        avatar: null,
        globalName: null,
      }),
    })
    const token = makeToken({ exp: nowSeconds() + 3600 })
    const user = await validateToken(token)
    expect(user?.username).toBe('alice')
  })

  it('returns null when fetch returns not-ok', async () => {
    ;(global.fetch as any).mockResolvedValueOnce({ ok: false })
    const token = makeToken({ exp: nowSeconds() + 3600 })
    const user = await validateToken(token)
    expect(user).toBeNull()
  })

  it('returns null on network error', async () => {
    ;(global.fetch as any).mockRejectedValueOnce(new Error('Network error'))
    const token = makeToken({ exp: nowSeconds() + 3600 })
    const user = await validateToken(token)
    expect(user).toBeNull()
  })
})

// ─── logout ──────────────────────────────────────────────────────────────────

describe('logout', () => {
  beforeEach(() => {
    localStorage.clear()
    global.fetch = vi.fn()
  })
  afterEach(() => {
    localStorage.clear()
    vi.restoreAllMocks()
  })

  it('calls logout endpoint and clears stored auth', async () => {
    const sampleAuth: AuthState = {
      accessToken: 'tok123',
      user: { id: 'u1', username: 'alice', discriminator: '0000', avatar: null, globalName: null },
    }
    saveAuth(sampleAuth)
    ;(global.fetch as any).mockResolvedValueOnce({ ok: true })

    await logout('tok123')

    expect(global.fetch).toHaveBeenCalledWith(
      '/api/auth/logout',
      expect.objectContaining({ method: 'POST' })
    )
    expect(loadAuth().accessToken).toBeNull()
  })

  it('still clears auth when logout endpoint throws', async () => {
    saveAuth({
      accessToken: 'tok123',
      user: { id: 'u1', username: 'alice', discriminator: '0000', avatar: null, globalName: null },
    })
    ;(global.fetch as any).mockRejectedValueOnce(new Error('Network error'))

    await logout('tok123')

    expect(loadAuth().accessToken).toBeNull()
  })
})

// ─── refreshToken ────────────────────────────────────────────────────────────

describe('refreshToken', () => {
  beforeEach(() => {
    localStorage.clear()
    global.fetch = vi.fn()
  })
  afterEach(() => {
    localStorage.clear()
    vi.restoreAllMocks()
  })

  it('returns new auth state on success', async () => {
    const newToken = makeToken({ exp: nowSeconds() + 7200 })
    // First call: refresh endpoint
    ;(global.fetch as any).mockResolvedValueOnce({
      ok: true,
      json: async () => ({ token: newToken }),
    })
    // Second call: fetchUserInfo with new token
    ;(global.fetch as any).mockResolvedValueOnce({
      ok: true,
      json: async () => ({
        id: 'u1',
        username: 'alice',
        discriminator: '0000',
        avatar: null,
        globalName: null,
      }),
    })

    const result = await refreshToken('old-token')
    expect(result).not.toBeNull()
    expect(result?.accessToken).toBe(newToken)
    expect(result?.user?.username).toBe('alice')
    // Should be saved to localStorage
    expect(loadAuth().accessToken).toBe(newToken)
  })

  it('returns null when refresh endpoint returns not-ok', async () => {
    ;(global.fetch as any).mockResolvedValueOnce({ ok: false })
    const result = await refreshToken('old-token')
    expect(result).toBeNull()
  })

  it('returns null when user info fetch fails after refresh', async () => {
    const newToken = makeToken({ exp: nowSeconds() + 7200 })
    ;(global.fetch as any).mockResolvedValueOnce({
      ok: true,
      json: async () => ({ token: newToken }),
    })
    ;(global.fetch as any).mockResolvedValueOnce({ ok: false })

    const result = await refreshToken('old-token')
    expect(result).toBeNull()
  })

  it('returns null on network error', async () => {
    ;(global.fetch as any).mockRejectedValueOnce(new Error('timeout'))
    const result = await refreshToken('old-token')
    expect(result).toBeNull()
  })
})

// ─── fetchDefaultPermissions ─────────────────────────────────────────────────

describe('fetchDefaultPermissions', () => {
  beforeEach(() => { global.fetch = vi.fn() })
  afterEach(() => { vi.restoreAllMocks() })

  it('returns guest user with transformed array permissions', async () => {
    ;(global.fetch as any).mockResolvedValueOnce({
      ok: true,
      json: async () => ({ permissions: ['play-sounds', 'download-sounds'] }),
    })

    const user = await fetchDefaultPermissions()
    expect(user?.id).toBe('guest')
    expect(user?.username).toBe('Guest')
    expect(user?.roles).toContain('default')
    expect(user?.permissions?.playSounds).toBe(true)
    expect(user?.permissions?.downloadSounds).toBe(true)
    expect(user?.permissions?.upload).toBeUndefined()
  })

  it('returns guest user with transformed object permissions', async () => {
    ;(global.fetch as any).mockResolvedValueOnce({
      ok: true,
      json: async () => ({
        permissions: { 'play-sounds': true, 'upload': false },
      }),
    })

    const user = await fetchDefaultPermissions()
    expect(user?.permissions?.playSounds).toBe(true)
    expect(user?.permissions?.upload).toBe(false)
  })

  it('returns null when response is not ok', async () => {
    ;(global.fetch as any).mockResolvedValueOnce({ ok: false })
    const user = await fetchDefaultPermissions()
    expect(user).toBeNull()
  })

  it('returns null when response has no permissions field', async () => {
    ;(global.fetch as any).mockResolvedValueOnce({
      ok: true,
      json: async () => ({}),
    })
    const user = await fetchDefaultPermissions()
    expect(user).toBeNull()
  })

  it('returns null on network error', async () => {
    ;(global.fetch as any).mockRejectedValueOnce(new Error('Network error'))
    const user = await fetchDefaultPermissions()
    expect(user).toBeNull()
  })
})
