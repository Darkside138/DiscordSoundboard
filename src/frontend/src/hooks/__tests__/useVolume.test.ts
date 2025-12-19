import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest'
import { renderHook, act, waitFor } from '@testing-library/react'
import { useVolume } from '../useVolume'

// Mock API utilities
const getAuthHeadersWithCsrfMock = vi.fn(() => ({ 'X-CSRF-TOKEN': 'test-token' }))
vi.mock('../../utils/api', () => ({
  getAuthHeadersWithCsrf: () => getAuthHeadersWithCsrfMock()
}))

// Mock config
vi.mock('../../config', () => ({
  API_ENDPOINTS: {
    VOLUME: 'http://localhost/api/volume'
  }
}))

describe('useVolume', () => {
  beforeEach(() => {
    localStorage.clear()
    getAuthHeadersWithCsrfMock.mockClear()
    global.fetch = vi.fn()
  })

  afterEach(() => {
    localStorage.clear()
    vi.restoreAllMocks()
  })

  it('initializes with volume 100 by default', () => {
    const { result } = renderHook(() => useVolume())
    expect(result.current.volume).toBe(100)
  })

  it('loads saved volume from localStorage on mount', () => {
    localStorage.setItem('soundboard-volume', '75')
    const { result } = renderHook(() => useVolume())
    expect(result.current.volume).toBe(75)
  })

  it('saves volume to localStorage when setVolume is called', () => {
    const { result } = renderHook(() => useVolume())

    act(() => {
      result.current.setVolume(50)
    })

    expect(result.current.volume).toBe(50)
    expect(localStorage.getItem('soundboard-volume')).toBe('50')
  })

  it('updates volume via API when updateVolume is called', async () => {
    ;(global.fetch as any).mockResolvedValueOnce({ ok: true })

    const { result } = renderHook(() => useVolume())

    await act(async () => {
      await result.current.updateVolume(80, 'user123')
    })

    expect(result.current.volume).toBe(80)
    expect(global.fetch).toHaveBeenCalledWith(
      'http://localhost/api/volume?username=user123&volume=80',
      expect.objectContaining({
        method: 'POST',
        mode: 'cors',
        credentials: 'include'
      })
    )
  })

  it('does not call API when selectedUserId is null', async () => {
    const { result } = renderHook(() => useVolume())

    await act(async () => {
      await result.current.updateVolume(80, null)
    })

    expect(global.fetch).not.toHaveBeenCalled()
  })

  it('handles API errors gracefully', async () => {
    const consoleErrorSpy = vi.spyOn(console, 'error').mockImplementation(() => {})
    ;(global.fetch as any).mockRejectedValueOnce(new Error('Network error'))

    const { result } = renderHook(() => useVolume())

    await act(async () => {
      await result.current.updateVolume(80, 'user123')
    })

    expect(result.current.volume).toBe(80) // Volume still updates locally
    expect(consoleErrorSpy).toHaveBeenCalledWith('Error updating volume:', expect.any(Error))

    consoleErrorSpy.mockRestore()
  })

  it('persists volume across remounts', () => {
    const { result: result1, unmount } = renderHook(() => useVolume())

    act(() => {
      result1.current.setVolume(65)
    })

    unmount()

    const { result: result2 } = renderHook(() => useVolume())
    expect(result2.current.volume).toBe(65)
  })
})