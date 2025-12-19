import { useMemo } from 'react';
import { DiscordUser } from '../utils/auth';

export interface Permissions {
  upload: boolean;
  delete: boolean;
  manageUsers: boolean;
  editSounds: boolean;
  playSounds: boolean;
  downloadSounds: boolean;
}

export function usePermissions(user: DiscordUser | null): Permissions {
  return useMemo(() => {
    if (!user || !user.permissions) {
      // Default permissions when not logged in
      return {
        upload: false,
        delete: false,
        manageUsers: false,
        editSounds: false,
        playSounds: true,  // Allow playing sounds without auth
        downloadSounds: true,  // Allow downloading without auth
      };
    }

    return user.permissions;
  }, [user]);
}

export function hasPermission(user: DiscordUser | null, permission: keyof Permissions): boolean {
  if (!user || !user.permissions) {
    // Allow certain actions without auth
    if (permission === 'playSounds' || permission === 'downloadSounds') {
      return true;
    }
    return false;
  }

  return user.permissions[permission] || false;
}

export function hasAnyPermission(user: DiscordUser | null, permissions: (keyof Permissions)[]): boolean {
  return permissions.some(permission => hasPermission(user, permission));
}

export function hasAllPermissions(user: DiscordUser | null, permissions: (keyof Permissions)[]): boolean {
  return permissions.every(permission => hasPermission(user, permission));
}
