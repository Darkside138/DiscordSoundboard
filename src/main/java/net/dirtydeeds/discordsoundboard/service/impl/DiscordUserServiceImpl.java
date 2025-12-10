package net.dirtydeeds.discordsoundboard.service.impl;

import net.dirtydeeds.discordsoundboard.SoundPlayer;
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

    private SoundPlayer soundPlayer;

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

    @Override
    public Page<DiscordUser> findByInVoiceIsTrueOrSelectedIsTrue(Pageable pageable) {
        return discordUserRepository.findByInVoiceIsTrueOrSelectedIsTrue(pageable);
    }

    @Override
    public void updateUsersInDb() {
        soundPlayer.updateUsersInDb();
    }

    public void setSoundPlayer(SoundPlayer soundPlayer) {
        this.soundPlayer = soundPlayer;
    }

    @Override
    public DiscordUser updateSounds(String userId, String entranceSound, String leaveSound) throws Exception {
        Optional<DiscordUser> optionalDiscordUser = discordUserRepository.findById(userId);

        if (optionalDiscordUser.isPresent()) {
            DiscordUser discordUser = optionalDiscordUser.get();
            if (entranceSound != null) {
                if (entranceSound.isEmpty()) {
                    discordUser.setEntranceSound(null);
                } else {
                    discordUser.setEntranceSound(entranceSound);
                }
            }
            if (leaveSound != null) {
                if (leaveSound.isEmpty()) {
                    discordUser.setLeaveSound(null);
                } else {
                    discordUser.setLeaveSound(leaveSound);
                }
            }

            discordUserRepository.save(discordUser);

            return discordUser;
        }

        throw new Exception("Could not load discord user");
    }
}
