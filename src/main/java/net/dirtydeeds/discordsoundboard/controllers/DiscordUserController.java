package net.dirtydeeds.discordsoundboard.controllers;

import io.swagger.v3.oas.annotations.Hidden;
import jakarta.annotation.PreDestroy;
import net.dirtydeeds.discordsoundboard.beans.DiscordUser;
import net.dirtydeeds.discordsoundboard.service.DiscordUserService;
import net.dirtydeeds.discordsoundboard.util.UserRoleConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Hidden
@RestController
@RequestMapping("/api/discordUsers")
@SuppressWarnings("unused")
public class DiscordUserController {

    @Autowired
    private final UserRoleConfig userRoleConfig;

    private final DiscordUserService discordUserService;
    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();

    private final ScheduledExecutorService sseHeartbeatExecutor =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "sse-heartbeat");
                t.setDaemon(true);
                return t;
            });

    @Autowired
    public DiscordUserController(DiscordUserService discordUserService, UserRoleConfig userRoleConfig) {
        this.discordUserService = discordUserService;
        this.userRoleConfig = userRoleConfig;

        // Send a heartbeat every 25 seconds (tweak as needed)
        sseHeartbeatExecutor.scheduleAtFixedRate(
                this::broadcastHeartbeatSafely,
                90, 90, TimeUnit.SECONDS
        );
    }

    @GetMapping()
    public Page<DiscordUser> getAll(@RequestParam(defaultValue = "1") int page,
                                    @RequestParam(defaultValue = "20") int size,
                                    @RequestParam(defaultValue = "username") String sortBy,
                                    @RequestParam(defaultValue = "asc") String sortDir) {
        //Need to call this to refresh the list of users.
        discordUserService.updateUsersInDb();
        Sort.Order sortOrder = Sort.Order.asc(sortBy);
        if (sortDir.equalsIgnoreCase("desc")) {
            sortOrder = Sort.Order.desc(sortBy);
        }
        return discordUserService.findAll(PageRequest.of(page,size, Sort.by(sortOrder, Sort.Order.asc("username"))));
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
            @RequestParam(required = false) String leaveSound,
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {

        String authId = userRoleConfig.getUserIdFromAuth(authorization);
        if (userId == null || !userRoleConfig.hasPermission(userId, "manage-users")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        // Update the user's entrance and leave sounds in your database
        try {
            DiscordUser user = discordUserService.updateSounds(userId, entranceSound, leaveSound);

            broadcastUpdate();

            return ResponseEntity.ok(user);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping(value = "/invoiceorselected/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
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
            emitter.complete();
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
                emitter.complete();
            }
        });

        // Remove dead emitters
        emitters.removeAll(deadEmitters);
    }

    private void broadcastHeartbeatSafely() {
        if (emitters.isEmpty()) return;

        List<SseEmitter> deadEmitters = new CopyOnWriteArrayList<>();
        emitters.forEach(emitter -> {
            try {
                // Keep the payload tiny; event name can be anything
                emitter.send(SseEmitter.event()
                        .name("heartbeat")
                        .data("ping"));
            } catch (IOException ex) {
                deadEmitters.add(emitter);
                emitter.complete();
            } catch (IllegalStateException ex) {
                // Can happen if emitter is already completed
                deadEmitters.add(emitter);
            }
        });

        emitters.removeAll(deadEmitters);
    }

    @PreDestroy
    public void shutdownHeartbeat() {
        sseHeartbeatExecutor.shutdownNow();
    }
}
