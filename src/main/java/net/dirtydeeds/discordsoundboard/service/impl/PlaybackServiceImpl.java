package net.dirtydeeds.discordsoundboard.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PreDestroy;
import net.dirtydeeds.discordsoundboard.PlaybackEvent;
import net.dirtydeeds.discordsoundboard.service.PlaybackService;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Service
public class PlaybackServiceImpl implements PlaybackService {

    private static final long EMITTER_TIMEOUT_MILLIS = TimeUnit.MINUTES.toMillis(5);

    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();
    private final ObjectMapper objectMapper;

    private final ScheduledExecutorService sseHeartbeatExecutor =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "sse-heartbeat");
                t.setDaemon(true);
                return t;
            });

    public PlaybackServiceImpl(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;

        // Send a heartbeat every 25 seconds (tweak as needed)
        sseHeartbeatExecutor.scheduleAtFixedRate(
                this::broadcastHeartbeatSafely,
                25, 25, TimeUnit.SECONDS
        );
    }

    @Override
    public SseEmitter createEmitter() {
        SseEmitter emitter = new SseEmitter(EMITTER_TIMEOUT_MILLIS);
        emitters.add(emitter);

        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> {
            emitters.remove(emitter);
            emitter.complete();
        });
        emitter.onError((e) -> {
            emitters.remove(emitter);
            emitter.complete();
        });

        return emitter;
    }

    @Override
    public void sendTrackStart(String soundFileId, String displayName, String user, String guildId) {
        PlaybackEvent event = new PlaybackEvent(soundFileId, displayName, user, guildId);
        sendEventToAll("trackStart", event);
    }

    @Override
    public void sendTrackEnd(String soundFileId, String guildId) {
        PlaybackEvent event = new PlaybackEvent(soundFileId, guildId);
        sendEventToAll("trackEnd", event);
    }

    private void sendEventToAll(String eventName, PlaybackEvent event) {
        if (emitters.isEmpty()) return;

        final String json;
        try {
            json = objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException e) {
            // If we can't serialize, there's nothing to send (and retrying won't help).
            return;
        }

        List<SseEmitter> deadEmitters = new CopyOnWriteArrayList<>();

        emitters.forEach(emitter -> {
            try {
                emitter.send(SseEmitter.event()
                        .name(eventName)
                        .data(json));
            } catch (IOException | IllegalStateException e) {
                deadEmitters.add(emitter);
                try {
                    emitter.complete();
                } catch (Exception ignored) {
                    // best-effort cleanup
                }
            }
        });

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
            } catch (IOException | IllegalStateException ex) {
                deadEmitters.add(emitter);
                try {
                    emitter.complete();
                } catch (Exception ignored) {
                    // best-effort cleanup
                }
            }
        });

        emitters.removeAll(deadEmitters);
    }

    @PreDestroy
    public void shutdownHeartbeat() {
        sseHeartbeatExecutor.shutdownNow();
    }
}
