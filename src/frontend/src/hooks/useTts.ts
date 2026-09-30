import { useState, useEffect, useCallback, useRef } from 'react';
import { API_ENDPOINTS } from '../config';
import { fetchWithAuth, fetchJsonWithAuth } from '../utils/api';

export interface TtsVoice {
  id: string;
  name: string;
  description?: string;
  provider: string;
}

interface TtsVoicePage {
  voices: TtsVoice[];
  page: number;
  total: number;
  hasMore: boolean;
}

export interface TtsStatus {
  enabled: boolean;
  provider: string;
}

const FAVORITES_KEY = 'tts-favorite-voices';
const LAST_VOICE_KEY = 'tts-last-voice-id';
const PAGE_SIZE = 20;
const SEARCH_DEBOUNCE_MS = 400;

/** Stored as an array of full TtsVoice objects so favorites are always displayable. */
function loadFavoritesFromStorage(): Map<string, TtsVoice> {
  try {
    const raw = localStorage.getItem(FAVORITES_KEY);
    if (!raw) return new Map();
    const items: TtsVoice[] = JSON.parse(raw);
    return new Map(items.map(v => [v.id, v]));
  } catch {
    return new Map();
  }
}

function saveFavoritesToStorage(favorites: Map<string, TtsVoice>): void {
  localStorage.setItem(FAVORITES_KEY, JSON.stringify([...favorites.values()]));
}

function voicesUrl(page: number, title: string): string {
  const params = new URLSearchParams({
    page: String(page),
    pageSize: String(PAGE_SIZE),
  });
  if (title) params.set('title', title);
  return `${API_ENDPOINTS.TTS_VOICES}?${params}`;
}

export function useTts() {
  const [ttsEnabled, setTtsEnabled] = useState(false);
  const [provider, setProvider] = useState('none');
  const [voices, setVoices] = useState<TtsVoice[]>([]);
  const [hasMore, setHasMore] = useState(false);
  const [total, setTotal] = useState(0);
  const [currentPage, setCurrentPage] = useState(0);
  const [activeSearchTitle, setActiveSearchTitle] = useState(''); // title param sent to API
  const [loadingVoices, setLoadingVoices] = useState(false);
  const [isGenerating, setIsGenerating] = useState(false);
  const [statusLoaded, setStatusLoaded] = useState(false);
  const [favoriteVoices, setFavoriteVoices] = useState<Map<string, TtsVoice>>(loadFavoritesFromStorage);

  const searchDebounceRef = useRef<ReturnType<typeof setTimeout> | null>(null);
  const enabledRef = useRef(false);

  // Load status and first page on mount
  useEffect(() => {
    let mounted = true;
    const load = async () => {
      try {
        const status = await fetchJsonWithAuth<TtsStatus>(API_ENDPOINTS.TTS_STATUS);
        if (!mounted) return;
        setTtsEnabled(status.enabled);
        setProvider(status.provider);
        enabledRef.current = status.enabled;

        if (status.enabled) {
          setLoadingVoices(true);
          const page = await fetchJsonWithAuth<TtsVoicePage>(voicesUrl(1, ''));
          if (!mounted) return;
          setVoices(page.voices);
          setHasMore(page.hasMore);
          setTotal(page.total);
          setCurrentPage(1);
        }
      } catch {
        // degrade silently
      } finally {
        if (mounted) {
          setStatusLoaded(true);
          setLoadingVoices(false);
        }
      }
    };
    load();
    return () => { mounted = false; };
  }, []);

  /**
   * Called from the UI's search input. Debounces then fires a server-side search,
   * resetting the voice list to page 1 of results.
   */
  const searchVoices = useCallback((query: string) => {
    if (searchDebounceRef.current) clearTimeout(searchDebounceRef.current);
    searchDebounceRef.current = setTimeout(async () => {
      const title = query.trim();
      setActiveSearchTitle(title);
      setLoadingVoices(true);
      try {
        const page = await fetchJsonWithAuth<TtsVoicePage>(voicesUrl(1, title));
        setVoices(page.voices);
        setHasMore(page.hasMore);
        setTotal(page.total);
        setCurrentPage(1);
      } catch {
        // silently ignore
      } finally {
        setLoadingVoices(false);
      }
    }, SEARCH_DEBOUNCE_MS);
  }, []);

  /** Load the next page, carrying the current search title. */
  const loadMoreVoices = useCallback(async () => {
    if (loadingVoices || !hasMore) return;
    setLoadingVoices(true);
    try {
      const nextPage = currentPage + 1;
      const page = await fetchJsonWithAuth<TtsVoicePage>(voicesUrl(nextPage, activeSearchTitle));
      setVoices(prev => [...prev, ...page.voices]);
      setHasMore(page.hasMore);
      setTotal(page.total);
      setCurrentPage(nextPage);
    } catch {
      // silently ignore
    } finally {
      setLoadingVoices(false);
    }
  }, [loadingVoices, hasMore, currentPage, activeSearchTitle]);

  /** Pass the full voice object so it can be persisted and shown even when not in current results. */
  const toggleFavoriteVoice = useCallback((voice: TtsVoice) => {
    setFavoriteVoices(prev => {
      const next = new Map(prev);
      next.has(voice.id) ? next.delete(voice.id) : next.set(voice.id, voice);
      saveFavoritesToStorage(next);
      return next;
    });
  }, []);

  const getLastVoiceId = (): string => localStorage.getItem(LAST_VOICE_KEY) ?? '';
  const saveLastVoiceId = (id: string) => localStorage.setItem(LAST_VOICE_KEY, id);

  const generateAndPlay = async (
    text: string,
    speakerId: string,
    username: string,
    voiceChannelId?: string,
  ): Promise<void> => {
    if (speakerId) saveLastVoiceId(speakerId);
    setIsGenerating(true);
    try {
      const response = await fetchWithAuth(API_ENDPOINTS.TTS_PLAY, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ text, speakerId, username, voiceChannelId }),
      });
      if (!response.ok) {
        const message = await response.text();
        throw new Error(message || `HTTP ${response.status}`);
      }
    } finally {
      setIsGenerating(false);
    }
  };

  return {
    ttsEnabled,
    provider,
    voices,
    hasMore,
    total,
    activeSearchTitle,
    loadingVoices,
    isGenerating,
    statusLoaded,
    favoriteVoices,
    toggleFavoriteVoice,
    searchVoices,
    loadMoreVoices,
    getLastVoiceId,
    generateAndPlay,
  };
}