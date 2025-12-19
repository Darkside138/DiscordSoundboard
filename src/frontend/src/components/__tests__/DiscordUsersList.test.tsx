import React from 'react'
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { render, screen, waitFor, act } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { DiscordUsersList } from '../DiscordUsersList'

// Mock API utils used by the component
vi.mock('../utils/api', () => {
  return {
    getAuthHeaders: vi.fn(() => ({ Authorization: 'Bearer test' })),
  }
})

// Mock config endpoints
vi.mock('../config', () => {
  return {
    API_BASE_URL: 'http://localhost',
    API_ENDPOINTS: {
      DISCORD_USERS: 'http://localhost/api/discord/users',
    },
  }
})

// Helper to build a paged response
const paged = (content: any[], { number = 0, totalPages = 1, size = content.length, totalElements = content.length } = {}) => ({
  content,
  page: { number, totalPages, size, totalElements },
})

const mkUser = (over: Partial<any> = {}) => ({
  id: '1',
  username: 'Alpha',
  entranceSound: null,
  leaveSound: null,
  selected: false,
  status: 'online',
  onlineStatus: 'online',
  inVoice: true,
  volume: 80,
  guildInAudioId: 'g1',
  guildInAudioName: 'Guildy',
  channelName: 'General',
  ...over,
})

const mockFetch = (impl: (input: RequestInfo | URL, init?: RequestInit) => Promise<Response>) => {
  vi.stubGlobal('fetch', vi.fn(impl) as any)
}

const okJson = (obj: any) =>
  new Response(JSON.stringify(obj), {
    status: 200,
    headers: { 'Content-Type': 'application/json' },
  })

describe('DiscordUsersList', () => {
  const onUserSelect = vi.fn()
  const onVolumeUpdate = vi.fn()
  const onPlaybackEnabledChange = vi.fn()
  const onGuildIdChange = vi.fn()

  beforeEach(() => {
    vi.clearAllMocks()
  })

  afterEach(() => {
    (global.fetch as any)?.mockClear?.()
  })

  it('falls back to backend-selected user when no selectedUserId prop is provided', async () => {
    const users = [
      mkUser({ id: 'a', username: 'Alpha', selected: false, volume: 50, inVoice: false }),
      mkUser({ id: 'b', username: 'Bravo', selected: true, volume: 30, inVoice: true, guildInAudioId: 'g2' }),
    ]

    mockFetch(async () => okJson(paged(users)))

    render(
      <DiscordUsersList
        theme="dark"
        onUserSelect={onUserSelect}
        selectedUserId={null}
        onVolumeUpdate={onVolumeUpdate}
        onPlaybackEnabledChange={onPlaybackEnabledChange}
        onGuildIdChange={onGuildIdChange}
      />
    )

    await waitFor(() => expect(onUserSelect).toHaveBeenCalledWith('b'))
    expect(onVolumeUpdate).toHaveBeenCalledWith(30)
    expect(onPlaybackEnabledChange).toHaveBeenCalledWith(true)
    expect(onGuildIdChange).toHaveBeenCalledWith('g2')
  })

  it('uses explicit selectedUserId prop when provided and updates side effects only when changed', async () => {
    const users = [
      mkUser({ id: 'x', username: 'Xavier', volume: 70, inVoice: true, guildInAudioId: 'gx' }),
      mkUser({ id: 'y', username: 'Yankee', volume: 40, inVoice: false, guildInAudioId: 'gy' }),
    ]
    mockFetch(async () => okJson(paged(users)))

    const { rerender } = render(
      <DiscordUsersList
        theme="dark"
        onUserSelect={onUserSelect}
        selectedUserId={'x'}
        onVolumeUpdate={onVolumeUpdate}
        onPlaybackEnabledChange={onPlaybackEnabledChange}
        onGuildIdChange={onGuildIdChange}
      />
    )

    await waitFor(() => expect(onVolumeUpdate).toHaveBeenCalledWith(70))
    expect(onPlaybackEnabledChange).toHaveBeenCalledWith(true)
    expect(onGuildIdChange).toHaveBeenCalledWith('gx')

    onVolumeUpdate.mockClear()
    onPlaybackEnabledChange.mockClear()
    onGuildIdChange.mockClear()

    // No change to selected user -> no duplicate side-effects
    rerender(
      <DiscordUsersList
        theme="dark"
        onUserSelect={onUserSelect}
        selectedUserId={'x'}
        onVolumeUpdate={onVolumeUpdate}
        onPlaybackEnabledChange={onPlaybackEnabledChange}
        onGuildIdChange={onGuildIdChange}
      />
    )

    await act(async () => {})
    expect(onVolumeUpdate).not.toHaveBeenCalled()
    expect(onPlaybackEnabledChange).not.toHaveBeenCalled()
    expect(onGuildIdChange).not.toHaveBeenCalled()
  })

  it('falls back to first user if no backend-selected user present', async () => {
    const users = [
      mkUser({ id: 'a', username: 'Alpha', selected: false, volume: 55, inVoice: true, guildInAudioId: 'g1' }),
      mkUser({ id: 'b', username: 'Bravo', selected: false, volume: 25, inVoice: false, guildInAudioId: 'g2' }),
    ]
    mockFetch(async () => okJson(paged(users)))

    render(
      <DiscordUsersList
        theme="dark"
        onUserSelect={onUserSelect}
        selectedUserId={null}
        onVolumeUpdate={onVolumeUpdate}
        onPlaybackEnabledChange={onPlaybackEnabledChange}
        onGuildIdChange={onGuildIdChange}
      />
    )

    await waitFor(() => expect(onUserSelect).toHaveBeenCalledWith('a'))
    expect(onVolumeUpdate).toHaveBeenCalledWith(55)
    expect(onPlaybackEnabledChange).toHaveBeenCalledWith(true)
    expect(onGuildIdChange).toHaveBeenCalledWith('g1')
  })

  it('handles selectedUserId not found in response by disabling playback and clearing guild', async () => {
    const users = [mkUser({ id: 'a', selected: false, inVoice: false })]
    mockFetch(async () => okJson(paged(users)))

    render(
      <DiscordUsersList
        theme="dark"
        onUserSelect={onUserSelect}
        selectedUserId={'missing'}
        onVolumeUpdate={onVolumeUpdate}
        onPlaybackEnabledChange={onPlaybackEnabledChange}
        onGuildIdChange={onGuildIdChange}
      />
    )

    await waitFor(() => expect(onPlaybackEnabledChange).toHaveBeenCalledWith(false))
    expect(onGuildIdChange).toHaveBeenCalledWith(null)
  })
})
