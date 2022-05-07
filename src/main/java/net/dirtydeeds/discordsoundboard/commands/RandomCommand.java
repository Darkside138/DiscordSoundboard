package net.dirtydeeds.discordsoundboard.commands;

import net.dirtydeeds.discordsoundboard.SoundPlaybackException;
import net.dirtydeeds.discordsoundboard.SoundPlayer;

/**
 * @author Dave Furrer
 * <p>
 * Command to play a random sound file from the list of sounds
 */
public class RandomCommand extends Command {

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