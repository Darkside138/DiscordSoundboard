package net.dirtydeeds.discordsoundboard.tts.impl;

import net.dirtydeeds.discordsoundboard.tts.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * Fallback TTS service used when no provider is configured (tts.enabled=false).
 * Satisfies the {@link TtsService} injection contract without requiring any
 * external API keys or dependencies.
 */
@Service
@ConditionalOnProperty(name = "tts.enabled", havingValue = "false", matchIfMissing = true)
public class NoOpTtsService implements TtsService {

    @Override
    public TtsResult generateAudio(TtsRequest request) {
        throw new TtsException(
                "TTS is not enabled. Set tts.enabled=true and configure tts.fish-audio.api-key " +
                "in application.properties to activate text-to-speech.");
    }

    @Override
    public TtsVoicePage getAvailableVoices(int page, int pageSize, String title) {
        return new TtsVoicePage(java.util.List.of(), page, 0, false);
    }

    @Override
    public String getProviderName() {
        return "none";
    }

    @Override
    public boolean isEnabled() {
        return false;
    }
}