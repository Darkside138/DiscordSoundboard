package net.dirtydeeds.discordsoundboard.service.impl;

import net.dirtydeeds.discordsoundboard.beans.Users;
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
    public Optional<Users> findById(String id) {
        return userRepository.findById(id);
    }

    @Override
    public Iterable<Users> saveAll(List<Users> users) {
        return userRepository.saveAll(users);
    }

    @Override
    public Users findOneByIdOrUsernameIgnoreCase(String userId, String userName) {
        return userRepository.findOneByIdOrUsernameIgnoreCase(userId, userName);
    }

    @Override
    public Users save(Users users) {
        return userRepository.save(users);
    }

    @Override
    public Iterable<Users> findAll(Pageable pageable) {
        return userRepository.findAll(pageable);
    }

    @Override
    public void delete(Users users) { userRepository.delete(users); }
}
