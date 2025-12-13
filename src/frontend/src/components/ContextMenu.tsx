import React, { useEffect, useRef, useState } from 'react';
import { Star, Trash2, Download, Clock, Calendar, Volume2, Edit3, Check, X, FileText, FolderOpen, Play } from 'lucide-react';
import { API_BASE_URL } from '../config';

interface ContextMenuProps {
  x: number;
  y: number;
  onClose: () => void;
  onFavorite: () => void | Promise<void>;
  onDelete: () => void;
  onDownload: () => void;
  onPlayLocally: () => void;
  isFavorite: boolean;
  theme: 'light' | 'dark';
  timesPlayed: number;
  dateAdded: string;
  volumeOffset: number | null;
  soundId: string;
  displayName: string | null;
  category: string;
  canEditSounds?: boolean;
  canDeleteSounds?: boolean;
}

export function ContextMenu({
  x,
  y,
  onClose,
  onFavorite,
  onDelete,
  onDownload,
  onPlayLocally,
  isFavorite,
  theme,
  timesPlayed,
  dateAdded,
  volumeOffset,
  soundId,
  displayName,
  category,
  canEditSounds = true,
  canDeleteSounds = true
}: ContextMenuProps) {
  const menuRef = useRef<HTMLDivElement>(null);
  const [localVolumeOffset, setLocalVolumeOffset] = useState(volumeOffset ?? 0);
  const [isUpdating, setIsUpdating] = useState(false);
  const lastSentValueRef = useRef<number>(volumeOffset ?? 0);
  const lastUpdateTimeRef = useRef<number>(0);
  const [isEditingName, setIsEditingName] = useState(false);
  const [editedDisplayName, setEditedDisplayName] = useState(displayName || '');
  const [isUpdatingName, setIsUpdatingName] = useState(false);
  const nameInputRef = useRef<HTMLInputElement>(null);
  const [adjustedPosition, setAdjustedPosition] = useState({ x, y });
  const [savedDisplayName, setSavedDisplayName] = useState<string | null>(null);

  // Use the saved display name if it exists, otherwise use the prop
  const currentDisplayName = savedDisplayName !== null ? savedDisplayName : displayName;

  // Format date for display
  const formatDate = (dateString: string) => {
    try {
      const date = new Date(dateString);
      return date.toLocaleDateString('en-US', {
        month: 'short',
        day: 'numeric',
        year: 'numeric'
      });
    } catch {
      return dateString;
    }
  };

  // Update volume offset on the backend
  const updateVolumeOffset = async (newValue: number) => {
    const now = Date.now();

    // Block if we just sent a request within the last 300ms
    if (now - lastUpdateTimeRef.current < 300) {
      console.log('Blocked duplicate API call (too soon)');
      return;
    }

    // Block if this exact value was already sent
    if (lastSentValueRef.current === newValue) {
      console.log('Blocked duplicate API call (same value)');
      return;
    }

    lastUpdateTimeRef.current = now;
    setIsUpdating(true);

    console.log(`Calling API to update volume offset to ${newValue}%`);

    try {
      const url = new URL(`${API_BASE_URL}/api/soundFiles/${soundId}`, window.location.origin);
      url.searchParams.append('volumeOffsetPercentage', newValue.toString());
      url.searchParams.append('displayName', displayName || '');

      const response = await fetch(url.toString(), {
        method: 'PATCH',
        mode: 'cors',
        headers: {
          'Content-Type': 'application/json',
        }
      });

      if (!response.ok) {
        console.error('Failed to update volume offset:', response.status, response.statusText);
        // Revert to original value on error
        setLocalVolumeOffset(volumeOffset ?? 0);
      } else {
        lastSentValueRef.current = newValue;
      }
    } catch (error) {
      console.error('Error updating volume offset:', error);
      // Revert to original value on error
      setLocalVolumeOffset(volumeOffset ?? 0);
    } finally {
      setIsUpdating(false);
    }
  };

  const handleVolumeChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setLocalVolumeOffset(Number(e.target.value));
  };

  const handleVolumeRelease = () => {
    console.log('handleVolumeRelease called');

    // Only update if the value has changed from what we last sent
    if (localVolumeOffset !== lastSentValueRef.current) {
      updateVolumeOffset(localVolumeOffset);
    } else {
      console.log('No change in volume, skipping API call');
    }
  };

  // Update display name on the backend
  const updateDisplayName = async (newName: string) => {
    setIsUpdatingName(true);
    console.log(`Calling API to update display name to: ${newName}`);

    try {
      const url = new URL(`${API_BASE_URL}/api/soundFiles/${soundId}`, window.location.origin);
      url.searchParams.append('displayName', newName);
      url.searchParams.append('volumeOffsetPercentage', localVolumeOffset.toString());

      const response = await fetch(url.toString(), {
        method: 'PATCH',
        mode: 'cors',
        headers: {
          'Content-Type': 'application/json',
        }
      });

      if (!response.ok) {
        console.error('Failed to update display name:', response.status, response.statusText);
        // Revert to original value on error
        setEditedDisplayName(displayName || '');
      } else {
        console.log('Display name updated successfully');
        setSavedDisplayName(newName);
      }
    } catch (error) {
      console.error('Error updating display name:', error);
      // Revert to original value on error
      setEditedDisplayName(displayName || '');
    } finally {
      setIsUpdatingName(false);
      setIsEditingName(false);
    }
  };

  const handleSaveDisplayName = () => {
    if (editedDisplayName.trim() !== currentDisplayName) {
      updateDisplayName(editedDisplayName.trim());
    } else {
      setIsEditingName(false);
    }
  };

  const handleCancelEditName = () => {
    setEditedDisplayName(currentDisplayName || '');
    setIsEditingName(false);
  };

  const handleNameKeyDown = (e: React.KeyboardEvent<HTMLInputElement>) => {
    if (e.key === 'Enter') {
      e.preventDefault();
      handleSaveDisplayName();
    } else if (e.key === 'Escape') {
      e.preventDefault();
      handleCancelEditName();
    }
  };

  // Focus input when editing starts
  useEffect(() => {
    if (isEditingName && nameInputRef.current) {
      nameInputRef.current.focus();
      nameInputRef.current.select();
    }
  }, [isEditingName]);

  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (menuRef.current && !menuRef.current.contains(event.target as Node)) {
        onClose();
      }
    };

    const handleEscape = (event: KeyboardEvent) => {
      if (event.key === 'Escape') {
        onClose();
      }
    };

    const handleContextMenu = (event: MouseEvent) => {
      // Close the menu if right-clicking outside of it
      if (menuRef.current && !menuRef.current.contains(event.target as Node)) {
        event.preventDefault();
        onClose();
      }
    };

    // Use setTimeout to avoid closing immediately on the same event that opened the menu
    const timeoutId = setTimeout(() => {
      document.addEventListener('mousedown', handleClickOutside);
      document.addEventListener('keydown', handleEscape);
      document.addEventListener('contextmenu', handleContextMenu);
    }, 0);

    return () => {
      clearTimeout(timeoutId);
      document.removeEventListener('mousedown', handleClickOutside);
      document.removeEventListener('keydown', handleEscape);
      document.removeEventListener('contextmenu', handleContextMenu);
    };
  }, [onClose]);

  // Adjust position to keep menu within viewport
  useEffect(() => {
    const currentRef = menuRef.current;
    if (currentRef) {
      const rect = currentRef.getBoundingClientRect();
      const windowWidth = window.innerWidth;
      const windowHeight = window.innerHeight;

      let newX = x;
      let newY = y;

      // Check if menu would be within 50px of right edge
      if (x + rect.width > windowWidth - 50) {
        newX = windowWidth - rect.width - 50;
      }

      // Check if menu overflows bottom edge
      if (y + rect.height > windowHeight) {
        newY = windowHeight - rect.height - 10; // 10px padding from edge
      }

      // Ensure menu doesn't go off left edge
      if (newX < 10) {
        newX = 10;
      }

      // Ensure menu doesn't go off top edge
      if (newY < 10) {
        newY = 10;
      }

      if (newX !== adjustedPosition.x || newY !== adjustedPosition.y) {
        setAdjustedPosition({ x: newX, y: newY });
      }
    }
  }, [x, y, adjustedPosition.x, adjustedPosition.y]);

  return (
    <div
      ref={menuRef}
      className={`fixed z-50 min-w-[280px] rounded-lg shadow-2xl border ${
        theme === 'dark'
          ? 'bg-gray-800 border-gray-700'
          : 'bg-white border-gray-200'
      }`}
      style={{ left: `${adjustedPosition.x}px`, top: `${adjustedPosition.y}px` }}
    >
      <div className="py-1">
        <button
          onClick={() => {
            onFavorite();
          }}
          className={`w-full px-4 py-2 text-left flex items-center gap-3 transition-colors ${
            theme === 'dark'
              ? 'hover:bg-gray-700 text-gray-200'
              : 'hover:bg-gray-100 text-gray-700'
          }`}
        >
          <Star
            className={`w-4 h-4 ${
              isFavorite ? 'fill-yellow-500 text-yellow-500' : ''
            }`}
          />
          {isFavorite ? 'Remove from Favorites' : 'Add to Favorites'}
        </button>

        <button
          onClick={() => {
            onDownload();
            onClose();
          }}
          className={`w-full px-4 py-2 text-left flex items-center gap-3 transition-colors ${
            theme === 'dark'
              ? 'hover:bg-gray-700 text-gray-200'
              : 'hover:bg-gray-100 text-gray-700'
          }`}
        >
          <Download className="w-4 h-4" />
          Download Sound
        </button>

        <button
          onClick={() => {
            onPlayLocally();
            onClose();
          }}
          className={`w-full px-4 py-2 text-left flex items-center gap-3 transition-colors ${
            theme === 'dark'
              ? 'hover:bg-gray-700 text-gray-200'
              : 'hover:bg-gray-100 text-gray-700'
          }`}
        >
          <Play className="w-4 h-4" />
          Play Locally
        </button>

        <div className={`my-1 h-px ${theme === 'dark' ? 'bg-gray-700' : 'bg-gray-200'}`} />

        {/* Sound Information Section */}
        <div className={`px-4 py-2 ${theme === 'dark' ? 'text-gray-300' : 'text-gray-600'}`}>
          <div className="flex items-center gap-3 py-1.5">
            <Clock className="w-4 h-4 flex-shrink-0" />
            <span className="text-sm">
              Played: <span className={theme === 'dark' ? 'text-white' : 'text-gray-900'}>{timesPlayed}</span> {timesPlayed === 1 ? 'time' : 'times'}
            </span>
          </div>

          <div className="flex items-center gap-3 py-1.5">
            <FolderOpen className="w-4 h-4 flex-shrink-0" />
            <span className="text-sm">
              Category: <span className={theme === 'dark' ? 'text-white' : 'text-gray-900'}>{category}</span>
            </span>
          </div>

          <div className="flex items-center gap-3 py-1.5">
            <Calendar className="w-4 h-4 flex-shrink-0" />
            <span className="text-sm">
              Added: <span className={theme === 'dark' ? 'text-white' : 'text-gray-900'}>{formatDate(dateAdded)}</span>
            </span>
          </div>

          <div className="flex items-center gap-3 py-1.5">
            <FileText className="w-4 h-4 flex-shrink-0" />
            <span className="text-sm">
              ID: <span className={`${theme === 'dark' ? 'text-white' : 'text-gray-900'} font-mono text-xs`}>{soundId}</span>
            </span>
          </div>

          {/* Volume Offset - Only visible with edit-sounds permission */}
          {canEditSounds && (
            <div className="py-1.5">
              <div className="flex items-center gap-3 mb-2">
                <Volume2 className="w-4 h-4 flex-shrink-0" />
                <span className="text-sm">
                  Volume: <span className={`${theme === 'dark' ? 'text-white' : 'text-gray-900'} font-medium`}>
                    {localVolumeOffset > 0 ? '+' : ''}{localVolumeOffset}%
                  </span>
                  {isUpdating && <span className="ml-2 text-xs">(saving...)</span>}
                </span>
              </div>
              <div className="pl-7">
                <input
                  type="range"
                  min="-100"
                  max="100"
                  value={localVolumeOffset}
                  onChange={handleVolumeChange}
                  onMouseUp={handleVolumeRelease}
                  onTouchEnd={handleVolumeRelease}
                  disabled={isUpdating}
                  className={`w-full h-2 rounded-lg appearance-none cursor-pointer ${
                    theme === 'dark'
                      ? 'bg-gray-600 accent-blue-500'
                      : 'bg-gray-300 accent-blue-600'
                  }`}
                  style={{
                    background: theme === 'dark'
                      ? `linear-gradient(to right, #4b5563 0%, #4b5563 ${(localVolumeOffset + 100) / 2}%, #6b7280 ${(localVolumeOffset + 100) / 2}%, #6b7280 100%)`
                      : `linear-gradient(to right, #9333ea 0%, #9333ea ${(localVolumeOffset + 100) / 2}%, #d1d5db ${(localVolumeOffset + 100) / 2}%, #d1d5db 100%)`
                  }}
                />
              </div>
            </div>
          )}

          {/* Display Name - Only visible with edit-sounds permission */}
          {canEditSounds && (
            <div className="py-1.5">
              <div className="flex items-center gap-3 mb-2">
                <Edit3 className="w-4 h-4 flex-shrink-0" />
                <span className="text-sm flex-1">
                  Display Name:
                  {isUpdatingName && <span className="ml-2 text-xs">(saving...)</span>}
                </span>
              </div>
              <div className="pl-7">
                {isEditingName ? (
                  <div className="flex items-center gap-2">
                    <input
                      ref={nameInputRef}
                      type="text"
                      value={editedDisplayName}
                      onChange={(e) => setEditedDisplayName(e.target.value)}
                      onKeyDown={handleNameKeyDown}
                      disabled={isUpdatingName}
                      className={`flex-1 px-2 py-1 rounded border text-sm ${
                        theme === 'dark'
                          ? 'bg-gray-700 border-gray-600 text-white'
                          : 'bg-white border-gray-300 text-gray-900'
                      }`}
                      placeholder="Enter display name"
                    />
                    <button
                      onClick={handleSaveDisplayName}
                      disabled={isUpdatingName}
                      className={`p-1 rounded transition-colors ${
                        theme === 'dark'
                          ? 'hover:bg-gray-700 text-green-400'
                          : 'hover:bg-gray-100 text-green-600'
                      }`}
                      title="Save"
                    >
                      <Check className="w-4 h-4" />
                    </button>
                    <button
                      onClick={handleCancelEditName}
                      disabled={isUpdatingName}
                      className={`p-1 rounded transition-colors ${
                        theme === 'dark'
                          ? 'hover:bg-gray-700 text-red-400'
                          : 'hover:bg-gray-100 text-red-600'
                      }`}
                      title="Cancel"
                    >
                      <X className="w-4 h-4" />
                    </button>
                  </div>
                ) : (
                  <div
                    onClick={() => setIsEditingName(true)}
                    className={`px-2 py-1 rounded cursor-pointer transition-colors text-sm ${
                      theme === 'dark'
                        ? 'hover:bg-gray-700 text-white'
                        : 'hover:bg-gray-100 text-gray-900'
                    }`}
                  >
                    {currentDisplayName || <span className="italic opacity-50">Click to set name</span>}
                  </div>
                )}
              </div>
            </div>
          )}
        </div>

        {/* Delete Button - Only visible with delete-sounds permission */}
        {canDeleteSounds && (
          <>
            <div className={`my-1 h-px ${theme === 'dark' ? 'bg-gray-700' : 'bg-gray-200'}`} />
            <button
              onClick={() => {
                onDelete();
                onClose();
              }}
              className={`w-full px-4 py-2 text-left flex items-center gap-3 transition-colors ${
                theme === 'dark'
                  ? 'hover:bg-red-900/30 text-red-400'
                  : 'hover:bg-red-50 text-red-600'
              }`}
            >
              <Trash2 className="w-4 h-4" />
              Delete Sound
            </button>
          </>
        )}
      </div>
    </div>
  );
}