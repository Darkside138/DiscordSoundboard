package net.dirtydeeds.discordsoundboard;

import net.dirtydeeds.discordsoundboard.beans.SoundFile;
import net.dirtydeeds.discordsoundboard.service.SoundPlayerImpl;
import net.dv8tion.jda.entities.Message;
import net.dv8tion.jda.events.message.MessageReceivedEvent;
import net.dv8tion.jda.hooks.ListenerAdapter;
import net.dv8tion.jda.utils.SimpleLog;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
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
    private boolean muted;
    private static final int MAX_FILE_SIZE_IN_BYTES = 1000000; // 1 MB

    public ChatSoundBoardListener(SoundPlayerImpl soundPlayer, String commandCharacter, String messageSizeLimit) {
        this.soundPlayer = soundPlayer;
        if (commandCharacter != null && !commandCharacter.isEmpty()) {
            this.commandCharacter = commandCharacter;
        }
        if (messageSizeLimit != null && !messageSizeLimit.isEmpty() && messageSizeLimit.matches("^-?\\d+$")) {
            this.messageSizeLimit = Integer.parseInt(messageSizeLimit);
            if (this.messageSizeLimit > 1994) {
                this.messageSizeLimit = 1994;
            }
        }
        muted=false;
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if(!event.getAuthor().isBot()) {
            String message = event.getMessage().getContent();
            String requestingUser = event.getAuthor().getUsername();
            final int maxLineLength = messageSizeLimit;

            //Respond
            if (message.startsWith(commandCharacter + "list")) {
                StringBuilder commandString = getCommandListString();
                List<String> soundList = getCommandList(commandString);

                LOG.info("Responding to " + message + " command. Requested by " + requestingUser + ".");
                if (message.equals(commandCharacter + "list")) {
                    if (commandString.length() > maxLineLength) {
                        replyByPrivateMessage(event, "You have " + soundList.size() + " pages of soundFiles. Reply: ```" + commandCharacter + "list pageNumber``` to request a specific page of results.");
                    } else {
                        replyByPrivateMessage(event, "Type any of the following into the chat to play the sound:");
                        replyByPrivateMessage(event, soundList.get(0));
                    }
                } else {
                    String[] messageSplit = message.split(" ");
                    try {
                        Integer pageNumber = Integer.parseInt(messageSplit[1]);
                        replyByPrivateMessage(event, soundList.get(pageNumber - 1));
                    } catch (IndexOutOfBoundsException e) {
                        replyByPrivateMessage(event, "The page number you entered is not valid.");
                    } catch (NumberFormatException e) {
                        replyByPrivateMessage(event, "The page number argument must be a number.");
                    }
                }
                //If the command is not list and starts with the specified command character try and play that "command" or sound file.
            } else if (message.startsWith(commandCharacter + "help")) {
                LOG.info("Responding to help command. Requested by " + requestingUser + ".");
                replyByPrivateMessage(event, "Type ```" + commandCharacter + "list``` to get a list of available sound files. Type ```" + commandCharacter + "soundFileName``` to play the a sound from the list.");
            } else if(message.startsWith(commandCharacter + "volume")) {
                int newVol = Integer.parseInt(message.substring(8));
                if(newVol >= 1 && newVol <= 100) {
                    muted = false;
                    soundPlayer.setSoundPlayerVolume(newVol);
                    replyByPrivateMessage(event, "*Volume set to " + newVol + "%*");
                    LOG.info("Volume set to " + newVol + "% by " + requestingUser + ".");
                } else if(newVol == 0) {
                    muted = true;
                    soundPlayer.setSoundPlayerVolume(newVol);
                    replyByPrivateMessage(event, requestingUser + " muted me.");
                    LOG.info("Bot muted by " + requestingUser + ".");
                    soundPlayer.playFileForUser("stop",requestingUser);
                }
            } else if (message.startsWith(commandCharacter + "stop")) {
                if (soundPlayer.stop()) {
                    replyByPrivateMessage(event, "Playback stopped.");    
                } else {
                    replyByPrivateMessage(event, "Nothing was playing.");
                }
                
            } else if (message.startsWith(commandCharacter) && message.length() >= 2) {
                if(!muted) {
                    try {
                        String fileNameRequested = message.substring(1, message.length());
                        LOG.info("Attempting to play file: " + fileNameRequested + ". Requested by " + requestingUser + ".");
                        soundPlayer.playFileForEvent(fileNameRequested, event);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else {
                    replyByPrivateMessage(event, "I seem to be muted! Try " + commandCharacter + "help");
                    LOG.info("Attempting to play a sound file while muted. Requested by " + requestingUser + ".");
                }
            } else {
                List<Message.Attachment> attachments = event.getMessage().getAttachments();
                if (attachments.size() > 0 && event.isPrivate()) {
                    for (Message.Attachment attachment : attachments) {
                        String name = attachment.getFileName();
                        String extension = name.substring(name.indexOf(".") + 1);
                        if (extension.equals("wav") || extension.equals("mp3")) {
                            if (attachment.getSize() < MAX_FILE_SIZE_IN_BYTES) {
                                attachment.download(new File(soundPlayer.getSoundsPath(), name));
                                event.getChannel().sendMessage("Downloaded file `" + name + "` and added to list of sounds " + event.getAuthor().getAsMention() + ".");
                            } else {
                                replyByPrivateMessage(event, "File `" + name + "` is too large to add to library.");
                            }
                        }
                    }
                } else {
                    if (message.startsWith(commandCharacter) || event.isPrivate()) {
                        nonRecognizedCommand(event, requestingUser);
                    }
                }
            }
        }
    }
    
    private void nonRecognizedCommand(MessageReceivedEvent event, String requestingUser) {
        replyByPrivateMessage(event, "Hello @" + requestingUser + ". I don't know how to respond to this message!");
        replyByPrivateMessage(event, "You can type " + commandCharacter + "list to see a list of all playable Sounds. Type " + commandCharacter + "volume 0 - 100 to set the bots volume.");
        LOG.info("Responding to PM of " + requestingUser + ". Unknown Command. Sending help text.");
    }

    private List<String> getCommandList(StringBuilder commandString) {
        final int maxLineLength = messageSizeLimit;
        List<String> soundFiles = new ArrayList<>();

        //if text has \n, \r or \t symbols it's better to split by \s+
        final String SPLIT_REGEXP= "(?<=[ \\n])";

        String[] tokens = commandString.toString().split(SPLIT_REGEXP);
        int lineLen = 0;
        StringBuilder output = new StringBuilder();
        output.append("```\n");
        for (int i = 0; i < tokens.length; i++) {
            String word = tokens[i];

            if (lineLen + (word).length() > maxLineLength) {
                if (i > 0) {
                    output.append("```\n");
                    soundFiles.add(output.toString());

                    output = new StringBuilder(maxLineLength);
                    output.append("```");
                }
                lineLen = 0;
            }
            output.append(word);
            lineLen += word.length();
        }
        if (output.length() > 0) {
            output.append("```");
        }
        soundFiles.add(output.toString());
        return soundFiles;
    }

    private StringBuilder getCommandListString() {
        StringBuilder sb = new StringBuilder();

        Set<Map.Entry<String, SoundFile>> entrySet = soundPlayer.getAvailableSoundFiles().entrySet();

        if (entrySet.size() > 0) {
            for (Map.Entry entry : entrySet) {
                sb.append(commandCharacter).append(entry.getKey()).append("\n");
            }
        }
        return sb;
    }
    
    private void replyByPrivateMessage(MessageReceivedEvent event, String message) {
        event.getAuthor().getPrivateChannel().sendMessage(message);
    }
}
