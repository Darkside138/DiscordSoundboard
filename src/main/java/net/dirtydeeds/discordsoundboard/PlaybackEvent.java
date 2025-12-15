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
    private String displayName;

    public PlaybackEvent(String soundFileId, String displayName, String user) {
        this.soundFileId = soundFileId;
        this.timestamp = Instant.now();
        this.user = user;
        this.displayName = displayName;
    }

    public PlaybackEvent(String soundFileId) {
        this.soundFileId = soundFileId;
        this.timestamp = Instant.now();
    }
}