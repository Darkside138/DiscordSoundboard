package net.dirtydeeds.discordsoundboard;

import java.util.Map;
import java.util.Set;

import net.dirtydeeds.discordsoundboard.beans.SoundFile;
import net.dirtydeeds.discordsoundboard.service.SoundPlayerImpl;
import net.dv8tion.jda.events.voice.VoiceJoinEvent;
import net.dv8tion.jda.hooks.ListenerAdapter;
import net.dv8tion.jda.utils.SimpleLog;

/**
 * @author asafatli.
 *
 * This class handles waiting for people to enter a discord voice channel and responding to their entrance.
 */
public class EntranceSoundBoardListener extends ListenerAdapter {

    private static final SimpleLog LOG = SimpleLog.getLog("EntranceListener");

    private SoundPlayerImpl bot;

    public EntranceSoundBoardListener(SoundPlayerImpl bot) {
        this.bot = bot;
    }

    @SuppressWarnings("rawtypes")
    public void onVoiceJoin(VoiceJoinEvent event) {
        if(!event.getUser().isBot()) {
            String joined = event.getUser().getUsername().toLowerCase();

            //Respond
            Set<Map.Entry<String, SoundFile>> entrySet = bot.getAvailableSoundFiles().entrySet();
            if (entrySet.size() > 0) {
                String fileToPlay = "";
                for (Map.Entry entry : entrySet) {
                    String fileEntry = (String) entry.getKey();
                    if (joined.toLowerCase().startsWith(fileEntry.toLowerCase())
                            && fileEntry.length() > fileToPlay.length())
                        fileToPlay = fileEntry;
                }
                if (!fileToPlay.equals("")) {
                    try {
                        bot.playFileForEntrance(fileToPlay, event);
                    } catch (Exception e) {
                        LOG.fatal("Could not play file for entrance of " + joined);
                    }
                } else {
                    LOG.info("Could not find any sound that starts with " + joined + ", so ignoring entrance.");
                }
            }
        }
    }
}