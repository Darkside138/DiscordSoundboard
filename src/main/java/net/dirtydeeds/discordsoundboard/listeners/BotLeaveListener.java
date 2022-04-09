package net.dirtydeeds.discordsoundboard.listeners;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceUpdateEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.util.Objects;

/**
 * @author Dave Furrer
 * <p>
 * Class to have the bot leave the channel when everyone else has disconnected
 */
public class BotLeaveListener extends ListenerAdapter {

    public BotLeaveListener() {
    }

    @Override
    public void onGuildVoiceUpdate(GuildVoiceUpdateEvent event) {
        if (isAlone(event.getGuild())) {
            event.getGuild().getAudioManager().closeAudioConnection();
        }
    }

    private boolean isAlone(Guild guild) {
        if(guild.getAudioManager().getConnectedChannel() == null) return false;
        return guild.getAudioManager().getConnectedChannel().getMembers().stream()
                .noneMatch(x ->
                        !Objects.requireNonNull(x.getVoiceState()).isDeafened()
                                && !x.getUser().isBot());
    }
}
