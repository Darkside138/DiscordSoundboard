package net.dirtydeeds.discordsoundboard.controllers;

import net.dirtydeeds.discordsoundboard.beans.SoundFile;
import net.dirtydeeds.discordsoundboard.SoundPlayer;
import net.dirtydeeds.discordsoundboard.service.SoundService;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.inject.Inject;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * REST Controller.
 *
 * @author dfurrer.
 */
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
    public ResponseEntity<Iterable<SoundFile>> getAll() {
        Pageable wholePage = Pageable.unpaged();
        return new ResponseEntity<>(soundService.findAll(wholePage), HttpStatus.OK);
    }

    @GetMapping(value = "/categories")
    public ResponseEntity<Set<String>> getSoundCategories() {
        Map<String, SoundFile> soundMap = soundPlayer.getAvailableSoundFiles();
        return new ResponseEntity<>(soundMap.values().stream()
                .map(SoundFile::getCategory)
                .collect(Collectors.toSet()), HttpStatus.OK);
    }
}
