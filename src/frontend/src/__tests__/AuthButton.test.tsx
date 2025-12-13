import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { AuthButton } from '../components/AuthButton'

describe('AuthButton', () => {
  const onLogin = vi.fn()
  const onLogout = vi.fn()

  beforeEach(() => {
    onLogin.mockClear()
    onLogout.mockClear()
  })

  it('renders Login when no user and calls onLogin on click', async () => {
    render(
      <AuthButton user={null} onLogin={onLogin} onLogout={onLogout} theme="dark" />
    )

    const btn = screen.getByRole('button', { name: /login/i })
    expect(btn).toBeInTheDocument()
    await userEvent.click(btn)
    expect(onLogin).toHaveBeenCalledTimes(1)
  })

  it('renders user avatar/name and logout button when user present', async () => {
    const user = {
      id: '1234567890',
      username: 'testuser',
      discriminator: '1234',
      avatar: null,
      globalName: 'Test User',
    }

    render(
      <AuthButton user={user} onLogin={onLogin} onLogout={onLogout} theme="light" />
    )

    expect(screen.getByText('Test User')).toBeInTheDocument()
    const logoutBtn = screen.getByRole('button', { name: /logout/i })
    await userEvent.click(logoutBtn)
    expect(onLogout).toHaveBeenCalledTimes(1)
  })
})
