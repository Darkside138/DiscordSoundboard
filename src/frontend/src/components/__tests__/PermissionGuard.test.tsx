import { render, screen } from '@testing-library/react'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { PermissionGuard, PermissionTooltip } from '../PermissionGuard'

vi.mock('../../hooks/userPermission', () => {
  return {
    hasPermission: vi.fn(),
  }
})

const { hasPermission } = await import('../../hooks/userPermission') as any

describe('PermissionGuard', () => {
  beforeEach(() => {
    ;(hasPermission as any).mockReset()
  })

  it('renders children when permission granted', () => {
    ;(hasPermission as any).mockReturnValue(true)
    render(
      <PermissionGuard user={{} as any} permission="upload">
        <button>Upload</button>
      </PermissionGuard>
    )
    expect(screen.getByRole('button', { name: /upload/i })).toBeInTheDocument()
  })

  it('renders fallback when provided and permission denied', () => {
    ;(hasPermission as any).mockReturnValue(false)
    render(
      <PermissionGuard user={null} permission="delete" fallback={<span>Nope</span>}>
        <button>Delete</button>
      </PermissionGuard>
    )
    expect(screen.getByText('Nope')).toBeInTheDocument()
  })

  it('renders locked UI when showLocked is true and permission denied', () => {
    ;(hasPermission as any).mockReturnValue(false)
    render(
      <PermissionGuard user={null} permission="manageUsers" showLocked lockedMessage="Locked feature" theme="light">
        <div>Secret</div>
      </PermissionGuard>
    )
    expect(screen.getByText('Locked feature')).toBeInTheDocument()
  })
})

describe('PermissionTooltip', () => {
  beforeEach(() => {
    ;(hasPermission as any).mockReset()
  })

  it('passes through children when allowed', () => {
    ;(hasPermission as any).mockReturnValue(true)
    render(
      <PermissionTooltip user={{} as any} permission="editSounds">
        <button>Save</button>
      </PermissionTooltip>
    )
    const btn = screen.getByRole('button', { name: /save/i })
    expect(btn).toBeInTheDocument()
    expect(btn).not.toBeDisabled()
  })

  it('wraps and disables when not allowed', () => {
    ;(hasPermission as any).mockReturnValue(false)
    render(
      <PermissionTooltip user={null} permission="editSounds" message="Need perms">
        <button>Save</button>
      </PermissionTooltip>
    )
    const btn = screen.getByRole('button', { name: /save/i })
    expect(btn).toBeDisabled()
    const wrapper = btn.parentElement as HTMLElement
    expect(wrapper).toHaveAttribute('title', expect.stringMatching(/need perms/i))
  })
})
