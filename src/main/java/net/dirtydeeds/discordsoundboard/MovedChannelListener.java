package net.dirtydeeds.discordsoundboard;

import net.dirtydeeds.discordsoundboard.service.SoundPlayerImpl;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceMoveEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.apache.commons.logging.impl.SimpleLog;

public class MovedChannelListener extends ListenerAdapter {

    private static final SimpleLog LOG = new SimpleLog("MovedChannelListener");

    private SoundPlayerImpl bot;

    public MovedChannelListener(SoundPlayerImpl bot) {
        this.bot = bot;
    }

    @SuppressWarnings({"rawtypes"})
    public void onGuildVoiceMove(GuildVoiceMoveEvent event) {
        if (!event.getMember().getUser().isBot()) {
            String user = event.getMember().getEffectiveName();
            String entranceFile = bot.getFileForUser(user, true);
            String disconnectFile = bot.getFileForUser(user, false);

            if (!entranceFile.equals("")) {
                try {
                    bot.playFileInChannel(entranceFile, event.getChannelJoined());
                } catch (Exception e) {
                    LOG.fatal("Could not play file for entrance of " + user);
                }
            } else if (!disconnectFile.equals("")) {
                try {
                    bot.playFileInChannel(disconnectFile, event.getChannelLeft());
                } catch (Exception e) {
                    LOG.fatal("Could not play file for disconnection of " + user);
                }
            } else {
                LOG.debug("Could not entrance or disconnect sound for " + user + ", so ignoring GuildVoiceMoveEvent.");
            }
        }
    }
}
