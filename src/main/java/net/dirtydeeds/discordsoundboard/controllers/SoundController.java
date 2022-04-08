package net.dirtydeeds.discordsoundboard.controllers;

import net.dirtydeeds.discordsoundboard.beans.SoundFile;
import net.dirtydeeds.discordsoundboard.SoundPlayer;
import net.dirtydeeds.discordsoundboard.util.SortIgnoreCase;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.inject.Inject;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * REST Controller.
 *
 * @author dfurrer.
 */
@RestController
@RequestMapping("/sounds")
@SuppressWarnings("unused")
public class SoundController {

    private final SoundPlayer soundPlayer;

    @Inject
    public SoundController (SoundPlayer soundPlayer) {
        this.soundPlayer = soundPlayer;
    }

    @GetMapping()
    public List<SoundFile> getSoundFileList() {
        Map<String, SoundFile> soundMap = soundPlayer.getAvailableSoundFiles();
        return soundMap.values().stream().sorted(new SortIgnoreCase()).collect(Collectors.toCollection(LinkedList::new));
    }

    @GetMapping(value = "/categories")
    public Set<String> getSoundCategories() {
        Map<String, SoundFile> soundMap = soundPlayer.getAvailableSoundFiles();
        return soundMap.values().stream().map(SoundFile::getCategory).collect(Collectors.toSet());
    }
}
