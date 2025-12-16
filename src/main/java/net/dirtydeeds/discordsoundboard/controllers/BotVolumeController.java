package net.dirtydeeds.discordsoundboard.controllers;

import io.swagger.v3.oas.annotations.Hidden;
import jakarta.annotation.PreDestroy;
import jakarta.inject.Inject;
import net.dirtydeeds.discordsoundboard.SoundPlayer;
import net.dirtydeeds.discordsoundboard.util.UserRoleConfig;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Hidden
@RestController
@RequestMapping("/api/volume")
public class BotVolumeController {

    private final SoundPlayer soundPlayer;
    private final UserRoleConfig userRoleConfig;

    // Store all active SSE connections
    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();

    private final ScheduledExecutorService sseHeartbeatExecutor =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "sse-heartbeat");
                t.setDaemon(true);
                return t;
            });

    @Inject
    private BotVolumeController (SoundPlayer soundPlayer, UserRoleConfig userRoleConfig) {
        this.soundPlayer = soundPlayer;
        this.userRoleConfig = userRoleConfig;

        // Send a heartbeat every 25 seconds (tweak as needed)
        sseHeartbeatExecutor.scheduleAtFixedRate(
                this::broadcastHeartbeatSafely,
                90, 90, TimeUnit.SECONDS
        );
    }

    @PostMapping(value = "")
    public ResponseEntity<Void> setVolume(@RequestParam Integer volume, @RequestParam String username,
                                          @RequestParam(defaultValue = "") String voiceChannelId,
                                          @RequestHeader(value = "Authorization", required = false) String authorization) {

        String userId = userRoleConfig.getUserIdFromAuth(authorization);
        if (userId == null || !userRoleConfig.hasPermission(userId, "edit-sounds")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        soundPlayer.setGlobalVolume(volume, username, null);
        broadcastUpdate(username);
        return ResponseEntity.ok().build();
    }

    @GetMapping(value = "{username}")
    public float getVolume(@PathVariable String username, @RequestParam(defaultValue = "") String voiceChannelId) {
        return soundPlayer.getGlobalVolume(username, voiceChannelId);
    }

    // SSE endpoint for real-time updates
    @GetMapping(value = "/stream/{username}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamSounds(@PathVariable String username) {
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);

        // Add emitter to the list
        emitters.add(emitter);

        // Remove emitter when completed or timed out
        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitters.remove(emitter));
        emitter.onError((e) -> emitters.remove(emitter));

        // Send initial data immediately
        try {
            Float globalVolume = soundPlayer.getGlobalVolume(username, null);
            emitter.send(SseEmitter.event()
                    .name("globalVolume")
                    .data(globalVolume));
        } catch (IOException e) {
            emitter.complete();
        }

        return emitter;
    }

    // Helper method to broadcast updates to all connected clients
    public void broadcastUpdate(String username) {
        List<SseEmitter> deadEmitters = new CopyOnWriteArrayList<>();
        emitters.forEach(emitter -> {
            try {
                emitter.send(SseEmitter.event()
                        .name("globalVolume")
                        .data(soundPlayer.getGlobalVolume(username, null)));
            } catch (IOException e) {
                deadEmitters.add(emitter);
            }
        });

        // Remove dead emitters
        emitters.removeAll(deadEmitters);
    }

    private void broadcastHeartbeatSafely() {
        if (emitters.isEmpty()) return;

        List<SseEmitter> deadEmitters = new CopyOnWriteArrayList<>();
        emitters.forEach(emitter -> {
            try {
                // Keep the payload tiny; event name can be anything
                emitter.send(SseEmitter.event()
                        .name("heartbeat")
                        .data("ping"));
            } catch (IOException ex) {
                deadEmitters.add(emitter);
                emitter.complete();
            } catch (IllegalStateException ex) {
                // Can happen if emitter is already completed
                deadEmitters.add(emitter);
            }
        });

        emitters.removeAll(deadEmitters);
    }

    @PreDestroy
    public void shutdownHeartbeat() {
        sseHeartbeatExecutor.shutdownNow();
    }
}
