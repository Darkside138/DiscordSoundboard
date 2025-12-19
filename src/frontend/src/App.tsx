import React, { useState, useEffect, useRef } from 'react';
import { SoundButton } from './components/SoundButton';
import { ContextMenu } from './components/ContextMenu';
import { DiscordUsersList } from './components/DiscordUsersList';
import { UsersOverlay } from './components/UsersOverlay';
import { SettingsMenu } from './components/SettingsMenu';
import { AuthButton } from './components/AuthButton';
import { Search, Star, Grid3x3, Volume2, Shuffle, StopCircle, Settings } from 'lucide-react';
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

export default function App() {
  // Authentication
  const { authUser, authLoading, handleLogin, handleLogout } = useAuth();

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

  // UI state
  const [contextMenu, setContextMenu] = useState<{
    x: number;
    y: number;
    soundId: string;
  } | null>(null);
  const [showUsersOverlay, setShowUsersOverlay] = useState(false);
  const [settingsMenu, setSettingsMenu] = useState<{ x: number; y: number } | null>(null);

  // Refs
  const searchInputRef = useRef<HTMLInputElement>(null);
  const fileInputRef = useRef<HTMLInputElement>(null);

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

  const handleContextMenu = (e: React.MouseEvent, soundId: string) => {
    e.preventDefault();
    setContextMenu({
      x: e.clientX,
      y: e.clientY,
      soundId
    });
  };

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
              theme={theme}
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

                {/* Category Filter and Playback Info */}
                <div className="mb-4 flex items-center gap-3 flex-wrap">
                  <label className={theme === 'dark' ? 'text-gray-300' : 'text-gray-700'}>Category</label>
                  <select
                    value={selectedCategory}
                    onChange={(e) => {
                      setSelectedCategory(e.target.value);
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

                  {/* Current Playback Info */}
                  <div className={`flex items-center gap-2 px-3 py-2 rounded-lg border w-[320px] ${
                    currentPlayback
                      ? theme === 'dark'
                        ? 'bg-blue-900/30 border-blue-700 text-blue-300'
                        : 'bg-blue-50 border-blue-300 text-blue-800'
                      : theme === 'dark'
                      ? 'bg-gray-800 border-gray-700 text-gray-500'
                      : 'bg-gray-50 border-gray-300 text-gray-500'
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
                          <span className={`shrink-0 ${theme === 'dark' ? 'text-gray-400' : 'text-gray-600'}`}>by</span>
                          <span className="font-semibold truncate">{currentPlayback.user}</span>
                        </>
                      ) : (
                        <span>No playback active</span>
                      )}
                    </div>
                  </div>
                </div>

                {/* Filter Buttons and Action Buttons */}
                <div className="flex flex-wrap items-center gap-3">
                  {/* Favorites Filter */}
                  <button
                    onClick={() => {
                      setActiveFilter(activeFilter === 'favorites' ? 'none' : 'favorites');
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
                    Favorites
                  </button>

                  {/* Popular Filter */}
                  <button
                    onClick={() => {
                      setActiveFilter(activeFilter === 'popular' ? 'none' : 'popular');
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
                    Popular
                  </button>

                  {/* Recent Filter */}
                  <button
                    onClick={() => {
                      setActiveFilter(activeFilter === 'recent' ? 'none' : 'recent');
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
                    Recent
                  </button>

                  {/* Volume Slider */}
                  <div className={`flex items-center gap-3 flex-1 min-w-[200px] ${selectedUserId && isPlaybackEnabled ? '' : 'invisible'}`}>
                    <Volume2 className={`w-5 h-5 ${theme === 'dark' ? 'text-gray-400' : 'text-gray-600'}`} />
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
                      title={!authUser?.permissions?.updateVolume ? 'You do not have permission to change volume' : ''}
                    />
                    <span className={`min-w-[3rem] text-right ${theme === 'dark' ? 'text-gray-300' : 'text-gray-700'}`}>
                      {volume}%
                    </span>
                  </div>

                  {/* Play Random Sound Button */}
                  <button
                    onClick={() => playRandomSound(filteredSounds)}
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
                  onGuildIdChange={setSelectedUserGuildId}
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
            onPlayLocally={() => playLocalSound(contextMenu.soundId)}
            isFavorite={favorites.has(contextMenu.soundId)}
            theme={theme}
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
          theme={theme}
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
          popularCount={popularCount}
          recentCount={recentCount}
          onPopularCountChange={setPopularCount}
          onRecentCountChange={setRecentCount}
          canUpload={authUser?.permissions?.upload}
          canManageUsers={authUser?.permissions?.manageUsers}
        />
      )}

      {/* Toast notifications */}
      <Toaster
        position="top-right"
        theme={theme}
        richColors
      />
    </div>
  );
}