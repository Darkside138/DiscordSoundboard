package net.dirtydeeds.discordsoundboard;

import net.dirtydeeds.discordsoundboard.beans.User;
import net.dirtydeeds.discordsoundboard.repository.UserRepository;
import net.dirtydeeds.discordsoundboard.service.SoundPlayerImpl;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceJoinEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.apache.commons.logging.impl.SimpleLog;
import org.h2.util.StringUtils;

/**
 * @author asafatli.
 *
 * This class handles waiting for people to enter a discord voice channel and responding to their entrance.
 */
public class EntranceSoundBoardListener extends ListenerAdapter {

    private static final SimpleLog LOG = new SimpleLog("EntranceListener");

    private SoundPlayerImpl bot;
    private UserRepository userRepository;

    public EntranceSoundBoardListener(SoundPlayerImpl bot, UserRepository userRepository) {
        this.bot = bot;
        this.userRepository = userRepository;
    }

    @SuppressWarnings({"rawtypes"})
    public void onGuildVoiceJoin(GuildVoiceJoinEvent event) {
        if(!event.getMember().getUser().isBot()) {
            String userJoined = event.getMember().getEffectiveName();
            String userId = event.getMember().getId();
            String entranceFile = bot.getFileForUser(userJoined, true);
            if (!entranceFile.equals("")) {
                try {
                    bot.playFileInChannel(entranceFile, event.getChannelJoined());
                } catch (Exception e) {
                    LOG.fatal("Could not play file for entrance of " + userJoined);
                }
            } else {
                User user = userRepository.findOneByIdOrUsernameIgnoreCase(userId, userJoined);
                if (user != null) {
                    if (!StringUtils.isNullOrEmpty(user.getEntranceSound())) {
                        bot.playFileInChannel(user.getEntranceSound(), event.getChannelJoined());
                    }
                }
                LOG.debug("Could not find any sound that starts with " + userJoined + ", so ignoring entrance.");
            }
        }
    }
}