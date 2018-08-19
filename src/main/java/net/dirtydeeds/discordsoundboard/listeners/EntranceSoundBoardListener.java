package net.dirtydeeds.discordsoundboard.listeners;

import java.util.Map;
import java.util.Set;

import net.dirtydeeds.discordsoundboard.beans.SoundFile;
import net.dirtydeeds.discordsoundboard.service.SoundPlayerImpl;
import net.dv8tion.jda.core.entities.VoiceChannel;
import net.dv8tion.jda.core.events.guild.voice.GenericGuildVoiceEvent;
import net.dv8tion.jda.core.events.guild.voice.GuildVoiceJoinEvent;
import net.dv8tion.jda.core.events.guild.voice.GuildVoiceMoveEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author asafatli.
 *
 * This class handles waiting for people to enter a discord voice channel and responding to their entrance.
 */
public class EntranceSoundBoardListener extends ListenerAdapter {

    private static final Logger log = LoggerFactory.getLogger(EntranceSoundBoardListener.class);

    private SoundPlayerImpl bot;

    public EntranceSoundBoardListener(SoundPlayerImpl bot) {
        this.bot = bot;
    }

    public void onGuildVoiceMove(GuildVoiceMoveEvent event) {
        voiceEntrance(event, event.getChannelJoined());
        super.onGuildVoiceMove(event);
    }

    @SuppressWarnings("rawtypes, unused")
    public void onGuildVoiceJoin(GuildVoiceJoinEvent event) {
        voiceEntrance(event, event.getChannelJoined());
        super.onGuildVoiceJoin(event);
    }

    private void voiceEntrance(GenericGuildVoiceEvent event, VoiceChannel channel) {
        if(!event.getMember().getUser().isBot()) {
            String joined = event.getMember().getUser().getName().toLowerCase();

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
                        bot.playFileForEntrance(fileToPlay, event, channel);
                    } catch (Exception e) {
                        log.error("Could not play file for entrance of {}", joined);
                    }
                } else {
                    log.info("Could not find any sound that starts with {}, so ignoring entrance.", joined);
                }
            }
        }
    }
}