import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest'
import { renderHook, act, waitFor } from '@testing-library/react'

// ─── module mocks ────────────────────────────────────────────────────────────

const fetchWithAuthMock = vi.fn()

vi.mock('../../utils/api', () => ({
  fetchWithAuth: (url: string, opts?: any) => fetchWithAuthMock(url, opts),
}))

vi.mock('../../config', () => ({
  API_ENDPOINTS: {
    ROLE_PERMISSIONS: '/api/rolePermissions',
    ROLE_PERMISSIONS_CONFIGURED: '/api/rolePermissions/configured',
    ROLE_PERMISSIONS_FOR_ROLE: (role: string) => `/api/rolePermissions/${role}`,
    ADD_PERMISSION_TO_ROLE: (role: string) => `/api/rolePermissions/${role}/permissions`,
    REMOVE_PERMISSION_FROM_ROLE: (role: string, perm: string) =>
      `/api/rolePermissions/${role}/permissions/${perm}`,
    RESET_ROLE_TO_DEFAULTS: (role: string) => `/api/rolePermissions/${role}/reset`,
  },
}))

// ─── helpers ─────────────────────────────────────────────────────────────────

const mockPermissions = [
  { id: 1, role: 'dj', permission: 'play-sounds', assignedAt: '2024-01-01', assignedBy: 'admin' },
  { id: 2, role: 'dj', permission: 'upload', assignedAt: '2024-01-02', assignedBy: 'admin' },
  { id: 3, role: 'user', permission: 'download-sounds', assignedAt: '2024-01-03', assignedBy: 'admin' },
]

function okResponse(body: any) {
  return { ok: true, json: async () => body }
}

function notOkResponse(status = 403) {
  return { ok: false, status }
}

// ─── tests ───────────────────────────────────────────────────────────────────

