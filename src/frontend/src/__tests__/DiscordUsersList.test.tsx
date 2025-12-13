import { render, screen, waitFor, cleanup } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { DiscordUsersList } from '../components/DiscordUsersList'

// Simple Mock for EventSource to use in jsdom
class MockEventSource {
  static instances: MockEventSource[] = []
  url: string
  readyState = 1
  onopen: ((this: EventSource, ev: Event) => any) | null = null
  onerror: ((this: EventSource, ev: Event) => any) | null = null
  private listeners: Record<string, ((e: MessageEvent<any>) => void)[]> = {}
  closed = false

  constructor(url: string) {
    this.url = url
    MockEventSource.instances.push(this)
    // Fire open asynchronously to mimic browser behavior
    setTimeout(() => {
      this.onopen && this.onopen(new Event('open'))
    }, 0)
  }

  addEventListener(type: string, cb: (e: MessageEvent<any>) => void) {
    this.listeners[type] = this.listeners[type] || []
    this.listeners[type].push(cb)
  }

  dispatch(type: string, data: any) {
    const event = new MessageEvent(type, { data })
    ;(this.listeners[type] || []).forEach((cb) => cb(event))
  }

  close() {
    this.readyState = 2
    this.closed = true
  }
}

// Attach mocks
beforeAll(() => {
  // @ts-expect-error override
  global.EventSource = MockEventSource as any
})

beforeEach(() => {
  vi.useRealTimers()
  MockEventSource.instances = []
  // @ts-expect-error override
  global.fetch = vi.fn()
})

afterEach(() => {
  cleanup()
  vi.resetAllMocks()
})

const makeUser = (over: Partial<{ id: string; username: string; entranceSound: string|null; leaveSound: string|null; selected: boolean; status: string; onlineStatus: string; inVoice: boolean; volume?: number }>) => ({
  id: over.id ?? '1',
  username: over.username ?? 'Alice',
  entranceSound: over.entranceSound ?? null,
  leaveSound: over.leaveSound ?? null,
  selected: over.selected ?? false,
  status: over.status ?? 'OK',
  onlineStatus: over.onlineStatus ?? 'ONLINE',
  inVoice: over.inVoice ?? false,
  volume: over.volume,
})

const respond = (obj: any, ok = true) => Promise.resolve({ ok, json: () => Promise.resolve(obj) } as Response)

