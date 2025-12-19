import { describe, it, expect } from 'vitest'
import { renderHook } from '@testing-library/react'
import { usePermissions, hasPermission, hasAnyPermission, hasAllPermissions } from '../userPermission'
import type { DiscordUser } from '../../utils/auth'

const createUser = (permissions: any): DiscordUser => ({
  userId: 'user123',
  username: 'testuser',
  discriminator: '1234',
  avatar: null,
  globalName: null,
  permissions
})

describe('userPermission', () => {
  describe('usePermissions', () => {
    it('returns default permissions when user is null', () => {
      const { result } = renderHook(() => usePermissions(null))

      expect(result.current).toEqual({
        upload: false,
        delete: false,
        manageUsers: false,
        editSounds: false,
        playSounds: true,
        downloadSounds: true
      })
    })

    it('returns default permissions when user has no permissions', () => {
      const user = createUser(undefined)
      const { result } = renderHook(() => usePermissions(user))

      expect(result.current).toEqual({
        upload: false,
        delete: false,
        manageUsers: false,
        editSounds: false,
        playSounds: true,
        downloadSounds: true
      })
    })

    it('returns user permissions when authenticated', () => {
      const permissions = {
        upload: true,
        delete: true,
        manageUsers: false,
        editSounds: true,
        playSounds: true,
        downloadSounds: true
      }
      const user = createUser(permissions)
      const { result } = renderHook(() => usePermissions(user))

      expect(result.current).toEqual(permissions)
    })

    it('memoizes permissions based on user', () => {
      const user = createUser({ upload: true, delete: false, manageUsers: false, editSounds: false, playSounds: true, downloadSounds: true })
      const { result, rerender } = renderHook(({ user }) => usePermissions(user), { initialProps: { user } })

      const firstResult = result.current

      // Rerender with same user
      rerender({ user })
      expect(result.current).toBe(firstResult) // Same reference

      // Rerender with different user
      const newUser = createUser({ upload: false, delete: true, manageUsers: false, editSounds: false, playSounds: true, downloadSounds: true })
      rerender({ user: newUser })
      expect(result.current).not.toBe(firstResult) // Different reference
    })
  })

  describe('hasPermission', () => {
    it('returns false for restricted permissions when user is null', () => {
      expect(hasPermission(null, 'upload')).toBe(false)
      expect(hasPermission(null, 'delete')).toBe(false)
      expect(hasPermission(null, 'manageUsers')).toBe(false)
      expect(hasPermission(null, 'editSounds')).toBe(false)
    })

    it('returns true for public permissions when user is null', () => {
      expect(hasPermission(null, 'playSounds')).toBe(true)
      expect(hasPermission(null, 'downloadSounds')).toBe(true)
    })

    it('returns true when user has the permission', () => {
      const user = createUser({
        upload: true,
        delete: false,
        manageUsers: false,
        editSounds: true,
        playSounds: true,
        downloadSounds: true
      })

      expect(hasPermission(user, 'upload')).toBe(true)
      expect(hasPermission(user, 'editSounds')).toBe(true)
    })

    it('returns false when user lacks the permission', () => {
      const user = createUser({
        upload: false,
        delete: false,
        manageUsers: false,
        editSounds: false,
        playSounds: true,
        downloadSounds: true
      })

      expect(hasPermission(user, 'upload')).toBe(false)
      expect(hasPermission(user, 'delete')).toBe(false)
    })
  })

  describe('hasAnyPermission', () => {
    it('returns true if user has at least one permission', () => {
      const user = createUser({
        upload: true,
        delete: false,
        manageUsers: false,
        editSounds: false,
        playSounds: true,
        downloadSounds: true
      })

      expect(hasAnyPermission(user, ['upload', 'delete'])).toBe(true)
      expect(hasAnyPermission(user, ['delete', 'manageUsers'])).toBe(false)
    })

    it('returns true for public permissions even with null user', () => {
      expect(hasAnyPermission(null, ['upload', 'playSounds'])).toBe(true)
      expect(hasAnyPermission(null, ['playSounds', 'downloadSounds'])).toBe(true)
    })

    it('returns false if user has none of the permissions', () => {
      const user = createUser({
        upload: false,
        delete: false,
        manageUsers: false,
        editSounds: false,
        playSounds: true,
        downloadSounds: true
      })

      expect(hasAnyPermission(user, ['upload', 'delete', 'manageUsers'])).toBe(false)
    })
  })

  describe('hasAllPermissions', () => {
    it('returns true if user has all permissions', () => {
      const user = createUser({
        upload: true,
        delete: true,
        manageUsers: false,
        editSounds: true,
        playSounds: true,
        downloadSounds: true
      })

      expect(hasAllPermissions(user, ['upload', 'delete'])).toBe(true)
      expect(hasAllPermissions(user, ['upload', 'editSounds'])).toBe(true)
    })

    it('returns false if user lacks any permission', () => {
      const user = createUser({
        upload: true,
        delete: false,
        manageUsers: false,
        editSounds: true,
        playSounds: true,
        downloadSounds: true
      })

      expect(hasAllPermissions(user, ['upload', 'delete'])).toBe(false)
    })

    it('returns true for only public permissions with null user', () => {
      expect(hasAllPermissions(null, ['playSounds', 'downloadSounds'])).toBe(true)
      expect(hasAllPermissions(null, ['playSounds', 'upload'])).toBe(false)
    })

    it('returns true for empty permissions array', () => {
      expect(hasAllPermissions(null, [])).toBe(true)
    })
  })
})