package net.dirtydeeds.discordsoundboard.commands;

import net.dirtydeeds.discordsoundboard.SoundPlayer;

public class ReloadCommand extends Command {

    private final SoundPlayer soundPlayer;

    public ReloadCommand(SoundPlayer soundPlayer) {
        this.soundPlayer = soundPlayer;
        this.name = "reload";
        this.help = "Reloads the sound files from disk";
    }

    @Override
    protected void execute(CommandEvent event) {
        soundPlayer.updateFileList();
        event.replyByPrivateMessage("Sound files reloaded");
    }
}