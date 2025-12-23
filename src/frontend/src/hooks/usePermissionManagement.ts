import { useState, useEffect } from 'react';
import { API_ENDPOINTS } from '../config';
import { fetchWithAuth } from '../utils/api';

export interface RolePermission {
  id: number;
  role: string;
  permission: string;
  assignedAt: string;
  assignedBy: string;
}

// All available permissions in the system
export const AVAILABLE_PERMISSIONS = [
  'upload',
  'delete-sounds',
  'edit-sounds',
  'manage-users',
  'play-sounds',
  'download-sounds',
  'update-volume'
];

export function usePermissionManagement(canManageUsers: boolean = false) {
  const [rolePermissions, setRolePermissions] = useState<RolePermission[]>([]);
  const [configuredRoles, setConfiguredRoles] = useState<Set<string>>(new Set());
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const fetchRolePermissions = async () => {
    // Don't fetch if user doesn't have permission
    if (!canManageUsers) {
      setLoading(false);
      return;
    }

    try {
      setLoading(true);
      setError(null);

      // Fetch both permissions and configured roles
      const [permsResponse, configuredResponse] = await Promise.all([
        fetchWithAuth(API_ENDPOINTS.ROLE_PERMISSIONS),
        fetchWithAuth(API_ENDPOINTS.ROLE_PERMISSIONS_CONFIGURED)
      ]);

      const permsData = await permsResponse.json();
      const configuredData = await configuredResponse.json();

      setRolePermissions(permsData);
      setConfiguredRoles(new Set(configuredData));
    } catch (err) {
      console.error('Failed to fetch role permissions:', err);
      setError('Failed to load permissions');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchRolePermissions();
  }, [canManageUsers]);

  const getPermissionsForRole = (role: string): string[] => {
    return rolePermissions
      .filter(rp => rp.role === role)
      .map(rp => rp.permission);
  };

  const isRoleConfigured = (role: string): boolean => {
    return configuredRoles.has(role);
  };

  const setPermissionsForRole = async (role: string, permissions: string[]): Promise<boolean> => {
    try {
      const response = await fetchWithAuth(
        API_ENDPOINTS.ROLE_PERMISSIONS_FOR_ROLE(role),
        {
          method: 'PUT',
          headers: {
            'Content-Type': 'application/json',
          },
          body: JSON.stringify(permissions),
        }
      );

      if (response.ok) {
        await fetchRolePermissions(); // Refresh the list
        return true;
      }
      return false;
    } catch (err) {
      console.error('Failed to set permissions for role:', err);
      return false;
    }
  };

  const addPermissionToRole = async (role: string, permission: string): Promise<boolean> => {
    try {
      const response = await fetchWithAuth(
        API_ENDPOINTS.ADD_PERMISSION_TO_ROLE(role),
        {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
          },
          body: JSON.stringify({ permission }),
        }
      );

      if (response.ok) {
        await fetchRolePermissions(); // Refresh the list
        return true;
      }
      return false;
    } catch (err) {
      console.error('Failed to add permission to role:', err);
      return false;
    }
  };

  const removePermissionFromRole = async (role: string, permission: string): Promise<boolean> => {
    try {
      const response = await fetchWithAuth(
        API_ENDPOINTS.REMOVE_PERMISSION_FROM_ROLE(role, permission),
        {
          method: 'DELETE',
        }
      );

      if (response.ok) {
        await fetchRolePermissions(); // Refresh the list
        return true;
      }
      return false;
    } catch (err) {
      console.error('Failed to remove permission from role:', err);
      return false;
    }
  };

  const resetRoleToDefaults = async (role: string): Promise<boolean> => {
    try {
      const response = await fetchWithAuth(
        API_ENDPOINTS.RESET_ROLE_TO_DEFAULTS(role),
        {
          method: 'POST',
        }
      );

      if (response.ok) {
        await fetchRolePermissions(); // Refresh the list
        return true;
      }
      return false;
    } catch (err) {
      console.error('Failed to reset role to defaults:', err);
      return false;
    }
  };

  return {
    rolePermissions,
    loading,
    error,
    getPermissionsForRole,
    isRoleConfigured,
    setPermissionsForRole,
    addPermissionToRole,
    removePermissionFromRole,
    resetRoleToDefaults,
    refreshPermissions: fetchRolePermissions,
  };
}
