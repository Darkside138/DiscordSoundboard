package net.dirtydeeds.discordsoundboard.web;

import net.dirtydeeds.discordsoundboard.beans.SoundFile;
import net.dirtydeeds.discordsoundboard.beans.User;
import net.dirtydeeds.discordsoundboard.service.SoundPlayerImpl;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author dfurrer.
 */
@RestController
@RequestMapping("/soundsApi")
public class SoundboardRestController {
    
    SoundPlayerImpl soundPlayer;

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
        return soundMap.entrySet().stream().map(Map.Entry::getValue).collect(Collectors.toCollection(LinkedList::new));
    }
    
    @RequestMapping("/getSoundCategories")
    public Set<String> getSoundCategories() {
        Set<String> categories = new HashSet<>();
        Map<String, SoundFile> soundMap = soundPlayer.getAvailableSoundFiles();
        for (Map.Entry<String, SoundFile> entry : soundMap.entrySet()) {
            categories.add(entry.getValue().getCategory());
        }
        return categories;
    }

    @RequestMapping("/getUsers")
    public List<User> getUsers() {
        return soundPlayer.getUsers();
    }
    
    @RequestMapping("/playFile")
    public HttpStatus playSoundFile(@RequestParam String soundFileId, @RequestParam String username) {
        soundPlayer.playFileForUser(soundFileId, username);
        return HttpStatus.OK;
    }
    
    @RequestMapping(value = "/setVolume", method = RequestMethod.POST)
    public HttpStatus setVolume(@RequestParam Integer volume) {
        soundPlayer.setSoundPlayerVolume(volume);
        return HttpStatus.OK;
    }
}
