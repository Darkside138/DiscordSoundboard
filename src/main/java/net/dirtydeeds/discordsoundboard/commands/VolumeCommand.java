package net.dirtydeeds.discordsoundboard.commands;

import net.dirtydeeds.discordsoundboard.SoundPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Dave Furrer
 * <p>
 * Command to set the volume of the bot
 */
public class VolumeCommand extends Command {

    private static final Logger LOG = LoggerFactory.getLogger(StopCommand.class);

    private final SoundPlayer soundPlayer;

    public VolumeCommand(SoundPlayer soundPlayer) {
        this.soundPlayer = soundPlayer;
        this.name = "volume";
        this.help = "0 - 100. Sets the playback volume";
    }

    @Override
    protected void execute(CommandEvent event) {
        if (event.getArguments().size() > 0) {
            int volume = Integer.parseInt(event.getArguments().getFirst());

            if (volume >= 1 && volume <= 100) {
                soundPlayer.setSoundPlayerVolume(volume, event.getRequestingUser(), null);
                event.replyByPrivateMessage("*Volume set to " + volume + "%*");
                LOG.info("Volume set to {}% by {}.", volume, event.getRequestingUser());
            } else if (volume == 0) {
                soundPlayer.setSoundPlayerVolume(volume, event.getRequestingUser(), null);
                event.replyByPrivateMessage(event.getRequestingUser() + " muted me.");
                LOG.info("Bot muted by {}", event.getRequestingUser());
            }
        }
    }
}
