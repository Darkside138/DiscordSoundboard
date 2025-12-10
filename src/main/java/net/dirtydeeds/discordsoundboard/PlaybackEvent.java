package net.dirtydeeds.discordsoundboard;

import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Setter
@Getter
public class PlaybackEvent {
    // Getters and setters
    private String soundFileId;
    private Instant timestamp;

    public PlaybackEvent(String soundFileId) {
        this.soundFileId = soundFileId;
        this.timestamp = Instant.now();
    }

}