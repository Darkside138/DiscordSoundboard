import { useState, useRef } from 'react';
import { API_ENDPOINTS } from '../config';

export function useLocalPlayback() {
  const [locallyPlayingSoundId, setLocallyPlayingSoundId] = useState<string | null>(null);
  const currentLocalAudioRef = useRef<HTMLAudioElement | null>(null);

  const playLocalSound = async (soundId: string) => {
    try {
      // Stop any currently playing local audio
      if (currentLocalAudioRef.current) {
        currentLocalAudioRef.current.pause();
        currentLocalAudioRef.current = null;
      }

      const audioUrl = `${API_ENDPOINTS.AUDIO_FILE}/${soundId}`;
      const audio = new Audio(audioUrl);
      
      audio.addEventListener('ended', () => {
        setLocallyPlayingSoundId(null);
        currentLocalAudioRef.current = null;
      });

      audio.addEventListener('error', () => {
        console.error('Error playing sound locally');
        setLocallyPlayingSoundId(null);
        currentLocalAudioRef.current = null;
      });

      audio.play().catch(err => {
        console.error('Error playing sound locally:', err);
        setLocallyPlayingSoundId(null);
        currentLocalAudioRef.current = null;
      });
      
      setLocallyPlayingSoundId(soundId);
      currentLocalAudioRef.current = audio;
    } catch (error) {
      console.error('Error playing sound locally:', error);
      setLocallyPlayingSoundId(null);
    }
  };

  const stopLocalSound = () => {
    if (currentLocalAudioRef.current) {
      currentLocalAudioRef.current.pause();
      currentLocalAudioRef.current = null;
      setLocallyPlayingSoundId(null);
    }
  };

  return {
    locallyPlayingSoundId,
    playLocalSound,
    stopLocalSound
  };
}
