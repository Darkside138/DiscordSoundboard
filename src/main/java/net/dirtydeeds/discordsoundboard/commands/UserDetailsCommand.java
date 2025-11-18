package net.dirtydeeds.discordsoundboard.commands;

import net.dirtydeeds.discordsoundboard.SoundPlayer;
import net.dirtydeeds.discordsoundboard.beans.Users;
import net.dirtydeeds.discordsoundboard.service.UserService;

/**
 * @author Dave Furrer
 * <p>
 * Command to get the details of the request user
 */
public class UserDetailsCommand extends Command {

    private final UserService userService;
    private final SoundPlayer soundPlayer;

    public UserDetailsCommand(UserService userService, SoundPlayer soundPlayer) {
        this.userService = userService;
        this.soundPlayer = soundPlayer;
        this.name = "userDetails";
        this.help = "userDetails userName - Get details for user";
    }

    @Override
    protected void execute(CommandEvent event) {
        if (!event.getArguments().isEmpty()) {
            String userNameOrId = event.getArguments().getFirst();
            Users users = userService.findOneByIdOrUsernameIgnoreCase(userNameOrId, userNameOrId);
            if (users == null) {
                net.dv8tion.jda.api.entities.User jdaUser = soundPlayer.retrieveUserById(userNameOrId);
                if (jdaUser != null) {
                    users = new Users(jdaUser.getId(), jdaUser.getName(), false, jdaUser.getJDA().getStatus(), jdaUser.getJDA().getPresence().getStatus());
                }
            }
            if (users != null) {
                StringBuilder response = new StringBuilder();
                response.append("User details for ").append(userNameOrId).append("```")
                        .append("\nDiscord Id: ").append(users.getId())
                        .append("\nUsername: ").append(users.getUsername())
                        .append("\nEntrance Sound: ");
                if (users.getEntranceSound() != null) {
                    response.append(users.getEntranceSound());
                }
                response.append("\nLeave Sound: ");
                if (users.getLeaveSound() != null) {
                    response.append(users.getLeaveSound());
                }
                response.append("```");
                event.replyByPrivateMessage(response.toString());
            }
        }
    }
}