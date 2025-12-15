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
}

export function DiscordUsersList({ theme, onUserSelect, selectedUserId, onVolumeUpdate, onPlaybackEnabledChange }: DiscordUsersListProps) {
  const [users, setUsers] = useState<DiscordUser[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const previousSelectedUserIdRef = useRef<string | null>(null);
  const previousVolumeRef = useRef<number | null>(null);
  const previousInVoiceRef = useRef<boolean | null>(null);

  useEffect(() => {
    // Prevent double SSE connections in StrictMode
    let eventSource: EventSource | null = null;
    let isMounted = true;
    const abortController = new AbortController();

    const fetchUsers = async () => {
      try {
        setLoading(true);

        console.log('📡 Fetching initial Discord users...');

        // Fetch users who are in voice or selected from dedicated endpoint
        const response = await fetch(`${API_BASE_URL}/api/discordUsers/invoiceorselected`, {
          signal: abortController.signal,
          headers: getAuthHeaders()
        });

        if (!response.ok) {
          throw new Error('Failed to fetch users');
        }

        const data: DiscordUsersResponse = await response.json();

        console.log('✅ Discord users fetched successfully:', data.content.length);

        // Sort users: selected user first, then the rest
        const sortedUsers = [...data.content].sort((a, b) => {
          if (a.selected && !b.selected) return -1;
          if (!a.selected && b.selected) return 1;
          return 0;
        });

        setUsers(sortedUsers);

        // Check if there's a currently selected user (from App.tsx state, not backend flag)
        // If selectedUserId is set, find that user in the list and check their status
        let shouldEnablePlayback = false;
        let currentVolume = 100;

        if (selectedUserId) {
          const currentUser = sortedUsers.find(user => user.id === selectedUserId);
          if (currentUser) {
            // User is still in the list, check if they're in voice
            shouldEnablePlayback = currentUser.inVoice;
            currentVolume = currentUser.volume ?? 100;

            const userIdChanged = previousSelectedUserIdRef.current !== currentUser.id;
            const volumeChanged = previousVolumeRef.current !== currentVolume;
            const inVoiceChanged = previousInVoiceRef.current !== currentUser.inVoice;

            // Only update volume if it changed OR if the user changed (new user might have different volume)
            if (volumeChanged || userIdChanged) {
              console.log('🔊 Volume or user changed, updating volume to:', currentVolume);
              onVolumeUpdate(currentVolume);
              previousVolumeRef.current = currentVolume;
            }

            // Update playback enabled state (enabled only if user is in voice)
            if (inVoiceChanged || userIdChanged) {
              console.log('🎤 In Voice status changed, updating playback enabled to:', currentUser.inVoice);
              onPlaybackEnabledChange(currentUser.inVoice);
              previousInVoiceRef.current = currentUser.inVoice;
            }

            previousSelectedUserIdRef.current = currentUser.id;
          } else {
            // Selected user is no longer in the list, disable playback
            console.log('❌ Selected user not found in updated list, disabling playback');
            onPlaybackEnabledChange(false);
            previousInVoiceRef.current = null;
          }
        } else {
          // No user selected in App.tsx, check if backend marked someone as selected
          const backendSelectedUser = sortedUsers.find(user => user.selected);
          if (backendSelectedUser) {
            // Backend wants us to select this user
            console.log('👤 Backend selected user, updating selection:', backendSelectedUser.username);
            onUserSelect(backendSelectedUser.id);
            currentVolume = backendSelectedUser.volume ?? 100;
            onVolumeUpdate(currentVolume);
            onPlaybackEnabledChange(backendSelectedUser.inVoice);

            previousSelectedUserIdRef.current = backendSelectedUser.id;
            previousVolumeRef.current = currentVolume;
            previousInVoiceRef.current = backendSelectedUser.inVoice;
          } else {
            // No selected user anywhere, ensure playback is disabled
            if (previousInVoiceRef.current !== null) {
              console.log('❌ No selected user, disabling playback');
              onPlaybackEnabledChange(false);
              previousInVoiceRef.current = null;
            }
          }
        }

        setError(null);
        setLoading(false);
      } catch (err) {
        // Don't update state if the request was aborted
        if (err instanceof Error && err.name === 'AbortError') {
          console.log('Fetch aborted');
          return;
        }
        console.error('Error fetching Discord users:', err);
        setError('Failed to load users');
        setLoading(false);
      }
    };

    // Initial fetch
    fetchUsers().then(() => {
      if (!isMounted) return;

      // After initial fetch, connect to SSE for real-time updates
      try {
        console.log('📡 Connecting to Discord Users SSE endpoint:', API_ENDPOINTS.DISCORD_USERS_STREAM);
        eventSource = new EventSource(API_ENDPOINTS.DISCORD_USERS_STREAM);

        eventSource.onopen = () => {
          console.log('✅ Discord Users SSE connection established');
        };

        eventSource.onerror = (error) => {
          console.error('❌ Discord Users SSE connection error:', error);
          console.error('Discord Users SSE readyState:', eventSource?.readyState);
          console.error('Discord Users SSE url:', eventSource?.url);
          // EventSource will automatically try to reconnect
        };

        eventSource.addEventListener('discordUsers', (event) => {
          if (!isMounted) return;
          try {
            console.log('🔄 Discord Users SSE update received');
            const data = JSON.parse(event.data);
            // Handle both array and paginated responses
            const usersData = Array.isArray(data) ? data : (data.content || []);

            console.log('📥 Discord Users SSE - Total users received:', usersData.length);

            // Sort users: selected user first, then the rest
            const sortedUsers = [...usersData].sort((a, b) => {
              if (a.selected && !b.selected) return -1;
              if (!a.selected && b.selected) return 1;
              return 0;
            });

            setUsers(sortedUsers);

            // Check if there's a currently selected user (from App.tsx state, not backend flag)
            // If selectedUserId is set, find that user in the list and check their status
            let shouldEnablePlayback = false;
            let currentVolume = 100;

            if (selectedUserId) {
              const currentUser = sortedUsers.find(user => user.id === selectedUserId);
              if (currentUser) {
                // User is still in the list, check if they're in voice
                shouldEnablePlayback = currentUser.inVoice;
                currentVolume = currentUser.volume ?? 100;

                const userIdChanged = previousSelectedUserIdRef.current !== currentUser.id;
                const volumeChanged = previousVolumeRef.current !== currentVolume;
                const inVoiceChanged = previousInVoiceRef.current !== currentUser.inVoice;

                // Only update volume if it changed OR if the user changed (new user might have different volume)
                if (volumeChanged || userIdChanged) {
                  console.log('🔊 Volume or user changed, updating volume to:', currentVolume);
                  onVolumeUpdate(currentVolume);
                  previousVolumeRef.current = currentVolume;
                }

                // Update playback enabled state (enabled only if user is in voice)
                if (inVoiceChanged || userIdChanged) {
                  console.log('🎤 In Voice status changed, updating playback enabled to:', currentUser.inVoice);
                  onPlaybackEnabledChange(currentUser.inVoice);
                  previousInVoiceRef.current = currentUser.inVoice;
                }

                previousSelectedUserIdRef.current = currentUser.id;
              } else {
                // Selected user is no longer in the list, disable playback
                console.log('❌ Selected user not found in updated list, disabling playback');
                onPlaybackEnabledChange(false);
                previousInVoiceRef.current = null;
              }
            } else {
              // No user selected in App.tsx, check if backend marked someone as selected
              const backendSelectedUser = sortedUsers.find(user => user.selected);
              if (backendSelectedUser) {
                // Backend wants us to select this user
                console.log('👤 Backend selected user, updating selection:', backendSelectedUser.username);
                onUserSelect(backendSelectedUser.id);
                currentVolume = backendSelectedUser.volume ?? 100;
                onVolumeUpdate(currentVolume);
                onPlaybackEnabledChange(backendSelectedUser.inVoice);

                previousSelectedUserIdRef.current = backendSelectedUser.id;
                previousVolumeRef.current = currentVolume;
                previousInVoiceRef.current = backendSelectedUser.inVoice;
              } else {
                // No selected user anywhere, ensure playback is disabled
                if (previousInVoiceRef.current !== null) {
                  console.log('❌ No selected user, disabling playback');
                  onPlaybackEnabledChange(false);
                  previousInVoiceRef.current = null;
                }
              }
            }
          } catch (error) {
            console.error('Error parsing Discord Users SSE data:', error);
          }
        });
      } catch (error) {
        console.error('Failed to create Discord Users SSE connection:', error);
      }
    });

    // Cleanup function
    return () => {
      console.log('🧹 Cleaning up Discord Users component');
      isMounted = false;
      abortController.abort();
      if (eventSource) {
        eventSource.close();
        console.log('✅ Discord Users SSE connection closed');
      }
    };
  }, [onUserSelect, onVolumeUpdate, onPlaybackEnabledChange, selectedUserId]);

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
    <div className={`rounded-lg border ${
      theme === 'dark'
        ? 'bg-gray-800 border-gray-700'
        : 'bg-white border-gray-200'
    }`}>
      <div className={`p-4 border-b ${
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

      <div className="max-h-[229px] overflow-y-auto">
        {users.length === 0 ? (
          <div className="p-4 text-center">
            <p className={theme === 'dark' ? 'text-gray-400' : 'text-gray-500'}>
              No active users
            </p>
          </div>
        ) : (
          <div className="divide-y divide-gray-700">
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
                    user.entranceSound || user.leaveSound
                      ? [
                          user.entranceSound ? `Entrance: ${user.entranceSound}` : null,
                          user.leaveSound ? `Leave: ${user.leaveSound}` : null
                        ].filter(Boolean).join(' | ')
                      : undefined
                  }
                >
                  {/* Online Status Indicator */}
                  <div className="relative">
                    <div className={`w-8 h-8 rounded-full flex items-center justify-center ${
                      theme === 'dark' ? 'bg-gray-700' : 'bg-gray-200'
                    }`}>
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
                    {user.inVoice && (
                      <div
                        className={`flex items-center gap-1 px-2 py-1 rounded text-xs ${
                          theme === 'dark'
                            ? 'bg-green-900/50 text-green-400'
                            : 'bg-green-100 text-green-700'
                        }`}
                        title="In Voice Channel"
                      >
                        <Mic className="w-3 h-3" />
                        Voice
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