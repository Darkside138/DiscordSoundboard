package net.dirtydeeds.discordsoundboard.service;

import net.dirtydeeds.discordsoundboard.beans.User;

import java.util.List;
import java.util.Optional;

public interface UserService {

    Optional<User> findById(String id);

    Iterable<User> saveAll(List<User> users);

    User findOneByIdOrUsernameIgnoreCase(String userNameOrId, String userNameOrId1);

    User save(User user);
}