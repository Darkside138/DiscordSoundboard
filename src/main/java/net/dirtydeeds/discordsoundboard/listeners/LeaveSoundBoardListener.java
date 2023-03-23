package net.dirtydeeds.discordsoundboard.listeners;

import net.dirtydeeds.discordsoundboard.BotConfig;
import net.dirtydeeds.discordsoundboard.beans.SoundFile;
import net.dirtydeeds.discordsoundboard.beans.User;
import net.dirtydeeds.discordsoundboard.SoundPlayer;
import net.dirtydeeds.discordsoundboard.service.SoundService;
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
    private final SoundService soundService;
    private final BotConfig botConfig;

    public LeaveSoundBoardListener(SoundPlayer bot, UserService userService, SoundService soundService,
                                   BotConfig botConfig) {
        this.bot = bot;
        this.userService = userService;
        this.soundService = soundService;
        this.botConfig = botConfig;
    }

    @Override
    public void onGuildVoiceLeave(GuildVoiceLeaveEvent event) {
        String userDisconnected = event.getMember().getEffectiveName();
        String userDisconnectedId = event.getMember().getId();
        User user = userService.findOneByIdOrUsernameIgnoreCase(userDisconnectedId, userDisconnected);
        if (user != null) {
            if (!StringUtils.isNullOrEmpty(user.getLeaveSound())) {
                bot.playFileInChannel(user.getLeaveSound(), event.getChannelLeft());
            } else {
                //If DB doesn't have a leave sound check for a file named with the userName + leave suffix
                SoundFile leaveFile = soundService.findOneBySoundFileIdIgnoreCase(
                        user.getUsername() + botConfig.getLeaveSuffix());
                if (leaveFile != null) {
                    try {
                        bot.playFileInChannel(leaveFile.getSoundFileId(), event.getChannelLeft());
                    } catch (Exception e) {
                        LOG.error("Could not play file for disconnection of {}", userDisconnected);
                    }
                } else {
                    LOG.debug("Could not find disconnection sound for {}, so ignoring disconnection event.", userDisconnected);
                }
            }
        }
    }
}
