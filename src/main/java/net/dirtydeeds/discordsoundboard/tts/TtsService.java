package net.dirtydeeds.discordsoundboard.tts;

import java.util.List;

/**
 * Abstraction over any TTS provider. Implement this interface to add a new provider;
 * no other code needs to change.
 */
public interface TtsService {

    /**
     * Generate audio from the given request.
     *
     * @throws TtsException if generation fails or TTS is disabled.
     */
    TtsResult generateAudio(TtsRequest request) throws TtsException;

    /**
     * Return a page of available voices for this provider.
     *
     * @param page     1-based page number.
     * @param pageSize Number of voices per page.
     * @param title    Optional server-side title search filter. Null or blank = no filter.
     * @return A {@link TtsVoicePage} containing the voices and pagination metadata.
     */
    TtsVoicePage getAvailableVoices(int page, int pageSize, String title) throws TtsException;

    /** The provider identifier shown to the frontend (e.g. "fish-audio", "none"). */
    String getProviderName();

    /** Whether TTS is active and configured. */
    boolean isEnabled();
}