describe('usePermissionManagement', () => {
  beforeEach(() => {
    fetchWithAuthMock.mockReset()
    vi.spyOn(console, 'error').mockImplementation(() => {})
  })

  afterEach(() => {
    vi.restoreAllMocks()
  })

  async function usePermissionManagementHook() {
    const { usePermissionManagement } = await import('../usePermissionManagement')
    return usePermissionManagement
  }

  // ── initialization ─────────────────────────────────────────────────────

  it('skips fetch when canManageUsers is false', async () => {
    const usePermissionManagement = await usePermissionManagementHook()
    const { result } = renderHook(() => usePermissionManagement(false))

    await waitFor(() => expect(result.current.loading).toBe(false))

    expect(fetchWithAuthMock).not.toHaveBeenCalled()
    expect(result.current.rolePermissions).toHaveLength(0)
    expect(result.current.error).toBeNull()
  })

  it('fetches permissions and configured roles when canManageUsers is true', async () => {
    const usePermissionManagement = await usePermissionManagementHook()
    fetchWithAuthMock
      .mockResolvedValueOnce(okResponse(mockPermissions))          // ROLE_PERMISSIONS
      .mockResolvedValueOnce(okResponse(['dj', 'user']))           // ROLE_PERMISSIONS_CONFIGURED

    const { result } = renderHook(() => usePermissionManagement(true))

    await waitFor(() => expect(result.current.loading).toBe(false))

    expect(result.current.rolePermissions).toHaveLength(3)
    expect(result.current.isRoleConfigured('dj')).toBe(true)
    expect(result.current.isRoleConfigured('user')).toBe(true)
    expect(result.current.error).toBeNull()
  })

  it('sets error state when fetch throws', async () => {
    const usePermissionManagement = await usePermissionManagementHook()
    fetchWithAuthMock.mockRejectedValueOnce(new Error('Network error'))

    const { result } = renderHook(() => usePermissionManagement(true))

    await waitFor(() => expect(result.current.loading).toBe(false))

    expect(result.current.error).toBe('Failed to load permissions')
  })

  // ── getPermissionsForRole ──────────────────────────────────────────────

  describe('getPermissionsForRole', () => {
    it('returns permissions for a specific role', async () => {
      const usePermissionManagement = await usePermissionManagementHook()
      fetchWithAuthMock
        .mockResolvedValueOnce(okResponse(mockPermissions))
        .mockResolvedValueOnce(okResponse(['dj']))

      const { result } = renderHook(() => usePermissionManagement(true))

      await waitFor(() => expect(result.current.loading).toBe(false))

      const djPerms = result.current.getPermissionsForRole('dj')
      expect(djPerms).toContain('play-sounds')
      expect(djPerms).toContain('upload')
      expect(djPerms).toHaveLength(2)
    })

    it('returns empty array for unknown role', async () => {
      const usePermissionManagement = await usePermissionManagementHook()
      fetchWithAuthMock
        .mockResolvedValueOnce(okResponse(mockPermissions))
        .mockResolvedValueOnce(okResponse([]))

      const { result } = renderHook(() => usePermissionManagement(true))

      await waitFor(() => expect(result.current.loading).toBe(false))

      expect(result.current.getPermissionsForRole('moderator')).toHaveLength(0)
    })
  })

  // ── isRoleConfigured ───────────────────────────────────────────────────

  describe('isRoleConfigured', () => {
    it('returns true for configured role, false for unconfigured', async () => {
      const usePermissionManagement = await usePermissionManagementHook()
      fetchWithAuthMock
        .mockResolvedValueOnce(okResponse([]))
        .mockResolvedValueOnce(okResponse(['dj']))

      const { result } = renderHook(() => usePermissionManagement(true))

      await waitFor(() => expect(result.current.loading).toBe(false))

      expect(result.current.isRoleConfigured('dj')).toBe(true)
      expect(result.current.isRoleConfigured('user')).toBe(false)
    })
  })

  // ── setPermissionsForRole ──────────────────────────────────────────────

  describe('setPermissionsForRole', () => {
    it('returns true and refreshes on success', async () => {
      const usePermissionManagement = await usePermissionManagementHook()
      fetchWithAuthMock
        .mockResolvedValueOnce(okResponse([]))   // initial perms
        .mockResolvedValueOnce(okResponse([]))   // initial configured
        .mockResolvedValueOnce({ ok: true })     // PUT
        .mockResolvedValueOnce(okResponse(mockPermissions))  // refresh perms
        .mockResolvedValueOnce(okResponse(['dj']))            // refresh configured

      const { result } = renderHook(() => usePermissionManagement(true))

      await waitFor(() => expect(result.current.loading).toBe(false))

      let success = false
      await act(async () => {
        success = await result.current.setPermissionsForRole('dj', ['play-sounds'])
      })

      expect(success).toBe(true)
      expect(fetchWithAuthMock).toHaveBeenCalledWith(
        '/api/rolePermissions/dj',
        expect.objectContaining({ method: 'PUT' })
      )
    })

    it('returns false when server responds not-ok', async () => {
      const usePermissionManagement = await usePermissionManagementHook()
      fetchWithAuthMock
        .mockResolvedValueOnce(okResponse([]))
        .mockResolvedValueOnce(okResponse([]))
        .mockResolvedValueOnce(notOkResponse())

      const { result } = renderHook(() => usePermissionManagement(true))

      await waitFor(() => expect(result.current.loading).toBe(false))

      let success = true
      await act(async () => {
        success = await result.current.setPermissionsForRole('dj', [])
      })

      expect(success).toBe(false)
    })

    it('returns false on throw', async () => {
      const usePermissionManagement = await usePermissionManagementHook()
      fetchWithAuthMock
        .mockResolvedValueOnce(okResponse([]))
        .mockResolvedValueOnce(okResponse([]))
        .mockRejectedValueOnce(new Error('fail'))

      const { result } = renderHook(() => usePermissionManagement(true))

      await waitFor(() => expect(result.current.loading).toBe(false))

      let success = true
      await act(async () => {
        success = await result.current.setPermissionsForRole('dj', ['play-sounds'])
      })

      expect(success).toBe(false)
    })
  })

  // ── addPermissionToRole ────────────────────────────────────────────────

  describe('addPermissionToRole', () => {
    it('returns true and refreshes on success', async () => {
      const usePermissionManagement = await usePermissionManagementHook()
      fetchWithAuthMock
        .mockResolvedValueOnce(okResponse([]))
        .mockResolvedValueOnce(okResponse([]))
        .mockResolvedValueOnce({ ok: true })
        .mockResolvedValueOnce(okResponse(mockPermissions))
        .mockResolvedValueOnce(okResponse(['dj']))

      const { result } = renderHook(() => usePermissionManagement(true))

      await waitFor(() => expect(result.current.loading).toBe(false))

      let success = false
      await act(async () => {
        success = await result.current.addPermissionToRole('dj', 'upload')
      })

      expect(success).toBe(true)
      expect(fetchWithAuthMock).toHaveBeenCalledWith(
        '/api/rolePermissions/dj/permissions',
        expect.objectContaining({ method: 'POST' })
      )
    })

    it('returns false on failure', async () => {
      const usePermissionManagement = await usePermissionManagementHook()
      fetchWithAuthMock
        .mockResolvedValueOnce(okResponse([]))
        .mockResolvedValueOnce(okResponse([]))
        .mockResolvedValueOnce(notOkResponse())

      const { result } = renderHook(() => usePermissionManagement(true))

      await waitFor(() => expect(result.current.loading).toBe(false))

      let success = true
      await act(async () => {
        success = await result.current.addPermissionToRole('dj', 'upload')
      })

      expect(success).toBe(false)
    })
  })

  // ── removePermissionFromRole ───────────────────────────────────────────

  describe('removePermissionFromRole', () => {
    it('returns true and refreshes on success', async () => {
      const usePermissionManagement = await usePermissionManagementHook()
      fetchWithAuthMock
        .mockResolvedValueOnce(okResponse(mockPermissions))
        .mockResolvedValueOnce(okResponse(['dj']))
        .mockResolvedValueOnce({ ok: true })
        .mockResolvedValueOnce(okResponse([]))
        .mockResolvedValueOnce(okResponse([]))

      const { result } = renderHook(() => usePermissionManagement(true))

      await waitFor(() => expect(result.current.loading).toBe(false))

      let success = false
      await act(async () => {
        success = await result.current.removePermissionFromRole('dj', 'upload')
      })

      expect(success).toBe(true)
      expect(fetchWithAuthMock).toHaveBeenCalledWith(
        '/api/rolePermissions/dj/permissions/upload',
        expect.objectContaining({ method: 'DELETE' })
      )
    })

    it('returns false on failure', async () => {
      const usePermissionManagement = await usePermissionManagementHook()
      fetchWithAuthMock
        .mockResolvedValueOnce(okResponse([]))
        .mockResolvedValueOnce(okResponse([]))
        .mockResolvedValueOnce(notOkResponse())

      const { result } = renderHook(() => usePermissionManagement(true))

      await waitFor(() => expect(result.current.loading).toBe(false))

      let success = true
      await act(async () => {
        success = await result.current.removePermissionFromRole('dj', 'upload')
      })

      expect(success).toBe(false)
    })
  })

  // ── resetRoleToDefaults ────────────────────────────────────────────────

  describe('resetRoleToDefaults', () => {
    it('returns true and refreshes on success', async () => {
      const usePermissionManagement = await usePermissionManagementHook()
      fetchWithAuthMock
        .mockResolvedValueOnce(okResponse([]))
        .mockResolvedValueOnce(okResponse([]))
        .mockResolvedValueOnce({ ok: true })
        .mockResolvedValueOnce(okResponse([]))
        .mockResolvedValueOnce(okResponse([]))

      const { result } = renderHook(() => usePermissionManagement(true))

      await waitFor(() => expect(result.current.loading).toBe(false))

      let success = false
      await act(async () => {
        success = await result.current.resetRoleToDefaults('dj')
      })

      expect(success).toBe(true)
      expect(fetchWithAuthMock).toHaveBeenCalledWith(
        '/api/rolePermissions/dj/reset',
        expect.objectContaining({ method: 'POST' })
      )
    })

    it('returns false on failure', async () => {
      const usePermissionManagement = await usePermissionManagementHook()
      fetchWithAuthMock
        .mockResolvedValueOnce(okResponse([]))
        .mockResolvedValueOnce(okResponse([]))
        .mockResolvedValueOnce(notOkResponse())

      const { result } = renderHook(() => usePermissionManagement(true))

      await waitFor(() => expect(result.current.loading).toBe(false))

      let success = true
      await act(async () => {
        success = await result.current.resetRoleToDefaults('dj')
      })

      expect(success).toBe(false)
    })
  })

  // ── refreshPermissions ─────────────────────────────────────────────────

  it('refreshPermissions re-fetches data', async () => {
    const usePermissionManagement = await usePermissionManagementHook()
    fetchWithAuthMock
      .mockResolvedValueOnce(okResponse([]))           // initial perms
      .mockResolvedValueOnce(okResponse([]))           // initial configured
      .mockResolvedValueOnce(okResponse(mockPermissions))  // refresh perms
      .mockResolvedValueOnce(okResponse(['dj']))            // refresh configured

    const { result } = renderHook(() => usePermissionManagement(true))

    await waitFor(() => expect(result.current.loading).toBe(false))
    expect(result.current.rolePermissions).toHaveLength(0)

    await act(async () => { await result.current.refreshPermissions() })

    expect(result.current.rolePermissions).toHaveLength(3)
  })
})
