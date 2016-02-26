package net.dirtydeeds.discordsoundboard;

import net.dv8tion.jda.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.hooks.ListenerAdapter;

import java.io.File;
import java.util.Map;
import java.util.Set;

/**
 * @author dfurrer.
 *
 * This class handles listening to commands in discord text channels and responding to them.
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

        //Respond
        if (message.startsWith("?list")) {
            Set<Map.Entry<String, File>> entrySet = soundPlayer.getAvailableSoundFiles().entrySet();
            if (entrySet.size() > 0) {
                sb.append("Type any of the following into the chat to play the sound: \n");
                for (Map.Entry entry : entrySet) {
                    sb.append("?").append(entry.getKey()).append("\n");
                }
                event.getChannel().sendMessage(sb.toString());
            } else {
                sb.append("The soundboard has no available sounds to play.");
            }
        //If the command is not list and starts with ? try and play that "command" or sound file.
        } else if (message.startsWith("?")) {
            try {
                soundPlayer.playFileForEvent(message.substring(1, message.length()), event);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
