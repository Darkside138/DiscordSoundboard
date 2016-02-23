package net.dirtydeeds.discordsoundboard;

import net.dv8tion.jda.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.hooks.ListenerAdapter;

import java.util.Map;

/**
 * @author dfurrer.
 */
public class ChatSoundBoardListener extends ListenerAdapter {
    
    private SoundPlayer soundPlayer;

    public ChatSoundBoardListener(SoundPlayer soundPlayer) {
        this.soundPlayer = soundPlayer;
    }

    public void onGuildMessageReceived(GuildMessageReceivedEvent event)
    {        
        String message = event.getMessage().getContent();
        
        StringBuilder sb = new StringBuilder();
        if (message.startsWith("?list")) {
            for (Map.Entry entry : soundPlayer.getAvailableSoundFiles().entrySet()) {
                sb.append("?").append(entry.getKey()).append("\n");
            }
            event.getChannel().sendMessage(sb.toString());
        } else if (message.startsWith("?")) {
            try {
                soundPlayer.playFileForEvent(message.substring(1, message.length()) + ".mp3", event);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
