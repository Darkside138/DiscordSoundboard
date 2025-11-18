package net.dirtydeeds.discordsoundboard.listeners;

import net.dirtydeeds.discordsoundboard.BotConfig;
import net.dirtydeeds.discordsoundboard.beans.SoundFile;
import net.dirtydeeds.discordsoundboard.beans.Users;
import net.dirtydeeds.discordsoundboard.SoundPlayer;
import net.dirtydeeds.discordsoundboard.service.SoundService;
import net.dirtydeeds.discordsoundboard.service.UserService;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceUpdateEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

/**
 * @author Dave Furrer
 * <p>
 * Class to handle user moving from one voice channel to another
 */
public class MovedChannelListener extends ListenerAdapter {

    private static final Logger LOG = LoggerFactory.getLogger(MovedChannelListener.class);

    private final SoundPlayer bot;
    private final UserService userService;
    private final boolean playEntranceOnMove;
    private final BotConfig botConfig;
    private final SoundService soundService;

    public MovedChannelListener(SoundPlayer bot, UserService userService, SoundService soundService,
                                boolean playEntranceOnMove, BotConfig botConfig) {
        this.bot = bot;
        this.userService = userService;
        this.soundService = soundService;
        this.playEntranceOnMove = playEntranceOnMove;
        this.botConfig = botConfig;
    }

    @Override
    public void onGuildVoiceUpdate(@NotNull GuildVoiceUpdateEvent event) {
        if (event.getChannelLeft() != null && event.getChannelJoined() != null) {
            if (playEntranceOnMove && !event.getMember().getUser().isBot()) {
                String discordUser = event.getMember().getEffectiveName();
                String discordUserId = event.getMember().getId();
                String entranceFile = null;
                String disconnectFile = null;

                Users users = userService.findOneByIdOrUsernameIgnoreCase(discordUserId, discordUser);
                if (users != null) {
                    if (StringUtils.hasText(users.getEntranceSound())) {
                        entranceFile = users.getEntranceSound();

                        LOG.info("Playing move sound {}", entranceFile);
                    } else {
                        SoundFile entranceSoundFile = soundService.findOneBySoundFileIdIgnoreCase(users.getUsername());
                        if (entranceSoundFile != null) {
                            entranceFile = entranceSoundFile.getSoundFileId();
                        }
                    }
                    if (StringUtils.hasText(users.getLeaveSound())) {
                        disconnectFile = users.getLeaveSound();

                        LOG.info("Playing leave sound {}", disconnectFile);
                    } else {
                        SoundFile disconnectSoundFile = soundService.findOneBySoundFileIdIgnoreCase(
                                users.getUsername() + botConfig.getLeaveSuffix());
                        if (disconnectSoundFile != null) {
                            disconnectFile = disconnectSoundFile.getSoundFileId();
                        }
                    }
                }
                if (StringUtils.hasText(botConfig.getEntranceForAll())) {
                    entranceFile = botConfig.getEntranceForAll();
                    LOG.info("Playing entrance for all sound {}", entranceFile);
                }

                if (StringUtils.hasText(entranceFile)) {
                    try {
                        bot.playFileInChannel(entranceFile, event.getChannelJoined());
                    } catch (Exception e) {
                        LOG.error("Could not play file for entrance of {}", users);
                    }
                } else if (StringUtils.hasText(disconnectFile)) {
                    try {
                        bot.playFileInChannel(disconnectFile, event.getChannelLeft());
                    } catch (Exception e) {
                        LOG.error("Could not play file for disconnection of {}", users);
                    }
                } else {
                    LOG.debug("Could not find entrance or disconnect sound for {}, so ignoring GuildVoiceMoveEvent.", users);
                }
            }
        }
    }
}
