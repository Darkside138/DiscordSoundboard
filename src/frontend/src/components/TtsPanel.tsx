import React, { useEffect, useRef, useState, useMemo } from 'react';
import { Loader2, Mic, X, Star, Search, ChevronDown } from 'lucide-react';
import { useTts, TtsVoice } from '../hooks/useTts';

interface TtsPanelProps {
  onClose: () => void;
  username: string;
  voiceChannelId?: string;
}

const MAX_CHARS = 1000;

function VoiceRow({
  voice,
  isSelected,
  isFavorite,
  onSelect,
  onToggleFavorite,
}: {
  voice: TtsVoice;
  isSelected: boolean;
  isFavorite: boolean;
  onSelect: () => void;
  onToggleFavorite: () => void;
}) {
  return (
    <div
      className={`flex items-center gap-2 px-3 py-2 border-b last:border-b-0 border-gray-100 dark:border-gray-700 transition-colors cursor-pointer ${
        isSelected ? 'bg-blue-50 dark:bg-blue-900/30' : 'hover:bg-gray-50 dark:hover:bg-gray-700/50'
      }`}
      onClick={onSelect}
    >
      <button
        onClick={e => { e.stopPropagation(); onToggleFavorite(); }}
        className={`shrink-0 transition-colors ${
          isFavorite
            ? 'text-yellow-500 hover:text-yellow-600'
            : 'text-gray-300 dark:text-gray-600 hover:text-yellow-400'
        }`}
        title={isFavorite ? 'Remove from favorites' : 'Add to favorites'}
        aria-label={isFavorite ? 'Unfavorite' : 'Favorite'}
      >
        <Star className={`w-4 h-4 ${isFavorite ? 'fill-current' : ''}`} />
      </button>
      <div className="flex-1 min-w-0">
        <p className={`text-sm font-medium truncate ${
          isSelected ? 'text-blue-700 dark:text-blue-300' : 'text-gray-900 dark:text-gray-100'
        }`}>
          {voice.name}
        </p>
        {voice.description && (
          <p className="text-xs text-gray-400 dark:text-gray-500 truncate">{voice.description}</p>
        )}
      </div>
    </div>
  );
}

