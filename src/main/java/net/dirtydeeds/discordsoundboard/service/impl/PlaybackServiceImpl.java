package net.dirtydeeds.discordsoundboard.service.impl;

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
                90, 90, TimeUnit.SECONDS
        );
    }

    @Override
    public SseEmitter createEmitter() {
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);
        emitters.add(emitter);

        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitters.remove(emitter));
        emitter.onError((e) -> emitters.remove(emitter));

        return emitter;
    }

    @Override
    public void sendTrackStart(String soundFileId, String displayName, String user) {
        PlaybackEvent event = new PlaybackEvent(soundFileId, displayName, user);
        sendEventToAll("trackStart", event);
    }

    @Override
    public void sendTrackEnd(String soundFileId) {
        PlaybackEvent event = new PlaybackEvent(soundFileId);
        sendEventToAll("trackEnd", event);
    }

    private void sendEventToAll(String eventName, PlaybackEvent event) {
        List<SseEmitter> deadEmitters = new CopyOnWriteArrayList<>();

        emitters.forEach(emitter -> {
            try {
                String json = objectMapper.writeValueAsString(event);
                emitter.send(SseEmitter.event()
                        .name(eventName)
                        .data(json));
            } catch (IOException e) {
                deadEmitters.add(emitter);
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
