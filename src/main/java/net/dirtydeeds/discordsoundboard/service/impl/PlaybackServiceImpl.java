package net.dirtydeeds.discordsoundboard.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.dirtydeeds.discordsoundboard.PlaybackEvent;
import net.dirtydeeds.discordsoundboard.service.PlaybackService;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class PlaybackServiceImpl implements PlaybackService {

    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();
    private final ObjectMapper objectMapper;

    public PlaybackServiceImpl(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
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
    public void sendTrackStart(String soundFileId, String user) {
        PlaybackEvent event = new PlaybackEvent(soundFileId, user);
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
}
