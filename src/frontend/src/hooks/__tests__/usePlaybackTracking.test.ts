import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest'
import { renderHook, act, waitFor } from '@testing-library/react'

// ─── Controllable EventSource mock ──────────────────────────────────────────

class MockEventSource {
  static instance: MockEventSource | null = null
  url: string
  withCredentials = false
  onopen: ((ev: any) => any) | null = null
  onerror: ((ev: any) => any) | null = null
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

  removeEventListener() {}

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
    PLAYBACK_STREAM: '/api/playback/stream',
  },
}))

// ─── tests ───────────────────────────────────────────────────────────────────

describe('usePlaybackTracking', () => {
  beforeEach(() => {
    MockEventSource.instance = null
    ;(globalThis as any).EventSource = MockEventSource
  })

  afterEach(() => {
    vi.restoreAllMocks()
  })

  async function usePlaybackTrackingHook() {
    const { usePlaybackTracking } = await import('../usePlaybackTracking')
    return usePlaybackTracking
  }

  it('connects to PLAYBACK_STREAM on mount', async () => {
    const usePlaybackTracking = await usePlaybackTrackingHook()
    renderHook(() => usePlaybackTracking({ selectedUserGuildId: 'guild1' }))

    await waitFor(() => expect(MockEventSource.instance).not.toBeNull())
    expect(MockEventSource.instance!.url).toBe('/api/playback/stream')
  })

  it('initializes with null currentlyPlayingSoundId and currentPlayback', async () => {
    const usePlaybackTracking = await usePlaybackTrackingHook()
    const { result } = renderHook(() =>
      usePlaybackTracking({ selectedUserGuildId: 'guild1' })
    )

    expect(result.current.currentlyPlayingSoundId).toBeNull()
    expect(result.current.currentPlayback).toBeNull()
  })

  it('sets currentlyPlayingSoundId and currentPlayback on trackStart with matching guildId', async () => {
    const usePlaybackTracking = await usePlaybackTrackingHook()
    const { result } = renderHook(() =>
      usePlaybackTracking({ selectedUserGuildId: 'guild1' })
    )

    await waitFor(() => expect(MockEventSource.instance).not.toBeNull())

    act(() => {
      MockEventSource.instance!.emit(
        'trackStart',
        JSON.stringify({ soundFileId: 'beep', guildId: 'guild1', user: 'alice', displayName: 'Beep' })
      )
    })

    expect(result.current.currentlyPlayingSoundId).toBe('beep')
    expect(result.current.currentPlayback?.soundFileId).toBe('beep')
    expect(result.current.currentPlayback?.user).toBe('alice')
    expect(result.current.currentPlayback?.displayName).toBe('Beep')
  })

  it('does NOT update state on trackStart with different guildId', async () => {
    const usePlaybackTracking = await usePlaybackTrackingHook()
    const { result } = renderHook(() =>
      usePlaybackTracking({ selectedUserGuildId: 'guild1' })
    )

    await waitFor(() => expect(MockEventSource.instance).not.toBeNull())

    act(() => {
      MockEventSource.instance!.emit(
        'trackStart',
        JSON.stringify({ soundFileId: 'beep', guildId: 'other-guild', user: 'alice' })
      )
    })

    expect(result.current.currentlyPlayingSoundId).toBeNull()
    expect(result.current.currentPlayback).toBeNull()
  })

  it('uses "Unknown" when user is absent in trackStart data', async () => {
    const usePlaybackTracking = await usePlaybackTrackingHook()
    const { result } = renderHook(() =>
      usePlaybackTracking({ selectedUserGuildId: 'guild1' })
    )

    await waitFor(() => expect(MockEventSource.instance).not.toBeNull())

    act(() => {
      MockEventSource.instance!.emit(
        'trackStart',
        JSON.stringify({ soundFileId: 'beep', guildId: 'guild1' })
      )
    })

    expect(result.current.currentPlayback?.user).toBe('Unknown')
  })

  it('clears state on trackEnd with matching guildId and soundFileId', async () => {
    const usePlaybackTracking = await usePlaybackTrackingHook()
    const { result } = renderHook(() =>
      usePlaybackTracking({ selectedUserGuildId: 'guild1' })
    )

    await waitFor(() => expect(MockEventSource.instance).not.toBeNull())

    // Start a track
    act(() => {
      MockEventSource.instance!.emit(
        'trackStart',
        JSON.stringify({ soundFileId: 'beep', guildId: 'guild1', user: 'alice' })
      )
    })
    expect(result.current.currentlyPlayingSoundId).toBe('beep')

    // End the same track
    act(() => {
      MockEventSource.instance!.emit(
        'trackEnd',
        JSON.stringify({ soundFileId: 'beep', guildId: 'guild1' })
      )
    })

    expect(result.current.currentlyPlayingSoundId).toBeNull()
    expect(result.current.currentPlayback).toBeNull()
  })

  it('does NOT clear state on trackEnd when soundFileId differs from current', async () => {
    const usePlaybackTracking = await usePlaybackTrackingHook()
    const { result } = renderHook(() =>
      usePlaybackTracking({ selectedUserGuildId: 'guild1' })
    )

    await waitFor(() => expect(MockEventSource.instance).not.toBeNull())

    act(() => {
      MockEventSource.instance!.emit(
        'trackStart',
        JSON.stringify({ soundFileId: 'beep', guildId: 'guild1', user: 'alice' })
      )
    })

    // End a different sound
    act(() => {
      MockEventSource.instance!.emit(
        'trackEnd',
        JSON.stringify({ soundFileId: 'whoosh', guildId: 'guild1' })
      )
    })

    expect(result.current.currentlyPlayingSoundId).toBe('beep') // unchanged
  })

  it('does NOT clear state on trackEnd with different guildId', async () => {
    const usePlaybackTracking = await usePlaybackTrackingHook()
    const { result } = renderHook(() =>
      usePlaybackTracking({ selectedUserGuildId: 'guild1' })
    )

    await waitFor(() => expect(MockEventSource.instance).not.toBeNull())

    act(() => {
      MockEventSource.instance!.emit(
        'trackStart',
        JSON.stringify({ soundFileId: 'beep', guildId: 'guild1', user: 'alice' })
      )
    })

    act(() => {
      MockEventSource.instance!.emit(
        'trackEnd',
        JSON.stringify({ soundFileId: 'beep', guildId: 'other-guild' })
      )
    })

    expect(result.current.currentlyPlayingSoundId).toBe('beep') // unchanged
  })

  it('setCurrentlyPlayingSoundId allows external override', async () => {
    const usePlaybackTracking = await usePlaybackTrackingHook()
    const { result } = renderHook(() =>
      usePlaybackTracking({ selectedUserGuildId: 'guild1' })
    )

    act(() => {
      result.current.setCurrentlyPlayingSoundId('manual-id')
    })

    expect(result.current.currentlyPlayingSoundId).toBe('manual-id')
  })

  it('closes EventSource on unmount', async () => {
    const usePlaybackTracking = await usePlaybackTrackingHook()
    const { unmount } = renderHook(() =>
      usePlaybackTracking({ selectedUserGuildId: 'guild1' })
    )

    await waitFor(() => expect(MockEventSource.instance).not.toBeNull())
    const es = MockEventSource.instance!

    unmount()
    expect(es.closed).toBe(true)
  })

  it('ignores invalid JSON in SSE event gracefully', async () => {
    const usePlaybackTracking = await usePlaybackTrackingHook()
    const { result } = renderHook(() =>
      usePlaybackTracking({ selectedUserGuildId: 'guild1' })
    )

    await waitFor(() => expect(MockEventSource.instance).not.toBeNull())

    expect(() => {
      act(() => {
        MockEventSource.instance!.emit('trackStart', 'not-json')
      })
    }).not.toThrow()

    expect(result.current.currentlyPlayingSoundId).toBeNull()
  })
})
