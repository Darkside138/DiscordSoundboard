package net.dirtydeeds.discordsoundboard.tts;

/**
 * Request to generate TTS audio.
 *
 * @param text      The text to synthesize.
 * @param speakerId The provider-specific voice/model ID. Null uses the provider default.
 * @param format    Audio format ("mp3", "wav", "opus"). Null uses the provider default.
 */
public record TtsRequest(
        String text,
        String speakerId,
        String format
) {}