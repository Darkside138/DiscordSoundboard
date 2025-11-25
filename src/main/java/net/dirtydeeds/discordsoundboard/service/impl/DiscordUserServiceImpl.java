package net.dirtydeeds.discordsoundboard.service.impl;

import net.dirtydeeds.discordsoundboard.beans.DiscordUser;
import net.dirtydeeds.discordsoundboard.repository.DiscordUserRepository;
import net.dirtydeeds.discordsoundboard.service.DiscordUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@SuppressWarnings("unused")
public class DiscordUserServiceImpl implements DiscordUserService {

    @Autowired
    private DiscordUserRepository discordUserRepository;

    @Override
    public Optional<DiscordUser> findById(String id) {
        return discordUserRepository.findById(id);
    }

    @Override
    public Iterable<DiscordUser> saveAll(List<DiscordUser> users) {
        return discordUserRepository.saveAll(users);
    }

    @Override
    public DiscordUser findOneByIdOrUsernameIgnoreCase(String userId, String userName) {
        return discordUserRepository.findOneByIdOrUsernameIgnoreCase(userId, userName);
    }

    @Override
    public DiscordUser save(DiscordUser discordUser) {
        return discordUserRepository.save(discordUser);
    }

    @Override
    public Page<DiscordUser> findAll(Pageable pageable) {
        return discordUserRepository.findAll(pageable);
    }

    @Override
    public void delete(DiscordUser discordUser) { discordUserRepository.delete(discordUser); }
}
