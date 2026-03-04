import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest'
import { renderHook, act } from '@testing-library/react'
import type { Sound } from '../useSounds'

// ─── module mocks ────────────────────────────────────────────────────────────

const getAuthHeadersWithCsrfMock = vi.fn(() => ({}))
const fetchWithAuthMock = vi.fn()

vi.mock('../../utils/api', () => ({
  getAuthHeadersWithCsrf: () => getAuthHeadersWithCsrfMock(),
  fetchWithAuth: (url: string, opts: any) => fetchWithAuthMock(url, opts),
}))

vi.mock('../../config', () => ({
  API_ENDPOINTS: {
    FAVORITE: '/api/soundFiles/favorite',
    PLAY_FILE: '/bot/playFile',
    RANDOM: '/bot/random',
    STOP: '/bot/stop',
    SOUND_FILE: '/api/soundFiles',
    DOWNLOAD: '/api/soundFiles/download',
    UPLOAD: '/api/soundFiles/upload',
  },
}))

// Mock sonner toast (mapped to sonner via vitest.config alias)
vi.mock('sonner', () => ({
  toast: {
    error: vi.fn(),
    success: vi.fn(),
    warning: vi.fn(),
  },
}))

// ─── helpers ─────────────────────────────────────────────────────────────────

function makeSound(overrides: Partial<Sound> = {}): Sound {
  return {
    id: 'sound1',
    name: 'Test Sound',
    category: 'sfx',
    url: '/sounds/sound1.mp3',
    timesPlayed: 3,
    dateAdded: '2024-01-01',
    favorite: false,
    displayName: 'Test Sound',
    volumeOffset: null,
    ...overrides,
  }
}

function defaultProps() {
  return {
    selectedUserId: 'user123',
    isPlaybackEnabled: true,
    setCurrentlyPlayingSoundId: vi.fn(),
    setSounds: vi.fn(),
    favorites: new Set<string>(),
    setFavorites: vi.fn(),
  }
}

// ─── tests ───────────────────────────────────────────────────────────────────

