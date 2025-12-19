package net.dirtydeeds.discordsoundboard.controllers;

import net.dirtydeeds.discordsoundboard.SoundPlaybackException;
import net.dirtydeeds.discordsoundboard.SoundPlayer;
import net.dirtydeeds.discordsoundboard.beans.DiscordUser;
import net.dirtydeeds.discordsoundboard.beans.SoundFile;
import net.dirtydeeds.discordsoundboard.controllers.response.ChannelResponse;
import net.dirtydeeds.discordsoundboard.service.DiscordUserService;
import net.dirtydeeds.discordsoundboard.util.UserRoleConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BotCommandControllerTest {

    @Mock
    private SoundPlayer soundPlayer;

    @Mock
    private BotVolumeController botVolumeController;

    @Mock
    private UserRoleConfig userRoleConfig;

    @Mock
    private DiscordUserService discordUserService;

    private BotCommandController botCommandController;

    private DiscordUser testUser;

    @BeforeEach
    void setUp() {
        botCommandController = new BotCommandController(
                soundPlayer, botVolumeController, userRoleConfig, discordUserService);

        testUser = new DiscordUser();
        testUser.setId("user123");
        testUser.setUsername("testuser");
    }

    @Test
    void playSoundFile_withoutAuthorization_playsSound() {
        // Arrange
        String soundFileId = "sound123";
        String username = "testuser";

        // Act
        ResponseEntity<?> response = botCommandController.playSoundFile(
                soundFileId, username, 1, "", null);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(soundPlayer).playForUser(soundFileId, username, 1, "", "anonymous");
    }

    @Test
    void playSoundFile_withAuthorization_playsSound() {
        // Arrange
        String soundFileId = "sound123";
        String username = "testuser";
        String authorization = "Bearer token";

        when(userRoleConfig.getUserIdFromAuth(authorization)).thenReturn("user123");
        when(userRoleConfig.hasPermission("user123", "play-sounds")).thenReturn(true);
        when(discordUserService.findOneByIdOrUsernameIgnoreCase("user123", "user123")).thenReturn(testUser);

        // Act
        ResponseEntity<?> response = botCommandController.playSoundFile(
                soundFileId, username, 1, "", authorization);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(soundPlayer).playForUser(soundFileId, username, 1, "", "testuser");
    }

    @Test
    void playSoundFile_withoutPermission_returns403() {
        // Arrange
        String authorization = "Bearer token";
        when(userRoleConfig.getUserIdFromAuth(authorization)).thenReturn("user123");
        when(userRoleConfig.hasPermission("user123", "play-sounds")).thenReturn(false);

        // Act
        ResponseEntity<?> response = botCommandController.playSoundFile(
                "sound123", "testuser", 1, "", authorization);

        // Assert
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertEquals("You don't have permission to play sounds", response.getBody());
        verify(soundPlayer, never()).playForUser(anyString(), anyString(), anyInt(), anyString(), anyString());
    }

    @Test
    void playSoundFile_withNullUserId_returns403() {
        // Arrange
        String authorization = "Bearer token";
        when(userRoleConfig.getUserIdFromAuth(authorization)).thenReturn(null);

        // Act
        ResponseEntity<?> response = botCommandController.playSoundFile(
                "sound123", "testuser", 1, "", authorization);

        // Assert
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        verify(soundPlayer, never()).playForUser(anyString(), anyString(), anyInt(), anyString(), anyString());
    }

    @Test
    void playSoundFile_withRepeatTimes_playsMultipleTimes() {
        // Arrange
        String soundFileId = "sound123";
        String username = "testuser";
        int repeatTimes = 3;

        // Act
        ResponseEntity<?> response = botCommandController.playSoundFile(
                soundFileId, username, repeatTimes, "", null);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(soundPlayer).playForUser(soundFileId, username, repeatTimes, "", "anonymous");
    }

    @Test
    void playSoundUrl_withoutAuthorization_playsUrl() {
        // Arrange
        String url = "http://example.com/sound.mp3";
        String username = "testuser";

        // Act
        ResponseEntity<?> response = botCommandController.playSoundUrl(url, username, "", null);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(soundPlayer).playForUser(url, username, 1, "", "anonymous");
    }

    @Test
    void playSoundUrl_withAuthorization_playsUrl() {
        // Arrange
        String url = "http://example.com/sound.mp3";
        String username = "testuser";
        String authorization = "Bearer token";

        when(userRoleConfig.getUserIdFromAuth(authorization)).thenReturn("user123");
        when(userRoleConfig.hasPermission("user123", "play-sounds")).thenReturn(true);
        when(discordUserService.findOneByIdOrUsernameIgnoreCase("user123", "user123")).thenReturn(testUser);

        // Act
        ResponseEntity<?> response = botCommandController.playSoundUrl(url, username, "", authorization);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(soundPlayer).playForUser(url, username, 1, "", "testuser");
    }

    @Test
    void playSoundUrl_withoutPermission_returns403() {
        // Arrange
        String authorization = "Bearer token";
        when(userRoleConfig.getUserIdFromAuth(authorization)).thenReturn("user123");
        when(userRoleConfig.hasPermission("user123", "play-sounds")).thenReturn(false);

        // Act
        ResponseEntity<?> response = botCommandController.playSoundUrl(
                "http://example.com/sound.mp3", "testuser", "", authorization);

        // Assert
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertEquals("You don't have permission to play URL", response.getBody());
        verify(soundPlayer, never()).playForUser(anyString(), anyString(), anyInt(), anyString(), anyString());
    }

    @Test
    void playRandom_withoutAuthorization_playsRandom() throws SoundPlaybackException {
        // Arrange
        String username = "testuser";
        SoundFile soundFile = new SoundFile();
        when(soundPlayer.playRandomSoundFile(username, null, "anonymous")).thenReturn(soundFile);

        // Act
        ResponseEntity<?> response = botCommandController.playRandom(username, "", null);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(soundPlayer).playRandomSoundFile(username, null, "anonymous");
    }

    @Test
    void playRandom_withAuthorization_playsRandom() throws SoundPlaybackException {
        // Arrange
        String username = "testuser";
        String authorization = "Bearer token";
        SoundFile soundFile = new SoundFile();

        when(userRoleConfig.getUserIdFromAuth(authorization)).thenReturn("user123");
        when(userRoleConfig.hasPermission("user123", "play-sounds")).thenReturn(true);
        when(discordUserService.findOneByIdOrUsernameIgnoreCase("user123", "user123")).thenReturn(testUser);
        when(soundPlayer.playRandomSoundFile(username, null, "testuser")).thenReturn(soundFile);

        // Act
        ResponseEntity<?> response = botCommandController.playRandom(username, "", authorization);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(soundPlayer).playRandomSoundFile(username, null, "testuser");
    }

    @Test
    void playRandom_withoutPermission_returns403() {
        // Arrange
        String authorization = "Bearer token";
        when(userRoleConfig.getUserIdFromAuth(authorization)).thenReturn("user123");
        when(userRoleConfig.hasPermission("user123", "play-sounds")).thenReturn(false);

        // Act
        ResponseEntity<?> response = botCommandController.playRandom("testuser", "", authorization);

        // Assert
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertEquals("You don't have permission to play sounds", response.getBody());
    }

    @Test
    void playRandom_whenExceptionThrown_returns500() throws SoundPlaybackException {
        // Arrange
        String username = "testuser";
        when(soundPlayer.playRandomSoundFile(username, null, "anonymous"))
                .thenThrow(new SoundPlaybackException("Error"));

        // Act
        ResponseEntity<?> response = botCommandController.playRandom(username, "", null);

        // Assert
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    }

    @Test
    void stopPlayback_withoutAuthorization_stopsSound() {
        // Arrange
        String username = "testuser";
        when(soundPlayer.stop(username, "")).thenReturn("sound123");

        // Act
        ResponseEntity<?> response = botCommandController.stopPlayback(username, "", null);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(soundPlayer).stop(username, "");
    }

    @Test
    void stopPlayback_withAuthorization_stopsSound() {
        // Arrange
        String username = "testuser";
        String authorization = "Bearer token";

        when(userRoleConfig.getUserIdFromAuth(authorization)).thenReturn("user123");
        when(userRoleConfig.hasPermission("user123", "play-sounds")).thenReturn(true);
        when(soundPlayer.stop(username, "")).thenReturn("sound123");

        // Act
        ResponseEntity<?> response = botCommandController.stopPlayback(username, "", authorization);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(soundPlayer).stop(username, "");
    }

    @Test
    void stopPlayback_withoutPermission_returns403() {
        // Arrange
        String authorization = "Bearer token";
        when(userRoleConfig.getUserIdFromAuth(authorization)).thenReturn("user123");
        when(userRoleConfig.hasPermission("user123", "play-sounds")).thenReturn(false);

        // Act
        ResponseEntity<?> response = botCommandController.stopPlayback("testuser", "", authorization);

        // Assert
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertEquals("You don't have permission to stop sounds", response.getBody());
        verify(soundPlayer, never()).stop(anyString(), anyString());
    }

    @Test
    void setVolume_setsVolumeAndBroadcasts() {
        // Arrange
        int volume = 75;
        String username = "testuser";

        // Act
        ResponseEntity<?> response = botCommandController.setVolume(volume, username, "", null);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(soundPlayer).setGlobalVolume(volume, username, null);
        verify(botVolumeController).broadcastUpdate(username);
    }

    @Test
    void getVolume_returnsVolume() {
        // Arrange
        String username = "testuser";
        float expectedVolume = 85.5f;
        when(soundPlayer.getGlobalVolume(username, "")).thenReturn(expectedVolume);

        // Act
        float result = botCommandController.getVolume(username, "");

        // Assert
        assertEquals(expectedVolume, result);
        verify(soundPlayer).getGlobalVolume(username, "");
    }

    @Test
    void getVoiceChannels_returnsChannels() {
        // Arrange
        List<ChannelResponse> expectedChannels = Arrays.asList(
                new ChannelResponse("Channel 1", "channel1", "Guild 1", "guild1", false),
                new ChannelResponse("Channel 2", "channel2", "Guild 2", "guild2", true)
        );
        when(soundPlayer.getVoiceChannels()).thenReturn(expectedChannels);

        // Act
        List<ChannelResponse> result = botCommandController.getVoiceChannels();

        // Assert
        assertEquals(expectedChannels, result);
        verify(soundPlayer).getVoiceChannels();
    }

    @Test
    void getVersion_returnsVersion() {
        // Arrange
        String expectedVersion = "1.2.3";
        when(soundPlayer.getVersion()).thenReturn(expectedVersion);

        // Act
        String result = botCommandController.getVersion();

        // Assert
        assertEquals(expectedVersion, result);
        verify(soundPlayer).getVersion();
    }
}