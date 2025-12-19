import { render, screen, fireEvent, waitFor } from '@testing-library/react'
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { SettingsMenu } from '../SettingsMenu'

describe('SettingsMenu', () => {
  const baseProps = {
    x: 100,
    y: 200,
    onClose: vi.fn(),
    theme: 'dark' as const,
    popularCount: 5,
    recentCount: 10,
    onPopularCountChange: vi.fn(),
    onRecentCountChange: vi.fn(),
    onThemeChange: vi.fn(),
  }

  beforeEach(() => {
    vi.restoreAllMocks()
  })

  afterEach(() => {
    vi.clearAllMocks()
  })

  it('fetches and displays bot version', async () => {
    vi.spyOn(global, 'fetch' as any).mockResolvedValue({ ok: true, text: () => Promise.resolve('1.2.3') })

    render(<SettingsMenu {...baseProps} />)

    // Initially shows Loading... then updates to fetched value
    expect(screen.getByText('Loading...')).toBeInTheDocument()
    await waitFor(() => expect(screen.getByText('1.2.3')).toBeInTheDocument())
  })

  it('changes theme via buttons', () => {
    render(<SettingsMenu {...baseProps} theme="light" />)

    fireEvent.click(screen.getByRole('button', { name: /dark/i }))
    expect(baseProps.onThemeChange).toHaveBeenCalledWith('dark')

    fireEvent.click(screen.getByRole('button', { name: /light/i }))
    expect(baseProps.onThemeChange).toHaveBeenCalledWith('light')
  })

  it('updates popular and recent counts within valid range', () => {
    render(<SettingsMenu {...baseProps} />)

    const inputs = screen.getAllByRole('spinbutton') as HTMLInputElement[]
    const popInput = inputs[0]
    const recentInput = inputs[1]

    fireEvent.change(popInput, { target: { value: '7' } })
    expect(baseProps.onPopularCountChange).toHaveBeenCalledWith(7)

    fireEvent.change(recentInput, { target: { value: '9' } })
    expect(baseProps.onRecentCountChange).toHaveBeenCalledWith(9)
  })

  it('closes on Escape key and outside click', () => {
    render(<SettingsMenu {...baseProps} />)

    fireEvent.keyDown(document, { key: 'Escape' })
    expect(baseProps.onClose).toHaveBeenCalled()

    vi.resetAllMocks()
    // Simulate outside click
    fireEvent.mouseDown(document)
    expect(baseProps.onClose).toHaveBeenCalled()
  })

  it('renders optional action buttons and triggers callbacks', () => {
    const onUploadClick = vi.fn()
    const onUsersClick = vi.fn()
    render(<SettingsMenu {...baseProps} canUpload canManageUsers onUploadClick={onUploadClick} onUsersClick={onUsersClick} />)

    fireEvent.click(screen.getByRole('button', { name: /upload sound/i }))
    expect(onUploadClick).toHaveBeenCalled()

    fireEvent.click(screen.getByRole('button', { name: /manage users/i }))
    expect(onUsersClick).toHaveBeenCalled()
  })
})
