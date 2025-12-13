import React, { useRef, useEffect } from 'react';
import { Sun, Moon } from 'lucide-react';

interface SettingsMenuProps {
  x: number;
  y: number;
  onClose: () => void;
  theme: 'light' | 'dark';
  popularCount: number;
  recentCount: number;
  onPopularCountChange: (count: number) => void;
  onRecentCountChange: (count: number) => void;
  onThemeChange: (theme: 'light' | 'dark') => void;
}

export function SettingsMenu({
  x,
  y,
  onClose,
  theme,
  popularCount,
  recentCount,
  onPopularCountChange,
  onRecentCountChange,
  onThemeChange,
}: SettingsMenuProps) {
  const menuRef = useRef<HTMLDivElement>(null);

  // Close menu when clicking outside
  useEffect(() => {
    const handleClickOutside = (e: MouseEvent) => {
      if (menuRef.current && !menuRef.current.contains(e.target as Node)) {
        onClose();
      }
    };

    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, [onClose]);

  // Close menu on Escape key
  useEffect(() => {
    const handleEscape = (e: KeyboardEvent) => {
      if (e.key === 'Escape') {
        onClose();
      }
    };

    document.addEventListener('keydown', handleEscape);
    return () => document.removeEventListener('keydown', handleEscape);
  }, [onClose]);

  // Position menu to not go off screen
  const menuStyle: React.CSSProperties = {
    position: 'fixed',
    left: `${x}px`,
    top: `${y}px`,
    zIndex: 1000,
  };

  return (
    <div
      ref={menuRef}
      style={menuStyle}
      className={`${
        theme === 'dark' ? 'bg-gray-800 border-gray-700' : 'bg-white border-gray-200'
      } border rounded-lg shadow-2xl p-4 min-w-[280px]`}
    >
      <h3 className={`${theme === 'dark' ? 'text-gray-200' : 'text-gray-800'} mb-4`}>
        Settings
      </h3>

      {/* Popular Count */}
      <div className="mb-4">
        <label for="popularCount" className={`block text-sm mb-2 ${theme === 'dark' ? 'text-gray-300' : 'text-gray-700'}`}>
          Popular Sounds Count
        </label>
        <input
          id="popularCount"
          type="number"
          min="1"
          max="100"
          value={popularCount}
          onChange={(e) => {
            const value = parseInt(e.target.value, 10);
            if (!isNaN(value) && value >= 1 && value <= 100) {
              onPopularCountChange(value);
            }
          }}
          className={`w-full px-3 py-2 rounded border ${
            theme === 'dark'
              ? 'bg-gray-700 border-gray-600 text-white'
              : 'bg-white border-gray-300 text-gray-900'
          } focus:outline-none focus:ring-2 focus:ring-blue-500`}
        />
        <p className={`text-xs mt-1 ${theme === 'dark' ? 'text-gray-400' : 'text-gray-500'}`}>
          Number of sounds to mark as popular (1-100)
        </p>
      </div>

      {/* Recent Count */}
      <div className="mb-4">
        <label className={`block text-sm mb-2 ${theme === 'dark' ? 'text-gray-300' : 'text-gray-700'}`} for="recentlyAddedCount">
          Recently Added Count
        </label>
        <input
          id="recentlyAddedCount"
          type="number"
          min="1"
          max="100"
          value={recentCount}
          onChange={(e) => {
            const value = parseInt(e.target.value, 10);
            if (!isNaN(value) && value >= 1 && value <= 100) {
              onRecentCountChange(value);
            }
          }}
          className={`w-full px-3 py-2 rounded border ${
            theme === 'dark'
              ? 'bg-gray-700 border-gray-600 text-white'
              : 'bg-white border-gray-300 text-gray-900'
          } focus:outline-none focus:ring-2 focus:ring-blue-500`}
        />
        <p className={`text-xs mt-1 ${theme === 'dark' ? 'text-gray-400' : 'text-gray-500'}`}>
          Number of sounds to mark as recently added (1-100)
        </p>
      </div>

      {/* Theme Toggle */}
      <div className="mb-4">
        <label className={`block text-sm mb-2 ${theme === 'dark' ? 'text-gray-300' : 'text-gray-700'}`}>
          Theme
        </label>
        <div className="flex gap-2">
          <button
            onClick={() => onThemeChange('light')}
            className={`flex-1 flex items-center justify-center gap-2 px-4 py-2 rounded-lg transition-colors ${
              theme === 'light'
                ? 'bg-blue-600 text-white'
                : theme === 'dark'
                ? 'bg-gray-700 text-gray-300 hover:bg-gray-600'
                : 'bg-gray-200 text-gray-700 hover:bg-gray-300'
            }`}
          >
            <Sun className="w-5 h-5" />
            Light
          </button>
          <button
            onClick={() => onThemeChange('dark')}
            className={`flex-1 flex items-center justify-center gap-2 px-4 py-2 rounded-lg transition-colors ${
              theme === 'dark'
                ? 'bg-blue-600 text-white'
                : 'bg-gray-200 text-gray-700 hover:bg-gray-300'
            }`}
          >
            <Moon className="w-5 h-5" />
            Dark
          </button>
        </div>
      </div>

      {/* Close Button */}
      <button
        onClick={onClose}
        className={`w-full px-4 py-2 rounded-lg transition-colors ${
          theme === 'dark'
            ? 'bg-blue-700 text-white hover:bg-blue-600'
            : 'bg-blue-600 text-white hover:bg-blue-700'
        }`}
      >
        Done
      </button>
    </div>
  );
}