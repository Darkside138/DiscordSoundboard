package net.dirtydeeds.discordsoundboard.listeners;

import net.dirtydeeds.discordsoundboard.BotConfig;
import net.dirtydeeds.discordsoundboard.beans.User;
import net.dirtydeeds.discordsoundboard.SoundPlayer;
import net.dirtydeeds.discordsoundboard.service.UserService;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceMoveEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.h2.util.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MovedChannelListener extends ListenerAdapter {

    private static final Logger LOG = LoggerFactory.getLogger(MovedChannelListener.class);

    private final SoundPlayer bot;
    private final UserService userService;
    private final boolean playEntranceOnMove;
    private final BotConfig botConfig;

    public MovedChannelListener(SoundPlayer bot, UserService userService,
                                boolean playEntranceOnMove, BotConfig botConfig) {
        this.bot = bot;
        this.userService = userService;
        this.playEntranceOnMove = playEntranceOnMove;
        this.botConfig = botConfig;
    }

    public void onGuildVoiceMove(@NotNull GuildVoiceMoveEvent event) {
        if (playEntranceOnMove && !event.getMember().getUser().isBot()) {
            String discordUser = event.getMember().getEffectiveName();
            String discordUserId = event.getMember().getId();
            String entranceFile = bot.getFileForUser(discordUser, true);
            String disconnectFile = bot.getFileForUser(discordUser, false);

            User user = userService.findOneByIdOrUsernameIgnoreCase(discordUserId, discordUser);
            if (user != null) {
                if (!StringUtils.isNullOrEmpty(user.getEntranceSound())) {
                    entranceFile = user.getEntranceSound();
                    LOG.info("Playing move sound {}", entranceFile);
                }
                if (!StringUtils.isNullOrEmpty(user.getLeaveSound())) {
                    disconnectFile = user.getLeaveSound();
                    LOG.info("Playing leave sound {}", disconnectFile);
                }
            }
            if (!StringUtils.isNullOrEmpty(botConfig.getEntranceForAll())) {
                entranceFile = botConfig.getEntranceForAll();
                LOG.info("Playing entrance for all sound {}", entranceFile);
            }

            if (!entranceFile.equals("")) {
                try {
                    bot.playFileInChannel(entranceFile, event.getChannelJoined());
                } catch (Exception e) {
                    LOG.error("Could not play file for entrance of {}", user);
                }
            } else if (!disconnectFile.equals("")) {
                try {
                    bot.playFileInChannel(disconnectFile, event.getChannelLeft());
                } catch (Exception e) {
                    LOG.error("Could not play file for disconnection of {}", user);
                }
            } else {
                LOG.debug("Could not find entrance or disconnect sound for {}, so ignoring GuildVoiceMoveEvent.", user);
            }
        }
    }
}
