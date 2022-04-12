package net.dirtydeeds.discordsoundboard.service.impl;

import net.dirtydeeds.discordsoundboard.beans.User;
import net.dirtydeeds.discordsoundboard.repository.UserRepository;
import net.dirtydeeds.discordsoundboard.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@SuppressWarnings("unused")
public class UserServiceImpl implements UserService {

    @Autowired
    private UserRepository userRepository;

    @Override
    public Optional<User> findById(String id) {
        return userRepository.findById(id);
    }

    @Override
    public Iterable<User> saveAll(List<User> users) {
        return userRepository.saveAll(users);
    }

    @Override
    public User findOneByIdOrUsernameIgnoreCase(String userNameOrId, String userNameOrId1) {
        return userRepository.findOneByIdOrUsernameIgnoreCase(userNameOrId, userNameOrId1);
    }

    @Override
    public User save(User user) {
        return userRepository.save(user);
    }

    @Override
    public Iterable<User> findAll(Pageable pageable) {
        return userRepository.findAll(pageable);
    }
}
