package net.dirtydeeds.discordsoundboard.beans;

import lombok.*;
import net.dv8tion.jda.api.JDA;

import javax.persistence.Entity;
import javax.persistence.Id;

/**
 * Class that represents a user of discord.
 *
 * @author dfurrer.
 */
@Data
@Entity
@Setter
@NoArgsConstructor
@AllArgsConstructor
@RequiredArgsConstructor
public class User {

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

    public User(@NonNull String id, String username, @NonNull Boolean selected, @NonNull JDA.Status status) {
        this.id = id;
        this.username = username;
        this.selected = selected;
        this.status = status;
    }
}