describe('useSoundActions', () => {
  beforeEach(() => {
    global.fetch = vi.fn()
    getAuthHeadersWithCsrfMock.mockReturnValue({})
    fetchWithAuthMock.mockReset()
  })

  afterEach(() => {
    vi.restoreAllMocks()
  })

  async function useSoundActionsHook() {
    const { useSoundActions } = await import('../useSoundActions')
    return useSoundActions
  }

  // ── toggleFavorite ─────────────────────────────────────────────────────

  describe('toggleFavorite', () => {
    it('adds to favorites when sound is not currently favorited', async () => {
      const useSoundActions = await useSoundActionsHook()
      const setFavorites = vi.fn()
      const props = { ...defaultProps(), favorites: new Set<string>(), setFavorites }

      ;(global.fetch as any).mockResolvedValueOnce({
        ok: true,
        headers: { get: () => '0' },
        text: async () => '',
        clone: () => ({ text: async () => '' }),
      })

      const { result } = renderHook(() => useSoundActions(props))

      await act(async () => {
        await result.current.toggleFavorite('sound1')
      })

      expect(global.fetch).toHaveBeenCalledWith(
        '/api/soundFiles/favorite/sound1?favorite=true',
        expect.objectContaining({ method: 'POST' })
      )
      expect(setFavorites).toHaveBeenCalled()
    })

    it('removes from favorites when sound is already favorited', async () => {
      const useSoundActions = await useSoundActionsHook()
      const setFavorites = vi.fn()
      const props = {
        ...defaultProps(),
        favorites: new Set<string>(['sound1']),
        setFavorites,
      }

      ;(global.fetch as any).mockResolvedValueOnce({
        ok: true,
        headers: { get: () => '0' },
        text: async () => '',
        clone: () => ({ text: async () => '' }),
      })

      const { result } = renderHook(() => useSoundActions(props))

      await act(async () => {
        await result.current.toggleFavorite('sound1')
      })

      expect(global.fetch).toHaveBeenCalledWith(
        '/api/soundFiles/favorite/sound1?favorite=false',
        expect.any(Object)
      )
    })

    it('does not call fetch when toggle is already in progress for the same sound', async () => {
      const useSoundActions = await useSoundActionsHook()
      let resolveFetch!: () => void
      ;(global.fetch as any).mockImplementationOnce(
        () =>
          new Promise(resolve => {
            resolveFetch = () =>
              resolve({
                ok: true,
                headers: { get: () => '0' },
                text: async () => '',
                clone: () => ({ text: async () => '' }),
              })
          })
      )

      const { result } = renderHook(() => useSoundActions(defaultProps()))

      // Start first toggle (will be pending)
      act(() => { result.current.toggleFavorite('sound1') })
      // Immediately start second toggle while first is in-flight
      await act(async () => { await result.current.toggleFavorite('sound1') })

      // Only one fetch should have been called
      expect(global.fetch).toHaveBeenCalledTimes(1)

      // Resolve first
      await act(async () => { resolveFetch() })
    })
  })

  // ── playSoundWithBot ───────────────────────────────────────────────────

  describe('playSoundWithBot', () => {
    it('calls play endpoint and sets currently playing sound id on success', async () => {
      const useSoundActions = await useSoundActionsHook()
      const setCurrentlyPlayingSoundId = vi.fn()
      const props = { ...defaultProps(), setCurrentlyPlayingSoundId }

      ;(global.fetch as any).mockResolvedValueOnce({ ok: true })

      const { result } = renderHook(() => useSoundActions(props))

      await act(async () => {
        await result.current.playSoundWithBot('sound1')
      })

      expect(global.fetch).toHaveBeenCalledWith(
        '/bot/playFile?soundFileId=sound1&username=user123',
        expect.objectContaining({ method: 'POST' })
      )
      expect(setCurrentlyPlayingSoundId).toHaveBeenCalledWith('sound1')
    })

    it('shows warning toast when no user is selected', async () => {
      const useSoundActions = await useSoundActionsHook()
      const props = { ...defaultProps(), selectedUserId: null }

      const { result } = renderHook(() => useSoundActions(props))

      await act(async () => {
        await result.current.playSoundWithBot('sound1')
      })

      expect(global.fetch).not.toHaveBeenCalled()
    })

    it('shows warning toast when playback is not enabled', async () => {
      const useSoundActions = await useSoundActionsHook()
      const props = { ...defaultProps(), isPlaybackEnabled: false }

      const { result } = renderHook(() => useSoundActions(props))

      await act(async () => {
        await result.current.playSoundWithBot('sound1')
      })

      expect(global.fetch).not.toHaveBeenCalled()
    })
  })

  // ── playRandomSound ────────────────────────────────────────────────────

  describe('playRandomSound', () => {
    it('calls random endpoint when playback is enabled and sounds exist', async () => {
      const useSoundActions = await useSoundActionsHook()
      ;(global.fetch as any).mockResolvedValueOnce({ ok: true })

      const { result } = renderHook(() => useSoundActions(defaultProps()))

      await act(async () => {
        await result.current.playRandomSound([makeSound()])
      })

      expect(global.fetch).toHaveBeenCalledWith(
        '/bot/random?username=user123',
        expect.objectContaining({ method: 'POST' })
      )
    })

    it('shows warning toast when playback is not enabled or no sounds', async () => {
      const useSoundActions = await useSoundActionsHook()
      const props = { ...defaultProps(), isPlaybackEnabled: false }

      const { result } = renderHook(() => useSoundActions(props))

      await act(async () => {
        await result.current.playRandomSound([makeSound()])
      })

      expect(global.fetch).not.toHaveBeenCalled()
    })

    it('shows warning toast when filtered sounds array is empty', async () => {
      const useSoundActions = await useSoundActionsHook()
      const { result } = renderHook(() => useSoundActions(defaultProps()))

      await act(async () => {
        await result.current.playRandomSound([]) // empty array
      })

      expect(global.fetch).not.toHaveBeenCalled()
    })
  })

  // ── stopCurrentSound ───────────────────────────────────────────────────

  describe('stopCurrentSound', () => {
    it('calls stop endpoint and clears currently playing id on success', async () => {
      const useSoundActions = await useSoundActionsHook()
      const setCurrentlyPlayingSoundId = vi.fn()
      const props = { ...defaultProps(), setCurrentlyPlayingSoundId }

      ;(global.fetch as any).mockResolvedValueOnce({ ok: true })

      const { result } = renderHook(() => useSoundActions(props))

      await act(async () => {
        await result.current.stopCurrentSound()
      })

      expect(global.fetch).toHaveBeenCalledWith(
        '/bot/stop?username=user123',
        expect.objectContaining({ method: 'POST' })
      )
      expect(setCurrentlyPlayingSoundId).toHaveBeenCalledWith(null)
    })

    it('shows warning toast when playback is not enabled', async () => {
      const useSoundActions = await useSoundActionsHook()
      const props = { ...defaultProps(), isPlaybackEnabled: false }

      const { result } = renderHook(() => useSoundActions(props))

      await act(async () => {
        await result.current.stopCurrentSound()
      })

      expect(global.fetch).not.toHaveBeenCalled()
    })
  })

  // ── deleteSound ────────────────────────────────────────────────────────

  describe('deleteSound', () => {
    it('calls fetchWithAuth DELETE and updates sounds list on success', async () => {
      const useSoundActions = await useSoundActionsHook()
      const setSounds = vi.fn()
      const setFavorites = vi.fn()
      const props = { ...defaultProps(), setSounds, setFavorites }

      fetchWithAuthMock.mockResolvedValueOnce({ ok: true })

      const { result } = renderHook(() => useSoundActions(props))

      await act(async () => {
        await result.current.deleteSound('sound1')
      })

      expect(fetchWithAuthMock).toHaveBeenCalledWith(
        '/api/soundFiles/sound1',
        expect.objectContaining({ method: 'DELETE' })
      )
      expect(setSounds).toHaveBeenCalled()
      expect(setFavorites).toHaveBeenCalled()
    })

    it('shows error toast when delete fails', async () => {
      const useSoundActions = await useSoundActionsHook()
      fetchWithAuthMock.mockResolvedValueOnce({ ok: false })

      const { result } = renderHook(() => useSoundActions(defaultProps()))

      await act(async () => {
        await result.current.deleteSound('sound1')
      })

      // setSounds should NOT be called on failure
      const { setSounds } = defaultProps()
      expect(setSounds).not.toHaveBeenCalled()
    })

    it('handles fetchWithAuth throw gracefully', async () => {
      const useSoundActions = await useSoundActionsHook()
      fetchWithAuthMock.mockRejectedValueOnce(new Error('Network error'))

      const { result } = renderHook(() => useSoundActions(defaultProps()))

      await expect(
        act(async () => { await result.current.deleteSound('sound1') })
      ).resolves.not.toThrow()
    })
  })

  // ── downloadSound ──────────────────────────────────────────────────────

  describe('downloadSound', () => {
    it('creates an anchor element with correct href and triggers download', async () => {
      const useSoundActions = await useSoundActionsHook()
      const appendSpy = vi.spyOn(document.body, 'appendChild').mockImplementation((el) => el)
      const removeSpy = vi.spyOn(document.body, 'removeChild').mockImplementation((el) => el)
      const clickSpy = vi.fn()

      const createElementOrig = document.createElement.bind(document)
      vi.spyOn(document, 'createElement').mockImplementation((tag) => {
        const el = createElementOrig(tag)
        if (tag === 'a') {
          el.click = clickSpy
        }
        return el
      })

      const { result } = renderHook(() => useSoundActions(defaultProps()))
      const sound = makeSound({ id: 'beep', name: 'Beep' })

      act(() => { result.current.downloadSound(sound) })

      expect(appendSpy).toHaveBeenCalled()
      expect(clickSpy).toHaveBeenCalled()
      expect(removeSpy).toHaveBeenCalled()

      appendSpy.mockRestore()
      removeSpy.mockRestore()
    })
  })

  // ── handleFileUpload ───────────────────────────────────────────────────

  describe('handleFileUpload', () => {
    function makeFileEvent(fileName: string, type: string, size = 1024): any {
      const file = new File(['x'.repeat(size)], fileName, { type })
      return { target: { files: [file], value: '' } }
    }

    it('uploads valid MP3 file successfully', async () => {
      const useSoundActions = await useSoundActionsHook()
      ;(global.fetch as any).mockResolvedValueOnce({ ok: true, status: 200 })

      const { result } = renderHook(() => useSoundActions(defaultProps()))

      await act(async () => {
        await result.current.handleFileUpload(makeFileEvent('test.mp3', 'audio/mpeg'))
      })

      expect(global.fetch).toHaveBeenCalledWith(
        '/api/soundFiles/upload',
        expect.objectContaining({ method: 'POST' })
      )
    })

    it('rejects file with invalid type and extension', async () => {
      const useSoundActions = await useSoundActionsHook()
      const { result } = renderHook(() => useSoundActions(defaultProps()))

      await act(async () => {
        await result.current.handleFileUpload(makeFileEvent('malware.exe', 'application/octet-stream'))
      })

      expect(global.fetch).not.toHaveBeenCalled()
    })

    it('rejects file exceeding 10MB size limit', async () => {
      const useSoundActions = await useSoundActionsHook()
      const oversizedEvent = makeFileEvent('big.mp3', 'audio/mpeg', 11 * 1024 * 1024)

      const { result } = renderHook(() => useSoundActions(defaultProps()))

      await act(async () => {
        await result.current.handleFileUpload(oversizedEvent)
      })

      expect(global.fetch).not.toHaveBeenCalled()
    })

    it('handles 403 response as permission denied', async () => {
      const useSoundActions = await useSoundActionsHook()
      ;(global.fetch as any).mockResolvedValueOnce({ ok: false, status: 403 })

      const { result } = renderHook(() => useSoundActions(defaultProps()))

      await act(async () => {
        await result.current.handleFileUpload(makeFileEvent('test.mp3', 'audio/mpeg'))
      })

      // Should not throw
    })

    it('does nothing when no file is selected', async () => {
      const useSoundActions = await useSoundActionsHook()
      const { result } = renderHook(() => useSoundActions(defaultProps()))

      await act(async () => {
        await result.current.handleFileUpload({ target: { files: [], value: '' } } as any)
      })

      expect(global.fetch).not.toHaveBeenCalled()
    })
  })
})
