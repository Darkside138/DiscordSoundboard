import { describe, it, expect, beforeEach, afterEach } from 'vitest'
import { renderHook, act } from '@testing-library/react'
import { useTheme } from '../useTheme'

describe('useTheme', () => {
  beforeEach(() => {
    localStorage.clear()
  })

  afterEach(() => {
    localStorage.clear()
  })

  it('initializes with dark theme by default', () => {
    const { result } = renderHook(() => useTheme())
    expect(result.current.theme).toBe('dark')
  })

  it('loads saved theme from localStorage on mount', () => {
    localStorage.setItem('soundboard-theme', 'light')
    const { result } = renderHook(() => useTheme())
    expect(result.current.theme).toBe('light')
  })

  it('saves theme to localStorage when changed', () => {
    const { result } = renderHook(() => useTheme())

    act(() => {
      result.current.setTheme('light')
    })

    expect(result.current.theme).toBe('light')
    expect(localStorage.getItem('soundboard-theme')).toBe('light')
  })

  it('toggles between light and dark themes', () => {
    const { result } = renderHook(() => useTheme())

    expect(result.current.theme).toBe('dark')

    act(() => {
      result.current.setTheme('light')
    })
    expect(result.current.theme).toBe('light')

    act(() => {
      result.current.setTheme('dark')
    })
    expect(result.current.theme).toBe('dark')
  })

  it('persists theme across remounts', () => {
    const { result: result1, unmount } = renderHook(() => useTheme())

    act(() => {
      result1.current.setTheme('light')
    })

    unmount()

    const { result: result2 } = renderHook(() => useTheme())
    expect(result2.current.theme).toBe('light')
  })
})