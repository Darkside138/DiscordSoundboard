package net.dirtydeeds.discordsoundboard.listeners;

import net.dirtydeeds.discordsoundboard.SoundPlayer;
import net.dirtydeeds.discordsoundboard.controllers.DiscordUserController;
import net.dv8tion.jda.api.events.user.update.*;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UserEventListener extends ListenerAdapter {

    private static final Logger LOG = LoggerFactory.getLogger(UserEventListener.class);

    private final SoundPlayer soundPlayer;
    private final DiscordUserController discordUserController;

    public UserEventListener(SoundPlayer soundPlayer, DiscordUserController discordUserController) {
        this.soundPlayer = soundPlayer;
        this.discordUserController = discordUserController;
    }

    @Override
    public void onUserUpdateName(UserUpdateNameEvent event) {
        LOG.debug("User changed name to {}", event.getJDA().getSelfUser().getName());
        soundPlayer.updateUsersInDb();
        discordUserController.broadcastUpdate();
    }

    @Override
    public void onUserUpdateGlobalName(UserUpdateGlobalNameEvent event) {
        LOG.debug("User change global name to {}", event.getJDA().getSelfUser().getGlobalName());
        soundPlayer.updateUsersInDb();
        discordUserController.broadcastUpdate();
    }

    @Override
    public void onUserUpdateDiscriminator(UserUpdateDiscriminatorEvent event) {
        LOG.debug("User change discriminator to {}", event.getJDA().getSelfUser().getDiscriminator());
        soundPlayer.updateUsersInDb();
        discordUserController.broadcastUpdate();
    }

    @Override
    public void onUserUpdateAvatar(UserUpdateAvatarEvent event) {
        LOG.debug("User updated avatar to {}", event.getJDA().getSelfUser().getAvatar());
        soundPlayer.updateUsersInDb();
        discordUserController.broadcastUpdate();
    }

    @Override
    public void onUserUpdateOnlineStatus(UserUpdateOnlineStatusEvent event) {
        soundPlayer.updateUsersInDb();
        discordUserController.broadcastUpdate();
    }
}
