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

    @RequestMapping(value = "/availableSounds", method = RequestMethod.GET)
    @Deprecated
    public List<SoundFile> getSoundFileList() {
        Map<String, SoundFile> soundMap = soundPlayer.getAvailableSoundFiles();
        List<SoundFile> returnSounds = soundMap.entrySet().stream().map(Map.Entry::getValue).collect(Collectors.toCollection(LinkedList::new));
        Collections.sort(returnSounds, new SortIgnoreCase());
        return returnSounds;
    }
    
    @RequestMapping(value = "/soundCategories", method = RequestMethod.GET)
    @Deprecated
    public Set<String> getSoundCategories() {
        Set<String> categories = new HashSet<>();
        Map<String, SoundFile> soundMap = soundPlayer.getAvailableSoundFiles();
        categories.addAll(soundMap.entrySet().stream().map(entry -> entry.getValue().getCategory()).collect(Collectors.toList()));
        return categories;
    }

    @RequestMapping(value = "/users", method = RequestMethod.GET)
    public List<User> getUsers() {
        return soundPlayer.getUsers();
    }
    
    @RequestMapping(value = "/playFile", method = RequestMethod.POST)
    public HttpStatus playSoundFile(@RequestParam String soundFileId, @RequestParam String username) {
        try {
            soundPlayer.playFileForUser(soundFileId, username);
            return HttpStatus.OK;
        } catch (SoundPlaybackException e) {
            return HttpStatus.NOT_FOUND;
        }
    }

    @RequestMapping(value = "/playUrl", method = RequestMethod.POST)
    public HttpStatus playSoundUrl(@RequestParam String url, @RequestParam String username) {
            soundPlayer.playUrlForUser(url, username);
            return HttpStatus.OK;
    }
    
    @RequestMapping(value = "/playRandom", method = RequestMethod.POST)
    @Deprecated
    public HttpStatus playRandomSoundFile(@RequestParam String username) {
        try {
            soundPlayer.playRandomSoundFile(username, null);
        } catch (SoundPlaybackException e) {
            return HttpStatus.INTERNAL_SERVER_ERROR;
        }
        return HttpStatus.OK;
    }

    @RequestMapping(value = "/sounds", method = RequestMethod.GET)
    public List<SoundFile> getSoundFileListNew() {
        Map<String, SoundFile> soundMap = soundPlayer.getAvailableSoundFiles();
        List<SoundFile> returnSounds = soundMap.entrySet().stream().map(Map.Entry::getValue).collect(Collectors.toCollection(LinkedList::new));
        Collections.sort(returnSounds, new SortIgnoreCase());
        return returnSounds;
    }

    @RequestMapping(value = "/sounds", method = RequestMethod.POST)
    public HttpStatus soundCommand(@RequestParam String username, @RequestParam String command) {
        switch (command) {
            case "random":
                try {
                    soundPlayer.playRandomSoundFile(username, null);
                } catch (SoundPlaybackException e) {
                    return HttpStatus.INTERNAL_SERVER_ERROR;
                }
                return HttpStatus.OK;
            default:
                return HttpStatus.NOT_IMPLEMENTED;
        }
    }

    @RequestMapping(value = "/sounds/category", method = RequestMethod.GET)
    public Set<String> getSoundCategoriesNew() {
        Set<String> categories = new HashSet<>();
        Map<String, SoundFile> soundMap = soundPlayer.getAvailableSoundFiles();
        categories.addAll(soundMap.entrySet().stream().map(entry -> entry.getValue().getCategory()).collect(Collectors.toList()));
        return categories;
    }

    @RequestMapping(value = "/stop", method = RequestMethod.POST)
    public HttpStatus stopPlayback() {
        soundPlayer.stop();
        return HttpStatus.OK;
    }
    
    @RequestMapping(value = "/volume", method = RequestMethod.POST)
    public HttpStatus setVolume(@RequestParam Integer volume) {
        soundPlayer.setSoundPlayerVolume(volume);
        return HttpStatus.OK;
    }
    
    @RequestMapping(value = "/volume", method = RequestMethod.GET) 
    public float getVolume() {
        return soundPlayer.getSoundPlayerVolume();
    }
}
