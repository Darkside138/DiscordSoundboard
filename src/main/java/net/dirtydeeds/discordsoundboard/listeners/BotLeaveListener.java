package net.dirtydeeds.discordsoundboard.listeners;

import net.dirtydeeds.discordsoundboard.BotConfig;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceUpdateEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author Dave Furrer
 * <p>
 * Class to have the bot leave the channel when everyone else has disconnected
 */
public class BotLeaveListener extends ListenerAdapter {

    private final BotConfig botConfig;

    public BotLeaveListener(BotConfig botConfig) {
        this.botConfig = botConfig;
    }

    @Override
    public void onGuildVoiceUpdate(GuildVoiceUpdateEvent event) {
        if (isAlone(event.getGuild()) && botConfig.isLeaveOnEmptyChannel()) {
            event.getGuild().getAudioManager().closeAudioConnection();
        }
    }

    private boolean isAlone(Guild guild) {
        if (guild.getAudioManager().getConnectedChannel() == null) return false;
        AtomicBoolean isAlone = new AtomicBoolean(true);
        guild.getAudioManager().getConnectedChannel().getMembers().forEach( member -> {
            if (!member.getUser().isBot() && !member.getUser().isSystem()) {
                isAlone.set(false);
            }
        });
        return isAlone.get();
    }
}
