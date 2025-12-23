import { useState, useEffect } from 'react';
import { API_ENDPOINTS } from '../config';
import { fetchWithAuth } from '../utils/api';

interface UserWithRole {
  id: string;
  username: string;
  avatarUrl?: string;
  assignedRole?: string;
  roleAssignedAt?: string;
  roleAssignedBy?: string;
  guildInAudioName?: string;
  guildInAudioId?: string;
}

export function useRoleManagement(canManageUsers: boolean = false) {
  const [users, setUsers] = useState<UserWithRole[]>([]);
  const [loading, setLoading] = useState(false);

  const fetchUsers = async () => {
    // Don't fetch if user doesn't have permission
    if (!canManageUsers) {
      setLoading(false);
      return;
    }

    setLoading(true);
    try {
      // Fetch all users by using a large page size
      const response = await fetchWithAuth(`${API_ENDPOINTS.USER_ROLES}?page=0&size=1000`);
      if (response.ok) {
        const data = await response.json();
        setUsers(data.content || []);
      }
    } catch (error) {
      console.error('Failed to fetch users:', error);
    } finally {
      setLoading(false);
    }
  };

  const assignRole = async (userId: string, role: string): Promise<boolean> => {
    try {
      const response = await fetchWithAuth(API_ENDPOINTS.ASSIGN_ROLE(userId), {
        method: 'PUT',
        body: new URLSearchParams({ role })
      });
      if (response.ok) {
        await fetchUsers(); // Refresh list
        return true;
      }
      return false;
    } catch (error) {
      console.error('Failed to assign role:', error);
      return false;
    }
  };

  const removeRole = async (userId: string): Promise<boolean> => {
    try {
      const response = await fetchWithAuth(API_ENDPOINTS.REMOVE_ROLE(userId), {
        method: 'DELETE'
      });
      if (response.ok) {
        await fetchUsers(); // Refresh list
        return true;
      }
      return false;
    } catch (error) {
      console.error('Failed to remove role:', error);
      return false;
    }
  };

  useEffect(() => {
    fetchUsers();
  }, [canManageUsers]);

  return {
    users,
    loading,
    assignRole,
    removeRole,
    refreshUsers: fetchUsers
  };
}
