package net.dirtydeeds.discordsoundboard.listeners;

import net.dirtydeeds.discordsoundboard.beans.SoundFile;
import net.dirtydeeds.discordsoundboard.service.SoundPlayerImpl;
import net.dv8tion.jda.core.events.guild.voice.GuildVoiceLeaveEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Set;

/**
 * author: Dave Furrer
 *
 * This class listens for users to leave a channel and plays a sound if there is one for the user.
 */
public class LeaveSoundBoardListener extends ListenerAdapter {

    private static final Logger log = LoggerFactory.getLogger(LeaveSoundBoardListener.class);

    private SoundPlayerImpl bot;
    private String suffix = "_leave";

    public LeaveSoundBoardListener(SoundPlayerImpl bot, String suffix) {
        this.bot = bot;
        if (suffix != null && !suffix.isEmpty()) {
            this.suffix = suffix;
        }
    }

    @SuppressWarnings("rawtypes, unused")
    public void onGuildVoiceLeave(GuildVoiceLeaveEvent event) {
        if(!event.getMember().getUser().isBot()) {
            String userDisconnected = event.getMember().getUser().getName().toLowerCase();

            //Respond
            Set<Map.Entry<String, SoundFile>> entrySet = bot.getAvailableSoundFiles().entrySet();
            if (entrySet.size() > 0) {
                String fileToPlay = "";
                for (Map.Entry entry : entrySet) {
                    String fileEntry = (String) entry.getKey();
                    if (fileEntry.toLowerCase().startsWith(userDisconnected.toLowerCase()) &&
                            fileEntry.toLowerCase().endsWith(suffix.toLowerCase())
                            && fileEntry.length() > fileToPlay.length())
                        fileToPlay = fileEntry;
                }
                if (!fileToPlay.equals("")) {
                    try {
                        bot.playFileForDisconnect(fileToPlay, event);
                    } catch (Exception e) {
                        log.error("Could not play file for disconnection of {}", userDisconnected);
                    }
                } else {
                    log.info("Could not disconnection sound for {}, so ignoring disconnection event.", userDisconnected);
                }
            }
        }
        super.onGuildVoiceLeave(event);
    }
}
