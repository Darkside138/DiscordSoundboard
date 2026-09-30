package net.dirtydeeds.discordsoundboard.tts;

import java.util.List;

/**
 * Paginated voice list returned by {@link TtsService#getAvailableVoices(int, int)}.
 *
 * @param voices   The voices on this page.
 * @param page     The current 1-based page number.
 * @param total    Total number of voices available from the provider.
 * @param hasMore  Whether additional pages exist beyond this one.
 */
public record TtsVoicePage(
        List<TtsVoice> voices,
        int page,
        int total,
        boolean hasMore
) {}