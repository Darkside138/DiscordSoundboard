import { useState, useEffect, useRef } from 'react';
import { API_ENDPOINTS } from '../config';

interface PlaybackState {
  soundFileId: string;
  user: string;
  displayName?: string | null;
}

interface UsePlaybackTrackingProps {
  selectedUserGuildId: string | null;
}

export function usePlaybackTracking({ selectedUserGuildId }: UsePlaybackTrackingProps) {
  const [currentlyPlayingSoundId, setCurrentlyPlayingSoundId] = useState<string | null>(null);
  const [currentPlayback, setCurrentPlayback] = useState<PlaybackState | null>(null);
  const selectedUserGuildIdRef = useRef<string | null>(selectedUserGuildId);

  useEffect(() => {
    selectedUserGuildIdRef.current = selectedUserGuildId;
  }, [selectedUserGuildId]);

  useEffect(() => {
    let playbackEventSource: EventSource | null = null;
    let isMounted = true;

    const handleTrackStart = (data: any) => {
      if (data.soundFileId && data.guildId === selectedUserGuildIdRef.current) {
        setCurrentlyPlayingSoundId(data.soundFileId);
        setCurrentPlayback({
          soundFileId: data.soundFileId,
          user: data.user || 'Unknown',
          displayName: data.displayName
        });
      }
    };

    const handleTrackEnd = (data: any) => {
      if (data.guildId === selectedUserGuildIdRef.current) {
        setCurrentlyPlayingSoundId((currentId) => {
          if (data.soundFileId && currentId === data.soundFileId) {
            return null;
          }
          return currentId;
        });
        setCurrentPlayback((current) => {
          if (current && data.soundFileId && current.soundFileId === data.soundFileId) {
            return null;
          }
          return current;
        });
      }
    };

    try {
      console.log('📡 Connecting to playback SSE endpoint:', API_ENDPOINTS.PLAYBACK_STREAM);
      playbackEventSource = new EventSource(API_ENDPOINTS.PLAYBACK_STREAM);
      
      playbackEventSource.onopen = () => {
        console.log('✅ Playback SSE connection established');
      };

      playbackEventSource.onerror = () => {
        // Let EventSource auto-reconnect silently
      };

      playbackEventSource.addEventListener('trackStart', (event) => {
        if (!isMounted) return;
        try {
          const data = JSON.parse(event.data);
          console.log('🎵 Track started:', data);
          handleTrackStart(data);
        } catch (error) {
          console.error('Error parsing trackStart event:', error);
        }
      });

      playbackEventSource.addEventListener('trackEnd', (event) => {
        if (!isMounted) return;
        try {
          const data = JSON.parse(event.data);
          console.log('🎵 Track ended:', data);
          handleTrackEnd(data);
        } catch (error) {
          console.error('Error parsing trackEnd event:', error);
        }
      });
    } catch (error) {
      console.error('Failed to create playback SSE connection:', error);
    }

    return () => {
      isMounted = false;
      if (playbackEventSource) {
        console.log('🧹 Closing playback SSE connection');
        playbackEventSource.close();
      }
    };
  }, []);

  return {
    currentlyPlayingSoundId,
    setCurrentlyPlayingSoundId,
    currentPlayback
  };
}
