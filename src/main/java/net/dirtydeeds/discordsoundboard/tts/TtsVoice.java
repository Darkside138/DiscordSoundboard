package net.dirtydeeds.discordsoundboard.tts;

/**
 * Represents an available TTS voice/model from a provider.
 *
 * @param id          The provider-specific ID used in TTS requests.
 * @param name        Human-readable display name.
 * @param description Optional description of the voice.
 * @param provider    The name of the TTS provider (e.g. "fish-audio").
 */
public record TtsVoice(
        String id,
        String name,
        String description,
        String provider
) {}