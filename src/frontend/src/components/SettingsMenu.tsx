import React, { useRef, useEffect, useState } from 'react';
import { Sun, Moon, Upload, Users, Shield } from 'lucide-react';
import { API_ENDPOINTS } from '../config';
import { getAuthHeaders } from '../utils/api';

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
  canUpload?: boolean;
  canManageUsers?: boolean;
  onUploadClick?: () => void;
  onUsersClick?: () => void;
  onRolesClick?: () => void;
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
  canUpload,
  canManageUsers,
  onUploadClick,
  onUsersClick,
  onRolesClick,
}: SettingsMenuProps) {
  const menuRef = useRef<HTMLDivElement>(null);
  const [botVersion, setBotVersion] = useState<string>('Loading...');

  // Fetch bot version on mount
  useEffect(() => {
    const fetchVersion = async () => {
      try {
        const response = await fetch(API_ENDPOINTS.BOT_VERSION, {
          headers: getAuthHeaders()
        });
        if (response.ok) {
          const version = await response.text();
          setBotVersion(version);
        } else {
          setBotVersion('Unknown');
        }
      } catch (error) {
        console.error('Failed to fetch bot version:', error);
        setBotVersion('Unknown');
      }
    };

    fetchVersion();
  }, []);

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

  const divider = (
    <hr className={`mt-4 mb-3 ${theme === 'dark' ? 'border-gray-700' : 'border-gray-200'}`} />
  );

  const sectionLabel = (text: string) => (
    <p className={`text-xs font-semibold uppercase tracking-wider mb-3 ${theme === 'dark' ? 'text-gray-500' : 'text-gray-400'}`}>
      {text}
    </p>
  );

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

      {/* Display */}
      {sectionLabel('Display')}
      <div className="flex gap-2 mb-1">
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

      {divider}

      {/* Filters */}
      {sectionLabel('Filters')}
      <div className="mb-3">
        <label className={`block text-sm mb-1.5 ${theme === 'dark' ? 'text-gray-300' : 'text-gray-700'}`}>
          Popular sounds count
        </label>
        <input
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
      </div>
      <div className="mb-1">
        <label className={`block text-sm mb-1.5 ${theme === 'dark' ? 'text-gray-300' : 'text-gray-700'}`}>
          Recently added count
        </label>
        <input
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
      </div>

      {divider}

      {/* About */}
      {sectionLabel('About')}
      <div className="mb-1 flex items-center justify-between">
        <span className={`text-sm ${theme === 'dark' ? 'text-gray-300' : 'text-gray-700'}`}>Bot version</span>
        <span className={`text-sm font-mono ${theme === 'dark' ? 'text-gray-400' : 'text-gray-500'}`}>{botVersion}</span>
      </div>

      {/* Administration */}
      {(canUpload || canManageUsers) && (
        <>
          {divider}
          {sectionLabel('Administration')}
          <div className="flex flex-col gap-2">
            {canUpload && onUploadClick && (
              <button
                onClick={onUploadClick}
                className={`w-full px-4 py-2 rounded-lg transition-colors flex items-center justify-center ${
                  theme === 'dark'
                    ? 'bg-blue-700 text-white hover:bg-blue-600'
                    : 'bg-blue-600 text-white hover:bg-blue-700'
                }`}
              >
                <Upload className="w-5 h-5 mr-2" />
                Upload Sound
              </button>
            )}
            {canManageUsers && onUsersClick && (
              <button
                onClick={() => { onUsersClick(); onClose(); }}
                className={`w-full px-4 py-2 rounded-lg transition-colors flex items-center justify-center ${
                  theme === 'dark'
                    ? 'bg-gray-700 text-gray-300 hover:bg-gray-600'
                    : 'bg-gray-100 text-gray-700 hover:bg-gray-200'
                }`}
              >
                <Users className="w-5 h-5 mr-2" />
                Manage Users
              </button>
            )}
            {canManageUsers && onRolesClick && (
              <button
                onClick={() => { onRolesClick(); onClose(); }}
                className={`w-full px-4 py-2 rounded-lg transition-colors flex items-center justify-center ${
                  theme === 'dark'
                    ? 'bg-gray-700 text-gray-300 hover:bg-gray-600'
                    : 'bg-gray-100 text-gray-700 hover:bg-gray-200'
                }`}
              >
                <Shield className="w-5 h-5 mr-2" />
                Manage Roles
              </button>
            )}
          </div>
        </>
      )}

      {divider}

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
