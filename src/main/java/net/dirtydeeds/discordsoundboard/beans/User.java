package net.dirtydeeds.discordsoundboard.beans;

/**
 * Class that represents a user of discord.
 *
 * @author dfurrer.
 */
@SuppressWarnings("unused")
public class User {
    private String id;
    private String username;
    private String status;
    private boolean selected;

    public User(String id, String username, String status) {
        this.id = id;
        this.username = username;
        this.status = status;
        this.selected = false;
    }
    
    public User(String id, String username, String status, boolean selected) {
        this.id = id;
        this.username = username;
        this.status = status;
        this.selected = selected;
    }

    public String getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getUsernameLowerCase() {
        return username.toLowerCase();
    }

    public String getStatus() {
        return status;
    }

    public boolean isSelected() {
        return selected;
    }
}
