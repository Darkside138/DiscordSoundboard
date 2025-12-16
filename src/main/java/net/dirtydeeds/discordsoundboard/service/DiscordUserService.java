package net.dirtydeeds.discordsoundboard.service;

import net.dirtydeeds.discordsoundboard.SoundPlayer;
import net.dirtydeeds.discordsoundboard.beans.DiscordUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface DiscordUserService {

    Optional<DiscordUser> findById(String id);

    DiscordUser findOneByIdOrUsernameIgnoreCase(String userNamId, String userName);

    DiscordUser save(DiscordUser discordUser);

    Page<DiscordUser> findAll(Pageable pageable);

    void delete(DiscordUser discordUser);

    Page<DiscordUser> findByInVoiceIsTrueOrSelectedIsTrue(Pageable pageable);

    void setSoundPlayer(SoundPlayer soundPlayer);

    DiscordUser updateSounds(String userId, String entranceSound, String leaveSound) throws Exception;
}
