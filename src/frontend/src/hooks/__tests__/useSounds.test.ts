import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest'
import { renderHook, act, waitFor } from '@testing-library/react'

// ─── Controllable EventSource mock ──────────────────────────────────────────

class MockEventSource {
  static instance: MockEventSource | null = null
  url: string
  withCredentials = false
  onopen: ((ev: any) => any) | null = null
  onerror: ((ev: any) => any) | null = null
  onmessage: ((ev: any) => any) | null = null
  private listeners: Record<string, Array<(ev: any) => void>> = {}
  closed = false

  constructor(url: string) {
    this.url = url
    MockEventSource.instance = this
  }

  addEventListener(type: string, fn: (ev: any) => void) {
    if (!this.listeners[type]) this.listeners[type] = []
    this.listeners[type].push(fn)
  }

  removeEventListener(type: string, fn: (ev: any) => void) {
    this.listeners[type] = (this.listeners[type] || []).filter(l => l !== fn)
  }

  emit(type: string, data: string) {
    ;(this.listeners[type] || []).forEach(fn => fn({ data }))
  }

  close() {
    this.closed = true
  }
}

// ─── module mocks ────────────────────────────────────────────────────────────

vi.mock('../../config', () => ({
  API_ENDPOINTS: {
    SOUNDS_STREAM: '/api/soundFiles/stream',
  },
}))

// ─── tests ───────────────────────────────────────────────────────────────────

describe('useSounds', () => {
  beforeEach(() => {
    localStorage.clear()
    MockEventSource.instance = null
    ;(globalThis as any).EventSource = MockEventSource
  })

  afterEach(() => {
    localStorage.clear()
    vi.restoreAllMocks()
  })

  async function useSoundsHook() {
    const { useSounds } = await import('../useSounds')
    return useSounds
  }

  it('opens EventSource connection to SOUNDS_STREAM on mount', async () => {
    const useSounds = await useSoundsHook()
    renderHook(() => useSounds())

    await waitFor(() => expect(MockEventSource.instance).not.toBeNull())
    expect(MockEventSource.instance!.url).toBe('/api/soundFiles/stream')
  })

  it('initially shows loading=true and connectionStatus=connecting', async () => {
    const useSounds = await useSoundsHook()
    const { result } = renderHook(() => useSounds())

    expect(result.current.loading).toBe(true)
    expect(result.current.connectionStatus).toBe('connecting')
  })

  it('transforms array SSE data and sets sounds', async () => {
    const useSounds = await useSoundsHook()
    const { result } = renderHook(() => useSounds())

    await waitFor(() => expect(MockEventSource.instance).not.toBeNull())

    const apiSounds = [
      {
        soundFileId: 'beep',
        soundFileLocation: '/sounds/beep.mp3',
        category: 'sfx',
        timesPlayed: 5,
        dateAdded: '2024-01-01',
        favorite: false,
        displayName: 'Beep Sound',
        volumeOffsetPercentage: null,
      },
    ]

    act(() => {
      MockEventSource.instance!.emit('sounds', JSON.stringify(apiSounds))
    })

    expect(result.current.sounds).toHaveLength(1)
    expect(result.current.sounds[0].id).toBe('beep')
    expect(result.current.sounds[0].name).toBe('Beep Sound')
    expect(result.current.sounds[0].category).toBe('sfx')
    expect(result.current.sounds[0].timesPlayed).toBe(5)
    expect(result.current.sounds[0].volumeOffset).toBeNull()
    expect(result.current.loading).toBe(false)
    expect(result.current.connectionStatus).toBe('connected')
  })

  it('transforms paginated SSE data (content property)', async () => {
    const useSounds = await useSoundsHook()
    const { result } = renderHook(() => useSounds())

    await waitFor(() => expect(MockEventSource.instance).not.toBeNull())

    const paginatedData = {
      content: [
        {
          soundFileId: 'whoosh',
          soundFileLocation: '/sounds/whoosh.ogg',
          category: 'sfx',
          timesPlayed: 12,
          dateAdded: '2024-02-01',
          favorite: true,
          displayName: null,
          volumeOffsetPercentage: 10,
        },
      ],
      page: { size: 20, number: 0, totalElements: 1, totalPages: 1 },
    }

    act(() => {
      MockEventSource.instance!.emit('sounds', JSON.stringify(paginatedData))
    })

    expect(result.current.sounds).toHaveLength(1)
    // displayName is null → falls back to soundFileId with _ replaced
    expect(result.current.sounds[0].name).toBe('whoosh')
    expect(result.current.sounds[0].volumeOffset).toBe(10)
  })

  it('updates favorites set from backend favorite flag', async () => {
    const useSounds = await useSoundsHook()
    const { result } = renderHook(() => useSounds())

    await waitFor(() => expect(MockEventSource.instance).not.toBeNull())

    const apiSounds = [
      { soundFileId: 'a', soundFileLocation: '/a', category: 'x', timesPlayed: 1, dateAdded: '2024-01-01', favorite: true, displayName: null, volumeOffsetPercentage: null },
      { soundFileId: 'b', soundFileLocation: '/b', category: 'x', timesPlayed: 2, dateAdded: '2024-01-02', favorite: false, displayName: null, volumeOffsetPercentage: null },
    ]

    act(() => {
      MockEventSource.instance!.emit('sounds', JSON.stringify(apiSounds))
    })

    expect(result.current.favorites.has('a')).toBe(true)
    expect(result.current.favorites.has('b')).toBe(false)
  })

  it('loads favorites from localStorage on mount', async () => {
    localStorage.setItem('soundboard-favorites', JSON.stringify(['x', 'y']))

    const useSounds = await useSoundsHook()
    const { result } = renderHook(() => useSounds())

    // The localStorage load happens synchronously during effect setup
    await waitFor(() => expect(result.current.favorites.has('x')).toBe(true))
    expect(result.current.favorites.has('y')).toBe(true)
  })

  it('saves favorites to localStorage when favorites change', async () => {
    const useSounds = await useSoundsHook()
    const { result } = renderHook(() => useSounds())

    await waitFor(() => expect(MockEventSource.instance).not.toBeNull())

    act(() => {
      result.current.setFavorites(new Set(['alpha', 'beta']))
    })

    await waitFor(() => {
      const stored = JSON.parse(localStorage.getItem('soundboard-favorites') || '[]')
      expect(stored).toContain('alpha')
      expect(stored).toContain('beta')
    })
  })

  it('closes EventSource on unmount', async () => {
    const useSounds = await useSoundsHook()
    const { unmount } = renderHook(() => useSounds())

    await waitFor(() => expect(MockEventSource.instance).not.toBeNull())
    const es = MockEventSource.instance!

    unmount()

    expect(es.closed).toBe(true)
  })

  it('handles invalid JSON in SSE event gracefully', async () => {
    const useSounds = await useSoundsHook()
    const { result } = renderHook(() => useSounds())

    await waitFor(() => expect(MockEventSource.instance).not.toBeNull())

    const errorSpy = vi.spyOn(console, 'error').mockImplementation(() => {})

    act(() => {
      MockEventSource.instance!.emit('sounds', 'not-valid-json')
    })

    expect(errorSpy).toHaveBeenCalled()
    expect(result.current.sounds).toHaveLength(0) // unchanged
  })
})
