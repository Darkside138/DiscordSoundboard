package net.dirtydeeds.discordsoundboard.controllers;

import net.dirtydeeds.discordsoundboard.SoundPlaybackException;
import net.dirtydeeds.discordsoundboard.SoundPlayer;
import net.dirtydeeds.discordsoundboard.controllers.response.ChannelResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import javax.inject.Inject;
import java.util.List;

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
    public HttpStatus playSoundFile(@RequestParam String soundFileId, @RequestParam String username,
                                    @RequestParam(defaultValue = "1") Integer repeatTimes,
                                    @RequestParam(defaultValue = "") String voiceChannelId) {
        soundPlayer.playForUser(soundFileId, username, repeatTimes, voiceChannelId);
        return HttpStatus.OK;
    }

    @PostMapping(value = "/playUrl")
    public HttpStatus playSoundUrl(@RequestParam String url, @RequestParam String username,
                                   @RequestParam(defaultValue = "") String voiceChannelId) {
        soundPlayer.playForUser(url, username, 1, voiceChannelId);
        return HttpStatus.OK;
    }

    @PostMapping(value = "/random")
    public HttpStatus soundCommand(@RequestParam String username,
                                   @RequestParam(defaultValue = "") String voiceChannelId) {
        try {
            soundPlayer.playRandomSoundFile(username, null);
        } catch (SoundPlaybackException e) {
            return HttpStatus.INTERNAL_SERVER_ERROR;
        }
        return HttpStatus.OK;
    }

    @PostMapping(value = "/stop")
    public HttpStatus stopPlayback(@RequestParam String username,
                                   @RequestParam(defaultValue = "") String voiceChannelId) {
        soundPlayer.stop(username, voiceChannelId);
        return HttpStatus.OK;
    }

    @PostMapping(value = "/volume")
    public HttpStatus setVolume(@RequestParam Integer volume, @RequestParam String username,
                                @RequestParam(defaultValue = "") String voiceChannelId) {
        soundPlayer.setSoundPlayerVolume(volume, username, null);
        return HttpStatus.OK;
    }

    @GetMapping(value = "/volume")
    public float getVolume(@RequestParam String username, @RequestParam(defaultValue = "") String voiceChannelId) {
        return soundPlayer.getSoundPlayerVolume(username, voiceChannelId);
    }

    @GetMapping(value = "/channels")
    public List<ChannelResponse> getVoiceChannels() {
        return soundPlayer.getVoiceChannels();
    }
}
