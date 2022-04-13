package net.dirtydeeds.discordsoundboard.commands;

import net.dirtydeeds.discordsoundboard.BotConfig;
import net.dirtydeeds.discordsoundboard.SoundPlayer;
import net.dirtydeeds.discordsoundboard.beans.SoundFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Dave Furrer
 * <p>
 * Command to return the list of sound files to the requesting user
 */
public class ListCommand extends Command {
    private static final Logger LOG = LoggerFactory.getLogger(ListCommand.class);

    private final SoundPlayer soundPlayer;
    private final BotConfig botConfig;

    public ListCommand(SoundPlayer soundPlayer, BotConfig botConfig) {
        this.botConfig = botConfig;
        this.soundPlayer = soundPlayer;
        this.name = "list";
        this.help = "Returns a list of available sound files";
    }

    @Override
    protected void execute(CommandEvent event) {
        StringBuilder commandString = getCommandListString(event);
        List<String> soundList = getCommandList(commandString);

        LOG.info("Responding to list command. Requested by {}", event.getRequestingUser());
        if (event.getArguments().isEmpty()) {
            if (commandString.length() > botConfig.getMessageSizeLimit()) {
                event.replyByPrivateMessage("You have " + soundList.size() + " pages of soundFiles. Reply: ```" + botConfig.getCommandCharacter() + "list pageNumber``` to request a specific page of results.");
            } else {
                event.replyByPrivateMessage("Type any of the following into the chat to play the sound:"+
                        "\n"+soundList.get(0));
            }
        } else {
            try {
                int pageNumber = Integer.parseInt(event.getArguments().getFirst());
                event.replyByPrivateMessage(soundList.get(pageNumber - 1));
            } catch (IndexOutOfBoundsException e) {
                event.replyByPrivateMessage("The page number you entered is not valid.");
            } catch (NumberFormatException e) {
                event.replyByPrivateMessage("The page number argument must be a number.");
            }
        }
    }

    private List<String> getCommandList(StringBuilder commandString) {
        final int maxLineLength = botConfig.getMessageSizeLimit() - 10;
        List<String> soundFiles = new ArrayList<>();

        //if text has \n, \r or \t symbols it's better to split by \s+
        final String SPLIT_REGEXP = "(?<=[ \\n])";

        String[] tokens = commandString.toString().split(SPLIT_REGEXP);
        int lineLen = 0;
        StringBuilder output = new StringBuilder();
        output.append("```\n");
        for (int i = 0; i < tokens.length; i++) {
            String word = tokens[i];

            if (lineLen + (word).length() > maxLineLength + 2) {
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

    private StringBuilder getCommandListString(CommandEvent event) {
        StringBuilder sb = new StringBuilder();

        Set<Map.Entry<String, SoundFile>> entrySet = soundPlayer.getAvailableSoundFiles().entrySet();

        if (entrySet.size() > 0) {
            entrySet.forEach(entry -> sb.append(event.getPrefix()).append(entry.getKey()).append("\n"));
        }
        return sb;
    }
}