export function TtsPanel({ onClose, username, voiceChannelId }: TtsPanelProps) {
  const {
    ttsEnabled,
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
  } = useTts();

  const [text, setText] = useState('');
  const [selectedVoiceId, setSelectedVoiceId] = useState('');
  const [voiceSearch, setVoiceSearch] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState(false);

  const textareaRef = useRef<HTMLTextAreaElement>(null);

  const favoriteList = useMemo(() => [...favoriteVoices.values()], [favoriteVoices]);

  // Restore last used voice when list loads
  useEffect(() => {
    if (voices.length === 0 && favoriteList.length === 0) return;
    if (selectedVoiceId) return;
    const last = getLastVoiceId();
    if (last && (favoriteVoices.has(last) || voices.some(v => v.id === last))) {
      setSelectedVoiceId(last);
    } else if (favoriteList.length > 0) {
      setSelectedVoiceId(favoriteList[0].id);
    } else if (voices.length > 0) {
      setSelectedVoiceId(voices[0].id);
    }
  }, [voices, favoriteList]); // eslint-disable-line react-hooks/exhaustive-deps

  // Results list: API voices excluding ones already shown in favorites
  const resultVoices = useMemo(
    () => voices.filter(v => !favoriteVoices.has(v.id)),
    [voices, favoriteVoices]
  );

  const selectedVoice: TtsVoice | undefined =
    favoriteVoices.get(selectedVoiceId) ?? voices.find(v => v.id === selectedVoiceId);

  const handlePlay = async () => {
    if (!text.trim()) { setError('Please enter some text.'); return; }
    setError(null);
    setSuccess(false);
    try {
      await generateAndPlay(text.trim(), selectedVoiceId, username, voiceChannelId);
      setSuccess(true);
      setTimeout(() => setSuccess(false), 3000);
    } catch (e: any) {
      setError(e.message ?? 'Failed to generate audio.');
    }
  };

  return (
    <div className="bg-white dark:bg-gray-800 border border-gray-200 dark:border-gray-700 rounded-lg shadow-md p-4 mb-6">
      {/* Header */}
      <div className="flex items-center justify-between mb-4">
        <div className="flex items-center gap-2 font-semibold text-gray-800 dark:text-gray-100">
          <Mic className="w-4 h-4 text-purple-500" />
          Text to Speech
          {selectedVoice && (
            <span className="text-xs font-normal text-purple-600 dark:text-purple-400 ml-1">
              — {selectedVoice.name}
            </span>
          )}
        </div>
        <button
          onClick={onClose}
          className="p-1 rounded-lg text-gray-400 hover:text-gray-600 hover:bg-gray-100 dark:hover:text-gray-200 dark:hover:bg-gray-700 transition-colors"
          aria-label="Close TTS panel"
        >
          <X className="w-4 h-4" />
        </button>
      </div>

      {statusLoaded && !ttsEnabled && (
        <div className="rounded-lg bg-amber-50 dark:bg-amber-900/30 border border-amber-200 dark:border-amber-700 px-4 py-3 text-sm text-amber-800 dark:text-amber-300">
          TTS is not enabled. An admin must set <code className="font-mono">tts.enabled=true</code> and configure an API key.
        </div>
      )}

      {ttsEnabled && (
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">

          {/* ── Left: Voice Picker ── */}
          <div className="flex flex-col gap-2">

            {/* Favorites */}
            {favoriteList.length > 0 && (
              <div>
                <p className="text-xs font-semibold uppercase tracking-wider text-yellow-600 dark:text-yellow-400 mb-1 flex items-center gap-1">
                  <Star className="w-3 h-3 fill-current" />
                  Favorites
                </p>
                <div className="rounded-lg border border-yellow-200 dark:border-yellow-700/50 overflow-hidden">
                  {favoriteList.map(voice => (
                    <VoiceRow
                      key={voice.id}
                      voice={voice}
                      isSelected={selectedVoiceId === voice.id}
                      isFavorite={true}
                      onSelect={() => setSelectedVoiceId(voice.id)}
                      onToggleFavorite={() => toggleFavoriteVoice(voice)}
                    />
                  ))}
                </div>
              </div>
            )}

            {/* Search */}
            <div className="relative">
              <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400 dark:text-gray-500" />
              <input
                type="text"
                placeholder="Search all voices…"
                value={voiceSearch}
                onChange={e => { setVoiceSearch(e.target.value); searchVoices(e.target.value); }}
                className="w-full pl-9 pr-3 py-2 text-sm rounded-lg border bg-white border-gray-300 text-gray-900 placeholder-gray-400 dark:bg-gray-700 dark:border-gray-600 dark:text-white dark:placeholder-gray-500 focus:outline-none focus:ring-2 focus:ring-purple-500"
              />
              {voiceSearch && (
                <button
                  onClick={() => { setVoiceSearch(''); searchVoices(''); }}
                  className="absolute right-2 top-1/2 -translate-y-1/2 p-0.5 text-gray-400 hover:text-gray-600 dark:hover:text-gray-200"
                >
                  <X className="w-3.5 h-3.5" />
                </button>
              )}
            </div>

            {/* Voice list */}
            <div className="rounded-lg border border-gray-200 dark:border-gray-600 overflow-y-auto max-h-48 [&::-webkit-scrollbar]:w-1.5 [&::-webkit-scrollbar-track]:bg-gray-100 dark:[&::-webkit-scrollbar-track]:bg-gray-700 [&::-webkit-scrollbar-thumb]:bg-gray-300 dark:[&::-webkit-scrollbar-thumb]:bg-gray-600 [&::-webkit-scrollbar-thumb]:rounded-full">
              {/* No-voice option */}
              <button
                onClick={() => setSelectedVoiceId('')}
                className={`w-full flex items-center px-3 py-2 text-sm text-left border-b border-gray-100 dark:border-gray-700 transition-colors ${
                  selectedVoiceId === ''
                    ? 'bg-blue-50 dark:bg-blue-900/30 text-blue-700 dark:text-blue-300'
                    : 'text-gray-500 dark:text-gray-400 hover:bg-gray-50 dark:hover:bg-gray-700/50'
                }`}
              >
                <span className="italic">— No specific voice —</span>
              </button>

              {total > 0 && (
                <div className="px-3 py-1 text-xs text-gray-400 dark:text-gray-500 border-b border-gray-100 dark:border-gray-700 bg-gray-50 dark:bg-gray-700/30">
                  {activeSearchTitle
                    ? `${voices.length} of ${total} results for "${activeSearchTitle}"`
                    : `${voices.length} of ${total} voices loaded`}
                </div>
              )}

              {resultVoices.length === 0 && !loadingVoices && (
                <div className="px-3 py-4 text-sm text-center text-gray-400 dark:text-gray-500 italic">
                  {voices.length === 0 ? 'No voices available' : 'All matching voices are in your favorites'}
                </div>
              )}

              {resultVoices.map(voice => (
                <VoiceRow
                  key={voice.id}
                  voice={voice}
                  isSelected={selectedVoiceId === voice.id}
                  isFavorite={false}
                  onSelect={() => setSelectedVoiceId(voice.id)}
                  onToggleFavorite={() => toggleFavoriteVoice(voice)}
                />
              ))}

              {hasMore && (
                <button
                  onClick={loadMoreVoices}
                  disabled={loadingVoices}
                  className="w-full flex items-center justify-center gap-2 px-3 py-2 text-sm text-purple-600 dark:text-purple-400 hover:bg-purple-50 dark:hover:bg-purple-900/20 transition-colors disabled:opacity-50"
                >
                  {loadingVoices
                    ? <><Loader2 className="w-4 h-4 animate-spin" />Loading…</>
                    : <><ChevronDown className="w-4 h-4" />Load more</>}
                </button>
              )}

              {loadingVoices && voices.length === 0 && (
                <div className="flex items-center justify-center gap-2 py-4 text-sm text-gray-400 dark:text-gray-500">
                  <Loader2 className="w-4 h-4 animate-spin" />
                  Loading voices…
                </div>
              )}
            </div>
          </div>

          {/* ── Right: Text + Play ── */}
          <div className="flex flex-col gap-3">
            <div className="flex-1">
              <label className="block text-sm font-medium mb-1.5 text-gray-700 dark:text-gray-300">
                Text
              </label>
              <textarea
                ref={textareaRef}
                value={text}
                onChange={e => setText(e.target.value.slice(0, MAX_CHARS))}
                disabled={isGenerating}
                rows={5}
                placeholder="Type something to speak…"
                className="w-full px-3 py-2 rounded-lg border bg-white border-gray-300 text-gray-900 placeholder-gray-400 dark:bg-gray-700 dark:border-gray-600 dark:text-white dark:placeholder-gray-500 focus:outline-none focus:ring-2 focus:ring-purple-500 resize-none disabled:opacity-50"
              />
              <p className={`text-xs mt-1 text-right ${text.length >= MAX_CHARS ? 'text-red-500' : 'text-gray-400 dark:text-gray-500'}`}>
                {text.length}/{MAX_CHARS}
              </p>
            </div>

            {error && <p className="text-sm text-red-600 dark:text-red-400">{error}</p>}
            {success && <p className="text-sm text-green-600 dark:text-green-400">Playing in voice channel…</p>}

            <button
              onClick={handlePlay}
              disabled={isGenerating || !text.trim()}
              className="flex items-center justify-center gap-2 px-4 py-2 rounded-lg text-sm font-medium transition-colors bg-purple-600 text-white hover:bg-purple-700 dark:bg-purple-700 dark:hover:bg-purple-600 disabled:opacity-50 disabled:cursor-not-allowed"
            >
              {isGenerating
                ? <><Loader2 className="w-4 h-4 animate-spin" />Generating…</>
                : <><Mic className="w-4 h-4" />Play</>}
            </button>
          </div>

        </div>
      )}
    </div>
  );
}