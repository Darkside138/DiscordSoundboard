package net.dirtydeeds.discordsoundboard.listeners;

import net.dirtydeeds.discordsoundboard.BotConfig;
import net.dirtydeeds.discordsoundboard.beans.SoundFile;
import net.dirtydeeds.discordsoundboard.beans.DiscordUser;
import net.dirtydeeds.discordsoundboard.SoundPlayer;
import net.dirtydeeds.discordsoundboard.controllers.DiscordUserController;
import net.dirtydeeds.discordsoundboard.service.DiscordUserService;
import net.dirtydeeds.discordsoundboard.service.SoundService;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceUpdateEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

/**
 * @author Dave Furrer
 * <p>
 * This class handles waiting for people to enter a discord voice channel and responding to their entrance.
 */
public class EntranceSoundBoardListener extends ListenerAdapter {

    private static final Logger LOG = LoggerFactory.getLogger(EntranceSoundBoardListener.class);

    private final SoundPlayer soundPlayer;
    private final DiscordUserService discordUserService;
    private final boolean playEntranceOnJoin;
    private final BotConfig botConfig;
    private final SoundService soundService;
    private final DiscordUserController discordUserController;

    public EntranceSoundBoardListener(SoundPlayer soundPlayer, DiscordUserService discordUserService,
                                        SoundService soundService,
                                        boolean playEntranceOnJoin,
                                        BotConfig botConfig,
                                        DiscordUserController discordUserController) {
        this.soundService = soundService;
        this.soundPlayer = soundPlayer;
        this.discordUserService = discordUserService;
        this.playEntranceOnJoin = playEntranceOnJoin;
        this.botConfig = botConfig;
        this.discordUserController = discordUserController;
    }

    @Override
    public void onGuildVoiceUpdate(@NotNull GuildVoiceUpdateEvent event) {
        if (event.getChannelLeft() == null && event.getChannelJoined() != null) {
            soundPlayer.updateUsersInDb();
            discordUserController.broadcastUpdate();
            if (playEntranceOnJoin && !event.getMember().getUser().isBot()) {
                String userJoined = event.getMember().getEffectiveName();
                String userId = event.getMember().getId();

                DiscordUser discordUser = discordUserService.findOneByIdOrUsernameIgnoreCase(userId, userJoined);
                if (discordUser != null) {
                    if (StringUtils.hasText(discordUser.getEntranceSound())) {
                        String entranceSound = discordUser.getEntranceSound();
                        LOG.info("Playing entrance sound {}", entranceSound);
                        soundPlayer.playFileInChannel(entranceSound, event.getChannelJoined(), discordUser);
                    } else if (StringUtils.hasText(botConfig.getEntranceForAll())) {
                        LOG.info("Playing entrance for all sound {}", botConfig.getEntranceForAll());
                        soundPlayer.playFileInChannel(botConfig.getEntranceForAll(), event.getChannelJoined(), discordUser);
                    } else {
                        //If DB doesn't have an entrance sound check for a file with the same name as the user
                        SoundFile entranceFile = soundService.findOneBySoundFileIdIgnoreCase(discordUser.getUsername());
                        if (entranceFile != null) {
                            try {
                                soundPlayer.playFileInChannel(entranceFile.getSoundFileId(), event.getChannelJoined(), discordUser);
                                LOG.info("Playing entrance sound {}", entranceFile.getSoundFileId());
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
}