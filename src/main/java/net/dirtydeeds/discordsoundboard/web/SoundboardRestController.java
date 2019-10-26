package net.dirtydeeds.discordsoundboard.web;

import net.dirtydeeds.discordsoundboard.SoundPlaybackException;
import net.dirtydeeds.discordsoundboard.beans.SoundFile;
import net.dirtydeeds.discordsoundboard.beans.User;
import net.dirtydeeds.discordsoundboard.service.SoundPlayerImpl;
import net.dirtydeeds.discordsoundboard.util.SortIgnoreCase;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import javax.inject.Inject;
import java.util.*;
import java.util.stream.Collectors;

/**
 * REST Controller.
 *
 * @author dfurrer.
 */
@RestController
@RequestMapping("/soundsApi")
@SuppressWarnings("unused")
public class SoundboardRestController {
    
    private SoundPlayerImpl soundPlayer;

    @SuppressWarnings("unused") //Damn spring and it's need for empty constructors
    public SoundboardRestController() {
    }

    @Inject
    public SoundboardRestController(final SoundPlayerImpl soundPlayer) {
        this.soundPlayer = soundPlayer;
    }

    @GetMapping(value = "/availableSounds")
    @Deprecated
    public List<SoundFile> getSoundFileList() {
        Map<String, SoundFile> soundMap = soundPlayer.getAvailableSoundFiles();
        return soundMap.values().stream().sorted(new SortIgnoreCase()).collect(Collectors.toCollection(LinkedList::new));
    }
    
    @GetMapping(value = "/soundCategories")
    @Deprecated
    public Set<String> getSoundCategories() {
        Map<String, SoundFile> soundMap = soundPlayer.getAvailableSoundFiles();
        return soundMap.values().stream().map(SoundFile::getCategory).collect(Collectors.toSet());
    }

    @GetMapping(value = "/users")
    public List<User> getUsers() {
        return soundPlayer.getUsers();
    }
    
    @PostMapping(value = "/playFile")
    public HttpStatus playSoundFile(@RequestParam String soundFileId, @RequestParam String username) {
        soundPlayer.playFileForUser(soundFileId, username);
        return HttpStatus.OK;
    }

    @PostMapping(value = "/playUrl")
    public HttpStatus playSoundUrl(@RequestParam String url, @RequestParam String username) {
            soundPlayer.playUrlForUser(url, username);
            return HttpStatus.OK;
    }
    
    @PostMapping(value = "/playRandom")
    @Deprecated
    public HttpStatus playRandomSoundFile(@RequestParam String username) {
        try {
            soundPlayer.playRandomSoundFile(username, null);
        } catch (SoundPlaybackException e) {
            return HttpStatus.INTERNAL_SERVER_ERROR;
        }
        return HttpStatus.OK;
    }

    @GetMapping(value = "/sounds")
    public List<SoundFile> getSoundFileListNew() {
        Map<String, SoundFile> soundMap = soundPlayer.getAvailableSoundFiles();
        return soundMap.values().stream().sorted(new SortIgnoreCase()).collect(Collectors.toCollection(LinkedList::new));
    }

    @PostMapping(value = "/sounds")
    public HttpStatus soundCommand(@RequestParam String username, @RequestParam String command) {
        if ("random".equals(command)) {
            try {
                soundPlayer.playRandomSoundFile(username, null);
            } catch (SoundPlaybackException e) {
                return HttpStatus.INTERNAL_SERVER_ERROR;
            }
            return HttpStatus.OK;
        }
        return HttpStatus.NOT_IMPLEMENTED;
    }

    @GetMapping(value = "/sounds/category")
    public Set<String> getSoundCategoriesNew() {
        Map<String, SoundFile> soundMap = soundPlayer.getAvailableSoundFiles();
        return soundMap.values().stream().map(SoundFile::getCategory).collect(Collectors.toSet());
    }

    @PostMapping(value = "/stop")
    public HttpStatus stopPlayback() {
        soundPlayer.stop();
        return HttpStatus.OK;
    }
    
    @PostMapping(value = "/volume")
    public HttpStatus setVolume(@RequestParam Integer volume) {
        soundPlayer.setSoundPlayerVolume(volume);
        return HttpStatus.OK;
    }
    
    @GetMapping(value = "/volume")
    public float getVolume() {
        return soundPlayer.getSoundPlayerVolume();
    }
}
