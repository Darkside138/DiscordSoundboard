package net.dirtydeeds.discordsoundboard.beans;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@NoArgsConstructor
public class AuthUser {

    // Getters and Setters
    private String id;
    private String username;
    private String discriminator;
    private String avatar;

    @JsonProperty("global_name")
    private String globalName;

    public AuthUser(String id, String username, String discriminator, String avatar, String globalName) {
        this.id = id;
        this.username = username;
        this.discriminator = discriminator;
        this.avatar = avatar;
        this.globalName = globalName;
    }

}
