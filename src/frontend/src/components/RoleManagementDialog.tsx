import React, { useState, useMemo } from 'react';
import { Shield, X, Search, Key } from 'lucide-react';
import { useRoleManagement } from '../hooks/useRoleManagement';
import { usePermissionManagement, AVAILABLE_PERMISSIONS } from '../hooks/usePermissionManagement';

interface RoleManagementDialogProps {
  isOpen: boolean;
  onClose: () => void;
  currentUserId: string;
  onRoleChanged: () => void;
  canManageUsers?: boolean;
}

export function RoleManagementDialog({
  isOpen,
  onClose,
  currentUserId,
  onRoleChanged,
  canManageUsers = false
}: RoleManagementDialogProps) {
  const { users, loading, assignRole, removeRole } = useRoleManagement(canManageUsers);
  const { getPermissionsForRole, isRoleConfigured, setPermissionsForRole, resetRoleToDefaults, loading: permLoading } = usePermissionManagement(canManageUsers);
  const [selectedUserId, setSelectedUserId] = useState<string | null>(null);
  const [selectedRole, setSelectedRole] = useState<string>('user');
  const [searchQuery, setSearchQuery] = useState<string>('');
  const [activeTab, setActiveTab] = useState<'roles' | 'permissions'>('roles');
  const [selectedPermissionRole, setSelectedPermissionRole] = useState<string>('admin');

  const roles = ['admin', 'dj', 'moderator', 'user'];
  const permissionRoles = ['default', 'admin', 'dj', 'moderator', 'user'];

  const filteredUsers = useMemo(() => {
    return users
      .filter(user => {
        const matchesSearch = !searchQuery ||
          user.username.toLowerCase().includes(searchQuery.toLowerCase());
        return matchesSearch;
      })
      .sort((a, b) => a.username.localeCompare(b.username));
  }, [users, searchQuery]);

  const handleAssignRole = async () => {
    if (!selectedUserId) return;

    const success = await assignRole(selectedUserId, selectedRole);
    if (success) {
      onRoleChanged();
      setSelectedUserId(null);
    }
  };

  const handleRemoveRole = async (userId: string) => {
    const success = await removeRole(userId);
    if (success) {
      onRoleChanged();
    }
  };

  const handleTogglePermission = async (role: string, permission: string) => {
    const currentPermissions = getPermissionsForRole(role);
    const hasPermission = currentPermissions.includes(permission);

    let newPermissions: string[];
    if (hasPermission) {
      newPermissions = currentPermissions.filter(p => p !== permission);
    } else {
      newPermissions = [...currentPermissions, permission];
    }

    const success = await setPermissionsForRole(role, newPermissions);
    if (success) {
      onRoleChanged();
    }
  };

  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black bg-opacity-50">
      <div className="w-full max-w-5xl h-[90vh] rounded-lg shadow-2xl flex flex-col bg-white text-gray-900 dark:bg-gray-800 dark:text-white">
        {/* Header with Tabs */}
        <div className="border-b border-gray-200 dark:border-gray-700">
          <div className="flex items-center justify-between p-6">
            <h2 className="text-2xl flex items-center gap-2">
              <Shield className="w-6 h-6" />
              Role & Permission Management
            </h2>
            <button
              onClick={onClose}
              className="p-2 rounded-lg transition-colors hover:bg-gray-100 text-gray-600 hover:text-gray-900 dark:hover:bg-gray-700 dark:text-gray-400 dark:hover:text-white"
              aria-label="Close"
            >
              <X className="w-6 h-6" />
            </button>
          </div>

          {/* Tabs */}
          <div className="flex px-6 gap-2">
            <button
              onClick={() => setActiveTab('roles')}
              className={`px-4 py-2 font-medium border-b-2 transition-colors cursor-pointer ${
                activeTab === 'roles'
                  ? 'border-blue-600 text-blue-600 dark:border-blue-500 dark:text-blue-400'
                  : 'border-transparent text-gray-600 hover:text-gray-900 dark:text-gray-400 dark:hover:text-gray-300'
              }`}
            >
              <Shield className="w-4 h-4 inline mr-2" />
              User Roles
            </button>
            <button
              onClick={() => setActiveTab('permissions')}
              className={`px-4 py-2 font-medium border-b-2 transition-colors cursor-pointer ${
                activeTab === 'permissions'
                  ? 'border-blue-600 text-blue-600 dark:border-blue-500 dark:text-blue-400'
                  : 'border-transparent text-gray-600 hover:text-gray-900 dark:text-gray-400 dark:hover:text-gray-300'
              }`}
            >
              <Key className="w-4 h-4 inline mr-2" />
              Role Permissions
            </button>
          </div>
        </div>

        {/* Content */}
        <div className="flex-1 overflow-auto p-6 [&::-webkit-scrollbar]:w-2 [&::-webkit-scrollbar-track]:bg-gray-200 dark:[&::-webkit-scrollbar-track]:bg-gray-700 [&::-webkit-scrollbar-thumb]:bg-gray-400 dark:[&::-webkit-scrollbar-thumb]:bg-gray-600 [&::-webkit-scrollbar-thumb]:rounded-full [&::-webkit-scrollbar-thumb]:hover:bg-gray-500">
          {activeTab === 'roles' && (
            <>
          {/* Search */}
          <div className="mb-6">
            <div className="relative">
              <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-gray-400 dark:text-gray-500" />
              <input
                type="text"
                placeholder="Search by username..."
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                className="w-full pl-10 pr-4 py-2 rounded border bg-white border-gray-300 text-gray-900 placeholder-gray-500 dark:bg-gray-700 dark:border-gray-600 dark:text-white dark:placeholder-gray-400"
              />
            </div>
            <p className="text-sm mt-2 text-gray-600 dark:text-gray-400">
              Showing {filteredUsers.length} of {users.length} users (sorted alphabetically)
            </p>
          </div>

          {/* Assign Role Section */}
        <div className="mb-6 p-4 border rounded border-gray-300 dark:border-gray-700">
          <h3 className="font-semibold mb-2 text-gray-900 dark:text-white">
            Assign Role
          </h3>
          <div className="flex gap-2">
            <select
              value={selectedUserId || ''}
              onChange={(e) => setSelectedUserId(e.target.value)}
              className="flex-1 px-3 py-2 rounded border bg-white border-gray-300 text-gray-900 dark:bg-gray-700 dark:border-gray-600 dark:text-white"
            >
              <option value="">Select User...</option>
              {filteredUsers.map(u => (
                <option key={u.id} value={u.id}>{u.username}</option>
              ))}
            </select>

            <select
              value={selectedRole}
              onChange={(e) => setSelectedRole(e.target.value)}
              className="px-3 py-2 rounded border bg-white border-gray-300 text-gray-900 dark:bg-gray-700 dark:border-gray-600 dark:text-white"
            >
              {roles.map(r => (
                <option key={r} value={r}>{r}</option>
              ))}
            </select>

            <button
              onClick={handleAssignRole}
              disabled={!selectedUserId}
              className="px-4 py-2 bg-blue-600 text-white rounded disabled:opacity-50 hover:bg-blue-700"
            >
              Assign
            </button>
          </div>
        </div>

          {/* Current Roles List */}
          <div>
            <h3 className="font-semibold mb-2 text-gray-900 dark:text-white">
              Current Role Assignments
            </h3>
            <div className="space-y-2">
              {filteredUsers.filter(u => u.assignedRole).map(user => (
                <div
                  key={user.id}
                  className="flex items-center justify-between p-3 rounded bg-gray-100 dark:bg-gray-700"
                >
                  <div>
                    <p className="font-medium text-gray-900 dark:text-white">
                      {user.username}
                    </p>
                    <p className="text-sm text-gray-600 dark:text-gray-400">
                      Role: {user.assignedRole}
                    </p>
                  </div>
                  <button
                    onClick={() => handleRemoveRole(user.id)}
                    disabled={user.id === currentUserId && user.assignedRole === 'admin'}
                    className="px-3 py-1 bg-red-600 text-white rounded text-sm disabled:opacity-50 hover:bg-red-700"
                    title={user.id === currentUserId && user.assignedRole === 'admin' ? "Cannot remove your own admin role" : "Remove role"}
                  >
                    Remove
                  </button>
                </div>
              ))}
              {filteredUsers.filter(u => u.assignedRole).length === 0 && (
                <p className="text-center py-4 text-gray-600 dark:text-gray-400">
                  {users.filter(u => u.assignedRole).length > 0
                    ? 'No roles match the current search'
                    : 'No roles assigned yet'}
                </p>
              )}
            </div>
          </div>
            </>
          )}

          {activeTab === 'permissions' && (
            <>
              <div className="mb-6 p-4 border rounded border-gray-300 dark:border-gray-700">
                <h3 className="font-semibold mb-2 text-gray-900 dark:text-white">
                  Manage Permissions by Role
                </h3>
                <p className="text-sm mb-4 text-gray-600 dark:text-gray-400">
                  Configure which permissions are assigned to each role. Changes override the default application.yml configuration.
                  If no custom permissions are set, the role will use the default permissions from configuration.
                </p>

                {/* Role Selector */}
                <div className="mb-4">
                  <label className="block text-sm font-medium mb-2 text-gray-700 dark:text-gray-300">
                    Select Role:
                  </label>
                  <select
                    value={selectedPermissionRole}
                    onChange={(e) => setSelectedPermissionRole(e.target.value)}
                    className="w-full px-3 py-2 rounded border bg-white border-gray-300 text-gray-900 dark:bg-gray-700 dark:border-gray-600 dark:text-white"
                  >
                    {permissionRoles.map(role => (
                      <option key={role} value={role}>
                        {role === 'default' ? 'Default (Unauthenticated Users)' : role}
                      </option>
                    ))}
                  </select>
                </div>

                {/* Admin warning message */}
                {selectedPermissionRole === 'admin' && (
                  <div className="mb-4 p-3 rounded bg-yellow-50 border border-yellow-200 dark:bg-yellow-900/20 dark:border-yellow-700">
                    <p className="text-sm text-yellow-800 dark:text-yellow-400">
                      <strong>Admin Role Protected:</strong> The admin role always has all permissions and cannot be customized.
                      This ensures system administrators always maintain full access.
                    </p>
                  </div>
                )}

                {/* Permission Checkboxes */}
                <div className="space-y-2">
                  <label className="block text-sm font-medium mb-2 text-gray-700 dark:text-gray-300">
                    Permissions:
                  </label>
                  {AVAILABLE_PERMISSIONS.map(permission => {
                    const isAdminRole = selectedPermissionRole === 'admin';
                    const currentPermissions = getPermissionsForRole(selectedPermissionRole);
                    const isChecked = isAdminRole || currentPermissions.includes(permission);
                    const isConfigured = isRoleConfigured(selectedPermissionRole);
                    const hasNoPermissions = isConfigured && currentPermissions.length === 0;

                    return (
                      <label
                        key={permission}
                        className={`flex items-center p-3 rounded transition-colors gap-4 ${
                          isAdminRole
                            ? 'bg-gray-100/50 dark:bg-gray-700/50'
                            : 'hover:bg-gray-100 cursor-pointer dark:hover:bg-gray-700'
                        }`}
                      >
                        <input
                          type="checkbox"
                          checked={isChecked}
                          onChange={() => handleTogglePermission(selectedPermissionRole, permission)}
                          disabled={isAdminRole}
                          className="w-4 h-4 rounded text-blue-600 focus:ring-blue-500 focus:ring-2 disabled:opacity-50 disabled:cursor-not-allowed flex-shrink-0"
                        />
                        <span className={`ml-2 flex-1 text-gray-900 dark:text-white ${isAdminRole ? 'opacity-75' : ''}`}>
                          {permission}
                        </span>
                        {isAdminRole ? (
                          <span className="text-xs px-2 py-1 rounded bg-green-100 text-green-800 dark:bg-green-900 dark:text-green-300">
                            Always enabled
                          </span>
                        ) : hasNoPermissions ? (
                          <span className="text-xs px-2 py-1 rounded bg-orange-100 text-orange-800 dark:bg-orange-900 dark:text-orange-300">
                            Custom: No permissions
                          </span>
                        ) : !isConfigured ? (
                          <span className="text-xs px-2 py-1 rounded bg-gray-200 text-gray-600 dark:bg-gray-700 dark:text-gray-400">
                            Using defaults from config
                          </span>
                        ) : null}
                      </label>
                    );
                  })}
                </div>

                {selectedPermissionRole !== 'admin' && isRoleConfigured(selectedPermissionRole) && (
                  <button
                    onClick={async () => {
                      const success = await resetRoleToDefaults(selectedPermissionRole);
                      if (success) {
                        onRoleChanged();
                      }
                    }}
                    className="mt-4 px-4 py-2 rounded text-sm bg-red-500 hover:bg-red-600 text-white dark:bg-red-600 dark:hover:bg-red-700"
                  >
                    Reset to Default Permissions
                  </button>
                )}
              </div>

              {/* Summary of All Custom Permissions */}
              <div>
                <h3 className="font-semibold mb-2 text-gray-900 dark:text-white">
                  Custom Permission Summary
                </h3>
                <p className="text-sm mb-4 text-gray-600 dark:text-gray-400">
                  Roles with custom permissions configured in the database.
                </p>
                <div className="space-y-3">
                  {permissionRoles.map(role => {
                    const customPermissions = getPermissionsForRole(role);
                    const isConfigured = isRoleConfigured(role);

                    if (!isConfigured) return null;

                    return (
                      <div
                        key={role}
                        className="p-4 rounded bg-gray-100 dark:bg-gray-700"
                      >
                        <div className="flex items-center justify-between mb-2">
                          <span className="font-medium text-gray-900 dark:text-white">
                            {role === 'default' ? 'Default (Unauthenticated Users)' : role}
                          </span>
                          <span className={`text-xs px-2 py-1 rounded ${
                            customPermissions.length === 0
                              ? 'bg-orange-100 text-orange-800 dark:bg-orange-900 dark:text-orange-300'
                              : 'bg-blue-100 text-blue-800 dark:bg-blue-900 dark:text-blue-300'
                          }`}>
                            {customPermissions.length === 0
                              ? 'No permissions'
                              : `${customPermissions.length} custom permission${customPermissions.length !== 1 ? 's' : ''}`}
                          </span>
                        </div>
                        {customPermissions.length > 0 ? (
                          <div className="flex flex-wrap gap-2">
                            {customPermissions.map(perm => (
                              <span
                                key={perm}
                                className="text-xs px-2 py-1 rounded bg-gray-200 text-gray-700 dark:bg-gray-600 dark:text-gray-200"
                              >
                                {perm}
                              </span>
                            ))}
                          </div>
                        ) : (
                          <p className="text-sm text-gray-600 dark:text-gray-400">
                            This role has been explicitly configured to have no permissions.
                          </p>
                        )}
                      </div>
                    );
                  })}
                  {permissionRoles.every(role => !isRoleConfigured(role)) && (
                    <p className="text-center py-4 text-gray-600 dark:text-gray-400">
                      No custom permissions configured. All roles are using default permissions from application.yml.
                    </p>
                  )}
                </div>
              </div>
            </>
          )}
        </div>
      </div>
    </div>
  );
}