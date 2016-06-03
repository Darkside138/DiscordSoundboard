package net.dirtydeeds.discordsoundboard.web;

import net.dirtydeeds.discordsoundboard.SoundPlaybackException;
import net.dirtydeeds.discordsoundboard.beans.SoundFile;
import net.dirtydeeds.discordsoundboard.beans.User;
import net.dirtydeeds.discordsoundboard.service.SoundPlayerImpl;
import net.dirtydeeds.discordsoundboard.util.SortIgnoreCase;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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

    @RequestMapping("/getAvailableSounds")
    public List<SoundFile> getSoundFileList() {
        Map<String, SoundFile> soundMap = soundPlayer.getAvailableSoundFiles();
        List<SoundFile> returnSounds = soundMap.entrySet().stream().map(Map.Entry::getValue).collect(Collectors.toCollection(LinkedList::new));
        Collections.sort(returnSounds, new SortIgnoreCase());
        return returnSounds;
    }
    
    @RequestMapping("/getSoundCategories")
    public Set<String> getSoundCategories() {
        Set<String> categories = new HashSet<>();
        Map<String, SoundFile> soundMap = soundPlayer.getAvailableSoundFiles();
        categories.addAll(soundMap.entrySet().stream().map(entry -> entry.getValue().getCategory()).collect(Collectors.toList()));
        return categories;
    }

    @RequestMapping("/getUsers")
    public List<User> getUsers() {
        return soundPlayer.getUsers();
    }
    
    @RequestMapping("/playFile")
    public HttpStatus playSoundFile(@RequestParam String soundFileId, @RequestParam String username) {
        try {
            soundPlayer.playFileForUser(soundFileId, username);
            return HttpStatus.OK;
        } catch (SoundPlaybackException e) {
            return HttpStatus.NOT_FOUND;
        }
    }

    @RequestMapping("/stop")
    public HttpStatus stopPlayback() {
        soundPlayer.stop();
        return HttpStatus.OK;
    }
    
    @RequestMapping(value = "/setVolume", method = RequestMethod.POST)
    public HttpStatus setVolume(@RequestParam Integer volume) {
        soundPlayer.setSoundPlayerVolume(volume);
        return HttpStatus.OK;
    }
}
