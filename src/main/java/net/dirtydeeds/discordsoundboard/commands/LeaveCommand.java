package net.dirtydeeds.discordsoundboard.commands;

import net.dirtydeeds.discordsoundboard.SoundPlayer;
import net.dirtydeeds.discordsoundboard.beans.SoundFile;
import net.dirtydeeds.discordsoundboard.beans.User;
import net.dirtydeeds.discordsoundboard.service.SoundService;
import net.dirtydeeds.discordsoundboard.service.UserService;

/**
 * @author Dave Furrer
 * <p>
 * Command to set or clear the sound that plays when a user leaves a voice channel.
 * Arugument can be username or userId
 */
public class LeaveCommand extends Command {

    private final UserService userService;
    private final SoundService soundService;
    private final SoundPlayer soundPlayer;

    public LeaveCommand(SoundPlayer soundPlayer, UserService userService,
                        SoundService soundService) {
        this.soundPlayer = soundPlayer;
        this.userService = userService;
        this.soundService = soundService;
        this.name = "leave";
        this.help = "Sets leave sound for user. Leave soundFileName empty to remove";
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
                User user = userService.findOneByIdOrUsernameIgnoreCase(userNameOrId, userNameOrId);
                if (user == null) {
                    net.dv8tion.jda.api.entities.User jdaUser = soundPlayer.retrieveUserById(userNameOrId);
                    if (jdaUser != null) {
                        user = new User(jdaUser.getId(), jdaUser.getName(), false, jdaUser.getJDA().getStatus());
                    }
                }
                if (user != null) {
                    if (soundFileName.isEmpty()) {
                        user.setLeaveSound(null);
                        event.replyByPrivateMessage("User: " + userNameOrId + " leave sound cleared");
                        userService.save(user);
                    } else {
                        SoundFile soundFile = soundService.findOneBySoundFileIdIgnoreCase(soundFileName);
                        if (soundFile == null) {
                            event.replyByPrivateMessage("Could not find sound file: " + soundFileName);
                        } else {
                            user.setLeaveSound(soundFileName);
                            event.replyByPrivateMessage("User: " + userNameOrId + " leave sound set to: " + soundFileName);
                            userService.save(user);
                        }
                    }
                } else {
                    event.replyByPrivateMessage("Could not find user with id or name: " + userNameOrId);
                }
            } else {
                event.replyByPrivateMessage("Entrance command incorrect. Required input is " +
                        event.getPrefix() + "entrance <userid/username> <soundfile>");
            }
        }
    }
}