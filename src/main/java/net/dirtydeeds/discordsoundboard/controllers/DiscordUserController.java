package net.dirtydeeds.discordsoundboard.controllers;

import io.swagger.v3.oas.annotations.Hidden;
import net.dirtydeeds.discordsoundboard.SoundPlayer;
import net.dirtydeeds.discordsoundboard.beans.DiscordUser;
import net.dirtydeeds.discordsoundboard.service.DiscordUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Hidden
@RestController
@RequestMapping("/api/discordUsers")
@SuppressWarnings("unused")
public class DiscordUserController {

    private final DiscordUserService discordUserService;
    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();

    @Autowired
    public DiscordUserController(DiscordUserService discordUserService) {
        this.discordUserService = discordUserService;
    }

    @GetMapping()
    public Page<DiscordUser> getAll(@RequestParam(defaultValue = "1") int page,
                                    @RequestParam(defaultValue = "20") int size,
                                    @RequestParam(defaultValue = "username") String sortBy) {
        //Need to call this to refresh the list of users.
        discordUserService.updateUsersInDb();
        return discordUserService.findAll(PageRequest.of(page,size, Sort.by(Sort.Order.asc(sortBy))));
    }

    @GetMapping("/invoiceorselected")
    public Page<DiscordUser> getInvoiceOrSelected(@RequestParam(defaultValue = "0") int page,
                                    @RequestParam(defaultValue = "200") int size) {
        //Need to call this to refresh the list of users.
        discordUserService.updateUsersInDb();
        return discordUserService.findByInVoiceIsTrueOrSelectedIsTrue(Pageable.ofSize(size).withPage(page));
    }

    @PatchMapping("/{userId}")
    public ResponseEntity<DiscordUser> updateUserSounds(
            @PathVariable String userId,
            @RequestParam(required = false) String entranceSound,
            @RequestParam(required = false) String leaveSound
    ) {
        // Update the user's entrance and leave sounds in your database
        try {
            DiscordUser user = discordUserService.updateSounds(userId, entranceSound, leaveSound);
            return ResponseEntity.ok(user);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/invoiceorselected/stream")
    public SseEmitter streamInvoiceOrSelected() {
        //Need to call this to refresh the list of users.
        discordUserService.updateUsersInDb();

        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);

        // Add emitter to the list
        emitters.add(emitter);

        // Remove emitter when completed or timed out
        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitters.remove(emitter));
        emitter.onError((e) -> emitters.remove(emitter));

        // Send initial data immediately
        try {
            Page<DiscordUser> discordUsers = discordUserService.findByInVoiceIsTrueOrSelectedIsTrue(
                    Pageable.ofSize(200).withPage(0));
            emitter.send(SseEmitter.event()
                    .name("discordUsers")
                    .data(discordUsers));
        } catch (IOException e) {
            emitter.completeWithError(e);
        }

        return emitter;
    }

    // Helper method to broadcast updates to all connected clients
    public void broadcastUpdate() {
        discordUserService.updateUsersInDb();

        List<SseEmitter> deadEmitters = new CopyOnWriteArrayList<>();
        Page<DiscordUser> discordUsers = discordUserService.findByInVoiceIsTrueOrSelectedIsTrue(
                Pageable.ofSize(200).withPage(0));
        emitters.forEach(emitter -> {
            try {
                emitter.send(SseEmitter.event()
                        .name("discordUsers")
                        .data(discordUsers));
            } catch (IOException e) {
                deadEmitters.add(emitter);
            }
        });

        // Remove dead emitters
        emitters.removeAll(deadEmitters);
    }
}
