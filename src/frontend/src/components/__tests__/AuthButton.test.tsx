import { render, screen, fireEvent } from '@testing-library/react'
import { describe, it, expect, vi } from 'vitest'
import { AuthButton } from '../AuthButton'

const baseUser = {
  id: '1234567890',
  username: 'testuser',
  discriminator: '1234',
  avatar: null as string | null,
  globalName: null as string | null,
}

describe('AuthButton', () => {
  it('renders login state and triggers onLogin', () => {
    const onLogin = vi.fn()
    const onLogout = vi.fn()

    render(
      <AuthButton user={null} onLogin={onLogin} onLogout={onLogout} theme="dark" />
    )

    const button = screen.getByRole('button', { name: /login/i })
    expect(button).toBeInTheDocument()
    fireEvent.click(button)
    expect(onLogin).toHaveBeenCalledTimes(1)
  })

  it('renders user state with avatar and triggers onLogout', () => {
    const onLogin = vi.fn()
    const onLogout = vi.fn()

    render(
      <AuthButton
        user={{ ...baseUser, globalName: 'Tester' }}
        onLogin={onLogin}
        onLogout={onLogout}
        theme="light"
      />
    )

    // Shows display name
    expect(screen.getByText('Tester')).toBeInTheDocument()

    // Has an avatar image with alt of username
    const img = screen.getByRole('img', { name: /testuser/i }) as HTMLImageElement
    expect(img).toBeInTheDocument()
    expect(img.src).toMatch(/discordapp\.com\/embed\/avatars\//)

    // Logout icon button triggers onLogout
    const buttons = screen.getAllByRole('button')
    // The component renders only one button in this state (logout)
    fireEvent.click(buttons[0])
    expect(onLogout).toHaveBeenCalledTimes(1)
  })

  it('uses user.username if globalName is null', () => {
    const onLogin = vi.fn()
    const onLogout = vi.fn()

    render(
      <AuthButton
        user={baseUser}
        onLogin={onLogin}
        onLogout={onLogout}
        theme="dark"
      />
    )

    expect(screen.getByText('testuser')).toBeInTheDocument()
  })
})
