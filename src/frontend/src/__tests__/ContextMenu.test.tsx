import { render, screen, fireEvent } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { ContextMenu } from '../components/ContextMenu'

describe('ContextMenu', () => {
  const baseProps = () => ({
    x: 100,
    y: 100,
    onClose: vi.fn(),
    onFavorite: vi.fn(),
    onDelete: vi.fn(),
    onDownload: vi.fn(),
    onPlayLocally: vi.fn(),
    isFavorite: false,
    theme: 'dark' as const,
    timesPlayed: 3,
    dateAdded: '2024-01-15T00:00:00.000Z',
    volumeOffset: 0,
    soundId: 'sound-123',
    displayName: 'My Sound',
    category: 'memes',
    canEditSounds: true,
    canDeleteSounds: true,
  })

  beforeEach(() => {
    vi.restoreAllMocks()
  })

  it('renders core actions and information', () => {
    render(<ContextMenu {...baseProps()} />)
    expect(screen.getByText(/add to favorites/i)).toBeInTheDocument()
    expect(screen.getByText(/download sound/i)).toBeInTheDocument()
    expect(screen.getByText(/play locally/i)).toBeInTheDocument()

    expect(screen.getByText(/played:/i)).toBeInTheDocument()
    expect(screen.getByText(/category:/i)).toBeInTheDocument()
    expect(screen.getByText(/added:/i)).toBeInTheDocument()
    expect(screen.getByText(/id:/i)).toBeInTheDocument()

    // Delete only visible when allowed
    expect(screen.getByText(/delete sound/i)).toBeInTheDocument()
  })

  it('calls action handlers on click', async () => {
    const props = baseProps()
    render(<ContextMenu {...props} />)

    await userEvent.click(screen.getByText(/add to favorites/i))
    expect(props.onFavorite).toHaveBeenCalledTimes(1)

    await userEvent.click(screen.getByText(/download sound/i))
    expect(props.onDownload).toHaveBeenCalledTimes(1)
    expect(props.onClose).toHaveBeenCalled() // closes after download

    await userEvent.click(screen.getByText(/play locally/i))
    expect(props.onPlayLocally).toHaveBeenCalledTimes(1)

    await userEvent.click(screen.getByText(/delete sound/i))
    expect(props.onDelete).toHaveBeenCalledTimes(1)
  })

  it('hides edit and delete sections based on permissions', () => {
    const props = { ...baseProps(), canEditSounds: false, canDeleteSounds: false }
    render(<ContextMenu {...props} />)

    // No slider if cannot edit
    expect(screen.queryByRole('slider')).not.toBeInTheDocument()
    // No delete button if cannot delete
    expect(screen.queryByText(/delete sound/i)).not.toBeInTheDocument()
  })

  it('updates volume offset only on release and calls backend with new value', async () => {
    const props = baseProps()
    const fetchSpy = vi.spyOn(global, 'fetch' as any).mockResolvedValue({ ok: true } as any)
    render(<ContextMenu {...props} />)

    const slider = screen.getByRole('slider') as HTMLInputElement

    // Changing value should not call fetch yet
    fireEvent.change(slider, { target: { value: '10' } })
    expect(fetchSpy).not.toHaveBeenCalled()

    // Release (mouse up) should trigger
    fireEvent.mouseUp(slider)
    expect(fetchSpy).toHaveBeenCalledTimes(1)
    const urlCalled: string = (fetchSpy.mock.calls[0][0] as string)
    expect(urlCalled).toContain('/api/soundFiles/sound-123')
    expect(urlCalled).toContain('volumeOffsetPercentage=10')

    // Releasing again without change should not trigger (duplicate/same value)
    fireEvent.mouseUp(slider)
    expect(fetchSpy).toHaveBeenCalledTimes(1)
  })

  it('cancel edit reverts and does not call backend', async () => {
    const props = { ...baseProps(), displayName: null }
    const fetchSpy = vi.spyOn(global, 'fetch' as any).mockResolvedValue({ ok: true } as any)
    render(<ContextMenu {...props} />)

    await userEvent.click(screen.getByText(/click to set name/i))
    const input = screen.getByPlaceholderText(/enter display name/i)
    await userEvent.type(input, 'Temp Name')
    await userEvent.click(screen.getByTitle(/cancel/i))

    expect(fetchSpy).not.toHaveBeenCalled()
    // Back to non-editing view
    expect(screen.getByText(/click to set name/i)).toBeInTheDocument()
  })

  it('closes on outside click and Escape', async () => {
    const props = baseProps()
    render(<ContextMenu {...props} />)

    // Wait a macrotask so the component registers document listeners (it uses setTimeout)
    await new Promise((r) => setTimeout(r, 0))

    await userEvent.click(document.body)
    expect(props.onClose).toHaveBeenCalled()

    // Escape
    const props2 = baseProps()
    render(<ContextMenu {...props2} />)
    await new Promise((r) => setTimeout(r, 0))
    fireEvent.keyDown(document, { key: 'Escape' })
    expect(props2.onClose).toHaveBeenCalled()
  })
})
