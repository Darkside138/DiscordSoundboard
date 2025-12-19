package net.dirtydeeds.discordsoundboard.listeners;

import net.dirtydeeds.discordsoundboard.BotConfig;
import net.dirtydeeds.discordsoundboard.beans.DiscordUser;
import net.dirtydeeds.discordsoundboard.beans.SoundFile;
import net.dirtydeeds.discordsoundboard.SoundPlayer;
import net.dirtydeeds.discordsoundboard.controllers.DiscordUserController;
import net.dirtydeeds.discordsoundboard.service.DiscordUserService;
import net.dirtydeeds.discordsoundboard.service.SoundService;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceUpdateEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * author: Dave Furrer
 * <p>
 * This class listens for users to leave a channel and plays a sound if there is one for the user.
 */
public class LeaveSoundBoardListener extends ListenerAdapter {

    private static final Logger LOG = LoggerFactory.getLogger(LeaveSoundBoardListener.class);

    private final SoundPlayer bot;
    private final DiscordUserService discordUserService;
    private final SoundService soundService;
    private final BotConfig botConfig;
    private final DiscordUserController discordUserController;
    private final SoundPlayer soundPlayer;

    public LeaveSoundBoardListener(SoundPlayer bot, DiscordUserService discordUserService, SoundService soundService,
                                   BotConfig botConfig, DiscordUserController discordUserController, SoundPlayer soundPlayer) {
        this.bot = bot;
        this.discordUserService = discordUserService;
        this.soundService = soundService;
        this.botConfig = botConfig;
        this.discordUserController = discordUserController;
        this.soundPlayer = soundPlayer;
    }

    @Override
    public void onGuildVoiceUpdate(GuildVoiceUpdateEvent event) {
        if (event.getChannelJoined() == null && event.getChannelLeft() != null) {
            soundPlayer.updateUsersInDb();
            discordUserController.broadcastUpdate();
            if (isAlone(event.getGuild()) && botConfig.isLeaveOnEmptyChannel()) {
                soundPlayer.disconnectFromChannel(event.getGuild());
            } else {
                String userNameDisconnected = event.getMember().getEffectiveName();
                String userIdDisconnected = event.getMember().getId();
                DiscordUser discordUser = discordUserService.findOneByIdOrUsernameIgnoreCase(userIdDisconnected, userNameDisconnected);
                if (discordUser != null) {
                    if (StringUtils.hasText(discordUser.getLeaveSound())) {
                        bot.playFileInChannel(discordUser.getLeaveSound(), event.getChannelLeft(), discordUser);
                    } else {
                        //If DB doesn't have a leave sound check for a file named with the userName + leave suffix
                        SoundFile leaveFile = soundService.findOneBySoundFileIdIgnoreCase(
                                discordUser.getUsername() + botConfig.getLeaveSuffix());
                        if (leaveFile != null) {
                            try {
                                bot.playFileInChannel(leaveFile.getSoundFileId(), event.getChannelLeft(), discordUser);
                            } catch (Exception e) {
                                LOG.error("Could not play file for disconnection of {}", userNameDisconnected);
                            }
                        } else {
                            LOG.debug("Could not find disconnection sound for {}, so ignoring disconnection event.", userNameDisconnected);
                        }
                    }
                }
            }
        }
    }

    private boolean isAlone(Guild guild) {
        if (guild.getAudioManager().getConnectedChannel() == null) return true;
        AtomicBoolean isAlone = new AtomicBoolean(true);
        guild.getAudioManager().getConnectedChannel().getMembers().forEach( member -> {
            if (!member.getUser().isBot() && !member.getUser().isSystem()) {
                isAlone.set(false);
            }
        });
        return isAlone.get();
    }
}
