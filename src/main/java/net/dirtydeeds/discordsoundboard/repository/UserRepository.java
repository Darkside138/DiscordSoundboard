package net.dirtydeeds.discordsoundboard.repository;

import net.dirtydeeds.discordsoundboard.beans.Users;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;

public interface UserRepository extends PagingAndSortingRepository<Users, String>, CrudRepository<Users, String> {
    Users findOneByIdOrUsernameIgnoreCase(String id, String userName);
}
