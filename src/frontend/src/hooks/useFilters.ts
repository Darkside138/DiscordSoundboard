import { useState, useEffect } from 'react';
import type { Sound } from './useSounds';

export function useFilters(sounds: Sound[], favorites: Set<string>) {
  const [selectedCategory, setSelectedCategory] = useState<string>('all');
  const [activeFilter, setActiveFilter] = useState<'none' | 'favorites' | 'popular' | 'recent'>('none');
  const [searchQuery, setSearchQuery] = useState('');
  const [popularCount, setPopularCount] = useState<number>(10);
  const [recentCount, setRecentCount] = useState<number>(10);

  // Load settings from localStorage
  useEffect(() => {
    const savedPopularCount = localStorage.getItem('soundboard-popular-count');
    if (savedPopularCount) {
      setPopularCount(parseInt(savedPopularCount, 10));
    }
    
    const savedRecentCount = localStorage.getItem('soundboard-recent-count');
    if (savedRecentCount) {
      setRecentCount(parseInt(savedRecentCount, 10));
    }
  }, []);

  // Save settings to localStorage
  useEffect(() => {
    localStorage.setItem('soundboard-popular-count', popularCount.toString());
    localStorage.setItem('soundboard-recent-count', recentCount.toString());
  }, [popularCount, recentCount]);

  // Get unique categories
  const categories = ['all', ...Array.from(new Set(sounds.map(s => s.category)))];

  // Calculate top sounds by timesPlayed
  const top10SoundIds = new Set(
    [...sounds]
      .filter(sound => !favorites.has(sound.id))
      .sort((a, b) => b.timesPlayed - a.timesPlayed)
      .slice(0, popularCount)
      .map(s => s.id)
  );

  // Calculate recently added sounds
  const recentlyAddedIds = new Set(
    [...sounds]
      .filter(sound => !favorites.has(sound.id) && !top10SoundIds.has(sound.id))
      .sort((a, b) => new Date(b.dateAdded).getTime() - new Date(a.dateAdded).getTime())
      .slice(0, recentCount)
      .map(s => s.id)
  );

  // Filter sounds
  const filteredSounds = sounds
    .filter(sound => {
      const matchesCategory = selectedCategory === 'all' || sound.category === selectedCategory;
      
      let matchesFilter = true;
      if (activeFilter === 'favorites') {
        matchesFilter = favorites.has(sound.id);
      } else if (activeFilter === 'popular') {
        matchesFilter = top10SoundIds.has(sound.id);
      } else if (activeFilter === 'recent') {
        matchesFilter = recentlyAddedIds.has(sound.id);
      }
      
      const matchesSearch = sound.name.toLowerCase().includes(searchQuery.toLowerCase());
      return matchesCategory && matchesFilter && matchesSearch;
    })
    .sort((a, b) => a.name.localeCompare(b.name));

  return {
    selectedCategory,
    setSelectedCategory,
    activeFilter,
    setActiveFilter,
    searchQuery,
    setSearchQuery,
    popularCount,
    setPopularCount,
    recentCount,
    setRecentCount,
    categories,
    top10SoundIds,
    recentlyAddedIds,
    filteredSounds
  };
}
