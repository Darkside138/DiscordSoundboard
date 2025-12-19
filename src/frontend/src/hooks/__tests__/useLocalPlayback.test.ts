import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest'
import { renderHook, act } from '@testing-library/react'
import { useLocalPlayback } from '../useLocalPlayback'

// Mock config
vi.mock('../../config', () => ({
  API_ENDPOINTS: {
    AUDIO_FILE: 'http://localhost/api/audio'
  }
}))

// Mock Audio constructor
class MockAudio {
  src: string
  private listeners: Record<string, Function[]> = {}

  constructor(src: string) {
    this.src = src
    ;(MockAudio as any).instances.push(this)
  }

  play() {
    return Promise.resolve()
  }

  pause() {
    // Mock pause
  }

  addEventListener(event: string, handler: Function) {
    if (!this.listeners[event]) {
      this.listeners[event] = []
    }
    this.listeners[event].push(handler)
  }

  removeEventListener(event: string, handler: Function) {
    if (this.listeners[event]) {
      this.listeners[event] = this.listeners[event].filter(h => h !== handler)
    }
  }

  triggerEvent(event: string) {
    if (this.listeners[event]) {
      this.listeners[event].forEach(handler => handler())
    }
  }

  static instances: MockAudio[] = []
  static reset() {
    MockAudio.instances = []
  }
}

describe('useLocalPlayback', () => {
  beforeEach(() => {
    MockAudio.reset()
    global.Audio = MockAudio as any
    vi.spyOn(console, 'error').mockImplementation(() => {})
  })

  afterEach(() => {
    vi.restoreAllMocks()
  })

  it('initializes with no playing sound', () => {
    const { result } = renderHook(() => useLocalPlayback())
    expect(result.current.locallyPlayingSoundId).toBeNull()
  })

  it('plays a sound locally', async () => {
    const { result } = renderHook(() => useLocalPlayback())

    await act(async () => {
      await result.current.playLocalSound('sound123')
    })

    expect(result.current.locallyPlayingSoundId).toBe('sound123')
    expect(MockAudio.instances.length).toBe(1)
    expect(MockAudio.instances[0].src).toBe('http://localhost/api/audio/sound123')
  })

  it('stops currently playing sound when playing a new one', async () => {
    const { result } = renderHook(() => useLocalPlayback())

    await act(async () => {
      await result.current.playLocalSound('sound1')
    })

    const firstAudio = MockAudio.instances[0]
    const pauseSpy = vi.spyOn(firstAudio, 'pause')

    await act(async () => {
      await result.current.playLocalSound('sound2')
    })

    expect(pauseSpy).toHaveBeenCalled()
    expect(result.current.locallyPlayingSoundId).toBe('sound2')
    expect(MockAudio.instances.length).toBe(2)
  })

  it('stops local playback', async () => {
    const { result } = renderHook(() => useLocalPlayback())

    await act(async () => {
      await result.current.playLocalSound('sound123')
    })

    expect(result.current.locallyPlayingSoundId).toBe('sound123')

    const audio = MockAudio.instances[0]
    const pauseSpy = vi.spyOn(audio, 'pause')

    act(() => {
      result.current.stopLocalSound()
    })

    expect(pauseSpy).toHaveBeenCalled()
    expect(result.current.locallyPlayingSoundId).toBeNull()
  })

  it('clears state when audio ends', async () => {
    const { result } = renderHook(() => useLocalPlayback())

    await act(async () => {
      await result.current.playLocalSound('sound123')
    })

    expect(result.current.locallyPlayingSoundId).toBe('sound123')

    const audio = MockAudio.instances[0]

    act(() => {
      audio.triggerEvent('ended')
    })

    expect(result.current.locallyPlayingSoundId).toBeNull()
  })

  it('handles audio error events', async () => {
    const { result } = renderHook(() => useLocalPlayback())

    await act(async () => {
      await result.current.playLocalSound('sound123')
    })

    const audio = MockAudio.instances[0]

    act(() => {
      audio.triggerEvent('error')
    })

    expect(result.current.locallyPlayingSoundId).toBeNull()
    expect(console.error).toHaveBeenCalledWith('Error playing sound locally')
  })

  it('handles play() promise rejection', async () => {
    const mockPlayError = new Error('Play failed')
    const OriginalAudio = global.Audio

    class FailingAudio extends MockAudio {
      play() {
        return Promise.reject(mockPlayError)
      }
    }

    global.Audio = FailingAudio as any

    const { result } = renderHook(() => useLocalPlayback())

    await act(async () => {
      await result.current.playLocalSound('sound123')
    })

    expect(console.error).toHaveBeenCalledWith('Error playing sound locally:', mockPlayError)
    expect(result.current.locallyPlayingSoundId).toBeNull()

    global.Audio = OriginalAudio
  })
})