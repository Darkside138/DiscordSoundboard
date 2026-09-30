package net.dirtydeeds.discordsoundboard.tts;

/**
 * The audio output produced by a TTS provider.
 *
 * @param audioBytes Raw audio bytes.
 * @param format     The audio format ("mp3", "wav", "opus", etc.).
 */
public record TtsResult(
        byte[] audioBytes,
        String format
) {}