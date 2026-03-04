import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest'
import { renderHook, act, waitFor } from '@testing-library/react'
import { TOKEN_EXPIRED_EVENT } from '../../utils/api'

// ─── module mocks ────────────────────────────────────────────────────────────

const loadAuthMock = vi.fn()
const initiateDiscordLoginMock = vi.fn()
const logoutMock = vi.fn()
const validateTokenMock = vi.fn()
const clearAuthMock = vi.fn()
const handleOAuthRedirectMock = vi.fn()
const refreshTokenMock = vi.fn()
const fetchDefaultPermissionsMock = vi.fn()
const isTokenExpiredMock = vi.fn()
const getTokenExpirationTimeMock = vi.fn()

vi.mock('../../utils/auth', () => ({
  loadAuth: () => loadAuthMock(),
  initiateDiscordLogin: () => initiateDiscordLoginMock(),
  logout: (token: string) => logoutMock(token),
  validateToken: (token: string) => validateTokenMock(token),
  clearAuth: () => clearAuthMock(),
  handleOAuthRedirect: (token: string) => handleOAuthRedirectMock(token),
  refreshToken: (token: string) => refreshTokenMock(token),
  fetchDefaultPermissions: () => fetchDefaultPermissionsMock(),
  isTokenExpired: (token: string) => isTokenExpiredMock(token),
  getTokenExpirationTime: (token: string) => getTokenExpirationTimeMock(token),
}))

vi.mock('../../utils/api', () => ({
  TOKEN_EXPIRED_EVENT: 'auth:token-expired',
}))

// ─── helpers ─────────────────────────────────────────────────────────────────

const guestUser = {
  id: 'guest',
  username: 'Guest',
  discriminator: '0000',
  avatar: null,
  globalName: null,
  roles: ['default'],
  permissions: { playSounds: true },
}

const realUser = {
  id: 'user123',
  username: 'alice',
  discriminator: '0001',
  avatar: null,
  globalName: null,
  roles: ['user'],
}

function resetMocks() {
  loadAuthMock.mockReset()
  initiateDiscordLoginMock.mockReset()
  logoutMock.mockReset()
  validateTokenMock.mockReset()
  clearAuthMock.mockReset()
  handleOAuthRedirectMock.mockReset()
  refreshTokenMock.mockReset()
  fetchDefaultPermissionsMock.mockReset()
  isTokenExpiredMock.mockReset()
  getTokenExpirationTimeMock.mockReset()
}

// ─── tests ───────────────────────────────────────────────────────────────────

