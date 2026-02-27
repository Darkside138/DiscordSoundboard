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
      playbackEventSource = new EventSource(API_ENDPOINTS.PLAYBACK_STREAM);

      playbackEventSource.onerror = () => {
        // Let EventSource auto-reconnect silently
      };

      playbackEventSource.addEventListener('trackStart', (event) => {
        if (!isMounted) return;
        try {
          const data = JSON.parse(event.data);
          handleTrackStart(data);
        } catch {
          // Ignore parse errors
        }
      });

      playbackEventSource.addEventListener('trackEnd', (event) => {
        if (!isMounted) return;
        try {
          const data = JSON.parse(event.data);
          handleTrackEnd(data);
        } catch {
          // Ignore parse errors
        }
      });
    } catch {
      // SSE connection failed, will retry on next mount
    }

    return () => {
      isMounted = false;
      if (playbackEventSource) {
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
