import React, { useRef } from 'react';
import { Star, Trophy, Sparkles, Volume2, CircleStop } from 'lucide-react';

interface Sound {
  id: string;
  name: string;
  category: string;
  url: string;
  displayName?: string | null;
  dateAdded?: string;
}

interface SoundButtonProps {
  sound: Sound;
  isFavorite: boolean;
  isTopPlayed: boolean;
  isRecentlyAdded: boolean;
  onPlay: () => void;
  onToggleFavorite: () => void;
  onContextMenu: (e: React.MouseEvent) => void;
  theme?: 'light' | 'dark';
  disabled?: boolean;
  disabledReason?: string;
  isCurrentlyPlaying?: boolean;
  isLocallyPlaying?: boolean;
  onStopLocalPlayback?: () => void;
}

export function SoundButton({ sound, isFavorite, isTopPlayed, isRecentlyAdded, onPlay, onToggleFavorite, onContextMenu, disabled, disabledReason, isCurrentlyPlaying, isLocallyPlaying, onStopLocalPlayback }: SoundButtonProps) {
  const longPressTimer = useRef<ReturnType<typeof setTimeout> | null>(null);
  const touchStartPos = useRef<{ x: number; y: number } | null>(null);

  const handleTouchStart = (e: React.TouchEvent) => {
    const touch = e.touches[0];
    touchStartPos.current = { x: touch.clientX, y: touch.clientY };
    longPressTimer.current = setTimeout(() => {
      onContextMenu({ clientX: touch.clientX, clientY: touch.clientY, preventDefault: () => {} } as unknown as React.MouseEvent);
    }, 500);
  };

  const handleTouchEnd = () => {
    if (longPressTimer.current) {
      clearTimeout(longPressTimer.current);
      longPressTimer.current = null;
    }
  };

  const handleTouchMove = (e: React.TouchEvent) => {
    if (!touchStartPos.current || !longPressTimer.current) return;
    const touch = e.touches[0];
    const dx = Math.abs(touch.clientX - touchStartPos.current.x);
    const dy = Math.abs(touch.clientY - touchStartPos.current.y);
    if (dx > 8 || dy > 8) {
      clearTimeout(longPressTimer.current);
      longPressTimer.current = null;
    }
  };

  const formatSoundName = (name: string) => {
    return name
      // Replace underscores and hyphens with spaces
      .replace(/[_-]/g, ' ')
      // Add space before capital letters in camelCase
      .replace(/([a-z])([A-Z])/g, '$1 $2')
      // Capitalize first letter of each word (but not after apostrophes)
      .replace(/(^|\s)\w/g, char => char.toUpperCase())
      .trim();
  };

  // If displayName is set, use it directly without formatting
  // Otherwise, format the sound.name
  const displayText = sound.displayName || formatSoundName(sound.name);

  return (
    <div
      className={`rounded-lg shadow-xl transition-shadow group relative bg-white dark:bg-gray-800 dark:border dark:border-gray-700 ${
        disabled ? 'opacity-50 cursor-not-allowed' : 'hover:shadow-2xl'
      }`}
      onContextMenu={onContextMenu}
      onTouchStart={handleTouchStart}
      onTouchEnd={handleTouchEnd}
      onTouchMove={handleTouchMove}
    >
      {isFavorite && (
        <div className="absolute -top-1.5 -right-1.5 z-20 bg-yellow-500 rounded-full p-1 shadow-lg" title="Favorite">
          <Star className="w-2.5 h-2.5 fill-white text-white" />
        </div>
      )}

      {isTopPlayed && (
        <div className={`absolute -top-1.5 ${isFavorite ? 'right-8' : '-right-1.5'} z-20 bg-gradient-to-br from-amber-500 to-orange-600 rounded-full p-1 shadow-lg`} title="Popular sound">
          <Trophy className="w-2.5 h-2.5 fill-white text-white" />
        </div>
      )}

      {isRecentlyAdded && (
        <div
          className="absolute -top-1.5 -right-1.5 z-20 bg-gradient-to-br from-green-500 to-emerald-600 rounded-full p-1 shadow-lg"
          title={sound.dateAdded ? `Recently Added: ${new Date(sound.dateAdded).toLocaleDateString()}` : 'Recently Added'}
        >
          <Sparkles className="w-2.5 h-2.5 fill-white text-white" />
        </div>
      )}

      <button
        onClick={disabled ? undefined : onPlay}
        className={`w-full h-full p-2 text-center transition-all group whitespace-nowrap overflow-hidden text-ellipsis text-sm rounded-lg relative text-gray-900 dark:text-white ${
          disabled
            ? 'cursor-not-allowed'
            : 'hover:bg-gradient-to-br hover:from-blue-500 hover:to-blue-300 hover:text-white dark:hover:from-blue-600 dark:hover:to-blue-400'
        } ${
          isCurrentlyPlaying ? 'bg-gradient-to-br from-blue-600 to-blue-400 text-white' : ''
        }`}
        title={disabled ? (disabledReason || 'User must be in voice channel to play sounds') : displayText}
        disabled={disabled}
      >
        {isCurrentlyPlaying && (
          <div className="absolute inset-0 flex items-center justify-center">
            <Volume2 className="w-8 h-8 animate-pulse" />
          </div>
        )}
        {isLocallyPlaying && (
          <button
            className="absolute inset-0 flex items-center justify-center bg-red-600 bg-opacity-90 rounded-lg hover:bg-opacity-100 transition-all cursor-pointer z-10"
            onClick={(e) => {
              e.stopPropagation();
              onStopLocalPlayback?.();
            }}
            title="Click to stop local playback"
          >
            <CircleStop className="w-8 h-8 text-white" />
          </button>
        )}
        <span className={isCurrentlyPlaying || isLocallyPlaying ? 'opacity-0' : ''}>{displayText}</span>
      </button>
    </div>
  );
}