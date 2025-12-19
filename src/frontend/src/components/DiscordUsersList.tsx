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
  theme: 'light' | 'dark';
  onUserSelect: (userId: string | null) => void;
  selectedUserId: string | null;
  onVolumeUpdate: (volume: number) => void;
  onPlaybackEnabledChange: (enabled: boolean) => void;
  onGuildIdChange: (guildId: string | null) => void;
}

export function DiscordUsersList({ theme, onUserSelect, selectedUserId, onVolumeUpdate, onPlaybackEnabledChange, onGuildIdChange }: DiscordUsersListProps) {
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

        // Selected user not found
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
    if (user.inVoice) {
      return theme === 'dark' ? 'text-green-400' : 'text-green-600';
    }
    return theme === 'dark' ? 'text-gray-400' : 'text-gray-500';
  };

  const getOnlineStatusColor = (status: string) => {
    switch (status.toUpperCase()) {
      case 'ONLINE':
        return 'bg-green-500';
      case 'IDLE':
        return 'bg-yellow-500';
      case 'DND':
        return 'bg-red-500';
      case 'OFFLINE':
      default:
        return 'bg-gray-500';
    }
  };

  if (loading) {
    return (
      <div className={`rounded-lg border p-4 ${
        theme === 'dark'
          ? 'bg-gray-800 border-gray-700'
          : 'bg-white border-gray-200'
      }`}>
        <div className="flex items-center gap-2 mb-3">
          <Users className="w-5 h-5" />
          <h2 className="font-semibold">Active Users</h2>
        </div>
        <p className={theme === 'dark' ? 'text-gray-400' : 'text-gray-500'}>
          Loading users...
        </p>
      </div>
    );
  }

  if (error) {
    return (
      <div className={`rounded-lg border p-4 ${
        theme === 'dark'
          ? 'bg-gray-800 border-gray-700'
          : 'bg-white border-gray-200'
      }`}>
        <div className="flex items-center gap-2 mb-3">
          <Users className="w-5 h-5" />
          <h2 className="font-semibold">Active Users</h2>
        </div>
        <p className={theme === 'dark' ? 'text-red-400' : 'text-red-600'}>
          {error}
        </p>
      </div>
    );
  }

  return (
    <div className={`rounded-lg border max-h-[229px] flex flex-col ${
      theme === 'dark'
        ? 'bg-gray-800 border-gray-700'
        : 'bg-white border-gray-200'
    }`}>
      <div className={`p-4 border-b flex-shrink-0 ${
        theme === 'dark' ? 'border-gray-700' : 'border-gray-200'
      }`}>
        <div className="flex items-center gap-2">
          <Users className={`w-5 h-5 ${theme === 'dark' ? 'text-white' : 'text-gray-900'}`} />
          <h2 className={`font-semibold ${theme === 'dark' ? 'text-white' : 'text-gray-900'}`}>Active Users</h2>
          <span className={`ml-auto px-2 py-0.5 rounded-full text-xs ${
            theme === 'dark'
              ? 'bg-blue-900 text-blue-200'
              : 'bg-blue-100 text-blue-700'
          }`}>
            {users.length}
          </span>
        </div>
      </div>

      <div className={`flex-1 overflow-y-auto min-h-0 ${
        theme === 'dark'
          ? '[&::-webkit-scrollbar]:w-2 [&::-webkit-scrollbar-track]:bg-gray-700 [&::-webkit-scrollbar-thumb]:bg-gray-600 [&::-webkit-scrollbar-thumb]:rounded-full [&::-webkit-scrollbar-thumb]:hover:bg-gray-500'
          : '[&::-webkit-scrollbar]:w-2 [&::-webkit-scrollbar-track]:bg-gray-200 [&::-webkit-scrollbar-thumb]:bg-gray-400 [&::-webkit-scrollbar-thumb]:rounded-full [&::-webkit-scrollbar-thumb]:hover:bg-gray-500'
      }`}>
        {users.length === 0 ? (
          <div className="p-4 text-center">
            <p className={theme === 'dark' ? 'text-gray-400' : 'text-gray-500'}>
              No active users
            </p>
          </div>
        ) : (
          <div className={`divide-y ${theme === 'dark' ? 'divide-gray-700' : 'divide-gray-200'}`}>
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
                  className={`p-3 flex items-center gap-3 transition-colors cursor-pointer ${
                    isSelected
                      ? theme === 'dark'
                        ? 'bg-blue-900/30 border-l-4 border-blue-500'
                        : 'bg-blue-100 border-l-4 border-blue-600'
                      : theme === 'dark'
                      ? 'hover:bg-gray-700'
                      : 'hover:bg-gray-50'
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
                        className="w-8 h-8 rounded-full object-cover"
                        onError={(e) => {
                          // Fallback to placeholder if image fails to load
                          e.currentTarget.style.display = 'none';
                          const fallback = e.currentTarget.nextElementSibling as HTMLElement;
                          if (fallback) fallback.style.display = 'flex';
                        }}
                      />
                    ) : null}
                    <div
                      className={`w-8 h-8 rounded-full flex items-center justify-center ${
                        theme === 'dark' ? 'bg-gray-700' : 'bg-gray-200'
                      }`}
                      style={{ display: user.avatarUrl ? 'none' : 'flex' }}
                    >
                      <Users className="w-4 h-4" />
                    </div>
                    <div
                      className={`absolute bottom-0 right-0 w-3 h-3 rounded-full border-2 ${
                        theme === 'dark' ? 'border-gray-800' : 'border-white'
                      } ${getOnlineStatusColor(user.onlineStatus)}`}
                    />
                  </div>

                  {/* Username */}
                  <div className="flex-1 min-w-0">
                    <p className={`truncate ${theme === 'dark' ? 'text-white' : 'text-gray-900'}`}>
                      {user.username}
                      {user.guildInAudioName && (
                        <span className={theme === 'dark' ? 'text-gray-400' : 'text-gray-500'}>
                          {' '}({user.guildInAudioName})
                        </span>
                      )}
                    </p>
                    {user.entranceSound && user.leaveSound ? (
                      <>
                        <p className={`text-xs truncate hidden sm:block ${
                          theme === 'dark' ? 'text-gray-400' : 'text-gray-500'
                        }`}>
                          Entrance: {user.entranceSound} | Leave: {user.leaveSound}
                        </p>
                        <p className={`text-xs truncate sm:hidden ${
                          theme === 'dark' ? 'text-gray-400' : 'text-gray-500'
                        }`}>
                          Entrance: {user.entranceSound}
                        </p>
                      </>
                    ) : (user.entranceSound || user.leaveSound) && (
                      <p className={`text-xs truncate ${
                        theme === 'dark' ? 'text-gray-400' : 'text-gray-500'
                      }`}>
                        {user.entranceSound ? `Entrance: ${user.entranceSound}` : `Leave: ${user.leaveSound}`}
                      </p>
                    )}
                  </div>

                  {/* Status Badges */}
                  <div className="flex items-center gap-2">
                    {user.selected && (
                      <div
                        className={`flex items-center gap-1 px-2 py-1 rounded text-xs ${
                          theme === 'dark'
                            ? 'bg-blue-900/50 text-blue-400'
                            : 'bg-blue-100 text-blue-700'
                        }`}
                        title="Selected"
                      >
                        <CheckCircle className="w-3 h-3" />
                      </div>
                    )}
                    {user.inVoice && user.channelName && (
                      <div
                        className={`flex items-center gap-1 px-2 py-1 rounded text-xs ${
                          theme === 'dark'
                            ? 'bg-green-900/50 text-green-400'
                            : 'bg-green-100 text-green-700'
                        }`}
                        title={`In Voice Channel: ${user.channelName}`}
                      >
                        <Mic className="w-3 h-3" />
                        <span className="truncate max-w-[80px]">{user.channelName}</span>
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