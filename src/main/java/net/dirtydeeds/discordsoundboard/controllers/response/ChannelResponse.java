package net.dirtydeeds.discordsoundboard.controllers.response;

import lombok.Getter;

@Getter
public class ChannelResponse {
    String name;
    String id;
    String guild;
    String guildId;
    boolean defaultChannel;

    public ChannelResponse(String name, String id, String guild, String guildId, boolean defaultChannel) {
        this.name = name;
        this.id = id;
        this.guild = guild;
        this.guildId = guildId;
        this.defaultChannel = defaultChannel;
    }
}
