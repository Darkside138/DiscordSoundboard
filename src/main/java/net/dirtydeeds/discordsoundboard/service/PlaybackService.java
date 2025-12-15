package net.dirtydeeds.discordsoundboard.service;

import net.dirtydeeds.discordsoundboard.PlaybackEvent;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public interface PlaybackService {

    SseEmitter createEmitter();

    void sendTrackStart(String soundFileId, String displayName, String user);

    void sendTrackEnd(String soundFileId);
}
