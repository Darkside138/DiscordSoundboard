import React, { useState, useEffect, useRef } from 'react';
import { SoundButton } from './components/SoundButton';
import { ContextMenu } from './components/ContextMenu';
import { DiscordUsersList } from './components/DiscordUsersList';
import { UsersOverlay } from './components/UsersOverlay';
import { SettingsMenu } from './components/SettingsMenu';
import { RoleManagementDialog } from './components/RoleManagementDialog';
import { AuthButton } from './components/AuthButton';
import { Search, Star, Trophy, Sparkles, Volume2, Shuffle, StopCircle, Settings, X, Music, History, PlayCircle } from 'lucide-react';
import { toast, Toaster } from 'sonner@2.0.3';

// Custom hooks
import { useAuth } from './hooks/useAuth';
import { useTheme } from './hooks/useTheme';
import { useSounds } from './hooks/useSounds';
import { useVolume } from './hooks/useVolume';
import { useVolumeSSE } from './hooks/useVolumeSSE';
import { usePlaybackTracking } from './hooks/usePlaybackTracking';
import { useLocalPlayback } from './hooks/useLocalPlayback';
import { useSoundActions } from './hooks/useSoundActions';
import { useFilters } from './hooks/useFilters';
import { usePlaybackHistory } from './hooks/usePlaybackHistory';

