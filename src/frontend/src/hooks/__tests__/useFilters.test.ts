import { describe, it, expect, beforeEach, afterEach } from 'vitest'
import { renderHook, act } from '@testing-library/react'
import { useFilters } from '../useFilters'
import type { Sound } from '../useSounds'

const createSound = (overrides: Partial<Sound> = {}): Sound => ({
  id: 'sound1',
  name: 'Test Sound',
  category: 'music',
  timesPlayed: 0,
  dateAdded: new Date().toISOString(),
  favorite: false,
  ...overrides
})

describe('useFilters', () => {
  beforeEach(() => {
    localStorage.clear()
  })

  afterEach(() => {
    localStorage.clear()
  })

  const mockSounds: Sound[] = [
    createSound({ id: '1', name: 'Alpha', category: 'music', timesPlayed: 100, dateAdded: '2023-01-01' }),
    createSound({ id: '2', name: 'Beta', category: 'sfx', timesPlayed: 50, dateAdded: '2023-06-01' }),
    createSound({ id: '3', name: 'Gamma', category: 'music', timesPlayed: 75, dateAdded: '2023-12-01' }),
    createSound({ id: '4', name: 'Delta', category: 'voice', timesPlayed: 25, dateAdded: '2024-01-01' }),
    createSound({ id: '5', name: 'Epsilon', category: 'music', timesPlayed: 10, dateAdded: '2024-02-01' })
  ]

  it('initializes with default filter values', () => {
    const { result } = renderHook(() => useFilters(mockSounds, new Set()))

    expect(result.current.selectedCategory).toBe('all')
    expect(result.current.activeFilter).toBe('none')
    expect(result.current.searchQuery).toBe('')
    expect(result.current.popularCount).toBe(10)
    expect(result.current.recentCount).toBe(10)
  })

  it('loads popular and recent counts from localStorage', () => {
    localStorage.setItem('soundboard-popular-count', '15')
    localStorage.setItem('soundboard-recent-count', '20')

    const { result } = renderHook(() => useFilters(mockSounds, new Set()))

    expect(result.current.popularCount).toBe(15)
    expect(result.current.recentCount).toBe(20)
  })

  it('saves popular and recent counts to localStorage', () => {
    const { result } = renderHook(() => useFilters(mockSounds, new Set()))

    act(() => {
      result.current.setPopularCount(25)
      result.current.setRecentCount(30)
    })

    expect(localStorage.getItem('soundboard-popular-count')).toBe('25')
    expect(localStorage.getItem('soundboard-recent-count')).toBe('30')
  })

  it('extracts unique categories from sounds', () => {
    const { result } = renderHook(() => useFilters(mockSounds, new Set()))

    expect(result.current.categories).toEqual(['all', 'music', 'sfx', 'voice'])
  })

  it('filters sounds by category', () => {
    const { result } = renderHook(() => useFilters(mockSounds, new Set()))

    act(() => {
      result.current.setSelectedCategory('music')
    })

    expect(result.current.filteredSounds).toHaveLength(3)
    expect(result.current.filteredSounds.every(s => s.category === 'music')).toBe(true)
  })

  it('filters sounds by search query', () => {
    const { result } = renderHook(() => useFilters(mockSounds, new Set()))

    act(() => {
      result.current.setSearchQuery('alp')
    })

    expect(result.current.filteredSounds).toHaveLength(1)
    expect(result.current.filteredSounds[0].name).toBe('Alpha')
  })

  it('filters sounds by favorites', () => {
    const favorites = new Set(['2', '4'])
    const { result } = renderHook(() => useFilters(mockSounds, favorites))

    act(() => {
      result.current.setActiveFilter('favorites')
    })

    expect(result.current.filteredSounds).toHaveLength(2)
    expect(result.current.filteredSounds.map(s => s.id)).toEqual(['2', '4'])
  })

  it('calculates top sounds by timesPlayed (popular filter)', () => {
    const { result } = renderHook(() => useFilters(mockSounds, new Set()))

    // Top 3 by timesPlayed: Alpha (100), Gamma (75), Beta (50)
    act(() => {
      result.current.setPopularCount(3)
      result.current.setActiveFilter('popular')
    })

    expect(result.current.filteredSounds).toHaveLength(3)
    expect(result.current.top10SoundIds.has('1')).toBe(true) // Alpha
    expect(result.current.top10SoundIds.has('3')).toBe(true) // Gamma
    expect(result.current.top10SoundIds.has('2')).toBe(true) // Beta
  })

  it('excludes favorites from popular sounds', () => {
    const favorites = new Set(['1']) // Alpha is favorite
    const { result } = renderHook(() => useFilters(mockSounds, favorites))

    act(() => {
      result.current.setPopularCount(3)
    })

    // Top should exclude Alpha (favorite): Gamma, Beta, Delta
    expect(result.current.top10SoundIds.has('1')).toBe(false) // Alpha excluded
    expect(result.current.top10SoundIds.has('3')).toBe(true) // Gamma
    expect(result.current.top10SoundIds.has('2')).toBe(true) // Beta
  })

  it('calculates recent sounds by dateAdded', () => {
    const { result } = renderHook(() => useFilters(mockSounds, new Set()))

    act(() => {
      // Set popularCount to 3 so not all sounds are "popular"
      result.current.setPopularCount(3)
      result.current.setRecentCount(2)
    })

    // Most recent (excluding top 3 popular): Delta (2024-01), Epsilon (2024-02)
    // Top 3 popular are: Alpha (100), Gamma (75), Beta (50)
    // Recent should be next 2 by date: Epsilon (2024-02), Delta (2024-01)
    expect(result.current.recentlyAddedIds.has('5')).toBe(true) // Epsilon
    expect(result.current.recentlyAddedIds.has('4')).toBe(true) // Delta

    act(() => {
      result.current.setActiveFilter('recent')
    })

    expect(result.current.filteredSounds.length).toBe(2)
  })

  it('excludes favorites and popular from recent sounds', () => {
    const favorites = new Set(['5']) // Epsilon is favorite
    const { result } = renderHook(() => useFilters(mockSounds, favorites))

    act(() => {
      result.current.setPopularCount(1) // Alpha will be top
      result.current.setRecentCount(2)
    })

    // Recent should exclude Epsilon (favorite) and not overlap with popular
    // Should get Delta and Gamma (next most recent after excluding Epsilon)
    expect(result.current.recentlyAddedIds.has('5')).toBe(false) // Epsilon excluded
    expect(result.current.recentlyAddedIds.has('1')).toBe(false) // Alpha is in popular
  })

  it('combines multiple filters (category + search)', () => {
    const { result } = renderHook(() => useFilters(mockSounds, new Set()))

    act(() => {
      result.current.setSelectedCategory('music')
      result.current.setSearchQuery('a')
    })

    // Music category with 'a' in name: Alpha, Gamma
    expect(result.current.filteredSounds).toHaveLength(2)
    expect(result.current.filteredSounds.map(s => s.name)).toContain('Alpha')
    expect(result.current.filteredSounds.map(s => s.name)).toContain('Gamma')
  })

  it('sorts filtered sounds alphabetically', () => {
    const { result } = renderHook(() => useFilters(mockSounds, new Set()))

    expect(result.current.filteredSounds.map(s => s.name)).toEqual([
      'Alpha',
      'Beta',
      'Delta',
      'Epsilon',
      'Gamma'
    ])
  })

  it('returns all sounds when filter is "none"', () => {
    const { result } = renderHook(() => useFilters(mockSounds, new Set()))

    expect(result.current.filteredSounds).toHaveLength(5)
  })

  it('handles empty sounds array', () => {
    const { result } = renderHook(() => useFilters([], new Set()))

    expect(result.current.filteredSounds).toHaveLength(0)
    expect(result.current.categories).toEqual(['all'])
    expect(result.current.top10SoundIds.size).toBe(0)
    expect(result.current.recentlyAddedIds.size).toBe(0)
  })
})