package net.dirtydeeds.discordsoundboard.service;

import net.dirtydeeds.discordsoundboard.beans.Users;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface UserService {

    Optional<Users> findById(String id);

    Iterable<Users> saveAll(List<Users> users);

    Users findOneByIdOrUsernameIgnoreCase(String userNamId, String userName);

    Users save(Users users);

    Iterable<Users> findAll(Pageable pageable);

    void delete(Users users);
}
