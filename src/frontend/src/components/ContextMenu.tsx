import React, { useState, useEffect, useRef } from 'react';
import { Star, Download, Play, Clock, Calendar, Volume2, Edit2, Check, X, Trash2, FolderOpen, FileText } from 'lucide-react';
import * as SliderPrimitive from '@radix-ui/react-slider@1.2.3';
import { API_BASE_URL } from '../config';
import { fetchWithAuth } from '../utils/api';
import { toast } from 'sonner@2.0.3';

interface ContextMenuProps {
  x: number;
  y: number;
  onClose: () => void;
  onFavorite: () => void | Promise<void>;
  onDelete: () => void | Promise<void>;
  onDownload: () => void;
  onPlayLocally: () => void;
  isFavorite: boolean;
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

  const currentDisplayName = savedDisplayName !== null ? savedDisplayName : displayName;

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

  const updateVolumeOffset = async (newValue: number) => {
    const now = Date.now();

    if (now - lastUpdateTimeRef.current < 300) {
      return;
    }

    if (lastSentValueRef.current === newValue) {
      return;
    }

    lastUpdateTimeRef.current = now;
    setIsUpdating(true);

    try {
      const url = new URL(`${API_BASE_URL}/api/soundFiles/${soundId}`, window.location.origin);
      url.searchParams.append('volumeOffsetPercentage', newValue.toString());
      url.searchParams.append('displayName', displayName || '');

      const response = await fetchWithAuth(url.toString(), {
        method: 'PATCH',
        mode: 'cors',
        headers: {
          'Content-Type': 'application/json',
        }
      });

      if (!response.ok) {
        setLocalVolumeOffset(volumeOffset ?? 0);
        toast.error('Failed to update volume offset');
      } else {
        lastSentValueRef.current = newValue;
        toast.success('Volume offset updated');
      }
    } catch {
      setLocalVolumeOffset(volumeOffset ?? 0);
      toast.error('Failed to update volume offset');
    } finally {
      setIsUpdating(false);
    }
  };

  const handleVolumeChange = (values: number[]) => {
    setLocalVolumeOffset(values[0]);
  };

  const handleVolumeCommit = (values: number[]) => {
    if (values[0] !== lastSentValueRef.current) {
      updateVolumeOffset(values[0]);
    }
  };

