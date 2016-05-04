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
    private boolean selected;

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

    public String getUsername() {
        return username;
    }

    public boolean isSelected() {
        return selected;
    }
}
