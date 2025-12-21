package net.dirtydeeds.discordsoundboard.commands;

import net.dirtydeeds.discordsoundboard.SoundPlayer;
import net.dirtydeeds.discordsoundboard.beans.SoundFile;
import net.dirtydeeds.discordsoundboard.beans.DiscordUser;
import net.dirtydeeds.discordsoundboard.controllers.DiscordUserController;
import net.dirtydeeds.discordsoundboard.service.DiscordUserService;
import net.dirtydeeds.discordsoundboard.service.SoundService;

/**
 * @author Dave Furrer
 * <p>
 * Command to set or clear the sound that plays when a user enters a voice channel.
 * Arugument can be username or userId
 */
public class EntranceCommand extends Command {

    private final DiscordUserService discordUserService;
    private final SoundService soundService;
    private final SoundPlayer soundPlayer;

    public EntranceCommand(SoundPlayer soundPlayer, DiscordUserService discordUserService,
                           SoundService soundService) {
        this.soundPlayer = soundPlayer;
        this.discordUserService = discordUserService;
        this.soundService = soundService;
        this.name = "entrance";
        this.help = "Sets entrance sound for user. Leave soundFileName empty to remove";
    }

    @Override
    protected void execute(CommandEvent event) {
        if (!event.getArguments().isEmpty()) {
            String userNameOrId = event.getArguments().getFirst();
            String soundFileName = "";
            if (event.getArguments().size() == 2) {
                soundFileName = event.getArguments().get(1);
            }

            net.dv8tion.jda.api.entities.User pmUser = event.getAuthor();
            if (event.userIsAdmin() ||
                    (pmUser.getName().equalsIgnoreCase(userNameOrId)
                            || pmUser.getId().equals(userNameOrId))) {
                DiscordUser discordUser = discordUserService.findOneByIdOrUsernameIgnoreCase(userNameOrId, userNameOrId);
                if (discordUser == null) {
                    net.dv8tion.jda.api.entities.User jdaUser = soundPlayer.retrieveUserById(userNameOrId);
                    if (jdaUser != null) {
                        discordUser = new DiscordUser(jdaUser.getId(), jdaUser.getName(), false, jdaUser.getJDA().getStatus(), jdaUser.getJDA().getPresence().getStatus());
                    }
                }
                if (discordUser != null) {
                    if (soundFileName.isEmpty()) {
                        discordUser.setEntranceSound(null);
                        event.replyByPrivateMessage("User: " + userNameOrId + " entrance sound cleared");
                        discordUserService.save(discordUser);
                        soundPlayer.broadcastUserUpdate();
                    } else {
                        SoundFile soundFile = soundService.findOneBySoundFileIdIgnoreCase(soundFileName);
                        if (soundFile == null) {
                            event.replyByPrivateMessage("Could not find sound file: " + soundFileName);
                        } else {
                            discordUser.setEntranceSound(soundFileName);
                            event.replyByPrivateMessage("User: " + userNameOrId + " entrance sound set to: " + soundFileName);
                            discordUserService.save(discordUser);
                            soundPlayer.broadcastUserUpdate();
                        }
                    }
                } else {
                    event.replyByPrivateMessage("Could not find user with id or name: " + userNameOrId);
                }
            } else {
                event.replyByPrivateMessage("You must have admin on the server to edit entrance for other users " +
                        event.getPrefix() + "entrance <userid/username> <soundFile>");
            }
        }
    }
}