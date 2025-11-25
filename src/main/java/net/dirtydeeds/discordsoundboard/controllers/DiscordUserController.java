package net.dirtydeeds.discordsoundboard.controllers;

import io.swagger.v3.oas.annotations.Hidden;
import net.dirtydeeds.discordsoundboard.SoundPlayer;
import net.dirtydeeds.discordsoundboard.beans.DiscordUser;
import net.dirtydeeds.discordsoundboard.service.DiscordUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Hidden
@RestController
@RequestMapping("/api/discordUsers")
@SuppressWarnings("unused")
public class DiscordUserController {

    private final DiscordUserService discordUserService;
    private final SoundPlayer soundPlayer;

    @Autowired
    public DiscordUserController(DiscordUserService discordUserService, SoundPlayer soundPlayer) {
        this.discordUserService = discordUserService;
        this.soundPlayer = soundPlayer;
    }

    @GetMapping()
    public Page<DiscordUser> getAll(@RequestParam(defaultValue = "1") int page,
                                    @RequestParam(defaultValue = "20") int size) {
        //Need to call this to refresh the list of users.
        soundPlayer.updateUsersInDb();
        return discordUserService.findAll(Pageable.ofSize(size).withPage(page));
    }
}
