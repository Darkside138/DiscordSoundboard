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
          headers: getAuthHeaders()
        });
        if (response.ok) {
          const volumeValue = await response.text();
          const parsedVolume = parseInt(volumeValue, 10);
          if (!isNaN(parsedVolume) && parsedVolume >= 0 && parsedVolume <= 100) {
            setVolume(parsedVolume);
          }
        }
      } catch (error) {
        console.error('Error fetching initial volume:', error);
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
          console.log('🔔 Volume SSE GLOBALVOLUME EVENT received!');
          console.log('📦 GlobalVolume event data:', event.data);
          
          if (!isMounted) {
            console.log('⚠ Component unmounted, ignoring globalVolume event');
            return;
          }
          try {
            console.log('Processing globalVolume SSE event:', event.data);
            const volumeValue = parseFloat(event.data);
            const volumePercentage = Math.round(volumeValue);
            console.log('Parsed globalVolume value:', volumeValue, '-> percentage:', volumePercentage);
            
            if (!isNaN(volumePercentage) && volumePercentage >= 0 && volumePercentage <= 100) {
              console.log('✅ VALID globalVolume value, calling setVolume with:', volumePercentage);
              setVolume(volumePercentage);
              console.log('✅ setVolume called successfully from globalVolume event');
            } else {
              console.warn('❌ Invalid globalVolume value:', event.data);
            }
          } catch (error) {
            console.error('Error parsing globalVolume SSE event data:', error);
          }
        });

        ['message', 'volume', 'update', 'change'].forEach(eventType => {
          volumeEventSource?.addEventListener(eventType, (event) => {
            console.log(`🎯 Received event of type "${eventType}":`, event);
          });
        });

        volumeEventSource.onerror = () => {
          // Let EventSource auto-reconnect silently
        };

        console.log('✅ EventSource created, waiting for events...');
      } catch (error) {
        console.error('Failed to create volume SSE connection:', error);
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
