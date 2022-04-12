package net.dirtydeeds.discordsoundboard.controllers;

import net.dirtydeeds.discordsoundboard.SoundPlayer;
import net.dirtydeeds.discordsoundboard.beans.User;
import net.dirtydeeds.discordsoundboard.service.UserService;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.inject.Inject;

@RestController
@RequestMapping("/api/users")
@SuppressWarnings("unused")
public class UserController {

    private final UserService userService;
    private final SoundPlayer soundPlayer;

    @Inject
    public UserController(UserService userService, SoundPlayer soundPlayer) {
        this.userService = userService;
        this.soundPlayer = soundPlayer;
    }

    @GetMapping("/findAll")
    public ResponseEntity<Iterable<User>> getAll() {
        //Need to call this to refresh the list of users.
        soundPlayer.getUsers();
        Pageable wholePage = Pageable.unpaged();
        return new ResponseEntity<>(userService.findAll(wholePage), HttpStatus.OK);
    }
}
