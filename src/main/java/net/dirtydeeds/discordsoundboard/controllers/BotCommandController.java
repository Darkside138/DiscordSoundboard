package net.dirtydeeds.discordsoundboard.controllers;

import io.swagger.v3.oas.annotations.Hidden;
import jakarta.inject.Inject;
import net.dirtydeeds.discordsoundboard.SoundPlaybackException;
import net.dirtydeeds.discordsoundboard.SoundPlayer;
import net.dirtydeeds.discordsoundboard.controllers.response.ChannelResponse;
import net.dirtydeeds.discordsoundboard.service.PlaybackService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Hidden
@RestController
@RequestMapping("/bot")
@SuppressWarnings("unused")
public class BotCommandController {

    private final SoundPlayer soundPlayer;
    private final SoundController soundController;
    private final BotVolumeController botVolumeController;
    private final PlaybackService playbackService;

    @Autowired
    public BotCommandController(SoundPlayer soundPlayer, SoundController soundController,
                                BotVolumeController botVolumeController, PlaybackService playbackService) {
        this.soundPlayer = soundPlayer;
        this.soundController = soundController;
        this.botVolumeController = botVolumeController;
        this.playbackService = playbackService;
    }

    @PostMapping(value = "/playFile")
    public ResponseEntity<Void> playSoundFile(@RequestParam String soundFileId, @RequestParam String username,
                                                @RequestParam(defaultValue = "1") Integer repeatTimes,
                                                @RequestParam(defaultValue = "") String voiceChannelId) {
        soundPlayer.playForUser(soundFileId, username, repeatTimes, voiceChannelId);
        playbackService.sendTrackStart(soundFileId);
        soundController.broadcastUpdate();
        return ResponseEntity.ok().build();
    }

    @PostMapping(value = "/playUrl")
    public ResponseEntity<Void> playSoundUrl(@RequestParam String url, @RequestParam String username,
                                   @RequestParam(defaultValue = "") String voiceChannelId) {
        soundPlayer.playForUser(url, username, 1, voiceChannelId);
        return ResponseEntity.ok().build();
    }

    @PostMapping(value = "/random")
    public ResponseEntity<Void> soundCommand(@RequestParam String username,
                                   @RequestParam(defaultValue = "") String voiceChannelId) {
        try {
            soundPlayer.playRandomSoundFile(username, null);
//            if (currentSoundId != null) {
//                playbackService.sendTrackEnd(currentSoundId);
//            }
            soundController.broadcastUpdate();
        } catch (SoundPlaybackException e) {
            return ResponseEntity.internalServerError().build();
        }
        return ResponseEntity.ok().build();
    }

    @PostMapping(value = "/stop")
    public ResponseEntity<Void> stopPlayback(@RequestParam String username,
                                   @RequestParam(defaultValue = "") String voiceChannelId) {
        soundPlayer.stop(username, voiceChannelId);

//        if (currentSoundId != null) {
//            playbackService.sendTrackEnd(currentSoundId);
//        }

        return ResponseEntity.ok().build();
    }

    @PostMapping(value = "/volume")
    public ResponseEntity<Void> setVolume(@RequestParam Integer volume, @RequestParam String username,
                                @RequestParam(defaultValue = "") String voiceChannelId) {
        soundPlayer.setGlobalVolume(volume, username, null);
        botVolumeController.broadcastUpdate(username);
        return ResponseEntity.ok().build();
    }

    @GetMapping(value = "/volume")
    public float getVolume(@RequestParam String username, @RequestParam(defaultValue = "") String voiceChannelId) {
        return soundPlayer.getGlobalVolume(username, voiceChannelId);
    }

    @GetMapping(value = "/channels")
    public List<ChannelResponse> getVoiceChannels() {
        return soundPlayer.getVoiceChannels();
    }
}
