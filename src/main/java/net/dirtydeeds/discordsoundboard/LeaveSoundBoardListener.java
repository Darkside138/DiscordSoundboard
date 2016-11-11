package net.dirtydeeds.discordsoundboard;

import net.dirtydeeds.discordsoundboard.beans.SoundFile;
import net.dirtydeeds.discordsoundboard.service.SoundPlayerImpl;
import net.dv8tion.jda.events.voice.VoiceLeaveEvent;
import net.dv8tion.jda.hooks.ListenerAdapter;
import net.dv8tion.jda.utils.SimpleLog;

import java.util.Map;
import java.util.Set;

/**
 * author: Dave Furrer
 *
 * This class listens for users to leave a channel and plays a sound if there is one for the user.
 */
public class LeaveSoundBoardListener extends ListenerAdapter {

    private static final SimpleLog LOG = SimpleLog.getLog("LeaveListener");

    private SoundPlayerImpl bot;
    private String suffix = "_leave";

    public LeaveSoundBoardListener(SoundPlayerImpl bot, String suffix) {
        this.bot = bot;
        if (suffix != null && !suffix.isEmpty()) {
            this.suffix = suffix;
        }
    }

    @SuppressWarnings("rawtypes")
    public void onVoiceLeave(VoiceLeaveEvent event) {
        if(!event.getUser().isBot()) {
            String userDisconnected = event.getUser().getUsername().toLowerCase();

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
                        LOG.fatal("Could not play file for disconnection of " + userDisconnected);
                    }
                } else {
                    LOG.info("Could not disconnection sound for " + userDisconnected + ", so ignoring disconnection event.");
                }
            }
        }
    }
}
