package net.dirtydeeds.discordsoundboard.commands;

import net.dirtydeeds.discordsoundboard.BotConfig;
import net.dirtydeeds.discordsoundboard.SoundPlayer;
import net.dirtydeeds.discordsoundboard.beans.SoundFile;
import net.dirtydeeds.discordsoundboard.service.SoundService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * @author Dave Furrer
 * <p>
 * Command to remove a sound file from the filesystem. Must have admin rights to call this
 */
public class RemoveCommand extends Command {
    private static final Logger LOG = LoggerFactory.getLogger(RemoveCommand.class);

    private final SoundPlayer soundPlayer;
    private final BotConfig botConfig;
    private final SoundService soundService;

    public RemoveCommand(SoundPlayer soundPlayer, BotConfig botConfig, SoundService soundService) {
        this.soundPlayer = soundPlayer;
        this.botConfig = botConfig;
        this.soundService = soundService;
        this.name = "remove";
        this.help = "Removes requested sound file from disk. Must be amdin";
    }

    @Override
    protected void execute(CommandEvent event) {
        if (event.getAuthor() != null) {
            boolean hasManageServerPerm = event.userIsAdmin();
            if (!event.getArguments().isEmpty()) {
                String soundToRemove = event.getArguments().getFirst();
                //They can remove their own entrance/leave sound else they have to have admin rights
                if (event.getAuthor().getName().equalsIgnoreCase(soundToRemove)
                        || event.getAuthor().getName().equalsIgnoreCase(
                                soundToRemove.replace(botConfig.getLeaveSuffix(), ""))
                        || hasManageServerPerm) {
                    SoundFile soundFileToRemove = soundPlayer.getAvailableSoundFiles().get(soundToRemove);
                    if (soundFileToRemove != null) {
                        try {
                            soundService.delete(soundFileToRemove);
                            boolean fileRemoved = Files.deleteIfExists(Paths.get(soundFileToRemove.getSoundFileLocation()));
                            if (fileRemoved) {
                                event.replyByPrivateMessage("Sound file " + soundToRemove + " was removed.");
                            } else {
                                event.replyByPrivateMessage("Could not find sound file: " + soundToRemove + ".");
                            }
                        } catch (IOException e) {
                            LOG.error("Could not remove sound file {}", soundToRemove);
                        }
                    }
                } else {
                    event.replyByPrivateMessage("You do not have permission to remove sound file: " + soundToRemove + ".");
                }
            }
        }
    }
}