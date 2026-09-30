import React, { useEffect, useRef, useState, useMemo } from 'react';
import { Loader2, Mic, X, Star, Search, ChevronDown } from 'lucide-react';
import { useTts, TtsVoice } from '../hooks/useTts';

interface TtsDialogProps {
  isOpen: boolean;
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

export function TtsDialog({ isOpen, onClose, username, voiceChannelId }: TtsDialogProps) {
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
  const overlayRef = useRef<HTMLDivElement>(null);

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

  useEffect(() => {
    if (isOpen) {
      setTimeout(() => textareaRef.current?.focus(), 50);
      setError(null);
      setSuccess(false);
    }
  }, [isOpen]);

  useEffect(() => {
    if (!isOpen) return;
    const handler = (e: KeyboardEvent) => { if (e.key === 'Escape') onClose(); };
    document.addEventListener('keydown', handler);
    return () => document.removeEventListener('keydown', handler);
  }, [isOpen, onClose]);

  // Results list: API voices excluding ones already shown in favorites
  const resultVoices = useMemo(
    () => voices.filter(v => !favoriteVoices.has(v.id)),
    [voices, favoriteVoices]
  );

  const selectedVoice: TtsVoice | undefined =
    favoriteVoices.get(selectedVoiceId) ?? voices.find(v => v.id === selectedVoiceId);

  if (!isOpen) return null;

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
    <div
      ref={overlayRef}
      className="fixed inset-0 z-50 flex items-center justify-center p-4"
      style={{ backgroundColor: 'rgba(0,0,0,0.5)' }}
      onMouseDown={(e) => { if (e.target === overlayRef.current) onClose(); }}
    >
      <div className="bg-white dark:bg-gray-800 rounded-xl shadow-2xl w-full max-w-xl border border-gray-200 dark:border-gray-700 flex flex-col max-h-[90vh]">

        {/* Header */}
        <div className="flex items-center justify-between px-5 py-4 border-b border-gray-200 dark:border-gray-700 shrink-0">
          <div className="flex items-center gap-2 font-semibold text-gray-800 dark:text-gray-100">
            <Mic className="w-5 h-5 text-blue-500" />
            Text to Speech
          </div>
          <button
            onClick={onClose}
            className="p-1 rounded-lg text-gray-400 hover:text-gray-600 hover:bg-gray-100 dark:hover:text-gray-200 dark:hover:bg-gray-700 transition-colors"
            aria-label="Close"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Body */}
        <div className="px-5 py-4 space-y-4 overflow-y-auto flex-1">

          {statusLoaded && !ttsEnabled && (
            <div className="rounded-lg bg-amber-50 dark:bg-amber-900/30 border border-amber-200 dark:border-amber-700 px-4 py-3 text-sm text-amber-800 dark:text-amber-300">
              TTS is not enabled on this server. An admin must set{' '}
              <code className="font-mono">tts.enabled=true</code> and configure an API key.
            </div>
          )}

