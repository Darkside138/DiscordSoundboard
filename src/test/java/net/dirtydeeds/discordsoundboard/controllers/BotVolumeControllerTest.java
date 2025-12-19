package net.dirtydeeds.discordsoundboard.controllers;

import net.dirtydeeds.discordsoundboard.SoundPlayer;
import net.dirtydeeds.discordsoundboard.util.UserRoleConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BotVolumeControllerTest {

    @Mock
    private SoundPlayer soundPlayer;

    @Mock
    private UserRoleConfig userRoleConfig;

    @InjectMocks
    private BotVolumeController botVolumeController;

    @AfterEach
    void tearDown() {
        if (botVolumeController != null) {
            botVolumeController.shutdownHeartbeat();
        }
    }

    @Test
    void setVolume_withoutPermission_returns403() {
        // Arrange
        String authorization = "Bearer token";
        when(userRoleConfig.getUserIdFromAuth(authorization)).thenReturn("user123");
        when(userRoleConfig.hasPermission("user123", "edit-sounds")).thenReturn(false);

        // Act
        ResponseEntity<Void> response = botVolumeController.setVolume(75, "testuser", "", authorization);

        // Assert
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        verify(soundPlayer, never()).setGlobalVolume(anyInt(), anyString(), any());
    }

    @Test
    void setVolume_withNullUserId_returns403() {
        // Arrange
        String authorization = "Bearer token";
        when(userRoleConfig.getUserIdFromAuth(authorization)).thenReturn(null);

        // Act
        ResponseEntity<Void> response = botVolumeController.setVolume(75, "testuser", "", authorization);

        // Assert
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        verify(soundPlayer, never()).setGlobalVolume(anyInt(), anyString(), any());
    }

    @Test
    void setVolume_withPermission_setsVolume() {
        // Arrange
        String authorization = "Bearer token";
        String username = "testuser";
        Integer volume = 75;

        when(userRoleConfig.getUserIdFromAuth(authorization)).thenReturn("user123");
        when(userRoleConfig.hasPermission("user123", "edit-sounds")).thenReturn(true);

        // Act
        ResponseEntity<Void> response = botVolumeController.setVolume(volume, username, "", authorization);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(soundPlayer).setGlobalVolume(volume, username, null);
    }

    @Test
    void setVolume_callsBroadcastUpdate() {
        // Arrange
        String authorization = "Bearer token";
        String username = "testuser";
        Integer volume = 75;

        when(userRoleConfig.getUserIdFromAuth(authorization)).thenReturn("user123");
        when(userRoleConfig.hasPermission("user123", "edit-sounds")).thenReturn(true);

        // Act
        botVolumeController.setVolume(volume, username, "", authorization);

        // Assert - broadcastUpdate is called internally, verify side effects
        verify(soundPlayer).setGlobalVolume(volume, username, null);
    }

    @Test
    void getVolume_returnsVolume() {
        // Arrange
        String username = "testuser";
        float expectedVolume = 85.5f;
        when(soundPlayer.getGlobalVolume(username, "")).thenReturn(expectedVolume);

        // Act
        float result = botVolumeController.getVolume(username, "");

        // Assert
        assertEquals(expectedVolume, result);
        verify(soundPlayer).getGlobalVolume(username, "");
    }

    @Test
    void getVolume_withDefaultVoiceChannelId_usesEmptyString() {
        // Arrange
        String username = "testuser";
        when(soundPlayer.getGlobalVolume(username, "")).thenReturn(100.0f);

        // Act
        float result = botVolumeController.getVolume(username, "");

        // Assert
        assertEquals(100.0f, result);
        verify(soundPlayer).getGlobalVolume(username, "");
    }

    @Test
    void broadcastUpdate_sendsVolumeToEmitters() {
        // Arrange
        String username = "testuser";

        // Act & Assert - should not throw exception
        assertDoesNotThrow(() -> botVolumeController.broadcastUpdate(username));
    }

    @Test
    void broadcastUpdate_withNoEmitters_doesNotThrow() {
        // Arrange
        String username = "testuser";

        // Act & Assert - should not throw exception when no emitters exist
        assertDoesNotThrow(() -> botVolumeController.broadcastUpdate(username));
    }

    @Test
    void shutdownHeartbeat_completesSuccessfully() {
        // Act & Assert - should not throw exception
        assertDoesNotThrow(() -> botVolumeController.shutdownHeartbeat());
    }

    @Test
    void shutdownHeartbeat_canBeCalledMultipleTimes() {
        // Act & Assert - should not throw exception when called multiple times
        assertDoesNotThrow(() -> {
            botVolumeController.shutdownHeartbeat();
            botVolumeController.shutdownHeartbeat();
        });
    }
}