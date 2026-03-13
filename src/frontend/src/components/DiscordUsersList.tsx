import React, { useEffect, useState, useRef } from 'react';
import { Users, CheckCircle, Mic } from 'lucide-react';
import { API_BASE_URL, API_ENDPOINTS } from '../config';
import { getAuthHeaders } from '../utils/api';

interface DiscordUser {
  id: string;
  username: string;
  entranceSound: string | null;
  leaveSound: string | null;
  selected: boolean;
  status: string;
  onlineStatus: string;
  inVoice: boolean;
  volume?: number;
  avatarUrl?: string;
  channelName?: string;
  guildInAudioName?: string;
  guildInAudioId?: string;
}

interface DiscordUsersResponse {
  content: DiscordUser[];
  page: {
    size: number;
    number: number;
    totalElements: number;
    totalPages: number;
  };
}

interface DiscordUsersListProps {
  onUserSelect: (userId: string | null) => void;
  selectedUserId: string | null;
  onVolumeUpdate: (volume: number) => void;
  onPlaybackEnabledChange: (enabled: boolean) => void;
  onGuildIdChange: (guildId: string | null) => void;
}

export function DiscordUsersList({ onUserSelect, selectedUserId, onVolumeUpdate, onPlaybackEnabledChange, onGuildIdChange }: DiscordUsersListProps) {
  const [users, setUsers] = useState<DiscordUser[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const previousSelectedUserIdRef = useRef<string | null>(null);
  const previousVolumeRef = useRef<number | null>(null);
  const previousInVoiceRef = useRef<boolean | null>(null);

  const selectedUserIdRef = useRef<string | null>(selectedUserId);

  useEffect(() => {
    selectedUserIdRef.current = selectedUserId;
  }, [selectedUserId]);

  useEffect(() => {
    let isMounted = true;
    const abortController = new AbortController();

    const applySelectionSideEffects = (sortedUsers: DiscordUser[]) => {
      let currentVolume = 100;

      const selectedId = selectedUserIdRef.current;

      if (selectedId) {
        const currentUser = sortedUsers.find(user => user.id === selectedId);
        if (currentUser) {
          currentVolume = currentUser.volume ?? 100;

          const userIdChanged = previousSelectedUserIdRef.current !== currentUser.id;
          const volumeChanged = previousVolumeRef.current !== currentVolume;
          const inVoiceChanged = previousInVoiceRef.current !== currentUser.inVoice;

          if (volumeChanged || userIdChanged) {
            onVolumeUpdate(currentVolume);
            previousVolumeRef.current = currentVolume;
          }

          if (inVoiceChanged || userIdChanged) {
            onPlaybackEnabledChange(currentUser.inVoice);
            previousInVoiceRef.current = currentUser.inVoice;
          }

          if (userIdChanged) {
            onGuildIdChange(currentUser.guildInAudioId || null);
          }

          previousSelectedUserIdRef.current = currentUser.id;
          return;
        }

        onPlaybackEnabledChange(false);
        onGuildIdChange(null);
        previousInVoiceRef.current = null;
        return;
      }

      // No user selected in App.tsx; fall back to backend-selected or first user
      const backendSelectedUser = sortedUsers.find(user => user.selected);
      if (backendSelectedUser) {
        onUserSelect(backendSelectedUser.id);
        currentVolume = backendSelectedUser.volume ?? 100;
        onVolumeUpdate(currentVolume);
        onPlaybackEnabledChange(backendSelectedUser.inVoice);
        onGuildIdChange(backendSelectedUser.guildInAudioId || null);

        previousSelectedUserIdRef.current = backendSelectedUser.id;
        previousVolumeRef.current = currentVolume;
        previousInVoiceRef.current = backendSelectedUser.inVoice;
      } else if (sortedUsers.length > 0) {
        const firstUser = sortedUsers[0];
        onUserSelect(firstUser.id);
        currentVolume = firstUser.volume ?? 100;
        onVolumeUpdate(currentVolume);
        onPlaybackEnabledChange(firstUser.inVoice);
        onGuildIdChange(firstUser.guildInAudioId || null);

        previousSelectedUserIdRef.current = firstUser.id;
        previousVolumeRef.current = currentVolume;
        previousInVoiceRef.current = firstUser.inVoice;
      } else {
        if (previousInVoiceRef.current !== null) {
          onPlaybackEnabledChange(false);
          onGuildIdChange(null);
          previousInVoiceRef.current = null;
        }
      }
    };

    const fetchUsers = async () => {
      try {
        setLoading(true);

        const response = await fetch(`${API_BASE_URL}/api/discordUsers/invoiceorselected`, {
          signal: abortController.signal,
          headers: getAuthHeaders()
        });

        if (!response.ok) {
          throw new Error('Failed to fetch users');
        }

        const data: DiscordUsersResponse = await response.json();

        const sortedUsers = [...data.content].sort((a, b) => {
          if (a.selected && !b.selected) return -1;
          if (!a.selected && b.selected) return 1;
          return 0;
        });

        if (!isMounted) return;

        setUsers(sortedUsers);
        applySelectionSideEffects(sortedUsers);

        setError(null);
        setLoading(false);
      } catch (err) {
        if (err instanceof Error && err.name === 'AbortError') {
          return;
        }
        console.error('Error fetching Discord users:', err);
        setError('Failed to load users');
        setLoading(false);
      }
    };

    fetchUsers();

    return () => {
      isMounted = false;
      abortController.abort();
    };
  }, [onUserSelect, onVolumeUpdate, onPlaybackEnabledChange, onGuildIdChange]);

  useEffect(() => {
    // Keep a single stable SSE connection for Discord users
    let eventSource: EventSource | null = null;
    let isMounted = true;

    const applySelectionSideEffects = (sortedUsers: DiscordUser[]) => {
      let currentVolume = 100;

      const selectedId = selectedUserIdRef.current;

      if (selectedId) {
        const currentUser = sortedUsers.find(user => user.id === selectedId);
        if (currentUser) {
          currentVolume = currentUser.volume ?? 100;

          const userIdChanged = previousSelectedUserIdRef.current !== currentUser.id;
          const volumeChanged = previousVolumeRef.current !== currentVolume;
          const inVoiceChanged = previousInVoiceRef.current !== currentUser.inVoice;

          if (volumeChanged || userIdChanged) {
            onVolumeUpdate(currentVolume);
            previousVolumeRef.current = currentVolume;
          }

          if (inVoiceChanged || userIdChanged) {
            onPlaybackEnabledChange(currentUser.inVoice);
            previousInVoiceRef.current = currentUser.inVoice;
          }

          if (userIdChanged) {
            onGuildIdChange(currentUser.guildInAudioId || null);
          }

          previousSelectedUserIdRef.current = currentUser.id;
          return;
        }

        onPlaybackEnabledChange(false);
        onGuildIdChange(null);
        previousInVoiceRef.current = null;
        return;
      }

      const backendSelectedUser = sortedUsers.find(user => user.selected);
      if (backendSelectedUser) {
        onUserSelect(backendSelectedUser.id);
        currentVolume = backendSelectedUser.volume ?? 100;
        onVolumeUpdate(currentVolume);
        onPlaybackEnabledChange(backendSelectedUser.inVoice);
        onGuildIdChange(backendSelectedUser.guildInAudioId || null);

        previousSelectedUserIdRef.current = backendSelectedUser.id;
        previousVolumeRef.current = currentVolume;
        previousInVoiceRef.current = backendSelectedUser.inVoice;
      } else if (sortedUsers.length > 0) {
        const firstUser = sortedUsers[0];
        onUserSelect(firstUser.id);
        currentVolume = firstUser.volume ?? 100;
        onVolumeUpdate(currentVolume);
        onPlaybackEnabledChange(firstUser.inVoice);
        onGuildIdChange(firstUser.guildInAudioId || null);

        previousSelectedUserIdRef.current = firstUser.id;
        previousVolumeRef.current = currentVolume;
        previousInVoiceRef.current = firstUser.inVoice;
      } else {
        if (previousInVoiceRef.current !== null) {
          onPlaybackEnabledChange(false);
          onGuildIdChange(null);
          previousInVoiceRef.current = null;
        }
      }
    };

    try {
      eventSource = new EventSource(API_ENDPOINTS.DISCORD_USERS_STREAM);

      eventSource.onopen = () => {
        // connected
      };

      eventSource.onerror = (error) => {
        // Let EventSource auto-reconnect; don't aggressively close/recreate here.
        console.error('Discord Users SSE error:', error);
      };

      eventSource.addEventListener('discordUsers', (event) => {
        if (!isMounted) return;
        try {
          const data = JSON.parse(event.data);
          const usersData = Array.isArray(data) ? data : (data.content || []);

          const sortedUsers = [...usersData].sort((a, b) => {
            if (a.selected && !b.selected) return -1;
            if (!a.selected && b.selected) return 1;
            return 0;
          });

          setUsers(sortedUsers);
          applySelectionSideEffects(sortedUsers);
        } catch (error) {
          console.error('Error parsing Discord Users SSE data:', error);
        }
      });
    } catch (error) {
      console.error('Failed to create Discord Users SSE connection:', error);
    }

    return () => {
      isMounted = false;
      if (eventSource) {
        eventSource.close();
      }
    };
  }, [onUserSelect, onVolumeUpdate, onPlaybackEnabledChange, onGuildIdChange]);

  const getStatusColor = (user: DiscordUser) => {
    if (user.inVoice) return 'text-green-600 dark:text-green-400';
    return 'text-gray-500 dark:text-gray-400';
  };

  const getOnlineStatusColor = (status: string) => {
    switch (status.toUpperCase()) {
      case 'ONLINE': return 'bg-green-500';
      case 'IDLE': return 'bg-yellow-500';
      case 'DND': return 'bg-red-500';
      case 'OFFLINE':
      default: return 'bg-gray-500';
    }
  };

  if (loading) {
    return (
      <div className="rounded-lg border p-4 bg-white border-gray-200 dark:bg-gray-800 dark:border-gray-700">
        <div className="flex items-center gap-2 mb-3">
          <Users className="w-5 h-5" />
          <h2 className="font-semibold">Active Users</h2>
        </div>
        <p className="text-gray-500 dark:text-gray-400">
          Loading users...
        </p>
      </div>
    );
  }

  if (error) {
    return (
      <div className="rounded-lg h-[152px] border p-4 bg-white border-gray-200 dark:bg-gray-800 dark:border-gray-700">
        <div className="flex items-center gap-2 mb-3">
          <Users className="w-5 h-5" />
          <h2 className="font-semibold">Active Users</h2>
        </div>
        <p className="text-red-600 dark:text-red-400">
          {error}
        </p>
      </div>
    );
  }

  return (
    <div className="rounded-lg border h-[155px] flex flex-col bg-white border-gray-200 dark:bg-gray-800 dark:border-gray-700">
      <div className="px-3 py-1 border-b flex-shrink-0 border-gray-200 dark:border-gray-700">
        <div className="flex items-center gap-2">
          <Users className="w-5 h-5 text-gray-900 dark:text-white" />
          <h2 className="font-semibold text-gray-900 dark:text-white">Active Users</h2>
          <span className="ml-auto px-2 py-0.5 rounded-full text-xs bg-blue-100 text-blue-700 dark:bg-blue-900 dark:text-blue-200">
            {users.length}
          </span>
        </div>
      </div>

      <div className="flex-1 overflow-y-auto min-h-0 [&::-webkit-scrollbar]:w-2 [&::-webkit-scrollbar-track]:bg-gray-200 dark:[&::-webkit-scrollbar-track]:bg-gray-700 [&::-webkit-scrollbar-thumb]:bg-gray-400 dark:[&::-webkit-scrollbar-thumb]:bg-gray-600 [&::-webkit-scrollbar-thumb]:rounded-full [&::-webkit-scrollbar-thumb]:hover:bg-gray-500">
        {users.length === 0 ? (
          <div className="p-4 text-center">
            <p className="text-gray-500 dark:text-gray-400">
              No active users
            </p>
          </div>
        ) : (
          <div className="divide-y divide-gray-200 dark:divide-gray-700">
            {users.map(user => {
              const isSelected = selectedUserId === user.id;
              return (
                <div
                  key={user.id}
                  onClick={() => {
                    const willBeSelected = !isSelected;
                    onUserSelect(willBeSelected ? user.id : null);
                    // Update playback enabled based on whether user will be selected AND is in voice
                    onPlaybackEnabledChange(willBeSelected && user.inVoice);
                    // Update guild ID when user is selected/deselected
                    onGuildIdChange(willBeSelected ? (user.guildInAudioId || null) : null);
                  }}
                  className={`py-0.5 px-3 flex items-center gap-2 transition-colors cursor-pointer ${
                    isSelected
                      ? 'bg-blue-100 border-l-4 border-blue-600 dark:bg-blue-900/30 dark:border-blue-500'
                      : 'hover:bg-gray-50 dark:hover:bg-gray-700'
                  }`}
                  title={
                    [
                      user.guildInAudioName ? `Guild: ${user.guildInAudioName}` : null,
                      user.entranceSound ? `Entrance: ${user.entranceSound}` : null,
                      user.leaveSound ? `Leave: ${user.leaveSound}` : null
                    ].filter(Boolean).join(' | ') || undefined
                  }
                >
                  {/* Online Status Indicator */}
                  <div className="relative">
                    {user.avatarUrl ? (
                      <img
                        src={user.avatarUrl}
                        alt={user.username}
                        className="w-6 h-6 rounded-full object-cover"
                        onError={(e) => {
                          // Fallback to placeholder if image fails to load
                          e.currentTarget.classList.add('hidden');
                          const fallback = e.currentTarget.nextElementSibling as HTMLElement;
                          if (fallback) fallback.classList.remove('hidden');
                        }}
                      />
                    ) : null}
                    <div className={`w-6 h-6 rounded-full flex items-center justify-center ${user.avatarUrl ? 'hidden' : ''} bg-gray-200 dark:bg-gray-700`}>
                      <Users className="w-3 h-3" />
                    </div>
                    <div
                      className={`absolute bottom-0 right-0 w-2.5 h-2.5 rounded-full border-2 border-white dark:border-gray-800 ${getOnlineStatusColor(user.onlineStatus)}`}
                    />
                  </div>

                  {/* Username */}
                  <div className="flex-1 min-w-0">
                    <p className="text-sm truncate text-gray-900 dark:text-white">
                      {user.username}
                      {user.guildInAudioName && (
                        <span className="text-xs text-gray-500 dark:text-gray-400">
                          {' '}({user.guildInAudioName})
                        </span>
                      )}
                    </p>
                    {user.entranceSound && user.leaveSound ? (
                      <p className="text-xs truncate text-gray-500 dark:text-gray-400">
                        {user.entranceSound} | {user.leaveSound}
                      </p>
                    ) : (user.entranceSound || user.leaveSound) && (
                      <p className="text-xs truncate text-gray-500 dark:text-gray-400">
                        {user.entranceSound ?? user.leaveSound}
                      </p>
                    )}
                  </div>

                  {/* Status Badges */}
                  <div className="flex items-center gap-1">
                    {user.selected && (
                      <div
                        className="p-1 rounded bg-blue-100 text-blue-700 dark:bg-blue-900/50 dark:text-blue-400"
                        title="Selected"
                      >
                        <CheckCircle className="w-3 h-3" />
                      </div>
                    )}
                    {user.inVoice && user.channelName && (
                      <div
                        className="p-1 rounded bg-green-100 text-green-700 dark:bg-green-900/50 dark:text-green-400"
                        title={`In Voice Channel: ${user.channelName}`}
                      >
                        <Mic className="w-3 h-3" />
                      </div>
                    )}
                  </div>
                </div>
              );
            })}
          </div>
        )}
      </div>
    </div>
  );
}