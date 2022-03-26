package net.dirtydeeds.discordsoundboard;

import net.dirtydeeds.discordsoundboard.beans.User;
import net.dirtydeeds.discordsoundboard.repository.UserRepository;
import net.dirtydeeds.discordsoundboard.service.SoundPlayerImpl;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceMoveEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.apache.commons.logging.impl.SimpleLog;
import org.h2.util.StringUtils;
import org.jetbrains.annotations.NotNull;

public class MovedChannelListener extends ListenerAdapter {

    private static final SimpleLog LOG = new SimpleLog("MovedChannelListener");

    private final SoundPlayerImpl bot;
    private final UserRepository userRepository;
    private final boolean playEntranceOnMove;

    public MovedChannelListener(SoundPlayerImpl bot, UserRepository userRepository,
                                boolean playEntranceOnMove) {
        this.bot = bot;
        this.userRepository = userRepository;
        this.playEntranceOnMove = playEntranceOnMove;
    }

    public void onGuildVoiceMove(@NotNull GuildVoiceMoveEvent event) {
        if (playEntranceOnMove && !event.getMember().getUser().isBot()) {
            String discordUser = event.getMember().getEffectiveName();
            String discordUserId = event.getMember().getId();
            String entranceFile = bot.getFileForUser(discordUser, true);
            String disconnectFile = bot.getFileForUser(discordUser, false);

            User user = userRepository.findOneByIdOrUsernameIgnoreCase(discordUserId, discordUser);
            if (user != null) {
                if (!StringUtils.isNullOrEmpty(user.getEntranceSound())) {
                    entranceFile = user.getEntranceSound();
                }
                if (!StringUtils.isNullOrEmpty(user.getLeaveSound())) {
                    disconnectFile = user.getLeaveSound();
                }
            }

            if (!entranceFile.equals("")) {
                try {
                    bot.playFileInChannel(entranceFile, event.getChannelJoined());
                } catch (Exception e) {
                    LOG.fatal("Could not play file for entrance of " + user);
                }
            } else if (!disconnectFile.equals("")) {
                try {
                    bot.playFileInChannel(disconnectFile, event.getChannelLeft());
                } catch (Exception e) {
                    LOG.fatal("Could not play file for disconnection of " + user);
                }
            } else {
                LOG.debug("Could not entrance or disconnect sound for " + user + ", so ignoring GuildVoiceMoveEvent.");
            }
        }
    }
}
