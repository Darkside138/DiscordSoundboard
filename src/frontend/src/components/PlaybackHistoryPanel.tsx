import { History, Trash2 } from 'lucide-react';
import { PlaybackHistoryEntry } from '../hooks/usePlaybackHistory';

interface PlaybackHistoryPanelProps {
  history: PlaybackHistoryEntry[];
  onClear: () => void;
  theme: 'light' | 'dark';
  formatSoundName: (id: string) => string;
}

function formatTimeAgo(timestamp: number): string {
  const seconds = Math.floor((Date.now() - timestamp) / 1000);
  if (seconds < 60) return 'just now';
  const minutes = Math.floor(seconds / 60);
  if (minutes < 60) return `${minutes}m ago`;
  const hours = Math.floor(minutes / 60);
  if (hours < 24) return `${hours}h ago`;
  return `${Math.floor(hours / 24)}d ago`;
}

export function PlaybackHistoryPanel({ history, onClear, theme, formatSoundName }: PlaybackHistoryPanelProps) {
  const dark = theme === 'dark';

  return (
    <div className={`rounded-lg border mb-6 overflow-hidden ${dark ? 'bg-gray-800 border-gray-700' : 'bg-white border-gray-200'}`}>
      <div className={`flex items-center justify-between px-4 py-3 border-b ${dark ? 'border-gray-700' : 'border-gray-200'}`}>
        <div className="flex items-center gap-2">
          <History className={`w-4 h-4 ${dark ? 'text-gray-400' : 'text-gray-500'}`} />
          <span className={`text-sm font-semibold ${dark ? 'text-gray-300' : 'text-gray-700'}`}>
            Playback History
          </span>
          {history.length > 0 && (
            <span className={`text-xs px-1.5 py-0.5 rounded-full ${dark ? 'bg-gray-700 text-gray-400' : 'bg-gray-100 text-gray-500'}`}>
              {history.length}
            </span>
          )}
        </div>
        {history.length > 0 && (
          <button
            onClick={onClear}
            className={`flex items-center gap-1 text-xs px-2 py-1 rounded transition-colors ${
              dark ? 'text-gray-400 hover:text-red-400 hover:bg-gray-700' : 'text-gray-500 hover:text-red-500 hover:bg-gray-100'
            }`}
          >
            <Trash2 className="w-3 h-3" />
            Clear
          </button>
        )}
      </div>

      {history.length === 0 ? (
        <div className={`px-4 py-6 text-center text-sm ${dark ? 'text-gray-500' : 'text-gray-400'}`}>
          No playback history yet
        </div>
      ) : (
        <div className="overflow-y-auto max-h-[300px]">
          {history.map((entry, i) => (
            <div
              key={`${entry.soundId}-${entry.timestamp}-${i}`}
              className={`flex items-center justify-between px-4 py-2 border-b last:border-b-0 ${
                dark ? 'border-gray-700/50 hover:bg-gray-750' : 'border-gray-100 hover:bg-gray-50'
              }`}
            >
              <div className="flex items-center gap-2 min-w-0 flex-1">
                {entry.count > 1 && (
                  <span className={`shrink-0 text-xs font-bold px-1.5 py-0.5 rounded-full ${
                    dark ? 'bg-blue-900/50 text-blue-300' : 'bg-blue-100 text-blue-700'
                  }`}>
                    ×{entry.count}
                  </span>
                )}
                <span className={`text-sm truncate ${dark ? 'text-gray-300' : 'text-gray-700'}`}>
                  {formatSoundName(entry.soundId)}
                </span>
                {entry.displayName && entry.displayName !== formatSoundName(entry.soundId) && (
                  <span className={`text-xs truncate ${dark ? 'text-gray-500' : 'text-gray-400'}`}>
                    ({entry.displayName})
                  </span>
                )}
              </div>
              <span className={`shrink-0 text-xs ml-2 ${dark ? 'text-gray-500' : 'text-gray-400'}`}>
                {formatTimeAgo(entry.timestamp)}
              </span>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}