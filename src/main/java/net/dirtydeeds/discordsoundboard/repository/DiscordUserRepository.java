package net.dirtydeeds.discordsoundboard.repository;

import net.dirtydeeds.discordsoundboard.beans.DiscordUser;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;

public interface DiscordUserRepository extends PagingAndSortingRepository<DiscordUser, String>, CrudRepository<DiscordUser, String> {
    DiscordUser findOneByIdOrUsernameIgnoreCase(String id, String userName);
}
