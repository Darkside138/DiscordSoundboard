package net.dirtydeeds.discordsoundboard.listeners;

import net.dirtydeeds.discordsoundboard.BotConfig;
import net.dirtydeeds.discordsoundboard.beans.SoundFile;
import net.dirtydeeds.discordsoundboard.beans.User;
import net.dirtydeeds.discordsoundboard.SoundPlayer;
import net.dirtydeeds.discordsoundboard.service.SoundService;
import net.dirtydeeds.discordsoundboard.service.UserService;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceJoinEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.h2.util.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Dave Furrer
 * <p>
 * This class handles waiting for people to enter a discord voice channel and responding to their entrance.
 */
public class EntranceSoundBoardListener extends ListenerAdapter {

    private static final Logger LOG = LoggerFactory.getLogger(EntranceSoundBoardListener.class);

    private final SoundPlayer bot;
    private final UserService userService;
    private final boolean playEntranceOnJoin;
    private final BotConfig botConfig;
    private final SoundService soundService;

    public EntranceSoundBoardListener(SoundPlayer bot, UserService userService,
                                      SoundService soundService,
                                      boolean playEntranceOnJoin,
                                      BotConfig botConfig) {
        this.soundService = soundService;
        this.bot = bot;
        this.userService = userService;
        this.playEntranceOnJoin = playEntranceOnJoin;
        this.botConfig = botConfig;
    }

    @Override
    public void onGuildVoiceJoin(@NotNull GuildVoiceJoinEvent event) {
        if (playEntranceOnJoin && !event.getMember().getUser().isBot()) {
            String userJoined = event.getMember().getEffectiveName();
            String userId = event.getMember().getId();

            User user = userService.findOneByIdOrUsernameIgnoreCase(userId, userJoined);
            if (user != null) {
                if (!StringUtils.isNullOrEmpty(user.getEntranceSound())) {
                    String entranceSound = user.getEntranceSound();
                    LOG.info(String.format("Playing entrance sound %s", entranceSound));
                    bot.playFileInChannel(entranceSound, event.getChannelJoined());
                } else if (!StringUtils.isNullOrEmpty(botConfig.getEntranceForAll())) {
                    LOG.info(String.format("Playing entrance for all sound %s", botConfig.getEntranceForAll()));
                    bot.playFileInChannel(botConfig.getEntranceForAll(), event.getChannelJoined());
                } else {
                    //If DB doesn't have an entrance sound check for a file with the same name as the user
                    SoundFile entranceFile = soundService.findOneBySoundFileIdIgnoreCase(user.getUsername());
                    if (entranceFile != null) {
                        try {
                            bot.playFileInChannel(entranceFile.getSoundFileId(), event.getChannelJoined());
                            LOG.info(String.format("Playing entrance sound %s", entranceFile.getSoundFileId()));
                        } catch (Exception e) {
                            LOG.error("Could not play file for entrance of {}", userJoined);
                        }
                    } else {
                        LOG.debug("Could not find any sound that starts with {}, so ignoring entrance.", userJoined);
                    }
                }
            }
        }
    }
}