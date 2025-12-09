package net.dirtydeeds.discordsoundboard.controllers;

import io.swagger.v3.oas.annotations.Hidden;
import jakarta.inject.Inject;
import net.dirtydeeds.discordsoundboard.beans.SoundFile;
import net.dirtydeeds.discordsoundboard.SoundPlayer;
import net.dirtydeeds.discordsoundboard.service.SoundService;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

/**
 * REST Controller.
 *
 * @author dfurrer.
 */
@Hidden
@RestController
@RequestMapping("/api/soundFiles")
@SuppressWarnings("unused")
public class SoundController {

    private SoundPlayer soundPlayer;
    private final SoundService soundService;

    @Inject
    public SoundController (SoundService soundService) {
        this.soundPlayer = soundPlayer;
        this.soundService = soundService;
    }

    @GetMapping("/findAll")
    public Page<SoundFile> getAll() {
        Pageable wholePage = Pageable.unpaged();
        return soundService.findAll(wholePage);
    }

    @GetMapping(value = "/categories")
    public Set<String> getSoundCategories() {
        Map<String, SoundFile> soundMap = soundPlayer.getAvailableSoundFiles();
        return soundMap.values().stream()
                .map(SoundFile::getCategory)
                .collect(Collectors.toSet());
    }

    @GetMapping("/download/{soundId}")
    public ResponseEntity<Resource> downloadFile(@PathVariable String soundId) {
        try {
            SoundFile soundFile = soundService.findOneBySoundFileIdIgnoreCase(soundId);

            Path filePath = Paths.get("files").resolve(soundFile.getSoundFileLocation()).normalize();
            Resource resource = new UrlResource(filePath.toUri());

            if (!resource.exists()) {
                return ResponseEntity.notFound().build();
            }

            // Set content type dynamically
            String contentType = "application/octet-stream";

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
                    .body(resource);

        } catch (MalformedURLException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping(value = "/favorite/{soundId}")
    public ResponseEntity<Void> setFavorite(@PathVariable String soundId, @RequestParam(defaultValue = "false") Boolean favorite) {
        SoundFile soundFile = soundService.findOneBySoundFileIdIgnoreCase(soundId);

        soundFile.setFavorite(favorite);

        soundService.save(soundFile);

        broadcastUpdate();

        return ResponseEntity.ok().build();
    }

    @PatchMapping(value = "/{soundId}")
    public ResponseEntity<Void> patchSoundFile(@PathVariable String soundId, @RequestParam(defaultValue = "0") Integer volumeOffsetPercentage, @RequestParam String displayName) {

        SoundFile soundFile = soundService.findOneBySoundFileIdIgnoreCase(soundId);

        soundFile.setVolumeOffsetPercentage(volumeOffsetPercentage);
        if (displayName != null) {
            soundFile.setDisplayName(displayName);
        }

        soundService.save(soundFile);

        broadcastUpdate();

        return ResponseEntity.ok().build();
    }

    // Store all active SSE connections
    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();

    // SSE endpoint for real-time updates
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamSounds() {
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);

        // Add emitter to the list
        emitters.add(emitter);

        // Remove emitter when completed or timed out
        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitters.remove(emitter));
        emitter.onError((e) -> emitters.remove(emitter));

        // Send initial data immediately
        try {
            Page<SoundFile> sounds = soundService.findAll(Pageable.unpaged());
            emitter.send(SseEmitter.event()
                    .name("sounds")
                    .data(sounds));
        } catch (IOException e) {
            emitter.completeWithError(e);
        }

        return emitter;
    }

    // Helper method to broadcast updates to all connected clients
    public void broadcastUpdate() {
        Page<SoundFile> sounds = soundService.findAll(Pageable.unpaged());

        List<SseEmitter> deadEmitters = new CopyOnWriteArrayList<>();
        emitters.forEach(emitter -> {
            try {
                emitter.send(SseEmitter.event()
                        .name("sounds")
                        .data(sounds));
            } catch (IOException e) {
                deadEmitters.add(emitter);
            }
        });

        // Remove dead emitters
        emitters.removeAll(deadEmitters);
    }

    @PostMapping("/upload")
    public ResponseEntity<String> uploadFile(@RequestParam("file") MultipartFile file) {
        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body("File is empty");
            }

            String originalFilename = file.getOriginalFilename();
            String uploadDir = soundPlayer.getSoundsDirectory();

            // Save the file
            String filePath = uploadDir + "/" + originalFilename;
            file.transferTo(new File(filePath));

            soundService.save(new SoundFile(originalFilename, filePath, "", 0, ZonedDateTime.now(), false, null, 0));

            broadcastUpdate();

            return ResponseEntity.ok("File uploaded successfully: " + originalFilename);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to upload file: " + e.getMessage());
        }
    }

    public void setSoundPlayer(SoundPlayer soundPlayer) {
        this.soundPlayer = soundPlayer;
    }
}
