import { useState, useEffect, useCallback } from 'react';

export interface PlaybackHistoryEntry {
  soundId: string;
  displayName: string;
  timestamp: number;
  count: number;
}

const STORAGE_KEY = 'soundboard-playback-history';
const MAX_ENTRIES = 100;

export function usePlaybackHistory() {
  const [history, setHistory] = useState<PlaybackHistoryEntry[]>(() => {
    try {
      return JSON.parse(localStorage.getItem(STORAGE_KEY) || '[]');
    } catch {
      return [];
    }
  });

  useEffect(() => {
    try {
      localStorage.setItem(STORAGE_KEY, JSON.stringify(history));
    } catch {}
  }, [history]);

  const recordPlay = useCallback((soundId: string, displayName: string) => {
    setHistory(prev => {
      const last = prev[0];
      if (last && last.soundId === soundId) {
        return [{ ...last, count: last.count + 1 }, ...prev.slice(1)];
      }
      return [{ soundId, displayName, timestamp: Date.now(), count: 1 }, ...prev].slice(0, MAX_ENTRIES);
    });
  }, []);

  const clearHistory = useCallback(() => setHistory([]), []);

  return { history, recordPlay, clearHistory };
}