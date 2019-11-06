package net.dirtydeeds.discordsoundboard.beans;

import javax.persistence.Entity;
import javax.persistence.Id;

/**
 * Class that represents a user of discord.
 *
 * @author dfurrer.
 */
@SuppressWarnings("unused")
@Entity
public class User {

    @Id
    private String id;
    private String username;
    private String entranceSound;
    private String leaveSound;
    private boolean selected;

    public User() {
    }

    public User(String id, String username) {
        this.id = id;
        this.username = username;
        this.selected = false;
    }
    
    public User(String id, String username, boolean selected) {
        this.id = id;
        this.username = username;
        this.selected = selected;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEntranceSound() {
        return entranceSound;
    }

    public void setEntranceSound(String entranceSound) {
        this.entranceSound = entranceSound;
    }

    public String getLeaveSound() {
        return leaveSound;
    }

    public void setLeaveSound(String leaveSound) {
        this.leaveSound = leaveSound;
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }
}