export default function App() {
  // Authentication
  const { authUser, authLoading, handleLogin, handleLogout, refreshAuthToken } = useAuth();

  // Theme
  const { theme, setTheme } = useTheme();

  // Sounds data
  const { sounds, setSounds, favorites, setFavorites, loading, connectionStatus } = useSounds();

  // Volume management
  const { volume, setVolume, updateVolume } = useVolume();

  // User selection and playback state
  const [selectedUserId, setSelectedUserId] = useState<string | null>(null);
  const [isPlaybackEnabled, setIsPlaybackEnabled] = useState<boolean>(false);
  const [selectedUserGuildId, setSelectedUserGuildId] = useState<string | null>(null);

  // Volume SSE connection
  useVolumeSSE({ selectedUserId, setVolume });

  // Playback tracking
  const { currentlyPlayingSoundId, setCurrentlyPlayingSoundId, currentPlayback } = usePlaybackTracking({
    selectedUserGuildId
  });

  // Local playback
  const { locallyPlayingSoundId, playLocalSound, stopLocalSound } = useLocalPlayback();

  // Filters and search
  const {
    selectedCategory,
    setSelectedCategory,
    activeFilter,
    setActiveFilter,
    searchQuery,
    setSearchQuery,
    popularCount,
    setPopularCount,
    recentCount,
    setRecentCount,
    categories,
    top10SoundIds,
    recentlyAddedIds,
    filteredSounds
  } = useFilters(sounds, favorites);

  // Sound actions
  const {
    toggleFavorite,
    playSoundWithBot,
    playRandomSound,
    stopCurrentSound,
    deleteSound,
    downloadSound,
    handleFileUpload
  } = useSoundActions({
    selectedUserId,
    isPlaybackEnabled,
    setCurrentlyPlayingSoundId,
    setSounds,
    favorites,
    setFavorites
  });

  // Playback history
  const { history, recordPlay, clearHistory } = usePlaybackHistory();
  const [showHistory, setShowHistory] = useState(false);

  // Record bot plays whenever currentPlayback changes
  useEffect(() => {
    if (currentPlayback) {
      const name = currentPlayback.displayName && currentPlayback.displayName.trim() !== ''
        ? currentPlayback.displayName
        : formatSoundFileId(currentPlayback.soundFileId);
      recordPlay(currentPlayback.soundFileId, name);
    }
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [currentPlayback]);

  // Tick every 5s to refresh relative timestamps in history
  const [, setTimeTick] = useState(0);
  useEffect(() => {
    const id = setInterval(() => setTimeTick(t => t + 1), 5000);
    return () => clearInterval(id);
  }, []);

  // Wrapped local play that also records history
  const playLocalSoundWithHistory = (soundId: string) => {
    const sound = sounds.find(s => s.id === soundId);
    const name = sound?.displayName && sound.displayName.trim() !== ''
      ? sound.displayName
      : formatSoundFileId(soundId);
    recordPlay(soundId, name);
    playLocalSound(soundId);
  };

  // UI state
  const [contextMenu, setContextMenu] = useState<{
    x: number;
    y: number;
    soundId: string;
  } | null>(null);
  const [showUsersOverlay, setShowUsersOverlay] = useState(false);
  const [showRoleDialog, setShowRoleDialog] = useState(false);
  const [settingsMenu, setSettingsMenu] = useState<{ x: number; y: number } | null>(null);

  // Refs
  const searchInputRef = useRef<HTMLInputElement>(null);
  const fileInputRef = useRef<HTMLInputElement>(null);

  const formatTimeAgo = (timestamp: number): string => {
    const seconds = Math.floor((Date.now() - timestamp) / 1000);
    if (seconds < 5) return 'just now';
    if (seconds < 10) return '5s ago';
    if (seconds < 30) return '10s ago';
    if (seconds < 45) return '30s ago';
    if (seconds < 60) return '45s ago';
    const minutes = Math.floor(seconds / 60);
    if (minutes < 60) return `${minutes}m ago`;
    const hours = Math.floor(minutes / 60);
    if (hours < 24) return `${hours}h ago`;
    return `${Math.floor(hours / 24)}d ago`;
  };

  // Format sound file ID to be human-readable
  const formatSoundFileId = (name: string) => {

    return name
      .replace(/[_-]/g, ' ')
      .replace(/([a-z])([A-Z])/g, '$1 $2')
      .replace(/(^|\s)\w/g, char => char.toUpperCase())
      .trim();
  };

  // Global ESC key handler
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
  }, [setSearchQuery]);

  // Auto-focus search box when user starts typing
  useEffect(() => {
    const handleKeyDown = (event: KeyboardEvent) => {
      const isTypingChar = event.key.length === 1 && /[a-zA-Z0-9]/.test(event.key);
      const activeElement = document.activeElement;
      const isInInputField = activeElement instanceof HTMLInputElement ||
                            activeElement instanceof HTMLTextAreaElement ||
                            activeElement instanceof HTMLSelectElement;
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

  // Handle role changes - refresh token to get updated permissions
  const handleRoleChanged = async () => {
    if (authUser) {
      await refreshAuthToken();
    }
  };

  const handleContextMenu = (e: React.MouseEvent, soundId: string) => {
    e.preventDefault();
    setContextMenu({
      x: e.clientX,
      y: e.clientY,
      soundId
    });
  };

  return (
    <div className="min-h-screen bg-gray-50 dark:bg-gradient-to-br dark:from-gray-900 dark:to-gray-800 p-4">
      <div>
        <header className="mb-4 flex items-center justify-between">
          <div>
            <h1 className="text-blue-900 dark:text-blue-400 mb-2 flex items-center gap-3">
              <img
                src="/favicon.png"
                alt="Discord Soundboard Logo"
                className="w-8 h-8 rounded-full"
              />
              Discord Soundboard
            </h1>
          </div>

          <div className="flex items-center gap-3">
            {/* Hidden file input for upload functionality */}
            {authUser?.permissions?.upload && (
              <input
                ref={fileInputRef}
                type="file"
                accept="audio/*,.mp3,.wav,.ogg,.webm,.flac"
                onChange={handleFileUpload}
                className="hidden"
                aria-label="File upload input"
              />
            )}

            {/* Auth Button */}
            <AuthButton
              user={authUser}
              onLogin={handleLogin}
              onLogout={handleLogout}
            />

            {/* Settings Button */}
            <button
              onClick={(e) => {
                const rect = e.currentTarget.getBoundingClientRect();
                setSettingsMenu({
                  x: rect.right - 280,
                  y: rect.bottom + 8,
                });
              }}
              className="flex items-center justify-center p-2 rounded-lg transition-colors bg-white text-gray-700 hover:bg-gray-100 shadow-md dark:bg-gray-700 dark:text-gray-300 dark:hover:bg-gray-600"
              aria-label="Settings"
            >
              <Settings className="w-5 h-5" />
            </button>
          </div>
        </header>

        {/* Main Content Area */}
        <div>
          {/* Filters */}
          <div className="bg-white dark:bg-gray-800 dark:border dark:border-gray-700 rounded-lg shadow-md p-4 mb-6">
            <div className="grid grid-cols-1 lg:grid-cols-[1fr_320px] gap-4">
              {/* Left side - Search and filters */}
              <div>
                {/* Top: Search+Category (left) | Playback+History (right) */}
                <div className="mb-4 grid grid-cols-1 md:grid-cols-[1fr_280px] gap-4">
                  {/* Left: search stacked above category */}
                  <div className="flex flex-col gap-3">
                    {/* Search */}
                    <div className="relative">
                      <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-gray-400 dark:text-gray-500" />
                      <input
                        id="soundSearch"
                        type="text"
                        placeholder={`Search ${filteredSounds.length} sound${filteredSounds.length !== 1 ? 's' : ''}...`}
                        value={searchQuery}
                        onChange={(e) => setSearchQuery(e.target.value)}
                        onKeyDown={(e) => {
                          if (e.key === 'Enter' && filteredSounds.length === 1) {
                            playSoundWithBot(filteredSounds[0].id);
                          }
                        }}
                        className={`w-full pl-10 py-2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent bg-white border-gray-300 text-gray-900 placeholder-gray-500 dark:bg-gray-700 dark:border-gray-600 dark:text-white dark:placeholder-gray-400 ${searchQuery ? 'pr-9' : 'pr-4'}`}
                        ref={searchInputRef}
                      />
                      {searchQuery && (
                        <button
                          onClick={() => { setSearchQuery(''); searchInputRef.current?.focus(); }}
                          aria-label="Clear search"
                          className="absolute right-2 top-1/2 -translate-y-1/2 p-0.5 rounded-full text-gray-400 hover:text-gray-600 hover:bg-gray-200 dark:hover:text-gray-200 dark:hover:bg-gray-600"
                        >
                          <X className="w-4 h-4" />
                        </button>
                      )}
                    </div>

                    {/* Category */}
                    <div className="relative">
                      <select
                        id="categorySelect"
                        value={selectedCategory}
                        onChange={(e) => {
                          setSelectedCategory(e.target.value);
                          if (e.target.value !== 'all') {
                            setActiveFilter('none');
                          }
                        }}
                        className={`w-full px-4 py-2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-purple-500 focus:border-transparent bg-white border-gray-300 text-gray-900 dark:bg-gray-700 dark:border-gray-600 dark:text-white ${selectedCategory !== 'all' ? 'pr-10' : ''}`}
                      >
                        {categories.map(category => (
                          <option key={category} value={category}>
                            {category === 'all' ? 'Select Category to Filter By' : category.charAt(0).toUpperCase() + category.slice(1)}
                          </option>
                        ))}
                      </select>
                      {selectedCategory !== 'all' && (
                        <button
                          onClick={() => setSelectedCategory('all')}
                          aria-label="Clear category filter"
                          className="absolute right-7 top-1/2 -translate-y-1/2 z-10 p-0.5 rounded-full text-gray-400 hover:text-gray-600 hover:bg-gray-200 dark:hover:text-gray-200 dark:hover:bg-gray-600"
                        >
                          <X className="w-4 h-4" />
                        </button>
                      )}
                    </div>
                  </div>

                  {/* Right: current playback + history */}
                  <div className={`flex flex-col gap-2`}>
                    {/* Current Playback Info */}
                    <div className={`flex items-center h-[51px] gap-2 px-3 py-2 rounded-lg border ${
                      currentPlayback
                        ? 'bg-blue-50 border-blue-300 text-blue-800 dark:bg-blue-900/30 dark:border-blue-700 dark:text-blue-300'
                        : 'bg-gray-50 border-gray-300 text-gray-500 dark:bg-gray-800 dark:border-gray-700'
                    }`}>
                      <div className="text-sm flex items-center gap-1 overflow-hidden min-w-0 flex-1">
                        {currentPlayback ? (
                          <>
                            <span className="shrink-0">🎵</span>
                            <span className="font-semibold truncate">
                              {currentPlayback.displayName && currentPlayback.displayName.trim() !== ''
                                ? currentPlayback.displayName
                                : formatSoundFileId(currentPlayback.soundFileId)}
                            </span>
                            <span className="shrink-0 text-gray-600 dark:text-gray-400">by</span>
                            <span className="font-semibold truncate">{currentPlayback.user}</span>
                          </>
                        ) : (
                          <span>No playback active</span>
                        )}
                      </div>
                    </div>

                    {/* History Toggle Button */}
                    <button
                      onClick={() => setShowHistory(h => !h)}
                      className={`flex items-center gap-2 px-3 py-2 rounded-lg transition-colors w-full ${
                        showHistory
                          ? 'bg-purple-600 dark:bg-purple-700 text-white'
                          : 'bg-white text-gray-700 hover:bg-gray-100 border border-gray-300 dark:bg-gray-700 dark:text-gray-300 dark:hover:bg-gray-600 dark:border-0'
                      }`}
                      title="Toggle playback history"
                    >
                      <History className="w-4 h-4" />
                      <span className="text-sm">History</span>
                      {history.length > 0 && (
                        <span className={`ml-auto text-xs px-1.5 py-0.5 rounded-full font-semibold ${
                          showHistory
                            ? 'bg-white/20 text-white'
                            : 'bg-gray-200 text-gray-600 dark:bg-gray-600 dark:text-gray-300'
                        }`}>
                          {history.length}
                        </span>
                      )}
                    </button>

                    {/* History List */}
                    {showHistory && (
                      <div className="overflow-y-auto max-h-[200px] rounded-lg border border-gray-200 bg-white dark:border-gray-700 dark:bg-gray-800/50 [&::-webkit-scrollbar]:w-2 [&::-webkit-scrollbar-track]:bg-gray-200 dark:[&::-webkit-scrollbar-track]:bg-gray-700 [&::-webkit-scrollbar-thumb]:bg-gray-400 dark:[&::-webkit-scrollbar-thumb]:bg-gray-600 [&::-webkit-scrollbar-thumb]:rounded-full [&::-webkit-scrollbar-thumb]:hover:bg-gray-500">
                        {history.length === 0 ? (
                          <div className="px-3 py-4 text-center text-sm text-gray-400 dark:text-gray-500">
                            No playback history yet
                          </div>
                        ) : (
                          <>
                            {history.map((entry, i) => (
                              <div
                                key={`${entry.soundId}-${entry.timestamp}-${i}`}
                                className="flex items-center gap-2 px-3 py-1.5 border-b last:border-b-0 text-sm border-gray-100 text-gray-700 dark:border-gray-700/50 dark:text-gray-300"
                              >
                                <button
                                  onClick={() => playSoundWithBot(entry.soundId)}
                                  disabled={!isPlaybackEnabled || !authUser?.permissions?.playSounds}
                                  className={`shrink-0 p-0.5 rounded transition-colors ${
                                    !isPlaybackEnabled || !authUser?.permissions?.playSounds
                                      ? 'opacity-30 cursor-not-allowed'
                                      : 'text-gray-400 hover:text-blue-600 dark:hover:text-blue-400'
                                  }`}
                                  title="Play again"
                                >
                                  <PlayCircle className="w-4 h-4" />
                                </button>
                                {entry.count > 1 && (
                                  <span className="shrink-0 text-xs font-bold px-1.5 py-0.5 rounded-full bg-blue-100 text-blue-700 dark:bg-blue-900/50 dark:text-blue-300">
                                    ×{entry.count}
                                  </span>
                                )}
                                <span className="truncate flex-1">
                                  {(() => {
                                    const s = sounds.find(s => s.id === entry.soundId);
                                    return s?.displayName && s.displayName.trim() !== ''
                                      ? s.displayName
                                      : formatSoundFileId(entry.soundId);
                                  })()}
                                </span>
                                <span className="shrink-0 text-xs text-gray-400 dark:text-gray-500">
                                  {formatTimeAgo(entry.timestamp)}
                                </span>
                              </div>
                            ))}
                            <div className="border-t px-3 py-1.5 border-gray-200 dark:border-gray-700">
                              <button
                                onClick={clearHistory}
                                className="text-xs text-gray-400 hover:text-red-500 dark:text-gray-500 dark:hover:text-red-400"
                              >
                                Clear history
                              </button>
                            </div>
                          </>
                        )}
                      </div>
                    )}
                  </div>
                </div>

                {/* Filter Buttons and Action Buttons */}
                <div className="flex flex-wrap items-center gap-3">
                  {/* Filter toggle group — connected segmented control */}
                  <div className="flex rounded-lg overflow-hidden border border-gray-300 dark:border-gray-600">
                    <button
                      onClick={() => {
                        setActiveFilter(activeFilter === 'favorites' ? 'none' : 'favorites');
                        if (activeFilter !== 'favorites') setSelectedCategory('all');
                      }}
                      className={`flex items-center gap-2 px-4 py-2 transition-colors ${
                        activeFilter === 'favorites'
                          ? 'bg-yellow-600 text-white'
                          : 'bg-white text-gray-700 hover:bg-gray-50 dark:bg-gray-700 dark:text-gray-300 dark:hover:bg-gray-600'
                      }`}
                    >
                      <Star className={`w-4 h-4 ${activeFilter === 'favorites' ? 'fill-current' : ''}`} />
                      Favorites
                    </button>

                    <div className="w-px self-stretch bg-gray-300 dark:bg-gray-600" />

                    <button
                      onClick={() => {
                        setActiveFilter(activeFilter === 'popular' ? 'none' : 'popular');
                        if (activeFilter !== 'popular') setSelectedCategory('all');
                      }}
                      className={`flex items-center gap-2 px-4 py-2 transition-colors ${
                        activeFilter === 'popular'
                          ? 'bg-gradient-to-br from-amber-500 to-orange-600 text-white'
                          : 'bg-white text-gray-700 hover:bg-gray-50 dark:bg-gray-700 dark:text-gray-300 dark:hover:bg-gray-600'
                      }`}
                    >
                      <Trophy className="w-4 h-4" />
                      Popular
                    </button>

                    <div className="w-px self-stretch bg-gray-300 dark:bg-gray-600" />

                    <button
                      onClick={() => {
                        setActiveFilter(activeFilter === 'recent' ? 'none' : 'recent');
                        if (activeFilter !== 'recent') setSelectedCategory('all');
                      }}
                      className={`flex items-center gap-2 px-4 py-2 transition-colors ${
                        activeFilter === 'recent'
                          ? 'bg-green-600 text-white'
                          : 'bg-white text-gray-700 hover:bg-gray-50 dark:bg-gray-700 dark:text-gray-300 dark:hover:bg-gray-600'
                      }`}
                    >
                      <Sparkles className="w-4 h-4" />
                      Recent
                    </button>
                  </div>

                  {/* Volume Slider */}
                  <div className={`flex items-center gap-3 flex-1 min-w-[200px] ${selectedUserId && isPlaybackEnabled ? '' : 'invisible'}`}>
                    <Volume2 className="w-5 h-5 text-gray-600 dark:text-gray-400" />
                    <input
                      type="range"
                      min="0"
                      max="100"
                      value={volume}
                      onChange={(e) => setVolume(parseInt(e.target.value, 10))}
                      onMouseUp={(e) => updateVolume(parseInt((e.target as HTMLInputElement).value, 10), selectedUserId)}
                      onTouchEnd={(e) => updateVolume(parseInt((e.target as HTMLInputElement).value, 10), selectedUserId)}
                      className="flex-1 h-2 bg-gray-200 rounded-lg appearance-none cursor-pointer dark:bg-gray-700"
                      style={{
                        background: `linear-gradient(to right, ${theme === 'dark' ? '#3b82f6' : '#60a5fa'} 0%, ${theme === 'dark' ? '#3b82f6' : '#60a5fa'} ${volume}%, ${theme === 'dark' ? '#374151' : '#e5e7eb'} ${volume}%, ${theme === 'dark' ? '#374151' : '#e5e7eb'} 100%)`
                      }}
                      disabled={!selectedUserId || !isPlaybackEnabled || !authUser?.permissions?.updateVolume}
                      title={
                        !authUser?.permissions?.updateVolume
                          ? "You don't have permission to change volume"
                          : !isPlaybackEnabled
                          ? 'User must be in voice channel'
                          : !selectedUserId
                          ? 'Select a user'
                          : 'Adjust volume'
                      }
                    />
                    <span className="min-w-[3rem] text-right text-gray-700 dark:text-gray-300">
                      {volume}%
                    </span>
                  </div>

                  {/* Play Random Sound Button */}
                  <button
                    onClick={() => playRandomSound()}
                    disabled={!isPlaybackEnabled || !authUser?.permissions?.playSounds}
                    className={`flex items-center gap-2 px-4 py-2 rounded-lg transition-colors ${
                      !isPlaybackEnabled || !authUser?.permissions?.playSounds
                        ? 'opacity-50 cursor-not-allowed bg-gray-600 text-gray-400'
                        : 'bg-blue-600 text-white hover:bg-blue-700 dark:bg-blue-700 dark:hover:bg-blue-600'
                    }`}
                    title={
                      !authUser?.permissions?.playSounds
                        ? "You don't have permission to play sounds"
                        : !isPlaybackEnabled
                        ? 'User must be in voice channel'
                        : 'Play random sound from filtered list'
                    }
                  >
                    <Shuffle className="w-5 h-5" />
                    Random
                  </button>

                  {/* Stop Sound Button */}
                  <button
                    onClick={stopCurrentSound}
                    disabled={!isPlaybackEnabled || !authUser?.permissions?.playSounds}
                    className={`flex items-center gap-2 px-4 py-2 rounded-lg transition-colors ${
                      !isPlaybackEnabled || !authUser?.permissions?.playSounds
                        ? 'opacity-50 cursor-not-allowed bg-gray-600 text-gray-400'
                        : 'bg-red-600 text-white hover:bg-red-700 dark:bg-red-700 dark:hover:bg-red-600'
                    }`}
                    title={
                      !authUser?.permissions?.playSounds
                        ? "You don't have permission to stop sounds"
                        : !isPlaybackEnabled
                        ? 'User must be in voice channel'
                        : 'Stop currently playing sound'
                    }
                  >
                    <StopCircle className="w-5 h-5" />
                    Stop
                  </button>
                </div>
              </div>

              {/* Right side - Discord Users */}
              <div>
                <DiscordUsersList
                  onUserSelect={setSelectedUserId}
                  selectedUserId={selectedUserId}
                  onVolumeUpdate={setVolume}
                  onPlaybackEnabledChange={setIsPlaybackEnabled}
                  onGuildIdChange={setSelectedUserGuildId}
                />
              </div>
            </div>
          </div>

          {/* Sound Grid / Skeleton / Empty State */}
          {loading ? (
            <div className="grid grid-cols-4 sm:grid-cols-6 md:grid-cols-8 lg:grid-cols-10 xl:grid-cols-12 2xl:grid-cols-14 gap-2">
              {Array.from({ length: 48 }).map((_, i) => (
                <div
                  key={i}
                  className="h-14 rounded-lg animate-pulse bg-gray-200 dark:bg-gray-700"
                />
              ))}
            </div>
          ) : filteredSounds.length === 0 ? (
            <div className="flex flex-col items-center justify-center py-12 rounded-lg border-2 border-dashed border-gray-300 text-gray-400 dark:border-gray-700 dark:text-gray-500">
              <Music className="w-12 h-12 mb-4 opacity-40" />
              <p className="text-lg font-semibold mb-1 text-gray-500 dark:text-gray-400">No sounds found</p>
              <p className="text-sm mb-4">
                {searchQuery || selectedCategory !== 'all' || activeFilter !== 'none'
                  ? 'Try adjusting your filters'
                  : 'No sounds have been added yet'}
              </p>
              {(searchQuery || selectedCategory !== 'all' || activeFilter !== 'none') && (
                <button
                  onClick={() => {
                    setSearchQuery('');
                    setSelectedCategory('all');
                    setActiveFilter('none');
                  }}
                  className="px-4 py-2 rounded-lg text-sm transition-colors bg-gray-100 text-gray-700 hover:bg-gray-200 dark:bg-gray-700 dark:text-gray-300 dark:hover:bg-gray-600"
                >
                  Clear all filters
                </button>
              )}
            </div>
          ) : (
            <div className="grid grid-cols-4 sm:grid-cols-6 md:grid-cols-8 lg:grid-cols-10 xl:grid-cols-12 2xl:grid-cols-14 gap-2">
              {(searchQuery || selectedCategory !== 'all' || activeFilter !== 'none') && <div
                className={`col-span-2 rounded-lg shadow-xl transition-shadow bg-blue-600 dark:bg-blue-700 dark:border dark:border-blue-600 ${
                  !isPlaybackEnabled || !authUser?.permissions?.playSounds ? 'opacity-50' : 'hover:shadow-2xl'
                }`}
              >
                <button
                  onClick={() => {
                    const randomSound = filteredSounds[Math.floor(Math.random() * filteredSounds.length)];
                    if (randomSound) playSoundWithBot(randomSound.id);
                  }}
                  disabled={!isPlaybackEnabled || !authUser?.permissions?.playSounds}
                  className={`w-full h-full p-2 flex items-center justify-center gap-2 text-xs rounded-lg transition-all text-white ${
                    !isPlaybackEnabled || !authUser?.permissions?.playSounds
                      ? 'cursor-not-allowed'
                      : 'hover:bg-blue-700 dark:hover:bg-blue-600'
                  }`}
                  title={
                    !authUser?.permissions?.playSounds
                      ? "You don't have permission to play sounds"
                      : !isPlaybackEnabled
                      ? 'User must be in voice channel'
                      : 'Play a random sound from the current results'
                  }
                >
                  <Shuffle className="w-4 h-4" />
                  <span>Random From Results</span>
                </button>
              </div>}
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
                  disabled={!isPlaybackEnabled || !authUser?.permissions?.playSounds}
                  disabledReason={
                    !authUser?.permissions?.playSounds
                      ? "You don't have permission to play sounds"
                      : !isPlaybackEnabled
                      ? 'User must be in voice channel to play sounds'
                      : undefined
                  }
                  isCurrentlyPlaying={currentlyPlayingSoundId === sound.id}
                  isLocallyPlaying={locallyPlayingSoundId === sound.id}
                  onStopLocalPlayback={stopLocalSound}
                />
              ))}
            </div>
          )}
        </div>
      </div>

      {/* Context Menu */}
      {contextMenu && (() => {
        const sound = sounds.find(s => s.id === contextMenu.soundId);
        if (!sound) return null;

        return (
          <ContextMenu
            x={contextMenu.x}
            y={contextMenu.y}
            onClose={() => setContextMenu(null)}
            onFavorite={() => toggleFavorite(contextMenu.soundId)}
            onDelete={() => deleteSound(contextMenu.soundId)}
            onDownload={() => downloadSound(contextMenu.soundId)}
            onPlayLocally={() => playLocalSoundWithHistory(contextMenu.soundId)}
            isFavorite={favorites.has(contextMenu.soundId)}
            timesPlayed={sound.timesPlayed}
            dateAdded={sound.dateAdded}
            volumeOffset={sound.volumeOffset}
            soundId={sound.id}
            displayName={sound.displayName || null}
            category={sound.category}
            canEditSounds={authUser?.permissions?.editSounds ?? false}
            canDeleteSounds={authUser?.permissions?.deleteSounds ?? false}
          />
        );
      })()}

      {/* Users Overlay */}
      {showUsersOverlay && (
        <UsersOverlay
          isOpen={showUsersOverlay}
          onClose={() => setShowUsersOverlay(false)}
          sounds={sounds}
        />
      )}

      {/* Settings Menu */}
      {settingsMenu && (
        <SettingsMenu
          x={settingsMenu.x}
          y={settingsMenu.y}
          theme={theme}
          onThemeChange={setTheme}
          onClose={() => setSettingsMenu(null)}
          onUploadClick={() => fileInputRef.current?.click()}
          onUsersClick={() => setShowUsersOverlay(true)}
          onRolesClick={() => setShowRoleDialog(true)}
          popularCount={popularCount}
          recentCount={recentCount}
          onPopularCountChange={setPopularCount}
          onRecentCountChange={setRecentCount}
          canUpload={authUser?.permissions?.upload}
          canManageUsers={authUser?.permissions?.manageUsers}
        />
      )}

      {/* Role Management Dialog */}
      <RoleManagementDialog
        isOpen={showRoleDialog}
        onClose={() => setShowRoleDialog(false)}
        currentUserId={authUser?.id || ''}
        onRoleChanged={handleRoleChanged}
        canManageUsers={authUser?.permissions?.manageUsers || false}
      />

      {/* Toast notifications */}
      <Toaster
        position="top-right"
        theme={theme}
        richColors
      />
    </div>
  );
}