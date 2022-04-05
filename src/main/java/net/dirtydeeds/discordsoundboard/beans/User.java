package net.dirtydeeds.discordsoundboard.beans;

import lombok.*;

import javax.persistence.Entity;
import javax.persistence.Id;

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
public class User {

    @Id
    @NonNull
    private String id;
    @NonNull
    private String username;
    private String entranceSound;
    private String leaveSound;
    @NonNull
    private boolean selected;
}
