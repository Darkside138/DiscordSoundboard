package net.dirtydeeds.discordsoundboard.commands;

import net.dirtydeeds.discordsoundboard.SoundPlayer;
import net.dirtydeeds.discordsoundboard.beans.User;
import net.dirtydeeds.discordsoundboard.repository.UserRepository;
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
        this.name = "userdetails";
        this.help = "userDetails userName - Get details for user";
    }

    @Override
    protected void execute(CommandEvent event) {
        if (!event.getArguments().isEmpty()) {
            String userNameOrId = event.getArguments().getFirst();
            User user = userService.findOneByIdOrUsernameIgnoreCase(userNameOrId, userNameOrId);
            if (user == null) {
                net.dv8tion.jda.api.entities.User jdaUser = soundPlayer.retrieveUserById(userNameOrId);
                if (jdaUser != null) {
                    user = new User(jdaUser.getId(), jdaUser.getName(), false, jdaUser.getJDA().getStatus());
                }
            }
            if (user != null) {
                StringBuilder response = new StringBuilder();
                response.append("User details for ").append(userNameOrId).append("```")
                        .append("\nDiscord Id: ").append(user.getId())
                        .append("\nUsername: ").append(user.getUsername())
                        .append("\nEntrance Sound: ");
                if (user.getEntranceSound() != null) {
                    response.append(user.getEntranceSound());
                }
                response.append("\nLeave Sound: ");
                if (user.getLeaveSound() != null) {
                    response.append(user.getLeaveSound());
                }
                response.append("```");
                event.replyByPrivateMessage(response.toString());
            }
        }
    }
}