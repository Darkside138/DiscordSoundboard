package net.dirtydeeds.discordsoundboard.commands;

import net.dirtydeeds.discordsoundboard.SoundPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Dave Furrer
 * <p>
 * Command to stop any playback
 */
public class StopCommand extends Command {

    private static final Logger LOG = LoggerFactory.getLogger(StopCommand.class);

    private final SoundPlayer soundPlayer;

    public StopCommand(SoundPlayer soundPlayer) {
        this.soundPlayer = soundPlayer;
        this.name = "stop";
        this.help = "Stops the sound that is currently playing";
    }

    @Override
    protected void execute(CommandEvent event) {
        int fadeoutTimeout = 0;
        if (event.getArguments().size() > 0) {
            fadeoutTimeout = Integer.parseInt(event.getArguments().getFirst());
        }

        LOG.info("Stop requested by {} with a fadeout of {} seconds", event.getRequestingUser(), fadeoutTimeout);
        if (soundPlayer.stop(event.getRequestingUser(), null)) {
            event.replyByPrivateMessage("Playback stopped.");
        } else {
            event.replyByPrivateMessage("Nothing was playing.");
        }
    }
}
