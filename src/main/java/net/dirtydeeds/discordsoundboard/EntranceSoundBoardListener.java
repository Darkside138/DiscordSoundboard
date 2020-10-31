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
 * <p>
 * This class handles waiting for people to enter a discord voice channel and responding to their entrance.
 */
public class EntranceSoundBoardListener extends ListenerAdapter {

    private static final SimpleLog LOG = new SimpleLog("EntranceListener");

    private final SoundPlayerImpl bot;
    private final UserRepository userRepository;

    public EntranceSoundBoardListener(SoundPlayerImpl bot, UserRepository userRepository) {
        this.bot = bot;
        this.userRepository = userRepository;
    }

    public void onGuildVoiceJoin(GuildVoiceJoinEvent event) {
        if (!event.getMember().getUser().isBot()) {
            String userJoined = event.getMember().getEffectiveName();
            String userId = event.getMember().getId();

            User user = userRepository.findOneByIdOrUsernameIgnoreCase(userId, userJoined);
            if (user != null && !StringUtils.isNullOrEmpty(user.getEntranceSound())) {
                bot.playFileInChannel(user.getEntranceSound(), event.getChannelJoined());
            } else {
                //If DB doesn't have an entrance sound check for a file.
                String entranceFile = bot.getFileForUser(userJoined, true);
                if (!entranceFile.equals("")) {
                    try {
                        bot.playFileInChannel(entranceFile, event.getChannelJoined());
                    } catch (Exception e) {
                        LOG.fatal("Could not play file for entrance of " + userJoined);
                    }

                } else {

                    LOG.debug("Could not find any sound that starts with " + userJoined + ", so ignoring entrance.");
                }
            }
        }
    }
}