describe('DiscordUsersList', () => {
  it('fetches initial users, selects the selected one, and updates volume/playback', async () => {
    const u1 = makeUser({ id: 'u1', username: 'One', selected: true, inVoice: true, volume: 75 })
    const u2 = makeUser({ id: 'u2', username: 'Two' })
    ;(global.fetch as any).mockImplementation(() => respond({ content: [u2, u1], page: {} }))

    const onUserSelect = vi.fn()
    const onVolumeUpdate = vi.fn()
    const onPlaybackEnabledChange = vi.fn()

    render(
      <DiscordUsersList
        theme="dark"
        onUserSelect={onUserSelect}
        selectedUserId={null}
        onVolumeUpdate={onVolumeUpdate}
        onPlaybackEnabledChange={onPlaybackEnabledChange}
      />
    )

    // Users appear
    await screen.findByText('One')
    expect(screen.getByText('Two')).toBeInTheDocument()

    // Badge shows count
    expect(screen.getByText('2')).toBeInTheDocument()

    // Callbacks from auto-selection
    expect(onUserSelect).toHaveBeenCalledWith('u1')
    expect(onVolumeUpdate).toHaveBeenCalledWith(75)
    expect(onPlaybackEnabledChange).toHaveBeenCalledWith(true)
  })

  it('when no user is selected in initial data, disables playback', async () => {
    const u1 = makeUser({ id: 'a', username: 'A', selected: false })
    const u2 = makeUser({ id: 'b', username: 'B', selected: false })
    ;(global.fetch as any).mockImplementation(() => respond({ content: [u1, u2], page: {} }))

    const onUserSelect = vi.fn()
    const onVolumeUpdate = vi.fn()
    const onPlaybackEnabledChange = vi.fn()

    render(
      <DiscordUsersList
        theme="dark"
        onUserSelect={onUserSelect}
        selectedUserId={null}
        onVolumeUpdate={onVolumeUpdate}
        onPlaybackEnabledChange={onPlaybackEnabledChange}
      />
    )

    await screen.findByText('A')
    expect(onUserSelect).not.toHaveBeenCalled()
    expect(onPlaybackEnabledChange).toHaveBeenCalledWith(false)
  })

  it('updates via SSE: selects user, updates volume and playback', async () => {
    ;(global.fetch as any).mockImplementation(() => respond({ content: [], page: {} }))

    const onUserSelect = vi.fn()
    const onVolumeUpdate = vi.fn()
    const onPlaybackEnabledChange = vi.fn()

    render(
      <DiscordUsersList
        theme="dark"
        onUserSelect={onUserSelect}
        selectedUserId={null}
        onVolumeUpdate={onVolumeUpdate}
        onPlaybackEnabledChange={onPlaybackEnabledChange}
      />
    )

    // Wait until EventSource is created
    await waitFor(() => expect(MockEventSource.instances.length).toBe(1))
    const es = MockEventSource.instances[0]

    const sseUser = makeUser({ id: 's1', username: 'SSE User', selected: true, inVoice: false, volume: 50 })
    es.dispatch('discordUsers', JSON.stringify([sseUser]))

    // UI updates and callbacks fired
    await screen.findByText('SSE User')
    expect(onUserSelect).toHaveBeenCalledWith('s1')
    expect(onVolumeUpdate).toHaveBeenCalledWith(50)
    expect(onPlaybackEnabledChange).toHaveBeenCalledWith(false)
  })

  it('clicking a user toggles selection and sets playback based on inVoice', async () => {
    const a = makeUser({ id: 'a', username: 'Alpha', inVoice: true })
    const b = makeUser({ id: 'b', username: 'Beta', inVoice: false })
    ;(global.fetch as any).mockImplementation(() => respond({ content: [a, b], page: {} }))

    const onUserSelect = vi.fn()
    const onPlaybackEnabledChange = vi.fn()

    render(
      <DiscordUsersList
        theme="dark"
        onUserSelect={onUserSelect}
        selectedUserId={null}
        onVolumeUpdate={vi.fn()}
        onPlaybackEnabledChange={onPlaybackEnabledChange}
      />
    )

    await screen.findByText('Alpha')
    await userEvent.click(screen.getByText('Alpha'))
    expect(onUserSelect).toHaveBeenCalledWith('a')
    expect(onPlaybackEnabledChange).toHaveBeenCalledWith(true)
  })

  it('shows error message when initial fetch fails', async () => {
    ;(global.fetch as any).mockResolvedValue({ ok: false })

    render(
      <DiscordUsersList
        theme="dark"
        onUserSelect={vi.fn()}
        selectedUserId={null}
        onVolumeUpdate={vi.fn()}
        onPlaybackEnabledChange={vi.fn()}
      />
    )

    await screen.findByText('Failed to load users')
  })

  it('closes EventSource on unmount (cleanup)', async () => {
    ;(global.fetch as any).mockImplementation(() => respond({ content: [], page: {} }))

    const { unmount } = render(
      <DiscordUsersList
        theme="dark"
        onUserSelect={vi.fn()}
        selectedUserId={null}
        onVolumeUpdate={vi.fn()}
        onPlaybackEnabledChange={vi.fn()}
      />
    )

    await waitFor(() => expect(MockEventSource.instances.length).toBe(1))
    const es = MockEventSource.instances[0]
    const closeSpy = vi.spyOn(es, 'close')
    unmount()
    expect(closeSpy).toHaveBeenCalled()
  })
})
