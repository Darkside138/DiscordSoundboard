import { render, screen, fireEvent } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { SettingsMenu } from '../components/SettingsMenu'

describe('SettingsMenu', () => {
  const baseProps = () => ({
    x: 50,
    y: 60,
    onClose: vi.fn(),
    theme: 'dark' as const,
    popularCount: 10,
    recentCount: 5,
    onPopularCountChange: vi.fn(),
    onRecentCountChange: vi.fn(),
    onThemeChange: vi.fn(),
  })

  it('renders headings and inputs with provided values', () => {
    render(<SettingsMenu {...baseProps()} />)
    expect(screen.getByText(/settings/i)).toBeInTheDocument()
    const popInput = screen.getByLabelText(/popular sounds count/i) as HTMLInputElement
    expect(popInput.value).toBe('10')
    const recentInput = screen.getByLabelText(/recently added count/i) as HTMLInputElement
    expect(recentInput.value).toBe('5')
  })

  it('toggles theme via buttons', async () => {
    const props = baseProps()
    render(<SettingsMenu {...props} />)
    await userEvent.click(screen.getByRole('button', { name: /light/i }))
    expect(props.onThemeChange).toHaveBeenCalledWith('light')

    await userEvent.click(screen.getByRole('button', { name: /dark/i }))
    expect(props.onThemeChange).toHaveBeenCalledWith('dark')
  })

  it('closes via Done button', async () => {
    const props = baseProps()
    render(<SettingsMenu {...props} />)
    await userEvent.click(screen.getByRole('button', { name: /done/i }))
    expect(props.onClose).toHaveBeenCalled()
  })

  it('closes on outside click and Escape', async () => {
    const props = baseProps()
    render(<SettingsMenu {...props} />)

    // Effects register listeners immediately in this component
    await userEvent.click(document.body)
    expect(props.onClose).toHaveBeenCalled()

    const props2 = baseProps()
    render(<SettingsMenu {...props2} />)
    fireEvent.keyDown(document, { key: 'Escape' })
    expect(props2.onClose).toHaveBeenCalled()
  })
})
