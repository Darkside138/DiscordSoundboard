package net.dirtydeeds.discordsoundboard.commands;

import net.dirtydeeds.discordsoundboard.SoundPlayer;
import net.dirtydeeds.discordsoundboard.beans.DiscordUser;
import net.dirtydeeds.discordsoundboard.service.DiscordUserService;

/**
 * @author Dave Furrer
 * <p>
 * Command to get the details of the request user
 */
public class UserDetailsCommand extends Command {

    private final DiscordUserService discordUserService;
    private final SoundPlayer soundPlayer;

    public UserDetailsCommand(DiscordUserService discordUserService, SoundPlayer soundPlayer) {
        this.discordUserService = discordUserService;
        this.soundPlayer = soundPlayer;
        this.name = "userDetails";
        this.help = "userDetails userName - Get details for user";
    }

    @Override
    protected void execute(CommandEvent event) {
        if (!event.getArguments().isEmpty()) {
            String userNameOrId = event.getArguments().getFirst();
            DiscordUser discordUser = discordUserService.findOneByIdOrUsernameIgnoreCase(userNameOrId, userNameOrId);
            if (discordUser == null) {
                net.dv8tion.jda.api.entities.User jdaUser = soundPlayer.retrieveUserById(userNameOrId);
                if (jdaUser != null) {
                    discordUser = new DiscordUser(jdaUser.getId(), jdaUser.getName(), false, jdaUser.getJDA().getStatus(), jdaUser.getJDA().getPresence().getStatus());
                }
            }
            if (discordUser != null) {
                StringBuilder response = new StringBuilder();
                response.append("User details for ").append(userNameOrId).append("```")
                        .append("\nDiscord Id: ").append(discordUser.getId())
                        .append("\nUsername: ").append(discordUser.getUsername())
                        .append("\nEntrance Sound: ");
                if (discordUser.getEntranceSound() != null) {
                    response.append(discordUser.getEntranceSound());
                }
                response.append("\nLeave Sound: ");
                if (discordUser.getLeaveSound() != null) {
                    response.append(discordUser.getLeaveSound());
                }
                response.append("```");
                event.replyByPrivateMessage(response.toString());
            }
        }
    }
}