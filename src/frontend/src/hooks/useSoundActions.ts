import { useRef } from 'react';
import type { Sound } from './useSounds';
import { API_ENDPOINTS } from '../config';
import { getAuthHeadersWithCsrf, fetchWithAuth } from '../utils/api';
import { toast } from 'sonner@2.0.3';

interface UseSoundActionsProps {
  selectedUserId: string | null;
  isPlaybackEnabled: boolean;
  setCurrentlyPlayingSoundId: (id: string | null) => void;
  setSounds: React.Dispatch<React.SetStateAction<Sound[]>>;
  favorites: Set<string>;
  setFavorites: React.Dispatch<React.SetStateAction<Set<string>>>;
}

export function useSoundActions({
  selectedUserId,
  isPlaybackEnabled,
  setCurrentlyPlayingSoundId,
  setSounds,
  favorites,
  setFavorites
}: UseSoundActionsProps) {
  const toggleFavoriteInProgressRef = useRef<Set<string>>(new Set());

  const toggleFavorite = async (soundId: string) => {
    const isFavorite = favorites.has(soundId);
    const newFavoriteState = !isFavorite;
    const callId = Math.random().toString(36).substring(7);
    
    if (toggleFavoriteInProgressRef.current.has(soundId)) {
      console.log(`[${callId}] toggleFavorite called for ${soundId}, but a toggle is already in progress`);
      return;
    }
    
    toggleFavoriteInProgressRef.current.add(soundId);
    
    try {
      console.log(`[${callId}] toggleFavorite called for ${soundId}, changing to ${newFavoriteState}`);
      console.trace(`[${callId}] Call stack`);
      
      console.log(`[${callId}] About to call fetch...`);
      const response = await fetch(
        `${API_ENDPOINTS.FAVORITE}/${soundId}?favorite=${newFavoriteState}`,
        { 
          method: 'POST',
          mode: 'cors',
          credentials: 'include',
          headers: {
            'Content-Type': 'application/json',
            ...getAuthHeadersWithCsrf()
          }
        }
      );
      console.log(`[${callId}] Fetch completed, response received`);

      if (!response.ok) {
        console.error(`[${callId}] Failed to update favorite status:`, response.status);
        throw new Error('Failed to update favorite status');
      }

      const contentLength = response.headers.get('Content-Length');
      if (contentLength && contentLength !== '0') {
        await response.text();
      } else {
        await response.clone().text().catch(() => {});
      }

      console.log(`[${callId}] Favorite status updated successfully: ${newFavoriteState}`);

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
      toast.error('Failed to update favorite. Please try again.', { duration: 3000 });
    } finally {
      toggleFavoriteInProgressRef.current.delete(soundId);
    }
  };

  const playSoundWithBot = async (soundId: string) => {
    if (!selectedUserId) {
      toast.warning('Please select a user from the Active Users list before playing a sound.', { duration: 3000 });
      return;
    }

    if (!isPlaybackEnabled) {
      toast.warning('The selected user must be in a voice channel to play sounds.', { duration: 3000 });
      return;
    }

    try {
      const response = await fetch(
        `${API_ENDPOINTS.PLAY_FILE}?soundFileId=${soundId}&username=${selectedUserId}`,
        {
          method: 'POST',
          mode: 'cors',
          credentials: 'include',
          headers: getAuthHeadersWithCsrf()
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
      if (error instanceof TypeError || (error instanceof Error && error.message.includes('Failed to play'))) {
        if (error instanceof TypeError) {
          toast.error('Failed to play sound. Please make sure the backend is running', { duration: 3000 });
        } else {
          toast.error(`Failed to play sound: ${error.message}`, { duration: 3000 });
        }
      }
    }
  };

  const playRandomSound = async (filteredSounds: Sound[]) => {
    if (!isPlaybackEnabled || filteredSounds.length === 0) {
      toast.warning('Please select a user from the Active Users list before playing a sound.', { duration: 3000 });
      return;
    }

    try {
      const response = await fetch(
        `${API_ENDPOINTS.RANDOM}?username=${selectedUserId}`,
        {
          method: 'POST',
          mode: 'cors',
          credentials: 'include',
          headers: getAuthHeadersWithCsrf()
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
        toast.error('Failed to play random sound. Please make sure the backend is running', { duration: 3000 });
      } else if (error instanceof Error) {
        toast.error(`Failed to play random sound: ${error.message}`, { duration: 3000 });
      }
    }
  };

  const stopCurrentSound = async () => {
    if (!isPlaybackEnabled) {
      toast.warning('Please select a user from the Active Users list.', { duration: 3000 });
      return;
    }

    try {
      const response = await fetch(
        `${API_ENDPOINTS.STOP}?username=${selectedUserId}`,
        {
          method: 'POST',
          mode: 'cors',
          credentials: 'include',
          headers: getAuthHeadersWithCsrf()
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
        toast.error('Failed to stop sound. Please make sure the backend is running', { duration: 3000 });
      } else if (error instanceof Error) {
        toast.error(`Failed to stop sound: ${error.message}`, { duration: 3000 });
      }
    }
  };

  const deleteSound = async (soundId: string) => {
    try {
      const response = await fetchWithAuth(`${API_ENDPOINTS.SOUND_FILE}/${soundId}`, {
        method: 'DELETE',
      });

      if (!response.ok) {
        console.error('Failed to delete sound:', response.status, response.statusText);
        toast.error('Failed to delete sound', { duration: 3000 });
        return;
      }

      setSounds(prev => prev.filter(s => s.id !== soundId));
      setFavorites(prev => {
        const newFavorites = new Set(prev);
        newFavorites.delete(soundId);
        return newFavorites;
      });

      toast.success('Sound deleted successfully', { duration: 3000 });
    } catch (error) {
      console.error('Error deleting sound:', error);
      toast.error('Failed to delete sound', { duration: 3000 });
    }
  };

  const downloadSound = (sound: Sound) => {
    const link = document.createElement('a');
    link.href = `${API_ENDPOINTS.DOWNLOAD}/${sound.id}`;
    link.download = `${sound.name}.ogg`;
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
  };

  const handleFileUpload = async (event: React.ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0];
    if (!file) return;

    const validTypes = ['audio/mpeg', 'audio/wav', 'audio/ogg', 'audio/mp3', 'audio/webm', 'audio/flac'];
    if (!validTypes.includes(file.type) && !file.name.match(/\.(mp3|wav|ogg|webm|flac)$/i)) {
      toast.error('Please upload a valid audio file (MP3, WAV, OGG, WEBM, or FLAC)', { duration: 3000 });
      event.target.value = '';
      return;
    }

    const maxSize = 10 * 1024 * 1024;
    if (file.size > maxSize) {
      toast.error('File size must be less than 10MB', { duration: 3000 });
      event.target.value = '';
      return;
    }

    const formData = new FormData();
    formData.append('file', file);

    try {
      console.log('📤 Uploading file:', file.name);

      const authHeaders = getAuthHeadersWithCsrf();

      const response = await fetch(API_ENDPOINTS.UPLOAD, {
        method: 'POST',
        mode: 'cors',
        credentials: 'include',
        headers: authHeaders,
        body: formData
      });

      if (response.status === 403) {
        toast.error('Permission denied: You do not have permission to upload sounds.', { duration: 3000 });
        event.target.value = '';
        return;
      }

      if (!response.ok) {
        const errorText = await response.text().catch(() => 'Unknown error');
        console.error(`Failed to upload file: ${response.status} - ${errorText}`);
        throw new Error(`Failed to upload file: ${response.status}`);
      }

      console.log('✅ File uploaded successfully');
      toast.success(`File "${file.name}" uploaded successfully!`, { duration: 3000 });

      event.target.value = '';
    } catch (error) {
      console.error('Error uploading file:', error);
      if (error instanceof TypeError) {
        toast.error('Failed to upload file. Please make sure the backend is running', { duration: 3000 });
      } else if (error instanceof Error) {
        toast.error(`Failed to upload file: ${error.message}`, { duration: 3000 });
      }
      event.target.value = '';
    }
  };

  return {
    toggleFavorite,
    playSoundWithBot,
    playRandomSound,
    stopCurrentSound,
    deleteSound,
    downloadSound,
    handleFileUpload
  };
}