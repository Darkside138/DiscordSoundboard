package net.dirtydeeds.discordsoundboard.controllers;

import net.dirtydeeds.discordsoundboard.SoundPlaybackException;
import net.dirtydeeds.discordsoundboard.SoundPlayer;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import javax.inject.Inject;

@RestController
@RequestMapping("/bot")
@SuppressWarnings("unused")
public class BotCommandController {

    private final SoundPlayer soundPlayer;

    @Inject
    public BotCommandController(SoundPlayer soundPlayer) {
        this.soundPlayer = soundPlayer;
    }

    @PostMapping(value = "/playFile")
    public HttpStatus playSoundFile(@RequestParam String soundFileId, @RequestParam String username, @RequestParam(defaultValue = "1") Integer repeatTimes) {
        soundPlayer.playFileForUser(soundFileId, username, repeatTimes);
        return HttpStatus.OK;
    }

    @PostMapping(value = "/playUrl")
    public HttpStatus playSoundUrl(@RequestParam String url, @RequestParam String username) {
        soundPlayer.playUrlForUser(url, username);
        return HttpStatus.OK;
    }

    @PostMapping(value = "/playRandom")
    public HttpStatus playRandomSoundFile(@RequestParam String username) {
        try {
            soundPlayer.playRandomSoundFile(username, null);
        } catch (SoundPlaybackException e) {
            return HttpStatus.INTERNAL_SERVER_ERROR;
        }
        return HttpStatus.OK;
    }

    @PostMapping(value = "/random")
    public HttpStatus soundCommand(@RequestParam String username) {
        try {
            soundPlayer.playRandomSoundFile(username, null);
        } catch (SoundPlaybackException e) {
            return HttpStatus.INTERNAL_SERVER_ERROR;
        }
        return HttpStatus.OK;
    }

    @PostMapping(value = "/stop")
    public HttpStatus stopPlayback(@RequestParam String username) {
        soundPlayer.stop(username);
        return HttpStatus.OK;
    }

    @PostMapping(value = "/volume")
    public HttpStatus setVolume(@RequestParam Integer volume, @RequestParam String username) {
        soundPlayer.setSoundPlayerVolume(volume, username);
        return HttpStatus.OK;
    }

    @GetMapping(value = "/volume")
    public float getVolume(@RequestParam String username) {
        return soundPlayer.getSoundPlayerVolume(username);
    }
}