          {ttsEnabled && (
            <>
              {/* ── Speaker section ── */}
              <div>
                <div className="flex items-center justify-between mb-1.5">
                  <label className="text-sm font-medium text-gray-700 dark:text-gray-300">
                    Speaker
                  </label>
                  {selectedVoice && (
                    <span className="text-xs text-blue-600 dark:text-blue-400 font-medium truncate max-w-[200px]">
                      {selectedVoice.name}
                    </span>
                  )}
                </div>

                {/* ── Favorites (always visible) ── */}
                {favoriteList.length > 0 && (
                  <div className="mb-2">
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

                {/* ── Search input ── */}
                <div className="relative mb-2">
                  <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400 dark:text-gray-500" />
                  <input
                    type="text"
                    placeholder="Search all voices…"
                    value={voiceSearch}
                    onChange={e => {
                      setVoiceSearch(e.target.value);
                      searchVoices(e.target.value);
                    }}
                    className="w-full pl-9 pr-3 py-2 text-sm rounded-lg border bg-white border-gray-300 text-gray-900 placeholder-gray-400 dark:bg-gray-700 dark:border-gray-600 dark:text-white dark:placeholder-gray-500 focus:outline-none focus:ring-2 focus:ring-blue-500"
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

                {/* ── Results list ── */}
                <div className="rounded-lg border border-gray-200 dark:border-gray-600 overflow-y-auto max-h-44 [&::-webkit-scrollbar]:w-1.5 [&::-webkit-scrollbar-track]:bg-gray-100 dark:[&::-webkit-scrollbar-track]:bg-gray-700 [&::-webkit-scrollbar-thumb]:bg-gray-300 dark:[&::-webkit-scrollbar-thumb]:bg-gray-600 [&::-webkit-scrollbar-thumb]:rounded-full">

                  {/* No-voice option */}
                  <button
                    onClick={() => setSelectedVoiceId('')}
                    className={`w-full flex items-center gap-2 px-3 py-2 text-sm text-left border-b border-gray-100 dark:border-gray-700 transition-colors ${
                      selectedVoiceId === ''
                        ? 'bg-blue-50 dark:bg-blue-900/30 text-blue-700 dark:text-blue-300'
                        : 'text-gray-500 dark:text-gray-400 hover:bg-gray-50 dark:hover:bg-gray-700/50'
                    }`}
                  >
                    <span className="italic">— No specific voice —</span>
                  </button>

                  {/* Header showing count */}
                  {total > 0 && (
                    <div className="px-3 py-1.5 text-xs text-gray-400 dark:text-gray-500 border-b border-gray-100 dark:border-gray-700 bg-gray-50 dark:bg-gray-700/30">
                      {activeSearchTitle
                        ? `${voices.length} of ${total} results for "${activeSearchTitle}"`
                        : `${voices.length} of ${total} voices loaded`}
                    </div>
                  )}

                  {resultVoices.length === 0 && !loadingVoices && (
                    <div className="px-3 py-4 text-sm text-center text-gray-400 dark:text-gray-500 italic">
                      {voices.length === 0
                        ? 'No voices available — check your API key'
                        : 'All matching voices are in your favorites'}
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

                  {/* Load more */}
                  {hasMore && (
                    <button
                      onClick={loadMoreVoices}
                      disabled={loadingVoices}
                      className="w-full flex items-center justify-center gap-2 px-3 py-2.5 text-sm text-blue-600 dark:text-blue-400 hover:bg-blue-50 dark:hover:bg-blue-900/20 transition-colors disabled:opacity-50"
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

              {/* Text input */}
              <div>
                <label className="block text-sm font-medium mb-1.5 text-gray-700 dark:text-gray-300">
                  Text
                </label>
                <textarea
                  ref={textareaRef}
                  value={text}
                  onChange={e => setText(e.target.value.slice(0, MAX_CHARS))}
                  disabled={isGenerating}
                  rows={4}
                  placeholder="Type something to speak…"
                  className="w-full px-3 py-2 rounded-lg border bg-white border-gray-300 text-gray-900 placeholder-gray-400 dark:bg-gray-700 dark:border-gray-600 dark:text-white dark:placeholder-gray-500 focus:outline-none focus:ring-2 focus:ring-blue-500 resize-none disabled:opacity-50"
                />
                <p className={`text-xs mt-1 text-right ${text.length >= MAX_CHARS ? 'text-red-500' : 'text-gray-400 dark:text-gray-500'}`}>
                  {text.length}/{MAX_CHARS}
                </p>
              </div>
            </>
          )}

          {error && <p className="text-sm text-red-600 dark:text-red-400">{error}</p>}
          {success && <p className="text-sm text-green-600 dark:text-green-400">Playing in voice channel…</p>}
        </div>

        {/* Footer */}
        <div className="flex justify-end gap-3 px-5 py-4 border-t border-gray-200 dark:border-gray-700 shrink-0">
          <button
            onClick={onClose}
            className="px-4 py-2 rounded-lg text-sm transition-colors bg-gray-100 text-gray-700 hover:bg-gray-200 dark:bg-gray-700 dark:text-gray-300 dark:hover:bg-gray-600"
          >
            Close
          </button>
          <button
            onClick={handlePlay}
            disabled={isGenerating || !ttsEnabled || !text.trim()}
            className="flex items-center gap-2 px-4 py-2 rounded-lg text-sm font-medium transition-colors bg-blue-600 text-white hover:bg-blue-700 dark:bg-blue-700 dark:hover:bg-blue-600 disabled:opacity-50 disabled:cursor-not-allowed"
          >
            {isGenerating
              ? <><Loader2 className="w-4 h-4 animate-spin" />Generating…</>
              : <><Mic className="w-4 h-4" />Play</>}
          </button>
        </div>
      </div>
    </div>
  );
}