import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest'
import { renderHook } from '@testing-library/react'

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

  removeEventListener() {}

  emit(type: string, data: string) {
    if (type === 'message' && this.onmessage) {
      this.onmessage({ data })
    }
    ;(this.listeners[type] || []).forEach(fn => fn({ data }))
  }

  close() {
    this.closed = true
  }
}

// ─── module mocks ────────────────────────────────────────────────────────────

const getAuthHeadersMock = vi.fn(() => ({}))

vi.mock('../../utils/api', () => ({
  getAuthHeaders: () => getAuthHeadersMock(),
}))

vi.mock('../../config', () => ({
  API_ENDPOINTS: {
    VOLUME: '/api/volume',
    VOLUME_STREAM: '/api/volume/stream',
  },
}))

// ─── tests ───────────────────────────────────────────────────────────────────

describe('useVolumeSSE', () => {
  beforeEach(() => {
    vi.useFakeTimers()
    global.fetch = vi.fn()
    MockEventSource.instance = null
    ;(globalThis as any).EventSource = MockEventSource
    getAuthHeadersMock.mockReturnValue({})
  })

  afterEach(() => {
    vi.useRealTimers()
    vi.restoreAllMocks()
  })

  async function useVolumeSSEHook() {
    const { useVolumeSSE } = await import('../useVolumeSSE')
    return useVolumeSSE
  }

  it('does nothing when selectedUserId is null', async () => {
    const useVolumeSSE = await useVolumeSSEHook()
    const setVolume = vi.fn()

    renderHook(() => useVolumeSSE({ selectedUserId: null, setVolume }))

    // Advance timers to beyond the 100ms SSE timeout
    vi.advanceTimersByTime(200)

    expect(global.fetch).not.toHaveBeenCalled()
    expect(MockEventSource.instance).toBeNull()
  })

  it('fetches initial volume when selectedUserId is provided', async () => {
    const useVolumeSSE = await useVolumeSSEHook()
    const setVolume = vi.fn()

    ;(global.fetch as any).mockResolvedValueOnce({
      ok: true,
      text: async () => '75',
    })

    renderHook(() => useVolumeSSE({ selectedUserId: 'user123', setVolume }))

    // Let the async fetch resolve
    await vi.runAllTimersAsync()

    expect(global.fetch).toHaveBeenCalledWith(
      '/api/volume/user123',
      expect.objectContaining({ credentials: 'include' })
    )
    expect(setVolume).toHaveBeenCalledWith(75)
  })

  it('does not call setVolume for out-of-range initial volume', async () => {
    const useVolumeSSE = await useVolumeSSEHook()
    const setVolume = vi.fn()

    ;(global.fetch as any).mockResolvedValueOnce({
      ok: true,
      text: async () => '150', // out of range
    })

    renderHook(() => useVolumeSSE({ selectedUserId: 'user123', setVolume }))

    await vi.runAllTimersAsync()

    expect(setVolume).not.toHaveBeenCalled()
  })

  it('does not call setVolume for non-numeric initial volume', async () => {
    const useVolumeSSE = await useVolumeSSEHook()
    const setVolume = vi.fn()

    ;(global.fetch as any).mockResolvedValueOnce({
      ok: true,
      text: async () => 'invalid',
    })

    renderHook(() => useVolumeSSE({ selectedUserId: 'user123', setVolume }))

    await vi.runAllTimersAsync()

    expect(setVolume).not.toHaveBeenCalled()
  })

  it('connects to SSE after 100ms delay', async () => {
    const useVolumeSSE = await useVolumeSSEHook()
    const setVolume = vi.fn()

    ;(global.fetch as any).mockResolvedValueOnce({ ok: false })

    renderHook(() => useVolumeSSE({ selectedUserId: 'user123', setVolume }))

    expect(MockEventSource.instance).toBeNull()

    // Advance past the 100ms timeout
    vi.advanceTimersByTime(150)

    expect(MockEventSource.instance).not.toBeNull()
    expect(MockEventSource.instance!.url).toBe('/api/volume/stream/user123')
  })

  it('handles volume SSE event (named "volume")', async () => {
    const useVolumeSSE = await useVolumeSSEHook()
    const setVolume = vi.fn()

    ;(global.fetch as any).mockResolvedValueOnce({ ok: false })

    renderHook(() => useVolumeSSE({ selectedUserId: 'user123', setVolume }))

    vi.advanceTimersByTime(150)
    setVolume.mockClear()

    MockEventSource.instance!.emit('volume', '60')

    expect(setVolume).toHaveBeenCalledWith(60)
  })

  it('handles globalVolume SSE event with float value', async () => {
    const useVolumeSSE = await useVolumeSSEHook()
    const setVolume = vi.fn()

    ;(global.fetch as any).mockResolvedValueOnce({ ok: false })

    renderHook(() => useVolumeSSE({ selectedUserId: 'user123', setVolume }))

    vi.advanceTimersByTime(150)
    setVolume.mockClear()

    MockEventSource.instance!.emit('globalVolume', '82.7')

    expect(setVolume).toHaveBeenCalledWith(83) // Math.round(82.7)
  })

  it('handles onmessage event', async () => {
    const useVolumeSSE = await useVolumeSSEHook()
    const setVolume = vi.fn()

    ;(global.fetch as any).mockResolvedValueOnce({ ok: false })

    renderHook(() => useVolumeSSE({ selectedUserId: 'user123', setVolume }))

    vi.advanceTimersByTime(150)
    setVolume.mockClear()

    MockEventSource.instance!.emit('message', '45')

    expect(setVolume).toHaveBeenCalledWith(45)
  })

  it('closes EventSource on unmount', async () => {
    const useVolumeSSE = await useVolumeSSEHook()
    const setVolume = vi.fn()

    ;(global.fetch as any).mockResolvedValueOnce({ ok: false })

    const { unmount } = renderHook(() =>
      useVolumeSSE({ selectedUserId: 'user123', setVolume })
    )

    vi.advanceTimersByTime(150)
    const es = MockEventSource.instance!

    unmount()

    expect(es.closed).toBe(true)
  })

  it('clears pending SSE timeout on unmount before 100ms', async () => {
    const useVolumeSSE = await useVolumeSSEHook()
    const setVolume = vi.fn()

    ;(global.fetch as any).mockResolvedValueOnce({ ok: false })

    const { unmount } = renderHook(() =>
      useVolumeSSE({ selectedUserId: 'user123', setVolume })
    )

    // Unmount before the 100ms SSE timer fires
    unmount()
    vi.advanceTimersByTime(200)

    // EventSource should never have been created
    expect(MockEventSource.instance).toBeNull()
  })
})
