package net.dirtydeeds.discordsoundboard.beans;

import javax.persistence.*;
import java.util.Date;
import java.util.UUID;

@Entity
public class PlayEvent {


    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;
    @Column(nullable = false)
    private String username;
    @Column(nullable = false)
    private String filename;
    @Column(nullable = false)
    private Date timestamp;

    protected PlayEvent() {

    }

    public PlayEvent(String username, String filename) {
        this.username = username;
        this.filename = filename;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    @PrePersist
    private void updateTimestamp() {
        timestamp = new Date();
    }

    public String toDisplay() {
        return String.format("%.20s\t%.20s\t%s", username, filename, timestamp);
    }

}
