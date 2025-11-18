package net.dirtydeeds.discordsoundboard.beans;

import jakarta.persistence.Id;
import lombok.*;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.OnlineStatus;

import jakarta.persistence.Entity;

/**
 * Class that represents a user of discord.
 *
 * @author dfurrer.
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@RequiredArgsConstructor
public class Users {

    @Id
    @NonNull
    private String id;
    private String username;
    private String entranceSound;
    private String leaveSound;
    @NonNull
    private Boolean selected;
    @NonNull
    private JDA.Status status;
    @NonNull
    private OnlineStatus onlineStatus;

    public Users(@NonNull String id, String username, @NonNull Boolean selected, @NonNull JDA.Status status, @NonNull OnlineStatus onlineStatus) {
        this.id = id;
        this.username = username;
        this.selected = selected;
        this.status = status;
        this.onlineStatus = onlineStatus;
    }
}