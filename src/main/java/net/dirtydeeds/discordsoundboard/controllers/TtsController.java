package net.dirtydeeds.discordsoundboard.controllers;

import net.dirtydeeds.discordsoundboard.SoundPlayer;
import net.dirtydeeds.discordsoundboard.service.DiscordUserService;
import net.dirtydeeds.discordsoundboard.tts.*;
import net.dirtydeeds.discordsoundboard.util.UserRoleConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST endpoints for text-to-speech functionality.
 * <p>
 * All play requests require the {@code use-tts} permission.
 * Voice listing is open to any authenticated user so the frontend can populate
 * the speaker dropdown without requiring TTS to be used.
 * </p>
 */
@RestController
@RequestMapping("/api/tts")
@SuppressWarnings("unused")
public class TtsController {

    private static final Logger LOG = LoggerFactory.getLogger(TtsController.class);

    private final TtsService ttsService;
    private final TtsFileService ttsFileService;
    private final SoundPlayer soundPlayer;
    private final UserRoleConfig userRoleConfig;
    private final DiscordUserService discordUserService;

    public TtsController(TtsService ttsService,
                         TtsFileService ttsFileService,
                         SoundPlayer soundPlayer,
                         UserRoleConfig userRoleConfig,
                         DiscordUserService discordUserService) {
        this.ttsService = ttsService;
        this.ttsFileService = ttsFileService;
        this.soundPlayer = soundPlayer;
        this.userRoleConfig = userRoleConfig;
        this.discordUserService = discordUserService;
    }

    /** Returns whether TTS is enabled and which provider is active. */
    @GetMapping("/status")
    public Map<String, Object> getStatus() {
        return Map.of(
                "enabled", ttsService.isEnabled(),
                "provider", ttsService.getProviderName()
        );
    }

    /**
     * Returns a page of available voices for the active TTS provider.
     * Returns an empty page when TTS is disabled so the frontend degrades gracefully.
     *
     * @param page     1-based page number (default 1).
     * @param pageSize Voices per page (default 20, max 100).
     */
    @GetMapping("/voices")
    public ResponseEntity<?> getVoices(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(defaultValue = "") String title) {
        pageSize = Math.min(pageSize, 100);
        try {
            return ResponseEntity.ok(ttsService.getAvailableVoices(page, pageSize, title));
        } catch (TtsException e) {
            LOG.warn("Failed to fetch TTS voices: {}", e.getMessage());
            return ResponseEntity.ok(new net.dirtydeeds.discordsoundboard.tts.TtsVoicePage(
                    List.of(), page, 0, false));
        }
    }

    /**
     * Generate TTS audio from the supplied text and play it in the user's
     * Discord voice channel.
     */
    @PostMapping("/play")
    public ResponseEntity<?> playTts(
            @RequestBody TtsPlayRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        // --- Permission check ---
        String requestingUserId = userRoleConfig.getUserIdFromAuth(authorization);
        if (!userRoleConfig.hasPermission(requestingUserId, "use-tts")) {
            return ResponseEntity.status(403).body("You don't have permission to use text-to-speech");
        }

        if (!ttsService.isEnabled()) {
            return ResponseEntity.status(503).body("TTS is not enabled on this server");
        }

        if (request.text() == null || request.text().isBlank()) {
            return ResponseEntity.badRequest().body("Text must not be empty");
        }

        if (request.text().length() > 1000) {
            return ResponseEntity.badRequest().body("Text must not exceed 1000 characters");
        }

        // --- Resolve requesting user display name ---
        String requestingUser = "anonymous";
        if (requestingUserId != null) {
            var discordUser = discordUserService.findOneByIdOrUsernameIgnoreCase(requestingUserId, requestingUserId);
            if (discordUser != null) {
                requestingUser = discordUser.getUsername();
            }
        }

        try {
            // 1. Generate audio bytes via TTS provider
            TtsRequest ttsRequest = new TtsRequest(request.text(), request.speakerId(), null);
            TtsResult result = ttsService.generateAudio(ttsRequest);

            // 2. Save to disk
            String filePath = ttsFileService.saveAudio(result);

            // 3. Play via existing Lavaplayer pipeline
            //    Passing an absolute path that doesn't exist in the DB causes SoundPlayer
            //    to fall through to loadItem(filePath, handler), which Lavaplayer handles.
            String username = request.username();
            if (username == null || username.isBlank()) {
                username = requestingUser;
            }

            soundPlayer.playForUser(filePath, username, 1, request.voiceChannelId(), requestingUser);

            LOG.info("TTS played for user '{}' (requesting: '{}')", username, requestingUser);
            return ResponseEntity.ok().build();

        } catch (TtsException e) {
            LOG.warn("TTS generation failed: {}", e.getMessage());
            return ResponseEntity.internalServerError().body(e.getMessage());
        } catch (Exception e) {
            LOG.error("Unexpected error during TTS playback", e);
            return ResponseEntity.internalServerError().body("An unexpected error occurred");
        }
    }

    /** Request body for the {@code POST /api/tts/play} endpoint. */
    public record TtsPlayRequest(
            String text,
            String speakerId,
            String username,
            String voiceChannelId
    ) {}
}