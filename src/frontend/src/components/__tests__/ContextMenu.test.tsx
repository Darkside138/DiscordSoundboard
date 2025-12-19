import { render, screen, fireEvent, act } from '@testing-library/react'
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { ContextMenu } from '../ContextMenu'

// Mock toast from sonner
const toastSuccess = vi.fn()
const toastError = vi.fn()
vi.mock('sonner@2.0.3', () => ({
  toast: {
    success: (...args: any[]) => toastSuccess(...args),
    error: (...args: any[]) => toastError(...args),
  },
}), { virtual: true })

// Mock fetchWithAuth (must resolve to the same module as the component import)
const fetchWithAuthMock = vi.fn()
vi.mock('../../utils/api', () => ({
  fetchWithAuth: (...args: any[]) => fetchWithAuthMock(...args),
}))

const baseProps = () => ({
  x: 100,
  y: 100,
  onClose: vi.fn(),
  onFavorite: vi.fn(),
  onDelete: vi.fn(),
  onDownload: vi.fn(),
  onPlayLocally: vi.fn(),
  isFavorite: false as boolean,
  theme: 'dark' as const,
  timesPlayed: 3,
  dateAdded: '2024-01-15T00:00:00Z',
  volumeOffset: 10,
  soundId: 'sound-123',
  displayName: null as string | null,
  category: 'memes',
  canEditSounds: true,
  canDeleteSounds: true,
})

beforeEach(() => {
  fetchWithAuthMock.mockReset()
  toastSuccess.mockReset()
  toastError.mockReset()
})

afterEach(() => {
  vi.useRealTimers()
  vi.restoreAllMocks()
})

describe('ContextMenu - basic actions', () => {
  it('renders and triggers favorite, download, play locally, and delete actions', () => {
    const props = baseProps()
    render(<ContextMenu {...props} />)

    // Favorite label depends on isFavorite
    expect(screen.getByRole('button', { name: /add to favorites/i })).toBeInTheDocument()

    fireEvent.click(screen.getByRole('button', { name: /add to favorites/i }))
    expect(props.onFavorite).toHaveBeenCalledTimes(1)

    fireEvent.click(screen.getByRole('button', { name: /download sound/i }))
    expect(props.onDownload).toHaveBeenCalledTimes(1)
    expect(props.onClose).toHaveBeenCalledTimes(1)

    fireEvent.click(screen.getByRole('button', { name: /play locally/i }))
    expect(props.onPlayLocally).toHaveBeenCalledTimes(1)
    expect(props.onClose).toHaveBeenCalledTimes(2)

    fireEvent.click(screen.getByRole('button', { name: /delete sound/i }))
    expect(props.onDelete).toHaveBeenCalledTimes(1)
    expect(props.onClose).toHaveBeenCalledTimes(3)
  })

  it('closes on outside click and on Escape key', async () => {
    vi.useFakeTimers()
    const props = baseProps()
    render(<ContextMenu {...props} />)

    // Wait for setTimeout to attach listeners
    await act(async () => {
      vi.runAllTimers()
    })

    // Outside click
    fireEvent.mouseDown(document.body)
    expect(props.onClose).toHaveBeenCalledTimes(1)

    // Escape
    fireEvent.keyDown(document, { key: 'Escape' })
    expect(props.onClose).toHaveBeenCalledTimes(2)
  })
})

describe('ContextMenu - volume offset updates', () => {
  it('does not call API when releasing with unchanged value', async () => {
    const props = baseProps()
    render(<ContextMenu {...props} />)
    const slider = screen.getByRole('slider') as HTMLInputElement
    // Initial value equals lastSentValueRef
    expect(slider.value).toBe(String(props.volumeOffset))
    // Release without change
    fireEvent.mouseUp(slider)
    expect(fetchWithAuthMock).not.toHaveBeenCalled()
  })

  it('calls API on change+release and handles success toast', async () => {
    const props = baseProps()
    fetchWithAuthMock.mockResolvedValue({ ok: true })
    render(<ContextMenu {...props} />)
    const slider = screen.getByRole('slider') as HTMLInputElement

    fireEvent.change(slider, { target: { value: '15' } })
    expect(slider.value).toBe('15')
    await act(async () => {
      fireEvent.mouseUp(slider)
    })

    expect(fetchWithAuthMock).toHaveBeenCalledTimes(1)
    expect(toastSuccess).toHaveBeenCalledWith(expect.stringMatching(/volume offset updated/i))
  })

  it('throttles rapid duplicate API calls within 300ms', async () => {
    const props = baseProps()
    fetchWithAuthMock.mockResolvedValue({ ok: true })
    // Control time
    const nowSpy = vi.spyOn(Date, 'now')
    nowSpy.mockReturnValue(1000)
    render(<ContextMenu {...props} />)
    const slider = screen.getByRole('slider') as HTMLInputElement

    // First change + release at t=1000
    fireEvent.change(slider, { target: { value: '20' } })
    await act(async () => { fireEvent.mouseUp(slider) })

    // Immediate second release within 300ms -> blocked
    nowSpy.mockReturnValue(1150)
    fireEvent.change(slider, { target: { value: '21' } })
    await act(async () => { fireEvent.mouseUp(slider) })

    // After 300ms threshold -> allowed
    nowSpy.mockReturnValue(1400)
    fireEvent.change(slider, { target: { value: '22' } })
    await act(async () => { fireEvent.mouseUp(slider) })

    expect(fetchWithAuthMock).toHaveBeenCalledTimes(2)
  })

  it('reverts value and shows error on API failure', async () => {
    const props = baseProps()
    fetchWithAuthMock.mockResolvedValue({ ok: false, status: 500, statusText: 'err' })
    render(<ContextMenu {...props} />)
    const slider = screen.getByRole('slider') as HTMLInputElement

    fireEvent.change(slider, { target: { value: '25' } })
    await act(async () => { fireEvent.mouseUp(slider) })

    // Should show error toast and revert to original value
    expect(toastError).toHaveBeenCalledWith(expect.stringMatching(/failed to update volume offset/i))
    expect(slider.value).toBe(String(props.volumeOffset))
  })
})

