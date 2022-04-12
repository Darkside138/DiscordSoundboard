package net.dirtydeeds.discordsoundboard.commands;

import net.dirtydeeds.discordsoundboard.SoundPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Dave Furrer
 * <p>
 * Command to play the requested sound file
 */
public class PlayCommand extends Command {
    private static final Logger LOG = LoggerFactory.getLogger(PlayCommand.class);

    private final SoundPlayer soundPlayer;

    public PlayCommand(SoundPlayer soundPlayer) {
        this.soundPlayer = soundPlayer;
        this.name = "Play";
        this.help = "Plays the specified sound from the list. Add ~number after sound file to repeat";
    }

    @Override
    protected void execute(CommandEvent event) {
        try {
            int repeatNumber = 1;
            String fileNameRequested = event.getCommandString();
            if (event.getCommandString().equals(this.name)) {
                fileNameRequested = event.getArguments().getFirst();
            }

            // If there is the repeat character (~) then cut up the message string.
            int repeatIndex = fileNameRequested.indexOf('~');
            if (repeatIndex > -1) {
                fileNameRequested = fileNameRequested.substring(1, repeatIndex).trim();
                if (repeatIndex + 1 != fileNameRequested.length()) { // If there is something after the ~ then repeat for that value
                    repeatNumber = Integer.parseInt(fileNameRequested.substring(repeatIndex + 1).trim()); // +1 to ignore the ~ character
                }
            }
            LOG.info("Attempting to play file: {} {} times. Requested by {}.", fileNameRequested, repeatNumber, event.getRequestingUser());

            soundPlayer.playForUser(fileNameRequested, event.getAuthor().getName(), repeatNumber, null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}