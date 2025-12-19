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
    private String guildId;

    public PlaybackEvent(String soundFileId, String displayName, String user, String guildId) {
        this.soundFileId = soundFileId;
        this.timestamp = Instant.now();
        this.user = user;
        this.displayName = displayName;
        this.guildId = guildId;
    }

    public PlaybackEvent(String soundFileId, String guildId) {
        this.soundFileId = soundFileId;
        this.guildId = guildId;
        this.timestamp = Instant.now();
    }
}