  const updateDisplayName = async (newName: string) => {
    setIsUpdatingName(true);

    try {
      const url = new URL(`${API_BASE_URL}/api/soundFiles/${soundId}`, window.location.origin);
      url.searchParams.append('displayName', newName);
      url.searchParams.append('volumeOffsetPercentage', localVolumeOffset.toString());

      const response = await fetchWithAuth(url.toString(), {
        method: 'PATCH',
        mode: 'cors',
        headers: {
          'Content-Type': 'application/json',
        }
      });

      if (!response.ok) {
        setEditedDisplayName(displayName || '');
        toast.error('Failed to update display name');
      } else {
        setSavedDisplayName(newName);
        toast.success('Display name updated');
      }
    } catch {
      setEditedDisplayName(displayName || '');
      toast.error('Failed to update display name');
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
      if (menuRef.current && !menuRef.current.contains(event.target as Node)) {
        event.preventDefault();
        onClose();
      }
    };

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

  useEffect(() => {
    const currentRef = menuRef.current;
    if (currentRef) {
      const rect = currentRef.getBoundingClientRect();
      const windowWidth = window.innerWidth;
      const windowHeight = window.innerHeight;

      let newX = x;
      let newY = y;

      if (x + rect.width > windowWidth - 50) {
        newX = windowWidth - rect.width - 50;
      }

      if (y + rect.height > windowHeight) {
        newY = windowHeight - rect.height - 10;
      }

      if (newX < 10) {
        newX = 10;
      }

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
      className="fixed z-50 min-w-[280px] rounded-lg shadow-2xl border bg-white border-gray-200 dark:bg-gray-800 dark:border-gray-700"
      style={{ left: `${adjustedPosition.x}px`, top: `${adjustedPosition.y}px` }}
    >
      <div className="py-1">
        <button
          onClick={() => { onFavorite(); }}
          className="w-full px-4 py-2 text-left flex items-center gap-3 transition-colors hover:bg-gray-100 text-gray-700 dark:hover:bg-gray-700 dark:text-gray-200"
        >
          <Star className={`w-4 h-4 ${isFavorite ? 'fill-yellow-500 text-yellow-500' : ''}`} />
          {isFavorite ? 'Remove from Favorites' : 'Add to Favorites'}
        </button>

        <button
          onClick={() => { onDownload(); onClose(); }}
          className="w-full px-4 py-2 text-left flex items-center gap-3 transition-colors hover:bg-gray-100 text-gray-700 dark:hover:bg-gray-700 dark:text-gray-200"
        >
          <Download className="w-4 h-4" />
          Download Sound
        </button>

        <button
          onClick={() => { onPlayLocally(); onClose(); }}
          className="w-full px-4 py-2 text-left flex items-center gap-3 transition-colors hover:bg-gray-100 text-gray-700 dark:hover:bg-gray-700 dark:text-gray-200"
        >
          <Play className="w-4 h-4" />
          Play Locally
        </button>

        <div className="my-1 h-px bg-gray-200 dark:bg-gray-700" />

        {/* Sound Information Section */}
        <div className="px-4 py-2 text-gray-600 dark:text-gray-300">
          <div className="flex items-center gap-3 py-1.5">
            <Clock className="w-4 h-4 flex-shrink-0" />
            <span className="text-sm">
              Played: <span className="text-gray-900 dark:text-white">{timesPlayed}</span> {timesPlayed === 1 ? 'time' : 'times'}
            </span>
          </div>

          <div className="flex items-center gap-3 py-1.5">
            <FolderOpen className="w-4 h-4 flex-shrink-0" />
            <span className="text-sm">
              Category: <span className="text-gray-900 dark:text-white">{category}</span>
            </span>
          </div>

          <div className="flex items-center gap-3 py-1.5">
            <Calendar className="w-4 h-4 flex-shrink-0" />
            <span className="text-sm">
              Added: <span className="text-gray-900 dark:text-white">{formatDate(dateAdded)}</span>
            </span>
          </div>

          <div className="flex items-center gap-3 py-1.5">
            <FileText className="w-4 h-4 flex-shrink-0" />
            <span className="text-sm">
              ID: <span className="text-gray-900 dark:text-white font-mono text-xs">{soundId}</span>
            </span>
          </div>

          {/* Volume Offset */}
          {canEditSounds && (
            <div className="py-1.5">
              <div className="flex items-center gap-3 mb-2">
                <Volume2 className="w-4 h-4 flex-shrink-0" />
                <span className="text-sm">
                  Volume: <span className="text-gray-900 dark:text-white font-medium">
                    {localVolumeOffset > 0 ? '+' : ''}{localVolumeOffset}%
                  </span>
                  {isUpdating && <span className="ml-2 text-xs">(saving...)</span>}
                </span>
              </div>
              <div className="pl-7">
                <SliderPrimitive.Root
                  min={-100}
                  max={100}
                  value={[localVolumeOffset]}
                  onValueChange={handleVolumeChange}
                  onValueCommit={handleVolumeCommit}
                  disabled={isUpdating}
                  className="relative flex w-full touch-none items-center"
                >
                  <SliderPrimitive.Track className="relative h-2 w-full grow overflow-hidden rounded-full bg-gray-300 dark:bg-gray-600">
                    <SliderPrimitive.Range className="absolute h-full bg-purple-600 dark:bg-purple-500" />
                  </SliderPrimitive.Track>
                  <SliderPrimitive.Thumb className="block h-4 w-4 rounded-full border-2 shadow transition-colors focus:outline-none disabled:pointer-events-none disabled:opacity-50 bg-white border-gray-400 dark:bg-gray-200 dark:border-gray-500" />
                </SliderPrimitive.Root>
              </div>
            </div>
          )}

          {/* Display Name */}
          {canEditSounds && (
            <div className="py-1.5">
              <div className="flex items-center gap-3 mb-2">
                <Edit2 className="w-4 h-4 flex-shrink-0" />
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
                      className="flex-1 px-2 py-1 rounded border text-sm bg-white border-gray-300 text-gray-900 dark:bg-gray-700 dark:border-gray-600 dark:text-white"
                      placeholder="Enter display name"
                    />
                    <button
                      onClick={handleSaveDisplayName}
                      disabled={isUpdatingName}
                      className="p-1 rounded transition-colors hover:bg-gray-100 text-green-600 dark:hover:bg-gray-700 dark:text-green-400"
                      title="Save"
                    >
                      <Check className="w-4 h-4" />
                    </button>
                    <button
                      onClick={handleCancelEditName}
                      disabled={isUpdatingName}
                      className="p-1 rounded transition-colors hover:bg-gray-100 text-red-600 dark:hover:bg-gray-700 dark:text-red-400"
                      title="Cancel"
                    >
                      <X className="w-4 h-4" />
                    </button>
                  </div>
                ) : (
                  <div
                    onClick={() => setIsEditingName(true)}
                    className="px-2 py-1 rounded cursor-pointer transition-colors text-sm hover:bg-gray-100 text-gray-900 dark:hover:bg-gray-700 dark:text-white"
                  >
                    {currentDisplayName || <span className="italic opacity-50">Click to set name</span>}
                  </div>
                )}
              </div>
            </div>
          )}
        </div>

        {/* Delete Button */}
        {canDeleteSounds && (
          <>
            <div className="my-1 h-px bg-gray-200 dark:bg-gray-700" />
            <button
              onClick={() => { onDelete(); onClose(); }}
              className="w-full px-4 py-2 text-left flex items-center gap-3 transition-colors hover:bg-red-50 text-red-600 dark:hover:bg-red-900/30 dark:text-red-400"
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