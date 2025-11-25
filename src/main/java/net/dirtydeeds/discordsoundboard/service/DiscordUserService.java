package net.dirtydeeds.discordsoundboard.service;

import net.dirtydeeds.discordsoundboard.beans.DiscordUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface DiscordUserService {

    Optional<DiscordUser> findById(String id);

    Iterable<DiscordUser> saveAll(List<DiscordUser> users);

    DiscordUser findOneByIdOrUsernameIgnoreCase(String userNamId, String userName);

    DiscordUser save(DiscordUser discordUser);

    Page<DiscordUser> findAll(Pageable pageable);

    void delete(DiscordUser discordUser);
}