describe('useAuth', () => {
  beforeEach(() => {
    resetMocks()
    // By default, no stored auth and no token in URL
    loadAuthMock.mockReturnValue({ accessToken: null, user: null })
    isTokenExpiredMock.mockReturnValue(false)
    getTokenExpirationTimeMock.mockReturnValue(null)
    // Remove any ?token= from the URL
    window.history.replaceState({}, '', '/')
  })

  afterEach(() => {
    vi.restoreAllMocks()
    window.history.replaceState({}, '', '/')
  })

  // Lazy import to pick up mocks
  async function useAuthHook() {
    const { useAuth } = await import('../useAuth')
    return useAuth
  }

  // ── no auth at all ──────────────────────────────────────────────────────

  it('fetches default permissions when no stored auth and no OAuth token', async () => {
    fetchDefaultPermissionsMock.mockResolvedValue(guestUser)

    const useAuth = await useAuthHook()
    const { result } = renderHook(() => useAuth())

    await waitFor(() => expect(result.current.authLoading).toBe(false))

    expect(fetchDefaultPermissionsMock).toHaveBeenCalled()
    expect(result.current.authUser?.id).toBe('guest')
  })

  it('sets authUser to null when fetchDefaultPermissions returns null', async () => {
    fetchDefaultPermissionsMock.mockResolvedValue(null)

    const useAuth = await useAuthHook()
    const { result } = renderHook(() => useAuth())

    await waitFor(() => expect(result.current.authLoading).toBe(false))

    expect(result.current.authUser).toBeNull()
  })

  // ── OAuth callback ──────────────────────────────────────────────────────

  it('handles OAuth callback token from URL', async () => {
    window.history.replaceState({}, '', '/?token=my-oauth-token')
    handleOAuthRedirectMock.mockResolvedValue({ accessToken: 'my-oauth-token', user: realUser })

    const useAuth = await useAuthHook()
    const { result } = renderHook(() => useAuth())

    await waitFor(() => expect(result.current.authLoading).toBe(false))

    expect(handleOAuthRedirectMock).toHaveBeenCalledWith('my-oauth-token')
    expect(result.current.authUser?.username).toBe('alice')
    // URL should be cleaned up
    expect(window.location.search).toBe('')
  })

  it('sets authLoading false on OAuth callback error', async () => {
    window.history.replaceState({}, '', '/?token=bad-token')
    handleOAuthRedirectMock.mockRejectedValue(new Error('auth failed'))
    vi.spyOn(console, 'error').mockImplementation(() => {})

    const useAuth = await useAuthHook()
    const { result } = renderHook(() => useAuth())

    await waitFor(() => expect(result.current.authLoading).toBe(false))

    expect(result.current.authUser).toBeNull()
  })

  // ── stored auth ─────────────────────────────────────────────────────────

  it('validates stored token and sets user when valid', async () => {
    loadAuthMock.mockReturnValue({ accessToken: 'stored-tok', user: realUser })
    validateTokenMock.mockResolvedValue(realUser)

    const useAuth = await useAuthHook()
    const { result } = renderHook(() => useAuth())

    await waitFor(() => expect(result.current.authLoading).toBe(false))

    expect(validateTokenMock).toHaveBeenCalledWith('stored-tok')
    expect(result.current.authUser?.username).toBe('alice')
  })

  it('clears auth and fetches guest when stored token is invalid', async () => {
    loadAuthMock.mockReturnValue({ accessToken: 'bad-tok', user: realUser })
    validateTokenMock.mockResolvedValue(null)
    fetchDefaultPermissionsMock.mockResolvedValue(guestUser)

    const useAuth = await useAuthHook()
    const { result } = renderHook(() => useAuth())

    await waitFor(() => expect(result.current.authLoading).toBe(false))

    expect(clearAuthMock).toHaveBeenCalled()
    expect(result.current.authUser?.id).toBe('guest')
  })

  // ── handleLogin ─────────────────────────────────────────────────────────

  it('handleLogin calls initiateDiscordLogin', async () => {
    fetchDefaultPermissionsMock.mockResolvedValue(guestUser)

    const useAuth = await useAuthHook()
    const { result } = renderHook(() => useAuth())

    await waitFor(() => expect(result.current.authLoading).toBe(false))

    act(() => { result.current.handleLogin() })

    expect(initiateDiscordLoginMock).toHaveBeenCalled()
  })

  // ── handleLogout ────────────────────────────────────────────────────────

  it('handleLogout calls logout and then fetches guest permissions', async () => {
    loadAuthMock
      .mockReturnValueOnce({ accessToken: 'tok', user: realUser }) // initial check
      .mockReturnValue({ accessToken: 'tok', user: realUser })       // in handleLogout
    validateTokenMock.mockResolvedValue(realUser)
    logoutMock.mockResolvedValue(undefined)
    fetchDefaultPermissionsMock.mockResolvedValue(guestUser)

    const useAuth = await useAuthHook()
    const { result } = renderHook(() => useAuth())

    await waitFor(() => expect(result.current.authLoading).toBe(false))

    await act(async () => { await result.current.handleLogout() })

    expect(logoutMock).toHaveBeenCalledWith('tok')
    expect(fetchDefaultPermissionsMock).toHaveBeenCalled()
  })

  // ── refreshAuthToken ────────────────────────────────────────────────────

  it('refreshAuthToken returns true and updates user on success', async () => {
    fetchDefaultPermissionsMock.mockResolvedValue(guestUser)
    loadAuthMock.mockReturnValue({ accessToken: 'tok', user: null })
    refreshTokenMock.mockResolvedValue({ accessToken: 'new-tok', user: realUser })

    const useAuth = await useAuthHook()
    const { result } = renderHook(() => useAuth())

    await waitFor(() => expect(result.current.authLoading).toBe(false))

    let success: boolean = false
    await act(async () => {
      success = await result.current.refreshAuthToken()
    })

    expect(success).toBe(true)
    expect(result.current.authUser?.username).toBe('alice')
  })

  it('refreshAuthToken returns false when no stored token', async () => {
    fetchDefaultPermissionsMock.mockResolvedValue(guestUser)
    loadAuthMock.mockReturnValue({ accessToken: null, user: null })

    const useAuth = await useAuthHook()
    const { result } = renderHook(() => useAuth())

    await waitFor(() => expect(result.current.authLoading).toBe(false))

    let success: boolean = true
    await act(async () => {
      success = await result.current.refreshAuthToken()
    })

    expect(success).toBe(false)
    expect(refreshTokenMock).not.toHaveBeenCalled()
  })

  it('refreshAuthToken returns false and clears auth when refresh fails', async () => {
    fetchDefaultPermissionsMock.mockResolvedValue(guestUser)
    loadAuthMock.mockReturnValue({ accessToken: 'tok', user: null })
    refreshTokenMock.mockResolvedValue(null)

    const useAuth = await useAuthHook()
    const { result } = renderHook(() => useAuth())

    await waitFor(() => expect(result.current.authLoading).toBe(false))

    let success: boolean = true
    await act(async () => {
      success = await result.current.refreshAuthToken()
    })

    expect(success).toBe(false)
    expect(clearAuthMock).toHaveBeenCalled()
    expect(result.current.authUser).toBeNull()
  })

  // ── TOKEN_EXPIRED_EVENT listener ────────────────────────────────────────

  it('handles TOKEN_EXPIRED_EVENT by fetching default permissions', async () => {
    fetchDefaultPermissionsMock.mockResolvedValue(guestUser)
    loadAuthMock.mockReturnValue({ accessToken: null, user: null })

    const useAuth = await useAuthHook()
    const { result } = renderHook(() => useAuth())

    await waitFor(() => expect(result.current.authLoading).toBe(false))

    fetchDefaultPermissionsMock.mockClear()
    fetchDefaultPermissionsMock.mockResolvedValue(guestUser)

    await act(async () => {
      window.dispatchEvent(new CustomEvent(TOKEN_EXPIRED_EVENT))
    })

    expect(fetchDefaultPermissionsMock).toHaveBeenCalled()
    expect(result.current.authUser?.id).toBe('guest')
  })
})
