package net.dirtydeeds.discordsoundboard;

import net.dirtydeeds.discordsoundboard.beans.User;
import net.dirtydeeds.discordsoundboard.repository.UserRepository;
import net.dirtydeeds.discordsoundboard.service.SoundPlayerImpl;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceLeaveEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.apache.commons.logging.impl.SimpleLog;
import org.h2.util.StringUtils;

/**
 * author: Dave Furrer
 * <p>
 * This class listens for users to leave a channel and plays a sound if there is one for the user.
 */
public class LeaveSoundBoardListener extends ListenerAdapter {

    private static final SimpleLog LOG = new SimpleLog("LeaveListener");

    private final SoundPlayerImpl bot;
    private final UserRepository userRepository;

    public LeaveSoundBoardListener(SoundPlayerImpl bot, UserRepository userRepository) {
        this.bot = bot;
        this.userRepository = userRepository;
    }

    public void onGuildVoiceLeave(GuildVoiceLeaveEvent event) {
        String userDisconnected = event.getMember().getEffectiveName();
        String userDisconnectedId = event.getMember().getId();
        User user = userRepository.findOneByIdOrUsernameIgnoreCase(userDisconnectedId, userDisconnected);
        if (user != null && !StringUtils.isNullOrEmpty(user.getLeaveSound())) {
            bot.playFileInChannel(user.getLeaveSound(), event.getChannelLeft());
        } else {
            //If DB doesn't have a leave sound check for a file
            String fileToPlay = bot.getFileForUser(userDisconnected, false);
            if (!fileToPlay.equals("")) {
                try {
                    bot.playFileInChannel(fileToPlay, event.getChannelLeft());
                } catch (Exception e) {
                    LOG.fatal("Could not play file for disconnection of " + userDisconnected);
                }
            } else {
                LOG.debug("Could not find disconnection sound for " + userDisconnected + ", so ignoring disconnection event.");
            }
        }
    }
}
