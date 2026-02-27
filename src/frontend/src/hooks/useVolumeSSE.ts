import { useEffect } from 'react';
import { API_ENDPOINTS } from '../config';
import { getAuthHeaders } from '../utils/api';

interface UseVolumeSSEProps {
  selectedUserId: string | null;
  setVolume: (volume: number) => void;
}

export function useVolumeSSE({ selectedUserId, setVolume }: UseVolumeSSEProps) {
  useEffect(() => {
    if (!selectedUserId) {
      return;
    }
    
    let volumeEventSource: EventSource | null = null;
    let isMounted = true;

    const handleVolumeUpdate = (volumeData: string) => {
      const volumeValue = parseInt(volumeData, 10);
      if (!isNaN(volumeValue) && volumeValue >= 0 && volumeValue <= 100) {
        setVolume(volumeValue);
      }
    };

    // Fetch initial volume
    const fetchInitialVolume = async () => {
      try {
        const response = await fetch(`${API_ENDPOINTS.VOLUME}/${selectedUserId}`, {
          credentials: 'include',
          headers: getAuthHeaders()
        });
        if (response.ok) {
          const volumeValue = await response.text();
          const parsedVolume = parseInt(volumeValue, 10);
          if (!isNaN(parsedVolume) && parsedVolume >= 0 && parsedVolume <= 100) {
            setVolume(parsedVolume);
          }
        }
      } catch {
        // Failed to fetch initial volume
      }
    };

    fetchInitialVolume();

    // Connect to SSE
    const sseTimeout = setTimeout(() => {
      if (!isMounted) return;

      try {
        const sseUrl = `${API_ENDPOINTS.VOLUME_STREAM}/${selectedUserId}`;
        volumeEventSource = new EventSource(sseUrl);

        volumeEventSource.onopen = () => {
          // Connected
        };

        volumeEventSource.onmessage = (event) => {
          if (!isMounted) return;
          handleVolumeUpdate(event.data);
        };

        volumeEventSource.addEventListener('volume', (event) => {
          if (!isMounted) return;
          handleVolumeUpdate(event.data);
        });

        volumeEventSource.addEventListener('globalVolume', (event) => {
          if (!isMounted) return;
          try {
            const volumeValue = parseFloat(event.data);
            const volumePercentage = Math.round(volumeValue);
            if (!isNaN(volumePercentage) && volumePercentage >= 0 && volumePercentage <= 100) {
              setVolume(volumePercentage);
            }
          } catch {
            // Ignore parse errors
          }
        });

        volumeEventSource.onerror = () => {
          // Let EventSource auto-reconnect silently
        };
      } catch {
        // SSE connection failed
      }
    }, 100);

    return () => {
      clearTimeout(sseTimeout);
      isMounted = false;
      if (volumeEventSource) {
        volumeEventSource.close();
      }
    };
  }, [selectedUserId, setVolume]);
}