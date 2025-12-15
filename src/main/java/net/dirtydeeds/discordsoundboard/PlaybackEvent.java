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
    private String user;

    public PlaybackEvent(String soundFileId, String user) {
        this.soundFileId = soundFileId;
        this.timestamp = Instant.now();
        this.user = user;
    }

    public PlaybackEvent(String soundFileId) {
        this.soundFileId = soundFileId;
        this.timestamp = Instant.now();
    }
}