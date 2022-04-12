package net.dirtydeeds.discordsoundboard.repository;

import net.dirtydeeds.discordsoundboard.beans.User;
import org.springframework.data.repository.PagingAndSortingRepository;

public interface UserRepository extends PagingAndSortingRepository<User, String> {
    User findOneByIdOrUsernameIgnoreCase(String id, String userName);
}
