package net.dirtydeeds.discordsoundboard.commands;

import net.dirtydeeds.discordsoundboard.SoundPlaybackException;
import net.dirtydeeds.discordsoundboard.SoundPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Dave Furrer
 * <p>
 * Command to play a random sound file from the list of sounds
 */
public class RandomCommand extends Command {
    private static final Logger LOG = LoggerFactory.getLogger(RandomCommand.class);

    private final SoundPlayer soundPlayer;

    public RandomCommand(SoundPlayer soundPlayer) {
        this.soundPlayer = soundPlayer;
        this.name = "random";
        this.help = "Plays a random sound from the list";
    }

    @Override
    protected void execute(CommandEvent event) {
        try {
            soundPlayer.playRandomSoundFile(event.getRequestingUser(), event.getMessageReceivedEvent());
        } catch (SoundPlaybackException e) {
            event.replyByPrivateMessage("Problem playing random file:" + e);
        }
    }
}