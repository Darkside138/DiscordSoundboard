package net.dirtydeeds.discordsoundboard.controllers;

import net.dirtydeeds.discordsoundboard.beans.User;
import net.dirtydeeds.discordsoundboard.SoundPlayer;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.inject.Inject;
import java.util.List;

@RestController
@RequestMapping("/users")
@SuppressWarnings("unused")
public class UserController {

    private final SoundPlayer soundPlayer;

    @Inject
    public UserController(SoundPlayer soundPlayer) {
        this.soundPlayer = soundPlayer;
    }

    @GetMapping()
    public List<User> getUsers() {
        return soundPlayer.getUsers();
    }
}
