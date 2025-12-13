import React, { useState, useEffect, useRef } from 'react';
import { SoundButton } from './components/SoundButton';
import { ContextMenu } from './components/ContextMenu';
import { DiscordUsersList } from './components/DiscordUsersList';
import { UsersOverlay } from './components/UsersOverlay';
import { SettingsMenu } from './components/SettingsMenu';
import { AuthButton } from './components/AuthButton';
import { Search, Star, Grid3x3, Volume2, Shuffle, StopCircle, Upload, Users, Settings } from 'lucide-react';
import { API_ENDPOINTS } from './config';
import {
  loadAuth,
  initiateDiscordLogin,
  logout,
  validateToken,
  clearAuth,
  handleOAuthRedirect,
  type DiscordUser
} from './utils/auth';
import { getAuthHeaders } from './utils/api';

interface Sound {
  id: string;
  name: string;
  category: string;
  url: string;
  favorite?: boolean;
  displayName?: string | null;
  timesPlayed: number;
  dateAdded: string;
  volumeOffset: number | null;
}

// API response type
interface ApiSoundFile {
  soundFileId: string;
  soundFileLocation: string;
  category: string;
  timesPlayed: number;
  dateAdded: string;
  favorite: boolean;
  displayName: string | null;
  volumeOffsetPercentage: number | null;
}

// API response can be either paginated or array
interface ApiResponse {
  content: ApiSoundFile[];
  page: {
    size: number;
    number: number;
    totalElements: number;
    totalPages: number;
  };
}

// Helper function to transform API response to Sound format
function transformApiSounds(apiSounds: ApiSoundFile[]): Sound[] {
  return apiSounds.map(sound => ({
    id: sound.soundFileId,
    name: sound.displayName || sound.soundFileId.replace(/_/g, ' '),
    category: sound.category,
    url: sound.soundFileLocation,
    favorite: sound.favorite,
    displayName: sound.displayName,
    timesPlayed: sound.timesPlayed,
    dateAdded: sound.dateAdded,
    volumeOffset: sound.volumeOffsetPercentage
  }));
}

