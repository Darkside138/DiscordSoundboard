package net.dirtydeeds.discordsoundboard.controllers;

import net.dirtydeeds.discordsoundboard.SoundPlayer;
import net.dirtydeeds.discordsoundboard.beans.SoundFile;
import net.dirtydeeds.discordsoundboard.service.SoundService;
import net.dirtydeeds.discordsoundboard.util.UserRoleConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;

import java.io.File;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SoundControllerTest {

    @Mock
    private SoundService soundService;

    @Mock
    private SoundPlayer soundPlayer;

    @Mock
    private UserRoleConfig userRoleConfig;

    private SoundController soundController;

    private SoundFile testSoundFile;

    @BeforeEach
    void setUp() {
        soundController = new SoundController(soundService, userRoleConfig);
        soundController.setSoundPlayer(soundPlayer);

        testSoundFile = new SoundFile();
        testSoundFile.setSoundFileId("test-sound");
        testSoundFile.setSoundFileLocation("test.mp3");
        testSoundFile.setCategory("music");
    }

    @AfterEach
    void tearDown() {
        soundController.shutdownHeartbeat();
    }

    @Test
    void getAll_returnsAllSoundFiles() {
        // Arrange
        Page<SoundFile> expectedPage = new PageImpl<>(Collections.singletonList(testSoundFile));
        when(soundService.findAll(any(Pageable.class))).thenReturn(expectedPage);

        // Act
        Page<SoundFile> result = soundController.getAll();

        // Assert
        assertEquals(expectedPage, result);
        verify(soundService).findAll(Pageable.unpaged());
    }

    @Test
    void getSoundCategories_returnsUniqueCategories() {
        // Arrange
        SoundFile sound1 = new SoundFile();
        sound1.setCategory("music");
        SoundFile sound2 = new SoundFile();
        sound2.setCategory("sfx");
        SoundFile sound3 = new SoundFile();
        sound3.setCategory("music");

        Map<String, SoundFile> soundMap = new HashMap<>();
        soundMap.put("sound1", sound1);
        soundMap.put("sound2", sound2);
        soundMap.put("sound3", sound3);

        when(soundPlayer.getAvailableSoundFiles()).thenReturn(soundMap);

        // Act
        Set<String> categories = soundController.getSoundCategories();

        // Assert
        assertEquals(2, categories.size());
        assertTrue(categories.contains("music"));
        assertTrue(categories.contains("sfx"));
    }

    @Test
    void deleteSoundFile_withoutPermission_returns403() {
        // Arrange
        String soundId = "test-sound";
        String authorization = "Bearer token";
        when(userRoleConfig.getUserIdFromAuth(authorization)).thenReturn("user123");
        when(userRoleConfig.hasPermission("user123", "delete-sounds")).thenReturn(false);

        // Act
        ResponseEntity<?> response = soundController.deleteSoundFile(soundId, authorization);

        // Assert
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertEquals("You don't have permission to delete sounds", response.getBody());
        verify(soundService, never()).delete(any());
    }

    @Test
    void deleteSoundFile_withNullUserId_returns403() {
        // Arrange
        String soundId = "test-sound";
        String authorization = "Bearer token";
        when(userRoleConfig.getUserIdFromAuth(authorization)).thenReturn(null);

        // Act
        ResponseEntity<?> response = soundController.deleteSoundFile(soundId, authorization);

        // Assert
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        verify(soundService, never()).delete(any());
    }

    @Test
    void deleteSoundFile_whenSoundNotFound_returns404() {
        // Arrange
        String soundId = "nonexistent";
        String authorization = "Bearer token";
        when(userRoleConfig.getUserIdFromAuth(authorization)).thenReturn("user123");
        when(userRoleConfig.hasPermission("user123", "delete-sounds")).thenReturn(true);
        when(soundService.findOneBySoundFileIdIgnoreCase(soundId)).thenThrow(new RuntimeException("Not found"));

        // Act
        ResponseEntity<?> response = soundController.deleteSoundFile(soundId, authorization);

        // Assert
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertTrue(response.getBody().toString().contains("Sound file not found"));
    }

    @Test
    void setFavorite_updatesSoundFile() {
        // Arrange
        String soundId = "test-sound";
        when(soundService.findOneBySoundFileIdIgnoreCase(soundId)).thenReturn(testSoundFile);
        when(soundService.save(any(SoundFile.class))).thenReturn(testSoundFile);
        when(soundService.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(Collections.emptyList()));

        // Act
        ResponseEntity<Void> response = soundController.setFavorite(soundId, true);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(testSoundFile.getFavorite());
        verify(soundService).save(testSoundFile);
    }

    @Test
    void setFavorite_withDefaultFalse_setsFavoriteToFalse() {
        // Arrange
        String soundId = "test-sound";
        testSoundFile.setFavorite(true);
        when(soundService.findOneBySoundFileIdIgnoreCase(soundId)).thenReturn(testSoundFile);
        when(soundService.save(any(SoundFile.class))).thenReturn(testSoundFile);
        when(soundService.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(Collections.emptyList()));

        // Act
        ResponseEntity<Void> response = soundController.setFavorite(soundId, false);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertFalse(testSoundFile.getFavorite());
        verify(soundService).save(testSoundFile);
    }

    @Test
    void patchSoundFile_withoutPermission_returns403() {
        // Arrange
        String soundId = "test-sound";
        String authorization = "Bearer token";
        when(userRoleConfig.getUserIdFromAuth(authorization)).thenReturn("user123");
        when(userRoleConfig.hasPermission("user123", "edit-sounds")).thenReturn(false);

        // Act
        ResponseEntity<Void> response = soundController.patchSoundFile(soundId, 0, "New Name", authorization);

        // Assert
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        verify(soundService, never()).save(any());
    }

    @Test
    void patchSoundFile_withNullUserId_returns403() {
        // Arrange
        String soundId = "test-sound";
        String authorization = "Bearer token";
        when(userRoleConfig.getUserIdFromAuth(authorization)).thenReturn(null);

        // Act
        ResponseEntity<Void> response = soundController.patchSoundFile(soundId, 0, "New Name", authorization);

        // Assert
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        verify(soundService, never()).save(any());
    }

    @Test
    void patchSoundFile_withPermission_updatesSoundFile() {
        // Arrange
        String soundId = "test-sound";
        String authorization = "Bearer token";
        String newDisplayName = "New Sound Name";
        int volumeOffset = 10;

        when(userRoleConfig.getUserIdFromAuth(authorization)).thenReturn("user123");
        when(userRoleConfig.hasPermission("user123", "edit-sounds")).thenReturn(true);
        when(soundService.findOneBySoundFileIdIgnoreCase(soundId)).thenReturn(testSoundFile);
        when(soundService.save(any(SoundFile.class))).thenReturn(testSoundFile);
        when(soundService.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(Collections.emptyList()));

        // Act
        ResponseEntity<Void> response = soundController.patchSoundFile(soundId, volumeOffset, newDisplayName, authorization);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(newDisplayName, testSoundFile.getDisplayName());
        assertEquals(volumeOffset, testSoundFile.getVolumeOffsetPercentage());
        verify(soundService).save(testSoundFile);
    }

    @Test
    void uploadFile_withoutPermission_returns403() {
        // Arrange
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.mp3",
                "audio/mpeg",
                "test content".getBytes()
        );
        String authorization = "Bearer token";

        when(userRoleConfig.getUserIdFromAuth(authorization)).thenReturn("user123");
        when(userRoleConfig.hasPermission("user123", "upload")).thenReturn(false);

        // Act
        ResponseEntity<String> response = soundController.uploadFile(file, authorization);

        // Assert
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertEquals("You don't have permission to upload sounds", response.getBody());
    }

    @Test
    void uploadFile_withEmptyFile_returns400() {
        // Arrange
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.mp3",
                "audio/mpeg",
                new byte[0]
        );
        String authorization = "Bearer token";

        when(userRoleConfig.getUserIdFromAuth(authorization)).thenReturn("user123");
        when(userRoleConfig.hasPermission("user123", "upload")).thenReturn(true);

        // Act
        ResponseEntity<String> response = soundController.uploadFile(file, authorization);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("File is empty", response.getBody());
    }

    @Test
    void uploadFile_withInvalidExtension_returns400() {
        // Arrange
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.exe",
                "application/x-msdownload",
                "test content".getBytes()
        );
        String authorization = "Bearer token";

        when(userRoleConfig.getUserIdFromAuth(authorization)).thenReturn("user123");
        when(userRoleConfig.hasPermission("user123", "upload")).thenReturn(true);

        // Act
        ResponseEntity<String> response = soundController.uploadFile(file, authorization);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertTrue(response.getBody().contains("Invalid file extension"));
    }

    @Test
    void uploadFile_withInvalidMimeType_returns400() {
        // Arrange
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.mp3",
                "application/pdf",
                "test content".getBytes()
        );
        String authorization = "Bearer token";

        when(userRoleConfig.getUserIdFromAuth(authorization)).thenReturn("user123");
        when(userRoleConfig.hasPermission("user123", "upload")).thenReturn(true);

        // Act
        ResponseEntity<String> response = soundController.uploadFile(file, authorization);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertTrue(response.getBody().contains("Invalid file type"));
    }

    @Test
    void uploadFile_withFileTooLarge_returns400() {
        // Arrange - create a file larger than 10 MB
        byte[] largeContent = new byte[11 * 1024 * 1024]; // 11 MB
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "large.mp3",
                "audio/mpeg",
                largeContent
        );
        String authorization = "Bearer token";

        when(userRoleConfig.getUserIdFromAuth(authorization)).thenReturn("user123");
        when(userRoleConfig.hasPermission("user123", "upload")).thenReturn(true);

        // Act
        ResponseEntity<String> response = soundController.uploadFile(file, authorization);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertTrue(response.getBody().contains("File size exceeds 10 MB limit"));
    }

    @Test
    void broadcastUpdate_sendsUpdateToAllEmitters() {
        // Arrange
        Page<SoundFile> sounds = new PageImpl<>(Collections.singletonList(testSoundFile));
        when(soundService.findAll(any(Pageable.class))).thenReturn(sounds);

        // Act - should not throw exception
        assertDoesNotThrow(() -> soundController.broadcastUpdate());
    }

    @Test
    void shutdownHeartbeat_completesSuccessfully() {
        // Act & Assert - should not throw exception
        assertDoesNotThrow(() -> soundController.shutdownHeartbeat());
    }
}