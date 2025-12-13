import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { PermissionGuard, PermissionTooltip } from '../components/PermissionGuard'

const makeUser = (perms?: any) => ({
  id: '1',
  username: 'u',
  discriminator: '0',
  avatar: null,
  globalName: 'User',
  permissions: perms,
})

describe('PermissionGuard', () => {
  it('renders children when user has permission', () => {
    const user = makeUser({ upload: true })
    render(
      <PermissionGuard user={user as any} permission="upload">
        <div data-testid="content">Secret</div>
      </PermissionGuard>
    )
    expect(screen.getByTestId('content')).toBeInTheDocument()
  })

  it('renders fallback when no permission and fallback provided', () => {
    const user = makeUser({ upload: false })
    render(
      <PermissionGuard user={user as any} permission="upload" fallback={<span>nope</span>}>
        <div>Secret</div>
      </PermissionGuard>
    )
    expect(screen.getByText('nope')).toBeInTheDocument()
    expect(screen.queryByText('Secret')).not.toBeInTheDocument()
  })

  it('renders locked view when showLocked is true', () => {
    const user = makeUser({ upload: false })
    render(
      <PermissionGuard user={user as any} permission="upload" showLocked theme="dark" />
    )
    const locked = screen.getByTitle(/requires the upload permission/i)
    expect(locked).toBeInTheDocument()
  })
})

describe('PermissionTooltip', () => {
  it('disables child and shows tooltip when no permission', async () => {
    const user = makeUser({ upload: false })
    const onClick = vi.fn()
    render(
      <PermissionTooltip user={user as any} permission="upload">
        <button onClick={onClick}>Action</button>
      </PermissionTooltip>
    )
    const wrapper = screen.getByTitle(/need the upload permission/i)
    expect(wrapper).toBeInTheDocument()
    const btn = screen.getByRole('button', { name: 'Action' }) as HTMLButtonElement
    expect(btn.disabled).toBe(true)
    await userEvent.click(btn)
    expect(onClick).not.toHaveBeenCalled()
  })

  it('passes through child unchanged when has permission', async () => {
    const user = makeUser({ upload: true })
    const onClick = vi.fn()
    render(
      <PermissionTooltip user={user as any} permission="upload">
        <button onClick={onClick}>Action</button>
      </PermissionTooltip>
    )
    const btn = screen.getByRole('button', { name: 'Action' }) as HTMLButtonElement
    expect(btn.disabled).toBeFalsy()
    await userEvent.click(btn)
    expect(onClick).toHaveBeenCalledTimes(1)
  })
})
