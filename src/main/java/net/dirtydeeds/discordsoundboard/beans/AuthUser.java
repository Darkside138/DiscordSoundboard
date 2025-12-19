package net.dirtydeeds.discordsoundboard.beans;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@SuppressWarnings("unused")
public class AuthUser {

    // Getters and Setters
    private String id;
    private String username;
    private String discriminator;
    private String avatar;

    @JsonProperty("global_name")
    private String globalName;

}
