package net.dirtydeeds.discordsoundboard;

import net.dirtydeeds.discordsoundboard.service.SoundPlayerImpl;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceLeaveEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.apache.commons.logging.impl.SimpleLog;

/**
 * author: Dave Furrer
 *
 * This class listens for users to leave a channel and plays a sound if there is one for the user.
 */
public class LeaveSoundBoardListener extends ListenerAdapter {

    private static final SimpleLog LOG = new SimpleLog("LeaveListener");

    private SoundPlayerImpl bot;

    public LeaveSoundBoardListener(SoundPlayerImpl bot) {
        this.bot = bot;
    }

    @SuppressWarnings({"rawtypes"})
    public void onGuildVoiceLeave(GuildVoiceLeaveEvent event) {
        String userDisconnected = event.getMember().getEffectiveName();
        String fileToPlay = bot.getFileForUser(userDisconnected, false);
        if (!fileToPlay.equals("")) {
            try {
                bot.playFileInChannel(fileToPlay, event.getChannelLeft());
            } catch (Exception e) {
                LOG.fatal("Could not play file for disconnection of " + userDisconnected);
            }
        } else {
            LOG.debug("Could not disconnection sound for " + userDisconnected + ", so ignoring disconnection event.");
        }
    }
}
