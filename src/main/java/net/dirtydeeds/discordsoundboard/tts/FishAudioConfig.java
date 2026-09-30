package net.dirtydeeds.discordsoundboard.tts;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "tts.fish-audio")
@Getter
@Setter
public class FishAudioConfig {

    /** Fish Audio Bearer API key. Required when tts.enabled=true. */
    private String apiKey = "";

    /** Fish Audio model to use: "s1" or "s2-pro" (default). */
    private String defaultModel = "s2-pro";

    /** Audio format returned by the API: "mp3", "wav", or "opus". */
    private String defaultFormat = "mp3";

    /** Optional fallback voice model ID when the user does not select one. */
    private String defaultVoiceId = "";

    /** How many voices to fetch per page when listing available models. */
    private int voicePageSize = 50;

    /** Voice list cache TTL in milliseconds (default 10 minutes). */
    private long voiceCacheTtlMs = 600_000;
}