import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest'
import { renderHook, act, waitFor } from '@testing-library/react'

// ─── module mocks ────────────────────────────────────────────────────────────

const fetchWithAuthMock = vi.fn()

vi.mock('../../utils/api', () => ({
  fetchWithAuth: (url: string, opts?: any) => fetchWithAuthMock(url, opts),
}))

vi.mock('../../config', () => ({
  API_ENDPOINTS: {
    USER_ROLES: '/api/discordUsers/roles',
    ASSIGN_ROLE: (userId: string) => `/api/discordUsers/${userId}/role`,
    REMOVE_ROLE: (userId: string) => `/api/discordUsers/${userId}/role`,
  },
}))

// ─── helpers ─────────────────────────────────────────────────────────────────

const mockUsers = [
  { id: 'u1', username: 'alice', assignedRole: 'dj' },
  { id: 'u2', username: 'bob', assignedRole: undefined },
]

function okResponse(body: any) {
  return { ok: true, json: async () => body }
}

function notOkResponse(status = 403) {
  return { ok: false, status }
}

// ─── tests ───────────────────────────────────────────────────────────────────

describe('useRoleManagement', () => {
  beforeEach(() => {
    fetchWithAuthMock.mockReset()
    vi.spyOn(console, 'error').mockImplementation(() => {})
  })

  afterEach(() => {
    vi.restoreAllMocks()
  })

  async function useRoleManagementHook() {
    const { useRoleManagement } = await import('../useRoleManagement')
    return useRoleManagement
  }

  // ── initialization ─────────────────────────────────────────────────────

  it('does not fetch when canManageUsers is false', async () => {
    const useRoleManagement = await useRoleManagementHook()
    renderHook(() => useRoleManagement(false))

    await waitFor(() => expect(fetchWithAuthMock).not.toHaveBeenCalled())
  })

  it('starts with empty users list and loading=false when no permission', async () => {
    const useRoleManagement = await useRoleManagementHook()
    const { result } = renderHook(() => useRoleManagement(false))

    await waitFor(() => expect(result.current.loading).toBe(false))
    expect(result.current.users).toHaveLength(0)
  })

  it('fetches users when canManageUsers is true', async () => {
    const useRoleManagement = await useRoleManagementHook()
    fetchWithAuthMock.mockResolvedValueOnce(okResponse({ content: mockUsers }))

    const { result } = renderHook(() => useRoleManagement(true))

    await waitFor(() => expect(result.current.loading).toBe(false))

    expect(fetchWithAuthMock.mock.calls[0][0]).toContain('/api/discordUsers/roles')
    expect(result.current.users).toHaveLength(2)
    expect(result.current.users[0].username).toBe('alice')
  })

  it('handles non-ok response gracefully', async () => {
    const useRoleManagement = await useRoleManagementHook()
    fetchWithAuthMock.mockResolvedValueOnce(notOkResponse())

    const { result } = renderHook(() => useRoleManagement(true))

    await waitFor(() => expect(result.current.loading).toBe(false))

    // users remain empty (response.json() not called on non-ok)
    expect(result.current.users).toHaveLength(0)
  })

  it('handles fetch throw gracefully', async () => {
    const useRoleManagement = await useRoleManagementHook()
    fetchWithAuthMock.mockRejectedValueOnce(new Error('Network error'))

    const { result } = renderHook(() => useRoleManagement(true))

    await waitFor(() => expect(result.current.loading).toBe(false))

    expect(result.current.users).toHaveLength(0)
  })

  // ── assignRole ─────────────────────────────────────────────────────────

  describe('assignRole', () => {
    it('returns true and refreshes users on success', async () => {
      const useRoleManagement = await useRoleManagementHook()
      // Initial fetch
      fetchWithAuthMock.mockResolvedValueOnce(okResponse({ content: mockUsers }))
      // assignRole call
      fetchWithAuthMock.mockResolvedValueOnce({ ok: true })
      // Refresh after assign
      fetchWithAuthMock.mockResolvedValueOnce(okResponse({ content: mockUsers }))

      const { result } = renderHook(() => useRoleManagement(true))

      await waitFor(() => expect(result.current.loading).toBe(false))

      let success = false
      await act(async () => {
        success = await result.current.assignRole('u2', 'dj')
      })

      expect(success).toBe(true)
      expect(fetchWithAuthMock).toHaveBeenCalledWith(
        '/api/discordUsers/u2/role',
        expect.objectContaining({ method: 'PUT' })
      )
    })

    it('returns false when server responds not-ok', async () => {
      const useRoleManagement = await useRoleManagementHook()
      fetchWithAuthMock.mockResolvedValueOnce(okResponse({ content: [] }))
      fetchWithAuthMock.mockResolvedValueOnce(notOkResponse())

      const { result } = renderHook(() => useRoleManagement(true))

      await waitFor(() => expect(result.current.loading).toBe(false))

      let success = true
      await act(async () => {
        success = await result.current.assignRole('u2', 'dj')
      })

      expect(success).toBe(false)
    })

    it('returns false on throw', async () => {
      const useRoleManagement = await useRoleManagementHook()
      fetchWithAuthMock.mockResolvedValueOnce(okResponse({ content: [] }))
      fetchWithAuthMock.mockRejectedValueOnce(new Error('fail'))

      const { result } = renderHook(() => useRoleManagement(true))

      await waitFor(() => expect(result.current.loading).toBe(false))

      let success = true
      await act(async () => {
        success = await result.current.assignRole('u2', 'dj')
      })

      expect(success).toBe(false)
    })
  })

  // ── removeRole ─────────────────────────────────────────────────────────

  describe('removeRole', () => {
    it('returns true and refreshes users on success', async () => {
      const useRoleManagement = await useRoleManagementHook()
      fetchWithAuthMock.mockResolvedValueOnce(okResponse({ content: mockUsers }))
      fetchWithAuthMock.mockResolvedValueOnce({ ok: true })
      fetchWithAuthMock.mockResolvedValueOnce(okResponse({ content: mockUsers }))

      const { result } = renderHook(() => useRoleManagement(true))

      await waitFor(() => expect(result.current.loading).toBe(false))

      let success = false
      await act(async () => {
        success = await result.current.removeRole('u1')
      })

      expect(success).toBe(true)
      expect(fetchWithAuthMock).toHaveBeenCalledWith(
        '/api/discordUsers/u1/role',
        expect.objectContaining({ method: 'DELETE' })
      )
    })

    it('returns false when server responds not-ok', async () => {
      const useRoleManagement = await useRoleManagementHook()
      fetchWithAuthMock.mockResolvedValueOnce(okResponse({ content: [] }))
      fetchWithAuthMock.mockResolvedValueOnce(notOkResponse())

      const { result } = renderHook(() => useRoleManagement(true))

      await waitFor(() => expect(result.current.loading).toBe(false))

      let success = true
      await act(async () => {
        success = await result.current.removeRole('u1')
      })

      expect(success).toBe(false)
    })

    it('returns false on throw', async () => {
      const useRoleManagement = await useRoleManagementHook()
      fetchWithAuthMock.mockResolvedValueOnce(okResponse({ content: [] }))
      fetchWithAuthMock.mockRejectedValueOnce(new Error('fail'))

      const { result } = renderHook(() => useRoleManagement(true))

      await waitFor(() => expect(result.current.loading).toBe(false))

      let success = true
      await act(async () => {
        success = await result.current.removeRole('u1')
      })

      expect(success).toBe(false)
    })
  })

  // ── refreshUsers ───────────────────────────────────────────────────────

  it('refreshUsers re-fetches the user list', async () => {
    const useRoleManagement = await useRoleManagementHook()
    fetchWithAuthMock
      .mockResolvedValueOnce(okResponse({ content: [] }))           // initial
      .mockResolvedValueOnce(okResponse({ content: mockUsers }))    // after refresh

    const { result } = renderHook(() => useRoleManagement(true))

    await waitFor(() => expect(result.current.loading).toBe(false))
    expect(result.current.users).toHaveLength(0)

    await act(async () => { await result.current.refreshUsers() })

    expect(result.current.users).toHaveLength(2)
  })
})
