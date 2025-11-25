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

import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Set;
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

    private final SoundPlayer soundPlayer;
    private final SoundService soundService;

    @Inject
    public SoundController (SoundPlayer soundPlayer, SoundService soundService) {
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
    public HttpStatus setFavorite(@PathVariable String soundId, @RequestParam(defaultValue = "false") Boolean favorite) {
        SoundFile soundFile = soundService.findOneBySoundFileIdIgnoreCase(soundId);

        soundFile.setFavorite(favorite);

        soundService.save(soundFile);

        return HttpStatus.OK;
    }

    @PatchMapping(value = "/{soundId}")
    public HttpStatus patchSoundFile(@PathVariable String soundId, @RequestParam(defaultValue = "0") Integer volumeOffsetPercentage) {

        SoundFile soundFile = soundService.findOneBySoundFileIdIgnoreCase(soundId);

        soundFile.setVolumeOffsetPercentage(volumeOffsetPercentage);

        soundService.save(soundFile);

        return HttpStatus.OK;
    }
}
