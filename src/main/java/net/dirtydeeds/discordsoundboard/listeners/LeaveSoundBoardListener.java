package net.dirtydeeds.discordsoundboard.listeners;

import net.dirtydeeds.discordsoundboard.beans.User;
import net.dirtydeeds.discordsoundboard.SoundPlayer;
import net.dirtydeeds.discordsoundboard.service.UserService;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceLeaveEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.h2.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * author: Dave Furrer
 * <p>
 * This class listens for users to leave a channel and plays a sound if there is one for the user.
 */
public class LeaveSoundBoardListener extends ListenerAdapter {

    private static final Logger LOG = LoggerFactory.getLogger(LeaveSoundBoardListener.class);

    private final SoundPlayer bot;
    private final UserService userService;

    public LeaveSoundBoardListener(SoundPlayer bot, UserService userService) {
        this.bot = bot;
        this.userService = userService;
    }

    public void onGuildVoiceLeave(GuildVoiceLeaveEvent event) {
        String userDisconnected = event.getMember().getEffectiveName();
        String userDisconnectedId = event.getMember().getId();
        User user = userService.findOneByIdOrUsernameIgnoreCase(userDisconnectedId, userDisconnected);
        if (user != null && !StringUtils.isNullOrEmpty(user.getLeaveSound())) {
            bot.playFileInChannel(user.getLeaveSound(), event.getChannelLeft());
        } else {
            //If DB doesn't have a leave sound check for a file
            String fileToPlay = bot.getFileForUser(userDisconnected, false);
            if (!fileToPlay.equals("")) {
                try {
                    bot.playFileInChannel(fileToPlay, event.getChannelLeft());
                } catch (Exception e) {
                    LOG.error("Could not play file for disconnection of {}", userDisconnected);
                }
            } else {
                LOG.debug("Could not find disconnection sound for {}, so ignoring disconnection event.", userDisconnected);
            }
        }
    }
}
