package net.dirtydeeds.discordsoundboard.controllers;

import io.swagger.v3.oas.annotations.Hidden;
import jakarta.inject.Inject;
import net.dirtydeeds.discordsoundboard.beans.SoundFile;
import net.dirtydeeds.discordsoundboard.SoundPlayer;
import net.dirtydeeds.discordsoundboard.service.SoundService;
import net.dirtydeeds.discordsoundboard.util.UserRoleConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.ZonedDateTime;
import java.util.*;
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

    @Autowired
    private final UserRoleConfig userRoleConfig;
    private SoundPlayer soundPlayer;
    private final SoundService soundService;

    // Allowed MIME types for sound files
    private static final Set<String> ALLOWED_MIME_TYPES = new HashSet<>(Arrays.asList(
            "audio/mpeg",   // mp3
            "audio/wav",    // wav
            "audio/ogg",    // ogg
            "audio/x-wav",
            "audio/x-m4a",
            "audio/mp4"
    ));

    // Allowed file extensions
    private static final Set<String> ALLOWED_EXTENSIONS = new HashSet<>(Arrays.asList(
            "mp3", "wav", "ogg", "m4a"
    ));

    // Max file size (10 MB)
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024;

    @Inject
    public SoundController (SoundService soundService, UserRoleConfig userRoleConfig) {
        this.soundService = soundService;
        this.userRoleConfig = userRoleConfig;
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

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteSoundFile(
            @PathVariable String id,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        try {

            String userId = userRoleConfig.getUserIdFromAuth(authorization);
            if (userId == null || !userRoleConfig.hasPermission(userId, "delete-sounds")) {
                return ResponseEntity.status(403).body("You don't have permission to delete sounds");
            }

            // Delete the sound file
            try {
                soundService.delete(soundService.findOneBySoundFileIdIgnoreCase(id));

                return ResponseEntity.ok()
                        .body(Map.of("message", "Sound file deleted successfully", "id", id));
            } catch (Exception e) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("Sound file not found with ID: " + id);
            }

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error deleting sound file: " + e.getMessage());
        }
    }

    @GetMapping(value = "/download/{soundId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
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
    public ResponseEntity<Void> patchSoundFile(
            @PathVariable String soundId,
            @RequestParam(defaultValue = "0") Integer volumeOffsetPercentage,
            @RequestParam String displayName,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        String userId = userRoleConfig.getUserIdFromAuth(authorization);
        if (userId == null || !userRoleConfig.hasPermission(userId, "edit-sounds")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        SoundFile soundFile = soundService.findOneBySoundFileIdIgnoreCase(soundId);

        soundFile.setVolumeOffsetPercentage(volumeOffsetPercentage);
        if (displayName != null) {
            soundFile.setDisplayName(displayName);
        }

        soundService.save(soundFile);

        broadcastUpdate();

        return ResponseEntity.ok().build();
    }

    @GetMapping(value = "/{soundFileId}", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity<Resource> getAudioFile(@PathVariable String soundFileId) throws IOException {
        SoundFile soundFile = soundService.findOneBySoundFileIdIgnoreCase(soundFileId);
        Path filePath = Paths.get(soundFile.getSoundFileLocation());
        Resource resource = new UrlResource(filePath.toUri());

        if (resource.exists() || resource.isReadable()) {
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
                    .contentType(MediaType.parseMediaType(Files.probeContentType(filePath)))
                    .body(resource);
        } else {
            return ResponseEntity.notFound().build();
        }
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
    public ResponseEntity<String> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        try {

            String userId = userRoleConfig.getUserIdFromAuth(authorization);
            if (userId == null || !userRoleConfig.hasPermission(userId, "upload")) {
                return ResponseEntity.status(403).body("You don't have permission to upload sounds");
            }

            if (file.isEmpty() || file.getOriginalFilename() == null) {
                return ResponseEntity.badRequest().body("File is empty");
            }

            // 2. Check file size
            if (file.getSize() > MAX_FILE_SIZE) {
                return ResponseEntity.badRequest().body("File size exceeds 10 MB limit.");
            }

            // 3. Validate extension
            String filename = StringUtils.cleanPath(file.getOriginalFilename());
            String extension = getFileExtension(filename);
            if (!ALLOWED_EXTENSIONS.contains(extension.toLowerCase())) {
                return ResponseEntity.badRequest().body("Invalid file extension. Allowed: " + ALLOWED_EXTENSIONS);
            }

            // 4. Validate MIME type
            String mimeType = file.getContentType();
            if (mimeType == null || !ALLOWED_MIME_TYPES.contains(mimeType.toLowerCase())) {
                return ResponseEntity.badRequest().body("Invalid file type. Allowed: " + ALLOWED_MIME_TYPES);
            }

            // 5. (Optional) Deep validation: check magic bytes
            if (!isAudioFile(file)) {
                return ResponseEntity.badRequest().body("File content is not a valid audio file.");
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

    private String getFileExtension(String filename) {
        int dotIndex = filename.lastIndexOf('.');
        return (dotIndex >= 0) ? filename.substring(dotIndex + 1) : "";
    }

    // Basic magic byte check for audio files
    private boolean isAudioFile(MultipartFile file) throws IOException {
        byte[] header = new byte[12];
        int bytesRead = file.getInputStream().read(header, 0, header.length);
        if (bytesRead < 4) return false;

        // MP3: starts with ID3 or 0xFFFB
        if ((header[0] == 'I' && header[1] == 'D' && header[2] == '3') ||
                ((header[0] & 0xFF) == 0xFF && (header[1] & 0xE0) == 0xE0)) {
            return true;
        }

        // WAV: starts with "RIFF" and "WAVE"
        if (header[0] == 'R' && header[1] == 'I' && header[2] == 'F' && header[3] == 'F') {
            return true;
        }

        // OGG: starts with "OggS"
        return header[0] == 'O' && header[1] == 'g' && header[2] == 'g' && header[3] == 'S';
    }
}
