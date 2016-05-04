package net.dirtydeeds.discordsoundboard;

import net.dirtydeeds.discordsoundboard.beans.SoundFile;
import net.dirtydeeds.discordsoundboard.service.SoundPlayerImpl;
import net.dv8tion.jda.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.hooks.ListenerAdapter;
import net.dv8tion.jda.utils.SimpleLog;

import java.util.Map;
import java.util.Set;

/**
 * @author dfurrer.
 *
 * This class handles listening to commands in discord text channels and responding to them.
 */
public class ChatSoundBoardListener extends ListenerAdapter {
    
    private static final SimpleLog LOG = SimpleLog.getLog("ChatListener");
    
    private SoundPlayerImpl soundPlayer;
    private String commandCharacter = "?";
    private Integer messageSizeLimit = 2000;

    public ChatSoundBoardListener(SoundPlayerImpl soundPlayer, String commandCharacter, String messageSizeLimit) {
        this.soundPlayer = soundPlayer;
        if (commandCharacter != null && !commandCharacter.isEmpty()) {
            this.commandCharacter = commandCharacter;
        }
        if (messageSizeLimit != null && !messageSizeLimit.isEmpty() && messageSizeLimit.matches("^-?\\d+$")) {
            this.messageSizeLimit = Integer.parseInt(messageSizeLimit);
        }
    }

    public void onGuildMessageReceived(GuildMessageReceivedEvent event) {        
        String message = event.getMessage().getContent();
        String requestingUser = event.getAuthor().getUsername();
        StringBuilder sb = new StringBuilder();

        //Respond
        if (message.startsWith(commandCharacter + "list")) {
            Set<Map.Entry<String, SoundFile>> entrySet = soundPlayer.getAvailableSoundFiles().entrySet();

            final int maxLineLength = messageSizeLimit;
            StringBuilder output = new StringBuilder(maxLineLength);
            if (entrySet.size() > 0) {
                sb.append("```Type any of the following into the chat to play the sound: \n");
                for (Map.Entry entry : entrySet) {
                    sb.append(commandCharacter).append(entry.getKey()).append("\n");
                }
                sb.append("```");
                
                LOG.info("Responding to chat request from " + requestingUser + ".");

                //if text has \n, \r or \t symbols it's better to split by \s+
                final String SPLIT_REGEXP= "(?<=[ \\n])";

                    String[] tokens = sb.toString().split(SPLIT_REGEXP);
                    int lineLen = 0;
                    for (int i = 0; i < tokens.length; i++) {
                        String word = tokens[i];

                        if (lineLen + (word).length() > maxLineLength) {
                            if (i > 0) {
                                output.append("```");
                                event.getChannel().sendMessage(output.toString());
                                
                                output = new StringBuilder(maxLineLength);
                                output.append("```");
                            }
                            lineLen = 0;
                        }
                        output.append(word);
                        lineLen += word.length();
                }
            } else {
                sb.append("The soundboard has no available sounds to play.");
            }
            event.getChannel().sendMessage(output.toString());
        //If the command is not list and starts with the specified command character try and play that "command" or sound file.
        } else if (message.startsWith(commandCharacter)) {
            try {
                String fileNameRequested = message.substring(1, message.length());
                LOG.info("Attempting to play file: " + fileNameRequested + ". Requested by " + requestingUser + ".");
                soundPlayer.playFileForEvent(fileNameRequested, event);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
