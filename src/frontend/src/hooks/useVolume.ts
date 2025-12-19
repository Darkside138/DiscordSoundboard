import { useState, useEffect } from 'react';
import { API_ENDPOINTS } from '../config';
import { getAuthHeadersWithCsrf } from '../utils/api';

export function useVolume() {
  const [volume, setVolume] = useState<number>(100);

  // Load volume from localStorage
  useEffect(() => {
    const savedVolume = localStorage.getItem('soundboard-volume');
    if (savedVolume) {
      setVolume(parseInt(savedVolume, 10));
    }
  }, []);

  // Save volume to localStorage
  useEffect(() => {
    localStorage.setItem('soundboard-volume', volume.toString());
  }, [volume]);

  const updateVolume = async (newVolume: number, selectedUserId: string | null) => {
    if (!selectedUserId) return;

    // Update local state immediately for responsive UI
    setVolume(newVolume);

    try {
      const response = await fetch(
        `${API_ENDPOINTS.VOLUME}?username=${selectedUserId}&volume=${newVolume}`,
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

      if (!response.ok) {
        console.error('Failed to update volume:', response.status);
      } else {
        console.log(`Volume updated successfully: ${newVolume}%`);
      }
    } catch (error) {
      console.error('Error updating volume:', error);
    }
  };

  return {
    volume,
    setVolume,
    updateVolume
  };
}