import React, { useState, useEffect } from 'react';
import { X, ChevronLeft, ChevronRight, ArrowUpDown, ArrowUp, ArrowDown } from 'lucide-react';
import { API_ENDPOINTS } from '../config';

interface DiscordUser {
  id: string;
  username: string;
  entranceSound: string | null;
  leaveSound: string | null;
}

interface UsersOverlayProps {
  isOpen: boolean;
  onClose: () => void;
  theme: 'light' | 'dark';
  sounds: Array<{ id: string; name: string }>;
}

export function UsersOverlay({ isOpen, onClose, theme, sounds }: UsersOverlayProps) {
  const [users, setUsers] = useState<DiscordUser[]>([]);
  const [loading, setLoading] = useState(false);
  const [currentPage, setCurrentPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [pageSize] = useState(10);
  const [editingUserId, setEditingUserId] = useState<string | null>(null);
  const [editingField, setEditingField] = useState<'entranceSound' | 'leaveSound' | null>(null);
  const [editValue, setEditValue] = useState<string>('');
  const [sortBy, setSortBy] = useState<string | null>('username');
  const [sortDirection, setSortDirection] = useState<'asc' | 'desc'>('asc');

  // Sort sounds alphabetically by name
  const sortedSounds = [...sounds].sort((a, b) => a.name.localeCompare(b.name));

  useEffect(() => {
    if (isOpen) {
      fetchUsers(currentPage);
    }
  }, [isOpen, currentPage, sortBy, sortDirection]);

  const fetchUsers = async (page: number) => {
    setLoading(true);
    try {
      const params = new URLSearchParams({
        page: page.toString(),
        size: pageSize.toString()
      });

      if (sortBy) {
        params.append('sortBy', sortBy);
        params.append('sortDir', sortDirection);
      }

      const response = await fetch(
        `${API_ENDPOINTS.DISCORD_USERS}?${params.toString()}`,
        { mode: 'cors' }
      );

      if (!response.ok) {
        throw new Error('Failed to fetch users');
      }

      const data = await response.json();
      setUsers(data.content || []);
      setTotalPages(data.page?.totalPages || 1);
    } catch (error) {
      console.error('Error fetching users:', error);
      alert('Failed to load users. Please try again.');
    } finally {
      setLoading(false);
    }
  };

  const handleSort = (column: string) => {
    if (sortBy === column) {
      // Cycle through: asc -> desc -> no sort
      if (sortDirection === 'asc') {
        setSortDirection('desc');
      } else {
        setSortBy(null);
        setSortDirection('asc');
      }
    } else {
      setSortBy(column);
      setSortDirection('asc');
    }
    // Reset to first page when sorting changes
    setCurrentPage(0);
  };

  const getSortIcon = (column: string) => {
    if (sortBy !== column) {
      return <ArrowUpDown className="w-4 h-4 ml-1 inline opacity-50" />;
    }
    return sortDirection === 'asc'
      ? <ArrowUp className="w-4 h-4 ml-1 inline" />
      : <ArrowDown className="w-4 h-4 ml-1 inline" />;
  };

  const startEditing = (userId: string, field: 'entranceSound' | 'leaveSound', currentValue: string | null) => {
    setEditingUserId(userId);
    setEditingField(field);
    setEditValue(currentValue || '');
  };

  const cancelEditing = () => {
    setEditingUserId(null);
    setEditingField(null);
    setEditValue('');
  };

  const saveEdit = async (userId: string) => {
    if (!editingField) return;

    try {
      // editValue contains the sound.id from the selected dropdown option
      console.log(`Updating user ${userId}: ${editingField} = ${editValue || 'null'}`);

      // Build query parameters - backend expects entranceSound/leaveSound as request parameters
      const params = new URLSearchParams();
      if (editValue) {
        params.append(editingField, editValue);
      } else {
        // Send empty string or handle null case as needed by your backend
        params.append(editingField, '');
      }

      // Call the Spring Boot PATCH endpoint with query parameters
      const response = await fetch(
        `${API_ENDPOINTS.DISCORD_USERS}/${userId}?${params.toString()}`,
        {
          method: 'PATCH',
          mode: 'cors'
        }
      );

      if (!response.ok) {
        const errorText = await response.text().catch(() => 'Unknown error');
        console.error(`Failed to update user: ${response.status} - ${errorText}`);
        throw new Error(`Failed to update user: ${response.status}`);
      }

      console.log(`Successfully updated user ${userId} with ${editingField}=${editValue}`);

      // Update local state after successful API call
      setUsers(prev => prev.map(user =>
        user.id === userId
          ? { ...user, [editingField]: editValue || null }
          : user
      ));

      cancelEditing();
    } catch (error) {
      console.error('Error updating user:', error);
      if (error instanceof TypeError) {
        alert('Failed to update user. Please make sure the backend is running.');
      } else if (error instanceof Error) {
        alert(`Failed to update user: ${error.message}`);
      }
    }
  };

  const goToPage = (page: number) => {
    if (page >= 0 && page < totalPages) {
      setCurrentPage(page);
    }
  };

  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black bg-opacity-50">
      <div className={`w-full max-w-5xl h-[90vh] rounded-lg shadow-2xl flex flex-col ${
        theme === 'dark' ? 'bg-gray-800 text-white' : 'bg-white text-gray-900'
      }`}>
        {/* Header */}
        <div className={`flex items-center justify-between p-6 border-b ${
          theme === 'dark' ? 'border-gray-700' : 'border-gray-200'
        }`}>
          <h2 className="text-2xl">Discord Users</h2>
          <button
            onClick={onClose}
            className={`p-2 rounded-lg transition-colors ${
              theme === 'dark'
                ? 'hover:bg-gray-700 text-gray-400 hover:text-white'
                : 'hover:bg-gray-100 text-gray-600 hover:text-gray-900'
            }`}
            aria-label="Close"
          >
            <X className="w-6 h-6" />
          </button>
        </div>

        {/* Content */}
        <div className="flex-1 overflow-auto p-6">
          {loading ? (
            <div className="flex items-center justify-center py-12">
              <div className="w-12 h-12 border-4 border-blue-500 border-t-transparent rounded-full animate-spin"></div>
            </div>
          ) : (
            <div className="overflow-x-auto">
              <table className="w-full">
                <thead>
                  <tr className={`border-b ${
                    theme === 'dark' ? 'border-gray-700' : 'border-gray-200'
                  }`}>
                    <th
                      className={`px-4 py-3 text-left cursor-pointer select-none transition-colors ${
                        theme === 'dark' ? 'hover:bg-gray-700' : 'hover:bg-gray-100'
                      }`}
                      onClick={() => handleSort('username')}
                    >
                      Username {getSortIcon('username')}
                    </th>
                    <th
                      className={`px-4 py-3 text-left cursor-pointer select-none transition-colors ${
                        theme === 'dark' ? 'hover:bg-gray-700' : 'hover:bg-gray-100'
                      }`}
                      onClick={() => handleSort('entranceSound')}
                    >
                      Entrance Sound {getSortIcon('entranceSound')}
                    </th>
                    <th
                      className={`px-4 py-3 text-left cursor-pointer select-none transition-colors ${
                        theme === 'dark' ? 'hover:bg-gray-700' : 'hover:bg-gray-100'
                      }`}
                      onClick={() => handleSort('leaveSound')}
                    >
                      Leave Sound {getSortIcon('leaveSound')}
                    </th>
                  </tr>
                </thead>
                <tbody>
                  {users.map(user => (
                    <tr
                      key={user.id}
                      className={`border-b cursor-pointer transition-colors ${
                        theme === 'dark'
                          ? 'border-gray-700 hover:bg-gray-700'
                          : 'border-gray-100 hover:bg-gray-50'
                      }`}
                      onClick={() => {
                        if (!editingUserId) {
                          startEditing(user.id, 'entranceSound', user.entranceSound);
                        }
                      }}
                    >
                      <td className="px-4 py-3">{user.username}</td>
                      <td className="px-4 py-3" onClick={(e) => e.stopPropagation()}>
                        {editingUserId === user.id && editingField === 'entranceSound' ? (
                          <div className="flex items-center gap-2">
                            <select
                              value={editValue}
                              onChange={(e) => setEditValue(e.target.value)}
                              className={`flex-1 px-2 py-1 border rounded text-sm ${
                                theme === 'dark'
                                  ? 'bg-gray-700 border-gray-600 text-white'
                                  : 'bg-white border-gray-300 text-gray-900'
                              }`}
                              autoFocus
                            >
                              <option value="">None</option>
                              {sortedSounds.map(sound => (
                                <option key={sound.id} value={sound.id}>
                                  {sound.name}
                                </option>
                              ))}
                            </select>
                            <button
                              onClick={() => saveEdit(user.id)}
                              className="px-3 py-1 bg-blue-600 text-white rounded text-sm hover:bg-blue-700"
                            >
                              Save
                            </button>
                            <button
                              onClick={cancelEditing}
                              className={`px-3 py-1 rounded text-sm ${
                                theme === 'dark'
                                  ? 'bg-gray-600 text-white hover:bg-gray-500'
                                  : 'bg-gray-200 text-gray-900 hover:bg-gray-300'
                              }`}
                            >
                              Cancel
                            </button>
                          </div>
                        ) : (
                          <button
                            onClick={() => startEditing(user.id, 'entranceSound', user.entranceSound)}
                            className={`text-left w-full px-2 py-1 rounded hover:bg-opacity-50 ${
                              theme === 'dark' ? 'hover:bg-gray-600' : 'hover:bg-gray-200'
                            }`}
                          >
                            {user.entranceSound || <span className="text-gray-500 italic">None</span>}
                          </button>
                        )}
                      </td>
                      <td className="px-4 py-3" onClick={(e) => e.stopPropagation()}>
                        {editingUserId === user.id && editingField === 'leaveSound' ? (
                          <div className="flex items-center gap-2">
                            <select
                              value={editValue}
                              onChange={(e) => setEditValue(e.target.value)}
                              className={`flex-1 px-2 py-1 border rounded text-sm ${
                                theme === 'dark'
                                  ? 'bg-gray-700 border-gray-600 text-white'
                                  : 'bg-white border-gray-300 text-gray-900'
                              }`}
                              autoFocus
                            >
                              <option value="">None</option>
                              {sortedSounds.map(sound => (
                                <option key={sound.id} value={sound.id}>
                                  {sound.name}
                                </option>
                              ))}
                            </select>
                            <button
                              onClick={() => saveEdit(user.id)}
                              className="px-3 py-1 bg-blue-600 text-white rounded text-sm hover:bg-blue-700"
                            >
                              Save
                            </button>
                            <button
                              onClick={cancelEditing}
                              className={`px-3 py-1 rounded text-sm ${
                                theme === 'dark'
                                  ? 'bg-gray-600 text-white hover:bg-gray-500'
                                  : 'bg-gray-200 text-gray-900 hover:bg-gray-300'
                              }`}
                            >
                              Cancel
                            </button>
                          </div>
                        ) : (
                          <button
                            onClick={() => startEditing(user.id, 'leaveSound', user.leaveSound)}
                            className={`text-left w-full px-2 py-1 rounded hover:bg-opacity-50 ${
                              theme === 'dark' ? 'hover:bg-gray-600' : 'hover:bg-gray-200'
                            }`}
                          >
                            {user.leaveSound || <span className="text-gray-500 italic">None</span>}
                          </button>
                        )}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>

              {users.length === 0 && !loading && (
                <div className="text-center py-12 text-gray-500">
                  No users found
                </div>
              )}
            </div>
          )}
        </div>

        {/* Footer with Pagination */}
        <div className={`flex items-center justify-between p-6 border-t ${
          theme === 'dark' ? 'border-gray-700' : 'border-gray-200'
        }`}>
          <div className="text-sm text-gray-500">
            Page {currentPage + 1} of {totalPages || 1}
          </div>
          <div className="flex items-center gap-2">
            <button
              onClick={() => goToPage(currentPage - 1)}
              disabled={currentPage === 0}
              className={`p-2 rounded-lg transition-colors ${
                currentPage === 0
                  ? 'opacity-50 cursor-not-allowed'
                  : theme === 'dark'
                  ? 'hover:bg-gray-700 text-gray-400'
                  : 'hover:bg-gray-100 text-gray-600'
              }`}
              aria-label="Previous page"
            >
              <ChevronLeft className="w-5 h-5" />
            </button>
            <button
              onClick={() => goToPage(currentPage + 1)}
              disabled={currentPage >= totalPages - 1}
              className={`p-2 rounded-lg transition-colors ${
                currentPage >= totalPages - 1
                  ? 'opacity-50 cursor-not-allowed'
                  : theme === 'dark'
                  ? 'hover:bg-gray-700 text-gray-400'
                  : 'hover:bg-gray-100 text-gray-600'
              }`}
              aria-label="Next page"
            >
              <ChevronRight className="w-5 h-5" />
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}