export default function App() {
  const [sounds, setSounds] = useState<Sound[]>([]);
  const [favorites, setFavorites] = useState<Set<string>>(new Set());
  const [selectedCategory, setSelectedCategory] = useState<string>('all');
  const [activeFilter, setActiveFilter] = useState<'none' | 'favorites' | 'popular' | 'recent'>('none');
  const [searchQuery, setSearchQuery] = useState('');
  const [loading, setLoading] = useState(true);
  const [theme, setTheme] = useState<'light' | 'dark'>('dark');
  const [contextMenu, setContextMenu] = useState<{
    x: number;
    y: number;
    soundId: string;
  } | null>(null);
  const [connectionStatus, setConnectionStatus] = useState<'connecting' | 'connected' | 'error'>('connecting');
  const [selectedUserId, setSelectedUserId] = useState<string | null>(null);
  const searchInputRef = useRef<HTMLInputElement>(null);
  const fileInputRef = useRef<HTMLInputElement>(null);
  const [volume, setVolume] = useState<number>(100);
  const toggleFavoriteInProgressRef = useRef<Set<string>>(new Set());
  const [isPlaybackEnabled, setIsPlaybackEnabled] = useState<boolean>(false);
  const [showUsersOverlay, setShowUsersOverlay] = useState(false);
  const [currentlyPlayingSoundId, setCurrentlyPlayingSoundId] = useState<string | null>(null);
  const [settingsMenu, setSettingsMenu] = useState<{ x: number; y: number } | null>(null);
  const [popularCount, setPopularCount] = useState<number>(10);
  const [recentCount, setRecentCount] = useState<number>(10);
  const [locallyPlayingSoundId, setLocallyPlayingSoundId] = useState<string | null>(null);
  const currentLocalAudioRef = useRef<HTMLAudioElement | null>(null);
  const [authUser, setAuthUser] = useState<DiscordUser | null>(null);
  const [authLoading, setAuthLoading] = useState<boolean>(true);

  // Handle Discord OAuth callback
  useEffect(() => {
    const handleCallback = async () => {
      const urlParams = new URLSearchParams(window.location.search);
      const token = urlParams.get('token');

      if (token) {
        try {
          console.log('🔐 Processing OAuth redirect with token');
          const authState = await handleOAuthRedirect(token);
          setAuthUser(authState.user);
          setAuthLoading(false);

          // Clean up URL
          window.history.replaceState({}, document.title, window.location.pathname);
        } catch (error) {
          console.error('Failed to authenticate:', error);
          setAuthLoading(false);
        }
      } else {
        // No OAuth callback, check for existing auth
        const storedAuth = loadAuth();
        if (storedAuth.accessToken && storedAuth.user) {
          // Validate token
          const user = await validateToken(storedAuth.accessToken);
          if (user) {
            setAuthUser(user);
          } else {
            // Token invalid, clear stored auth
            clearAuth();
          }
        }
        setAuthLoading(false);
      }
    };

    handleCallback();
  }, []);

  const handleLogin = () => {
    initiateDiscordLogin();
  };

  const handleLogout = async () => {
    const storedAuth = loadAuth();
    if (storedAuth.accessToken) {
      await logout(storedAuth.accessToken);
    }
    setAuthUser(null);
  };

  // Global ESC key handler - works from anywhere
  useEffect(() => {
    const handleEscapeKey = (e: KeyboardEvent) => {
      if (e.key === 'Escape') {
        setSearchQuery('');
      }
    };

    document.addEventListener('keydown', handleEscapeKey);
    return () => {
      document.removeEventListener('keydown', handleEscapeKey);
    };
  }, []);

  useEffect(() => {
    // Prevent double SSE connections in StrictMode
    let eventSource: EventSource | null = null;
    let isMounted = true;
    let reconnectTimeout: ReturnType<typeof setTimeout> | null = null;
    let reconnectAttempts = 0;
    const MAX_RECONNECT_ATTEMPTS = 10;
    const BASE_RECONNECT_DELAY = 1000; // 1 second
    const MAX_RECONNECT_DELAY = 30000; // 30 seconds

    const connectToSounds = () => {
      // Close existing connection if any
      if (eventSource) {
        eventSource.close();
        eventSource = null;
      }

      try {
        console.log('📡 Connecting to SSE endpoint:', API_ENDPOINTS.SOUNDS_STREAM);
        eventSource = new EventSource(API_ENDPOINTS.SOUNDS_STREAM);

        eventSource.onopen = () => {
          if (!isMounted) return;
          console.log('✅ SSE connection established');
          setConnectionStatus('connected');
          reconnectAttempts = 0; // Reset reconnect attempts on successful connection
        };

        eventSource.onerror = (error) => {
          // Only log errors if we've exceeded max attempts or if it's the first error
          if (reconnectAttempts === 0) {
            console.warn('⚠️ SSE connection interrupted, will attempt to reconnect...');
          }

          setConnectionStatus('error');

          // Close the connection
          if (eventSource) {
            eventSource.close();
            eventSource = null;
          }

          // Attempt to reconnect if not unmounted and haven't exceeded max attempts
          if (isMounted && reconnectAttempts < MAX_RECONNECT_ATTEMPTS) {
            reconnectAttempts++;
            const delay = Math.min(
              BASE_RECONNECT_DELAY * Math.pow(2, reconnectAttempts - 1),
              MAX_RECONNECT_DELAY
            );

            reconnectTimeout = setTimeout(() => {
              if (isMounted) {
                connectToSounds();
              }
            }, delay);
          } else if (reconnectAttempts >= MAX_RECONNECT_ATTEMPTS) {
            console.error('❌ Max reconnection attempts reached for sounds SSE. Connection failed.');
            setConnectionStatus('error');
          }
        };

        eventSource.addEventListener('sounds', (event) => {
          if (!isMounted) return;
          try {
            const data = JSON.parse(event.data);
            // Handle both array and paginated responses
            const apiSounds = Array.isArray(data) ? data : (data.content || []);
            const transformedSounds = transformApiSounds(apiSounds);
            setSounds(transformedSounds);

            // Update favorites based on sounds with favorite=true from backend
            const newFavorites = new Set<string>();
            transformedSounds.forEach(sound => {
              if (sound.favorite) {
                newFavorites.add(sound.id);
              }
            });

            console.log('🔄 SSE Update - Favorites from backend:', Array.from(newFavorites));
            console.log('🔄 SSE Update - Total sounds received:', transformedSounds.length);
            console.log('🔄 SSE Update - Favorited sounds:', transformedSounds.filter(s => s.favorite).map(s => ({ id: s.id, favorite: s.favorite })));

            setFavorites(newFavorites);

            setLoading(false);
            setConnectionStatus('connected');
          } catch (error) {
            console.error('Error parsing SSE data:', error);
          }
        });
      } catch (error) {
        console.error('Failed to create SSE connection:', error);
        setConnectionStatus('error');

        // Try to reconnect on exception as well
        if (isMounted && reconnectAttempts < MAX_RECONNECT_ATTEMPTS) {
          reconnectAttempts++;
          const delay = Math.min(
            BASE_RECONNECT_DELAY * Math.pow(2, reconnectAttempts - 1),
            MAX_RECONNECT_DELAY
          );

          reconnectTimeout = setTimeout(() => {
            if (isMounted) {
              connectToSounds();
            }
          }, delay);
        }
      }
    };

    // Initial connection
    connectToSounds();

    // Load favorites from localStorage
    const savedFavorites = localStorage.getItem('soundboard-favorites');
    if (savedFavorites) {
      setFavorites(new Set(JSON.parse(savedFavorites)));
    }

    // Load theme from localStorage
    const savedTheme = localStorage.getItem('soundboard-theme');
    if (savedTheme) {
      setTheme(savedTheme as 'light' | 'dark');
    }

    // Load volume from localStorage
    const savedVolume = localStorage.getItem('soundboard-volume');
    if (savedVolume) {
      setVolume(parseInt(savedVolume, 10));
    }

    // Load settings from localStorage
    const savedPopularCount = localStorage.getItem('soundboard-popular-count');
    if (savedPopularCount) {
      setPopularCount(parseInt(savedPopularCount, 10));
    }

    const savedRecentCount = localStorage.getItem('soundboard-recent-count');
    if (savedRecentCount) {
      setRecentCount(parseInt(savedRecentCount, 10));
    }

    // Cleanup: close the SSE connection when component unmounts
    return () => {
      isMounted = false;
      if (reconnectTimeout) {
        clearTimeout(reconnectTimeout);
      }
      if (eventSource) {
        eventSource.close();
      }
    };
  }, []);

  useEffect(() => {
    // Save favorites to localStorage
    localStorage.setItem('soundboard-favorites', JSON.stringify(Array.from(favorites)));
  }, [favorites]);

  useEffect(() => {
    // Save theme to localStorage
    localStorage.setItem('soundboard-theme', theme);
  }, [theme]);

  useEffect(() => {
    // Save volume to localStorage
    localStorage.setItem('soundboard-volume', volume.toString());
  }, [volume]);

  useEffect(() => {
    // Save settings to localStorage
    localStorage.setItem('soundboard-popular-count', popularCount.toString());
    localStorage.setItem('soundboard-recent-count', recentCount.toString());
  }, [popularCount, recentCount]);

  // SSE connection for volume updates based on selected user
  useEffect(() => {
    if (!selectedUserId) {
      console.log('No user selected, skipping volume SSE connection');
      return; // No user selected, don't connect
    }

    console.log('🚀 Setting up volume SSE connection for user:', selectedUserId);
    let volumeEventSource: EventSource | null = null;
    let isMounted = true;

    // First, fetch the current volume for this user
    const fetchInitialVolume = async () => {
      try {
        console.log('📥 Fetching initial volume for user:', selectedUserId);
        const response = await fetch(`${API_ENDPOINTS.VOLUME}/${selectedUserId}`);
        if (response.ok) {
          const volumeValue = await response.text();
          const parsedVolume = parseInt(volumeValue, 10);
          if (!isNaN(parsedVolume) && parsedVolume >= 0 && parsedVolume <= 100) {
            console.log('✅ Initial volume fetched:', parsedVolume);
            setVolume(parsedVolume);
          } else {
            console.warn('Invalid initial volume value:', volumeValue);
          }
        } else {
          console.warn('Failed to fetch initial volume:', response.status);
        }
      } catch (error) {
        console.error('Error fetching initial volume:', error);
      }
    };

    fetchInitialVolume();

    // Add a small delay to ensure initial fetch completes before opening SSE
    const sseTimeout = setTimeout(() => {
      if (!isMounted) return;

      try {
        const sseUrl = `${API_ENDPOINTS.VOLUME_STREAM}/${selectedUserId}`;
        console.log('🔌 Creating EventSource for:', sseUrl);
        volumeEventSource = new EventSource(sseUrl);

        // CATCH ALL - Log absolutely everything
        const originalDispatchEvent = volumeEventSource.dispatchEvent.bind(volumeEventSource);
        volumeEventSource.dispatchEvent = function(event: Event) {
          console.log('🌊 RAW EVENT DISPATCHED:', event.type, event);
          return originalDispatchEvent(event);
        };

        // Log all possible event types
        volumeEventSource.onopen = () => {
          if (!isMounted) return;
          console.log('✅ Volume SSE connection OPENED successfully for user:', selectedUserId);
          console.log('📊 Volume SSE readyState:', volumeEventSource?.readyState, '(0=CONNECTING, 1=OPEN, 2=CLOSED)');
        };

        volumeEventSource.onmessage = (event) => {
          console.log('🔔 Volume SSE onmessage fired!');
          console.log('📦 Raw event object:', event);
          console.log('📦 Event type:', event.type);
          console.log('📦 Event data:', event.data);
          console.log('📦 Event lastEventId:', event.lastEventId);

          if (!isMounted) {
            console.log('⚠️ Component unmounted, ignoring message');
            return;
          }
          try {
            console.log('Processing volume SSE message:', event.data);
            const volumeValue = parseInt(event.data, 10);
            console.log('Parsed volume value:', volumeValue);
            if (!isNaN(volumeValue) && volumeValue >= 0 && volumeValue <= 100) {
              console.log('✅ VALID volume value, calling setVolume with:', volumeValue);
              setVolume(volumeValue);
              console.log('✅ setVolume called successfully');
            } else {
              console.warn('❌ Invalid volume value received:', event.data);
            }
          } catch (error) {
            console.error('Error parsing volume SSE data:', error);
          }
        };

        // Listen for named 'volume' events
        volumeEventSource.addEventListener('volume', (event) => {
          console.log('🔔 Volume SSE NAMED EVENT received!');
          console.log('📦 Named event data:', event.data);

          if (!isMounted) {
            console.log('⚠️ Component unmounted, ignoring named event');
            return;
          }
          try {
            console.log('Processing volume SSE named event:', event.data);
            const volumeValue = parseInt(event.data, 10);
            console.log('Parsed named event volume:', volumeValue);
            if (!isNaN(volumeValue) && volumeValue >= 0 && volumeValue <= 100) {
              console.log('✅ VALID named event volume, calling setVolume with:', volumeValue);
              setVolume(volumeValue);
              console.log('✅ setVolume called successfully from named event');
            } else {
              console.warn('❌ Invalid volume value from named event:', event.data);
            }
          } catch (error) {
            console.error('Error parsing volume SSE named event data:', error);
          }
        });

        // Listen for 'globalVolume' events from backend
        volumeEventSource.addEventListener('globalVolume', (event) => {
          console.log('🔔 Volume SSE GLOBALVOLUME EVENT received!');
          console.log('📦 GlobalVolume event data:', event.data);

          if (!isMounted) {
            console.log('⚠ Component unmounted, ignoring globalVolume event');
            return;
          }
          try {
            console.log('Processing globalVolume SSE event:', event.data);
            const volumeValue = parseFloat(event.data);

            // Backend sends volume as percentage (0-100), not decimal (0-1)
            const volumePercentage = Math.round(volumeValue);
            console.log('Parsed globalVolume value:', volumeValue, '-> percentage:', volumePercentage);

            if (!isNaN(volumePercentage) && volumePercentage >= 0 && volumePercentage <= 100) {
              console.log('✅ VALID globalVolume value, calling setVolume with:', volumePercentage);
              setVolume(volumePercentage);
              console.log('✅ setVolume called successfully from globalVolume event');
            } else {
              console.warn('❌ Invalid globalVolume value:', event.data);
            }
          } catch (error) {
            console.error('Error parsing globalVolume SSE event data:', error);
          }
        });

        // Listen for ALL event types to debug
        ['message', 'volume', 'update', 'change'].forEach(eventType => {
          volumeEventSource?.addEventListener(eventType, (event) => {
            console.log(`🎯 Received event of type "${eventType}":`, event);
          });
        });

        volumeEventSource.onerror = (error) => {
          console.error('❌ Volume SSE connection ERROR:', error);
          console.log('📊 Volume SSE readyState:', volumeEventSource?.readyState, '(0=CONNECTING, 1=OPEN, 2=CLOSED)');
          console.log('🔗 Volume SSE url:', volumeEventSource?.url);

          if (volumeEventSource?.readyState === EventSource.CLOSED) {
            console.error('🚫 SSE connection is CLOSED - will not receive updates');
          } else if (volumeEventSource?.readyState === EventSource.CONNECTING) {
            console.log('🔄 SSE connection is CONNECTING - retrying...');
          }
        };

        console.log('✅ EventSource created, waiting for events...');
      } catch (error) {
        console.error('Failed to create volume SSE connection:', error);
      }
    }, 100);

    return () => {
      console.log('🧹 Cleaning up volume SSE connection for user:', selectedUserId);
      clearTimeout(sseTimeout);
      isMounted = false;
      if (volumeEventSource) {
        console.log('🔌 Closing volume EventSource, readyState was:', volumeEventSource.readyState);
        volumeEventSource.close();
        console.log('✅ Volume SSE connection closed');
      }
    };
  }, [selectedUserId]);

  // SSE connection for playback status tracking
  useEffect(() => {
    let playbackEventSource: EventSource | null = null;
    let isMounted = true;
    let reconnectTimeout: ReturnType<typeof setTimeout> | null = null;
    let reconnectAttempts = 0;
    const MAX_RECONNECT_ATTEMPTS = 10;
    const BASE_RECONNECT_DELAY = 1000; // 1 second
    const MAX_RECONNECT_DELAY = 30000; // 30 seconds

    const connectToPlayback = () => {
      // Close existing connection if any
      if (playbackEventSource) {
        playbackEventSource.close();
        playbackEventSource = null;
      }

      try {
        console.log('📡 Connecting to playback SSE endpoint:', API_ENDPOINTS.PLAYBACK_STREAM);
        playbackEventSource = new EventSource(API_ENDPOINTS.PLAYBACK_STREAM);

        playbackEventSource.onopen = () => {
          console.log('✅ Playback SSE connection established');
          reconnectAttempts = 0; // Reset reconnect attempts on successful connection
        };

        // Listen for track start event
        playbackEventSource.addEventListener('trackStart', (event) => {
          if (!isMounted) return;
          try {
            const data = JSON.parse(event.data);
            console.log('🎵 Track started:', data);
            if (data.soundFileId) {
              setCurrentlyPlayingSoundId(data.soundFileId);
            }
          } catch (error) {
            console.error('Error parsing trackStart event:', error);
          }
        });

        // Listen for track end event
        playbackEventSource.addEventListener('trackEnd', (event) => {
          if (!isMounted) return;
          try {
            const data = JSON.parse(event.data);
            console.log('🎵 Track ended:', data);
            setCurrentlyPlayingSoundId(null);
          } catch (error) {
            console.error('Error parsing trackEnd event:', error);
          }
        });

        playbackEventSource.onerror = (error) => {
          // Only log errors if it's the first error or we've exceeded max attempts
          if (reconnectAttempts === 0) {
            console.warn('⚠️ Playback SSE connection interrupted, will attempt to reconnect...');
          }

          // Close the connection
          if (playbackEventSource) {
            playbackEventSource.close();
            playbackEventSource = null;
          }

          // Attempt to reconnect if not unmounted and haven't exceeded max attempts
          if (isMounted && reconnectAttempts < MAX_RECONNECT_ATTEMPTS) {
            reconnectAttempts++;
            const delay = Math.min(
              BASE_RECONNECT_DELAY * Math.pow(2, reconnectAttempts - 1),
              MAX_RECONNECT_DELAY
            );

            reconnectTimeout = setTimeout(() => {
              if (isMounted) {
                connectToPlayback();
              }
            }, delay);
          } else if (reconnectAttempts >= MAX_RECONNECT_ATTEMPTS) {
            console.error('❌ Max reconnection attempts reached for playback SSE. Connection failed.');
          }
        };
      } catch (error) {
        console.error('Failed to create playback SSE connection:', error);

        // Try to reconnect on exception as well
        if (isMounted && reconnectAttempts < MAX_RECONNECT_ATTEMPTS) {
          reconnectAttempts++;
          const delay = Math.min(
            BASE_RECONNECT_DELAY * Math.pow(2, reconnectAttempts - 1),
            MAX_RECONNECT_DELAY
          );

          reconnectTimeout = setTimeout(() => {
            if (isMounted) {
              connectToPlayback();
            }
          }, delay);
        }
      }
    };

    // Initial connection
    connectToPlayback();

    return () => {
      isMounted = false;
      if (reconnectTimeout) {
        clearTimeout(reconnectTimeout);
      }
      if (playbackEventSource) {
        console.log('🧹 Closing playback SSE connection');
        playbackEventSource.close();
      }
    };
  }, []);

  // Auto-focus search box when user starts typing
  useEffect(() => {
    const handleKeyDown = (event: KeyboardEvent) => {
      // Check if user is typing a letter or number
      const isTypingChar = event.key.length === 1 && /[a-zA-Z0-9]/.test(event.key);

      // Don't interfere if user is already in an input, textarea, or select
      const activeElement = document.activeElement;
      const isInInputField = activeElement instanceof HTMLInputElement ||
                            activeElement instanceof HTMLTextAreaElement ||
                            activeElement instanceof HTMLSelectElement;

      // Don't interfere with keyboard shortcuts (Ctrl, Alt, Cmd, etc.)
      const hasModifier = event.ctrlKey || event.metaKey || event.altKey;

      if (isTypingChar && !isInInputField && !hasModifier && searchInputRef.current) {
        searchInputRef.current.focus();
      }
    };

    document.addEventListener('keydown', handleKeyDown);

    return () => {
      document.removeEventListener('keydown', handleKeyDown);
    };
  }, []);

  const toggleFavorite = async (soundId: string) => {
    const isFavorite = favorites.has(soundId);
    const newFavoriteState = !isFavorite;

    // Generate unique call ID to track if this function is called multiple times
    const callId = Math.random().toString(36).substring(7);

    // Check if a toggle is already in progress for this sound
    if (toggleFavoriteInProgressRef.current.has(soundId)) {
      console.log(`[${callId}] toggleFavorite called for ${soundId}, but a toggle is already in progress`);
      return;
    }

    // Mark this sound as being toggled
    toggleFavoriteInProgressRef.current.add(soundId);

    try {
      console.log(`[${callId}] toggleFavorite called for ${soundId}, changing to ${newFavoriteState}`);
      console.trace(`[${callId}] Call stack`);

      // Call the API to update favorite status on the backend
      console.log(`[${callId}] About to call fetch...`);
      const response = await fetch(
        `${API_ENDPOINTS.FAVORITE}/${soundId}?favorite=${newFavoriteState}`,
        {
          method: 'POST',
          mode: 'cors',
          headers: {
            'Content-Type': 'application/json',
          }
        }
      );
      console.log(`[${callId}] Fetch completed, response received`);

      if (!response.ok) {
        console.error(`[${callId}] Failed to update favorite status:`, response.status);
        throw new Error('Failed to update favorite status');
      }

      // Check if there's a response body before trying to read it
      const contentLength = response.headers.get('Content-Length');
      if (contentLength && contentLength !== '0') {
        await response.text();
      } else {
        // No body to consume, but we need to clone and read to fully close the connection
        await response.clone().text().catch(() => {});
      }

      console.log(`[${callId}] Favorite status updated successfully: ${newFavoriteState}`);

      // Update local state after successful API call
      console.log(`[${callId}] Updating local favorites state...`);
      setFavorites(prev => {
        const newFavorites = new Set(prev);
        if (newFavoriteState) {
          newFavorites.add(soundId);
          console.log(`[${callId}] Added ${soundId} to favorites. New favorites:`, Array.from(newFavorites));
        } else {
          newFavorites.delete(soundId);
          console.log(`[${callId}] Removed ${soundId} from favorites. New favorites:`, Array.from(newFavorites));
        }
        return newFavorites;
      });
    } catch (error) {
      console.error(`[${callId}] Error updating favorite status:`, error);
      alert('Failed to update favorite. Please try again.');
    } finally {
      // Remove the sound from the in-progress set
      toggleFavoriteInProgressRef.current.delete(soundId);
    }
  };

  const updateVolume = async (newVolume: number) => {
    if (!selectedUserId) return;

    // Update local state immediately for responsive UI
    setVolume(newVolume);

    try {
      const response = await fetch(
        `${API_ENDPOINTS.VOLUME}?username=${selectedUserId}&volume=${newVolume}`,
        {
          method: 'POST',
          mode: 'cors',
          headers: {
            'Content-Type': 'application/json',
          }
        }
      );

      if (!response.ok) {
        console.error('Failed to update volume:', response.status);
      } else {
        console.log(`Volume updated successfully: ${newVolume}%`);
      }
    } catch (error) {
      console.error('Error updating volume:', error);
      // Don't show an alert for volume updates to avoid interrupting the user
    }
  };

  const playSound = (url: string) => {
    const audio = new Audio(url);
    audio.play().catch(err => console.error('Error playing sound:', err));
  };

  const playLocalSound = async (soundId: string) => {
    try {
      // Stop any currently playing local audio
      if (currentLocalAudioRef.current) {
        currentLocalAudioRef.current.pause();
        currentLocalAudioRef.current = null;
      }

      const audioUrl = `${API_ENDPOINTS.AUDIO_FILE}/${soundId}`;
      const audio = new Audio(audioUrl);

      // Set up event listeners
      audio.addEventListener('ended', () => {
        setLocallyPlayingSoundId(null);
        currentLocalAudioRef.current = null;
      });

      audio.addEventListener('error', () => {
        console.error('Error playing sound locally');
        setLocallyPlayingSoundId(null);
        currentLocalAudioRef.current = null;
      });

      audio.play().catch(err => {
        console.error('Error playing sound locally:', err);
        setLocallyPlayingSoundId(null);
        currentLocalAudioRef.current = null;
      });

      setLocallyPlayingSoundId(soundId);
      currentLocalAudioRef.current = audio;
    } catch (error) {
      console.error('Error playing sound locally:', error);
      setLocallyPlayingSoundId(null);
    }
  };

  const stopLocalSound = () => {
    if (currentLocalAudioRef.current) {
      currentLocalAudioRef.current.pause();
      currentLocalAudioRef.current = null;
      setLocallyPlayingSoundId(null);
    }
  };

  const playSoundWithBot = async (soundId: string) => {
    // Check if a user is selected
    if (!selectedUserId) {
      alert('Please select a user from the Active Users list before playing a sound.');
      return;
    }

    // Check if playback is enabled (user must be in voice)
    if (!isPlaybackEnabled) {
      alert('The selected user must be in a voice channel to play sounds.');
      return;
    }

    try {
      const response = await fetch(
        `${API_ENDPOINTS.PLAY_FILE}?soundFileId=${soundId}&username=${selectedUserId}`,
        {
          method: 'POST',
          mode: 'cors'
        }
      );

      if (!response.ok) {
        const errorText = await response.text().catch(() => 'Unknown error');
        console.error(`Failed to play sound through bot: ${response.status} - ${errorText}`);
        throw new Error(`Failed to play sound through bot: ${response.status}`);
      }

      console.log('Sound played successfully through bot');
      setCurrentlyPlayingSoundId(soundId);
    } catch (error) {
      console.error('Error playing sound through bot:', error);
      // Only show alert for actual errors, not successful plays
      if (error instanceof TypeError || (error instanceof Error && error.message.includes('Failed to play'))) {
        if (error instanceof TypeError) {
          alert('Failed to play sound. Please make sure the backend is running');
        } else {
          alert(`Failed to play sound: ${error.message}`);
        }
      }
    }
  };

  const deleteSound = (soundId: string) => {
    setSounds(prev => prev.filter(s => s.id !== soundId));
    // Also remove from favorites if it was favorited
    setFavorites(prev => {
      const newFavorites = new Set(prev);
      newFavorites.delete(soundId);
      return newFavorites;
    });
  };

  const downloadSound = (sound: Sound) => {
    const link = document.createElement('a');
    link.href = `${API_ENDPOINTS.DOWNLOAD}/${sound.id}`;
    link.download = `${sound.name}.ogg`;
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
  };

  const handleContextMenu = (e: React.MouseEvent, soundId: string) => {
    e.preventDefault();
    setContextMenu({
      x: e.clientX,
      y: e.clientY,
      soundId
    });
  };

  const playRandomSound = async () => {
    if (!isPlaybackEnabled || filteredSounds.length === 0) {
      alert('Please select a user from the Active Users list before playing a sound.');
      return;
    }

    try {
      const response = await fetch(
        `${API_ENDPOINTS.RANDOM}?username=${selectedUserId}`,
        {
          method: 'POST',
          mode: 'cors'
        }
      );

      if (!response.ok) {
        const errorText = await response.text().catch(() => 'Unknown error');
        console.error(`Failed to play random sound: ${response.status} - ${errorText}`);
        throw new Error(`Failed to play random sound: ${response.status}`);
      }

      console.log('Random sound played successfully');
    } catch (error) {
      console.error('Error playing random sound:', error);
      if (error instanceof TypeError) {
        alert('Failed to play random sound. Please make sure the backend is running');
      } else if (error instanceof Error) {
        alert(`Failed to play random sound: ${error.message}`);
      }
    }
  };

  const stopCurrentSound = async () => {
    if (!isPlaybackEnabled) {
      alert('Please select a user from the Active Users list.');
      return;
    }

    try {
      const response = await fetch(
        `${API_ENDPOINTS.STOP}?username=${selectedUserId}`,
        {
          method: 'POST',
          mode: 'cors'
        }
      );

      if (!response.ok) {
        const errorText = await response.text().catch(() => 'Unknown error');
        console.error(`Failed to stop sound: ${response.status} - ${errorText}`);
        throw new Error(`Failed to stop sound: ${response.status}`);
      }

      console.log('Sound stopped successfully');
      setCurrentlyPlayingSoundId(null);
    } catch (error) {
      console.error('Error stopping sound:', error);
      if (error instanceof TypeError) {
        alert('Failed to stop sound. Please make sure the backend is running');
      } else if (error instanceof Error) {
        alert(`Failed to stop sound: ${error.message}`);
      }
    }
  };

  const handleFileUpload = async (event: React.ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0];
    if (!file) return;

    // Validate file type (audio files only)
    const validTypes = ['audio/mpeg', 'audio/wav', 'audio/ogg', 'audio/mp3', 'audio/webm', 'audio/flac'];
    if (!validTypes.includes(file.type) && !file.name.match(/\.(mp3|wav|ogg|webm|flac)$/i)) {
      alert('Please upload a valid audio file (MP3, WAV, OGG, WEBM, or FLAC)');
      event.target.value = ''; // Reset file input
      return;
    }

    // Optional: Check file size (e.g., max 10MB)
    const maxSize = 10 * 1024 * 1024; // 10MB
    if (file.size > maxSize) {
      alert('File size must be less than 10MB');
      event.target.value = ''; // Reset file input
      return;
    }

    const formData = new FormData();
    formData.append('file', file);

    try {
      console.log('📤 Uploading file:', file.name);

      // Get auth headers
      const authHeaders = getAuthHeaders();
      console.log('📋 Auth headers:', authHeaders);

      const response = await fetch(API_ENDPOINTS.UPLOAD, {
        method: 'POST',
        mode: 'cors',
        headers: authHeaders,
        body: formData
      });

      if (response.status === 403) {
        alert('Permission denied: You do not have permission to upload sounds.');
        event.target.value = '';
        return;
      }

      if (!response.ok) {
        const errorText = await response.text().catch(() => 'Unknown error');
        console.error(`Failed to upload file: ${response.status} - ${errorText}`);
        throw new Error(`Failed to upload file: ${response.status}`);
      }

      console.log('✅ File uploaded successfully');
      alert(`File "${file.name}" uploaded successfully!`);

      // Reset file input so the same file can be uploaded again if needed
      event.target.value = '';
    } catch (error) {
      console.error('Error uploading file:', error);
      if (error instanceof TypeError) {
        alert('Failed to upload file. Please make sure the backend is running');
      } else if (error instanceof Error) {
        alert(`Failed to upload file: ${error.message}`);
      }
      event.target.value = ''; // Reset file input
    }
  };

  // Get unique categories
  const categories = ['all', ...Array.from(new Set(sounds.map(s => s.category)))];

  // Calculate top 10 sounds by timesPlayed (excluding already favorited sounds)
  const top10SoundIds = new Set(
    [...sounds]
      .filter(sound => !favorites.has(sound.id))
      .sort((a, b) => b.timesPlayed - a.timesPlayed)
      .slice(0, popularCount)
      .map(s => s.id)
  );

  // Calculate top 10 most recently added sounds (excluding favorites and top played)
  const recentlyAddedIds = new Set(
    [...sounds]
      .filter(sound => !favorites.has(sound.id) && !top10SoundIds.has(sound.id))
      .sort((a, b) => new Date(b.dateAdded).getTime() - new Date(a.dateAdded).getTime())
      .slice(0, recentCount)
      .map(s => s.id)
  );

  // Filter sounds
  const filteredSounds = sounds
    .filter(sound => {
      const matchesCategory = selectedCategory === 'all' || sound.category === selectedCategory;

      // Apply active filter
      let matchesFilter = true;
      if (activeFilter === 'favorites') {
        matchesFilter = favorites.has(sound.id);
      } else if (activeFilter === 'popular') {
        matchesFilter = top10SoundIds.has(sound.id);
      } else if (activeFilter === 'recent') {
        matchesFilter = recentlyAddedIds.has(sound.id);
      }

      const matchesSearch = sound.name.toLowerCase().includes(searchQuery.toLowerCase());
      return matchesCategory && matchesFilter && matchesSearch;
    })
    .sort((a, b) => a.name.localeCompare(b.name));

  if (loading) {
    return (
      <div className={`min-h-screen ${theme === 'dark' ? 'bg-gradient-to-br from-gray-900 to-gray-800' : 'bg-gradient-to-br from-blue-50 to-blue-100'} flex items-center justify-center`}>
        <div className="text-center">
          <div className="w-12 h-12 border-4 border-blue-500 border-t-transparent rounded-full animate-spin mx-auto mb-4"></div>
          <p className={theme === 'dark' ? 'text-gray-300' : 'text-gray-600'}>Loading sounds...</p>
        </div>
      </div>
    );
  }

  return (
    <div className={`min-h-screen ${theme === 'dark' ? 'bg-gradient-to-br from-gray-900 to-gray-800' : 'bg-gradient-to-br from-blue-50 to-blue-100'} p-6`}>
      <div>
        <header className="mb-4 flex items-start justify-between">
          <div>
            <h1 className={`${theme === 'dark' ? 'text-blue-400' : 'text-blue-900'} mb-2 flex items-center gap-3`}>
              <img
                src="/favicon.png"
                alt="Discord Soundboard Logo"
                className="w-8 h-8 rounded-full"
              />
              Discord Soundboard
            </h1>
          </div>

          <div className="flex items-center gap-3">
            {/* Upload Button - Only visible with upload permission */}
            {authUser?.permissions?.upload && (
              <>
                <button
                  onClick={() => fileInputRef.current?.click()}
                  className={`flex items-center gap-2 px-4 py-2 rounded-lg transition-colors ${
                    theme === 'dark'
                      ? 'bg-blue-700 text-white hover:bg-blue-600'
                      : 'bg-blue-600 text-white hover:bg-blue-700 shadow-md'
                  }`}
                  aria-label="Upload sound file"
                >
                  <Upload className="w-5 h-5" />
                  Upload
                </button>
                <input
                  ref={fileInputRef}
                  type="file"
                  accept="audio/*,.mp3,.wav,.ogg,.webm,.flac"
                  onChange={handleFileUpload}
                  className="hidden"
                  aria-label="File upload input"
                />
              </>
            )}

            {/* Users Button - Only visible with manage-users permission */}
            {authUser?.permissions?.manageUsers && (
              <button
                onClick={() => setShowUsersOverlay(true)}
                className={`flex items-center gap-2 px-4 py-2 rounded-lg transition-colors ${
                  theme === 'dark'
                    ? 'bg-teal-700 text-white hover:bg-teal-600'
                    : 'bg-teal-600 text-white hover:bg-teal-700 shadow-md'
                }`}
                aria-label="Manage users"
              >
                <Users className="w-5 h-5" />
                Users
              </button>
            )}

            {/* Auth Button */}
            <AuthButton
              user={authUser}
              onLogin={handleLogin}
              onLogout={handleLogout}
              theme={theme}
            />

            {/* Settings Button - Far Right */}
            <button
              onClick={(e) => {
                const rect = e.currentTarget.getBoundingClientRect();
                setSettingsMenu({
                  x: rect.right - 280, // Position to align right edge of menu with button
                  y: rect.bottom + 8, // 8px below button
                });
              }}
              className={`flex items-center justify-center p-2 rounded-lg transition-colors ${
                theme === 'dark'
                  ? 'bg-gray-700 text-gray-300 hover:bg-gray-600'
                  : 'bg-white text-gray-700 hover:bg-gray-100 shadow-md'
              }`}
              aria-label="Settings"
            >
              <Settings className="w-5 h-5" />
            </button>
          </div>
        </header>

        {/* Main Content Area */}
        <div>
          {/* Filters */}
          <div className={`${theme === 'dark' ? 'bg-gray-800 border-gray-700' : 'bg-white'} rounded-lg shadow-md p-6 mb-6 ${theme === 'dark' ? 'border' : ''}`}>
            <div className="grid grid-cols-1 lg:grid-cols-[1fr_320px] gap-6">
              {/* Left side - Search and filters */}
              <div>
                {/* Search */}
                <div className="mb-4">
                  <div className="relative">
                    <Search className={`absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 ${theme === 'dark' ? 'text-gray-500' : 'text-gray-400'}`} />
                    <input
                      type="text"
                      placeholder="Search sounds..."
                      value={searchQuery}
                      onChange={(e) => setSearchQuery(e.target.value)}
                      onKeyDown={(e) => {
                        if (e.key === 'Enter' && filteredSounds.length === 1) {
                          // Play the single filtered sound when Enter is pressed
                          playSoundWithBot(filteredSounds[0].id);
                        }
                      }}
                      className={`w-full pl-10 pr-4 py-2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent ${
                        theme === 'dark'
                          ? 'bg-gray-700 border-gray-600 text-white placeholder-gray-400'
                          : 'bg-white border-gray-300 text-gray-900 placeholder-gray-500'
                      }`}
                      ref={searchInputRef}
                    />
                  </div>
                  <p className={`text-sm mt-2 ${theme === 'dark' ? 'text-gray-400' : 'text-gray-600'}`}>
                    Search from {filteredSounds.length} sound{filteredSounds.length !== 1 ? 's' : ''}
                  </p>
                </div>

                {/* Category Filter */}
                <div className="mb-4 flex items-center gap-3">
                  <label className={theme === 'dark' ? 'text-gray-300' : 'text-gray-700'}>Category</label>
                  <select
                    value={selectedCategory}
                    onChange={(e) => {
                      setSelectedCategory(e.target.value);
                      // Disable all filters when selecting a specific category
                      if (e.target.value !== 'all') {
                        setActiveFilter('none');
                      }
                    }}
                    className={`flex-1 px-4 py-2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-purple-500 focus:border-transparent ${
                      theme === 'dark'
                        ? 'bg-gray-700 border-gray-600 text-white'
                        : 'bg-white border-gray-300 text-gray-900'
                    }`}
                  >
                    {categories.map(category => (
                      <option key={category} value={category}>
                        {category.charAt(0).toUpperCase() + category.slice(1)}
                      </option>
                    ))}
                  </select>
                </div>

                {/* Filter Buttons and Action Buttons Row - All on one line when possible */}
                <div className="flex flex-wrap items-center gap-3">
                  {/* Filter Buttons */}
                  <button
                    onClick={() => {
                      setActiveFilter(activeFilter === 'favorites' ? 'none' : 'favorites');
                      // Reset category to "all" when showing favorites
                      if (activeFilter !== 'favorites') {
                        setSelectedCategory('all');
                      }
                    }}
                    className={`flex items-center gap-2 px-4 py-2 rounded-lg transition-colors ${
                      activeFilter === 'favorites'
                        ? 'bg-yellow-600 text-white'
                        : theme === 'dark'
                        ? 'bg-gray-700 text-gray-300 hover:bg-gray-600'
                        : 'bg-gray-100 text-gray-700 hover:bg-gray-200'
                    }`}
                  >
                    <Star className={`w-5 h-5 ${activeFilter === 'favorites' ? 'fill-current' : ''}`} />
                    {activeFilter === 'favorites' ? 'Favorites' : 'Favorites'}
                  </button>

                  <button
                    onClick={() => {
                      setActiveFilter(activeFilter === 'popular' ? 'none' : 'popular');
                      // Reset category to "all" when showing popular
                      if (activeFilter !== 'popular') {
                        setSelectedCategory('all');
                      }
                    }}
                    className={`flex items-center gap-2 px-4 py-2 rounded-lg transition-colors ${
                      activeFilter === 'popular'
                        ? 'bg-gradient-to-br from-amber-500 to-orange-600 text-white'
                        : theme === 'dark'
                        ? 'bg-gray-700 text-gray-300 hover:bg-gray-600'
                        : 'bg-gray-100 text-gray-700 hover:bg-gray-200'
                    }`}
                  >
                    <Grid3x3 className="w-5 h-5" />
                    {activeFilter === 'popular' ? 'Popular' : 'Popular'}
                  </button>

                  <button
                    onClick={() => {
                      setActiveFilter(activeFilter === 'recent' ? 'none' : 'recent');
                      // Reset category to "all" when showing recent
                      if (activeFilter !== 'recent') {
                        setSelectedCategory('all');
                      }
                    }}
                    className={`flex items-center gap-2 px-4 py-2 rounded-lg transition-colors ${
                      activeFilter === 'recent'
                        ? 'bg-green-600 text-white'
                        : theme === 'dark'
                        ? 'bg-gray-700 text-gray-300 hover:bg-gray-600'
                        : 'bg-gray-100 text-gray-700 hover:bg-gray-200'
                    }`}
                  >
                    <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" />
                    </svg>
                    {activeFilter === 'recent' ? 'Recent' : 'Recent'}
                  </button>

                  {/* Volume Slider - Reserve space even when hidden */}
                  <div className={`flex items-center gap-3 flex-1 min-w-[200px] ${selectedUserId && isPlaybackEnabled ? '' : 'invisible'}`}>
                    <Volume2 className={`w-5 h-5 ${theme === 'dark' ? 'text-gray-400' : 'text-gray-600'}`} />
                    <input
                      type="range"
                      min="0"
                      max="100"
                      value={volume}
                      onChange={(e) => setVolume(parseInt(e.target.value, 10))}
                      onMouseUp={(e) => updateVolume(parseInt((e.target as HTMLInputElement).value, 10))}
                      onTouchEnd={(e) => updateVolume(parseInt((e.target as HTMLInputElement).value, 10))}
                      className="flex-1 h-2 bg-gray-200 rounded-lg appearance-none cursor-pointer dark:bg-gray-700"
                      style={{
                        background: `linear-gradient(to right, ${theme === 'dark' ? '#3b82f6' : '#60a5fa'} 0%, ${theme === 'dark' ? '#3b82f6' : '#60a5fa'} ${volume}%, ${theme === 'dark' ? '#374151' : '#e5e7eb'} ${volume}%, ${theme === 'dark' ? '#374151' : '#e5e7eb'} 100%)`
                      }}
                      disabled={!selectedUserId || !isPlaybackEnabled}
                    />
                    <span className={`min-w-[3rem] text-right ${theme === 'dark' ? 'text-gray-300' : 'text-gray-700'}`}>
                      {volume}%
                    </span>
                  </div>

                  {/* Play Random Sound Button */}
                  <button
                    onClick={playRandomSound}
                    disabled={!isPlaybackEnabled || filteredSounds.length === 0}
                    className={`flex items-center gap-2 px-4 py-2 rounded-lg transition-colors ${
                      !isPlaybackEnabled || filteredSounds.length === 0
                        ? 'opacity-50 cursor-not-allowed bg-gray-600 text-gray-400'
                        : theme === 'dark'
                        ? 'bg-blue-700 text-white hover:bg-blue-600'
                        : 'bg-blue-600 text-white hover:bg-blue-700'
                    }`}
                    title={!isPlaybackEnabled ? 'User must be in voice channel' : 'Play random sound from filtered list'}
                  >
                    <Shuffle className="w-5 h-5" />
                    Random
                  </button>

                  {/* Stop Sound Button */}
                  <button
                    onClick={stopCurrentSound}
                    disabled={!isPlaybackEnabled}
                    className={`flex items-center gap-2 px-4 py-2 rounded-lg transition-colors ${
                      !isPlaybackEnabled
                        ? 'opacity-50 cursor-not-allowed bg-gray-600 text-gray-400'
                        : theme === 'dark'
                        ? 'bg-red-700 text-white hover:bg-red-600'
                        : 'bg-red-600 text-white hover:bg-red-700'
                    }`}
                    title={!isPlaybackEnabled ? 'User must be in voice channel' : 'Stop currently playing sound'}
                  >
                    <StopCircle className="w-5 h-5" />
                    Stop
                  </button>
                </div>
              </div>

              {/* Right side - Discord Users */}
              <div>
                <DiscordUsersList
                  theme={theme}
                  onUserSelect={setSelectedUserId}
                  selectedUserId={selectedUserId}
                  onVolumeUpdate={setVolume}
                  onPlaybackEnabledChange={setIsPlaybackEnabled}
                />
              </div>
            </div>
          </div>

          {/* Sound Grid */}
          <div className="grid grid-cols-4 sm:grid-cols-6 md:grid-cols-8 lg:grid-cols-10 xl:grid-cols-12 2xl:grid-cols-14 gap-2">
            {filteredSounds.map(sound => (
              <SoundButton
                key={sound.id}
                sound={sound}
                isFavorite={favorites.has(sound.id)}
                isTopPlayed={top10SoundIds.has(sound.id)}
                isRecentlyAdded={recentlyAddedIds.has(sound.id)}
                onPlay={() => playSoundWithBot(sound.id)}
                onToggleFavorite={() => toggleFavorite(sound.id)}
                onContextMenu={(e) => handleContextMenu(e, sound.id)}
                theme={theme}
                disabled={!isPlaybackEnabled}
                isCurrentlyPlaying={currentlyPlayingSoundId === sound.id}
                isLocallyPlaying={locallyPlayingSoundId === sound.id}
                onStopLocalPlayback={stopLocalSound}
              />
            ))}
          </div>

          {/* No results */}
          {filteredSounds.length === 0 && (
            <div className="text-center py-12">
              <p className={theme === 'dark' ? 'text-gray-400' : 'text-gray-500'}>No sounds found matching your filters</p>
            </div>
          )}
        </div>

        {/* Context Menu */}
        {contextMenu && (() => {
          const sound = sounds.find(s => s.id === contextMenu.soundId);
          return sound ? (
            <ContextMenu
              x={contextMenu.x}
              y={contextMenu.y}
              onClose={() => setContextMenu(null)}
              onFavorite={async () => {
                await toggleFavorite(contextMenu.soundId);
                setContextMenu(null);
              }}
              onDelete={() => deleteSound(contextMenu.soundId)}
              onDownload={() => downloadSound(sound)}
              onPlayLocally={() => playLocalSound(contextMenu.soundId)}
              isFavorite={favorites.has(contextMenu.soundId)}
              theme={theme}
              timesPlayed={sound.timesPlayed}
              dateAdded={sound.dateAdded}
              volumeOffset={sound.volumeOffset}
              soundId={sound.id}
              displayName={sound.displayName ?? null}
              category={sound.category}
              canEditSounds={authUser?.permissions?.editSounds ?? false}
              canDeleteSounds={authUser?.permissions?.deleteSounds ?? false}
            />
          ) : null;
        })()}

        {/* Users Overlay */}
        <UsersOverlay
          isOpen={showUsersOverlay}
          onClose={() => setShowUsersOverlay(false)}
          theme={theme}
          sounds={sounds.map(s => ({ id: s.id, name: s.name }))}
        />

        {/* Settings Menu */}
        {settingsMenu && (
          <SettingsMenu
            x={settingsMenu.x}
            y={settingsMenu.y}
            onClose={() => setSettingsMenu(null)}
            theme={theme}
            popularCount={popularCount}
            recentCount={recentCount}
            onPopularCountChange={setPopularCount}
            onRecentCountChange={setRecentCount}
            onThemeChange={setTheme}
          />
        )}
      </div>
    </div>
  );
}