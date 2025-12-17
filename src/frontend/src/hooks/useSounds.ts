import { useState, useEffect } from 'react';
import { API_ENDPOINTS } from '../config';

export interface Sound {
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

interface ApiResponse {
  content: ApiSoundFile[];
  page: {
    size: number;
    number: number;
    totalElements: number;
    totalPages: number;
  };
}

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

export function useSounds() {
  const [sounds, setSounds] = useState<Sound[]>([]);
  const [favorites, setFavorites] = useState<Set<string>>(new Set());
  const [loading, setLoading] = useState(true);
  const [connectionStatus, setConnectionStatus] = useState<'connecting' | 'connected' | 'error'>('connecting');

  useEffect(() => {
    let eventSource: EventSource | null = null;
    let isMounted = true;

    const handleSoundsUpdate = (data: any) => {
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
    };

    try {
      console.log('📡 Connecting to SSE endpoint:', API_ENDPOINTS.SOUNDS_STREAM);
      eventSource = new EventSource(API_ENDPOINTS.SOUNDS_STREAM);
      
      eventSource.onopen = () => {
        if (!isMounted) return;
        console.log('✅ SSE connection established');
        setConnectionStatus('connected');
      };

      eventSource.onerror = () => {
        // Let EventSource auto-reconnect silently
      };

      eventSource.addEventListener('sounds', (event) => {
        if (!isMounted) return;
        try {
          const data = JSON.parse(event.data);
          handleSoundsUpdate(data);
        } catch (error) {
          console.error('Error parsing SSE data:', error);
        }
      });
    } catch (error) {
      console.error('Failed to create SSE connection:', error);
      setConnectionStatus('error');
    }

    // Load favorites from localStorage
    const savedFavorites = localStorage.getItem('soundboard-favorites');
    if (savedFavorites) {
      setFavorites(new Set(JSON.parse(savedFavorites)));
    }

    return () => {
      isMounted = false;
      if (eventSource) {
        eventSource.close();
      }
    };
  }, []);

  // Save favorites to localStorage
  useEffect(() => {
    localStorage.setItem('soundboard-favorites', JSON.stringify(Array.from(favorites)));
  }, [favorites]);

  return {
    sounds,
    setSounds,
    favorites,
    setFavorites,
    loading,
    connectionStatus
  };
}
