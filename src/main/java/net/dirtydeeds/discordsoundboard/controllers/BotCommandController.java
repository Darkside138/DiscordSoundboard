package net.dirtydeeds.discordsoundboard.controllers;

import io.swagger.v3.oas.annotations.Hidden;
import net.dirtydeeds.discordsoundboard.SoundPlaybackException;
import net.dirtydeeds.discordsoundboard.SoundPlayer;
import net.dirtydeeds.discordsoundboard.beans.SoundFile;
import net.dirtydeeds.discordsoundboard.controllers.response.ChannelResponse;
import net.dirtydeeds.discordsoundboard.service.DiscordUserService;
import net.dirtydeeds.discordsoundboard.util.UserRoleConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Hidden
@RestController
@RequestMapping("/bot")
@SuppressWarnings("unused")
public class BotCommandController {

    private final SoundPlayer soundPlayer;
    private final BotVolumeController botVolumeController;
    private final UserRoleConfig userRoleConfig;
    private final DiscordUserService discordUserService;

    @Autowired
    public BotCommandController(SoundPlayer soundPlayer,
                                BotVolumeController botVolumeController,
                                UserRoleConfig userRoleConfig,
                                DiscordUserService discordUserService) {
        this.soundPlayer = soundPlayer;
        this.botVolumeController = botVolumeController;
        this.userRoleConfig = userRoleConfig;
        this.discordUserService = discordUserService;
    }

    @PostMapping(value = "/playFile")
    public ResponseEntity<Void> playSoundFile(@RequestParam String soundFileId,
                            @RequestParam String username,
                            @RequestParam(defaultValue = "1") Integer repeatTimes,
                            @RequestParam(defaultValue = "") String voiceChannelId,
                            @RequestHeader(value = "Authorization", required = false) String authorization) {
        String requestingUser = "anonymous";
        if (authorization != null) {
            String requestingUserId = userRoleConfig.getUserIdFromAuth(authorization);
            requestingUser = discordUserService.findOneByIdOrUsernameIgnoreCase(requestingUserId, requestingUserId).getUsername();
        }
        soundPlayer.playForUser(soundFileId, username, repeatTimes, voiceChannelId, requestingUser);
        return ResponseEntity.ok().build();
    }

    @PostMapping(value = "/playUrl")
    public ResponseEntity<Void> playSoundUrl(@RequestParam String url, @RequestParam String username,
                        @RequestParam(defaultValue = "") String voiceChannelId,
                        @RequestHeader(value = "Authorization", required = false) String authorization) {
        String requestingUser = "anonymous";
        if (authorization != null) {
            String requestingUserId = userRoleConfig.getUserIdFromAuth(authorization);
            requestingUser = discordUserService.findOneByIdOrUsernameIgnoreCase(requestingUserId, requestingUserId).getUsername();
        }
        soundPlayer.playForUser(url, username, 1, voiceChannelId, requestingUser);
        return ResponseEntity.ok().build();
    }

    @PostMapping(value = "/random")
    public ResponseEntity<Void> soundCommand(@RequestParam String username,
                                @RequestParam(defaultValue = "") String voiceChannelId,
                                @RequestHeader(value = "Authorization", required = false) String authorization) {
        try {
            String requestingUser = "anonymous";
            if (authorization != null) {
                String requestingUserId = userRoleConfig.getUserIdFromAuth(authorization);
                requestingUser = discordUserService.findOneByIdOrUsernameIgnoreCase(requestingUserId, requestingUserId).getUsername();
            }
            SoundFile soundFile = soundPlayer.playRandomSoundFile(username, null, requestingUser);
        } catch (SoundPlaybackException e) {
            return ResponseEntity.internalServerError().build();
        }
        return ResponseEntity.ok().build();
    }

    @PostMapping(value = "/stop")
    public ResponseEntity<Void> stopPlayback(@RequestParam String username,
                                   @RequestParam(defaultValue = "") String voiceChannelId) {
        String soundFileId = soundPlayer.stop(username, voiceChannelId);

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

    @GetMapping(value = "/version")
    public String getVersion() {
        return soundPlayer.getVersion();
    }
}
