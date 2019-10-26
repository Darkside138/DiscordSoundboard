package net.dirtydeeds.discordsoundboard;

import net.dirtydeeds.discordsoundboard.service.SoundPlayerImpl;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceMoveEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.apache.commons.logging.impl.SimpleLog;

/**
 * @author asafatli.
 *
 * This class handles waiting for people to enter a discord voice channel and responding to their entrance.
 */
public class EntranceSoundBoardListener extends ListenerAdapter {

    private static final SimpleLog LOG = new SimpleLog("EntranceListener");

    private SoundPlayerImpl bot;

    public EntranceSoundBoardListener(SoundPlayerImpl bot) {
        this.bot = bot;
    }

    @SuppressWarnings({"rawtypes"})
    public void onGuildVoiceMove(GuildVoiceMoveEvent event) {
        if(!event.getMember().getUser().isBot()) {
            String userJoined = event.getMember().getEffectiveName();
            String entranceFile = bot.getFileForUser(userJoined, true);
            if (!entranceFile.equals("")) {
                try {
                    bot.playFileInChannel(entranceFile, event.getChannelJoined());
                } catch (Exception e) {
                    LOG.fatal("Could not play file for entrance of " + userJoined);
                }
            } else {
                LOG.debug("Could not find any sound that starts with " + userJoined + ", so ignoring entrance.");
            }
        }
    }
}