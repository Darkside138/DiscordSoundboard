package net.dirtydeeds.discordsoundboard.commands;

import net.dirtydeeds.discordsoundboard.SoundPlayer;

/**
 * @author Dave Furrer
 * <p>
 * Command to set or clear the sound that plays when a user enters a voice channel.
 * Arugument can be username or userId
 */
public class URLCommand extends Command {

    private final SoundPlayer soundPlayer;

    public URLCommand(SoundPlayer soundPlayer) {
        this.soundPlayer = soundPlayer;
        this.name = "url";
        this.help = "Plays requested URL";
    }

    @Override
    protected void execute(CommandEvent event) {
        if (!event.getArguments().isEmpty()) {
            soundPlayer.playForUser(event.getArguments().getFirst(), event.getRequestingUser(), 1, null);
        }
    }
}