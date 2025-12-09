package net.dirtydeeds.discordsoundboard.controllers;

import io.swagger.v3.oas.annotations.Hidden;
import jakarta.inject.Inject;
import net.dirtydeeds.discordsoundboard.SoundPlayer;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Hidden
@RestController
@RequestMapping("/api/volume")
public class BotVolumeController {

    private final SoundPlayer soundPlayer;

    @Inject
    private BotVolumeController (SoundPlayer soundPlayer) {
        this.soundPlayer = soundPlayer;
    }

    // Store all active SSE connections
    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();

    @PostMapping(value = "")
    public ResponseEntity<Void> setVolume(@RequestParam Integer volume, @RequestParam String username,
                                          @RequestParam(defaultValue = "") String voiceChannelId) {
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
            emitter.completeWithError(e);
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
}