describe('ContextMenu - display name editing', () => {
  it('enters edit mode, saves successfully, and shows new name', async () => {
    const props = baseProps()
    fetchWithAuthMock.mockResolvedValue({ ok: true })
    render(<ContextMenu {...props} />)

    // Initially shows placeholder
    const clickable = screen.getByText(/click to set name/i)
    fireEvent.click(clickable)

    const input = screen.getByPlaceholderText(/enter display name/i) as HTMLInputElement
    fireEvent.change(input, { target: { value: 'Nice Name' } })

    const saveBtn = screen.getByRole('button', { name: /save/i })
    await act(async () => {
      fireEvent.click(saveBtn)
    })

    expect(fetchWithAuthMock).toHaveBeenCalledTimes(1)
    expect(toastSuccess).toHaveBeenCalledWith(expect.stringMatching(/display name updated/i))
    // After save, edit UI closes and displays new name
    expect(screen.getByText('Nice Name')).toBeInTheDocument()
  })

  it('cancels editing using Cancel button and Escape key', async () => {
    const props = { ...baseProps(), displayName: 'Existing' }
    render(<ContextMenu {...props} />)

    // Enter edit mode
    fireEvent.click(screen.getByText('Existing'))
    const input = screen.getByDisplayValue('Existing') as HTMLInputElement

    // Change then cancel via button
    fireEvent.change(input, { target: { value: 'Changed' } })
    const cancelBtn = screen.getByRole('button', { name: /cancel/i })
    fireEvent.click(cancelBtn)
    expect(screen.getByText('Existing')).toBeInTheDocument()

    // Enter edit mode again and cancel via Escape
    fireEvent.click(screen.getByText('Existing'))
    const input2 = screen.getByDisplayValue('Existing') as HTMLInputElement
    fireEvent.keyDown(input2, { key: 'Escape' })
    expect(screen.getByText('Existing')).toBeInTheDocument()
  })

  it('shows error and reverts on failed save', async () => {
    const props = { ...baseProps(), displayName: 'Start' }
    fetchWithAuthMock.mockResolvedValue({ ok: false, status: 500, statusText: 'bad' })
    render(<ContextMenu {...props} />)

    fireEvent.click(screen.getByText('Start'))
    const input = screen.getByDisplayValue('Start') as HTMLInputElement
    fireEvent.change(input, { target: { value: 'New Name' } })
    await act(async () => {
      fireEvent.keyDown(input, { key: 'Enter' })
    })

    expect(fetchWithAuthMock).toHaveBeenCalled()
    expect(toastError).toHaveBeenCalledWith(expect.stringMatching(/failed to update display name/i))
    // Reverted back to original
    expect(screen.getByText('Start')).toBeInTheDocument()
  })
})

describe('ContextMenu - position adjustment', () => {
  it('adjusts position to keep within viewport', () => {
    // Small viewport
    ;(globalThis as any).innerWidth = 300
    ;(globalThis as any).innerHeight = 300

    const props = { ...baseProps(), x: 260, y: 260 }
    // Mock before render so effect uses it
    const rectSpy = vi
      .spyOn(HTMLElement.prototype as any, 'getBoundingClientRect')
      .mockReturnValue({ width: 200, height: 200, top: 0, left: 0, right: 0, bottom: 0, x: 0, y: 0, toJSON: () => {} } as any)

    const { container } = render(<ContextMenu {...props} />)
    const menuDiv = container.firstElementChild as HTMLElement

    // Force effect by re-rendering with same props (effect runs after mount)
    // In RTL, effects run automatically; just assert style was adjusted
    // Since window width is 300 and menu width 200, right margin of 50 -> newX should be 300-200-50 = 50
    // For height: newY should be 300-200-10 = 90
    expect(menuDiv.style.left).toBe('50px')
    expect(menuDiv.style.top).toBe('90px')

    rectSpy.mockRestore()
  })
})
