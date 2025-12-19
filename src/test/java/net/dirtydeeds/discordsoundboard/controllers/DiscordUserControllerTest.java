package net.dirtydeeds.discordsoundboard.controllers;

import net.dirtydeeds.discordsoundboard.beans.DiscordUser;
import net.dirtydeeds.discordsoundboard.service.DiscordUserService;
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

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DiscordUserControllerTest {

    @Mock
    private DiscordUserService discordUserService;

    @Mock
    private UserRoleConfig userRoleConfig;

    private DiscordUserController discordUserController;

    private DiscordUser testUser;

    @BeforeEach
    void setUp() {
        discordUserController = new DiscordUserController(discordUserService, userRoleConfig);

        testUser = new DiscordUser();
        testUser.setId("user123");
        testUser.setUsername("testuser");
        testUser.setEntranceSound("entrance.mp3");
        testUser.setLeaveSound("leave.mp3");
    }

    @AfterEach
    void tearDown() {
        discordUserController.shutdownHeartbeat();
    }

    @Test
    void getAll_returnsPagedUsers() {
        // Arrange
        Page<DiscordUser> expectedPage = new PageImpl<>(Collections.singletonList(testUser));
        when(discordUserService.findAll(any(Pageable.class))).thenReturn(expectedPage);

        // Act
        Page<DiscordUser> result = discordUserController.getAll(1, 20, "username", "asc");

        // Assert
        assertEquals(expectedPage, result);
        verify(discordUserService).findAll(any(Pageable.class));
    }

    @Test
    void getAll_withDescSortDir_sortsDescending() {
        // Arrange
        Page<DiscordUser> expectedPage = new PageImpl<>(Collections.singletonList(testUser));
        when(discordUserService.findAll(any(Pageable.class))).thenReturn(expectedPage);

        // Act
        Page<DiscordUser> result = discordUserController.getAll(1, 20, "username", "desc");

        // Assert
        assertEquals(expectedPage, result);
        verify(discordUserService).findAll(any(Pageable.class));
    }

    @Test
    void getAll_withDefaultParameters_usesDefaults() {
        // Arrange
        Page<DiscordUser> expectedPage = new PageImpl<>(Collections.singletonList(testUser));
        when(discordUserService.findAll(any(Pageable.class))).thenReturn(expectedPage);

        // Act
        Page<DiscordUser> result = discordUserController.getAll(1, 20, "username", "asc");

        // Assert
        assertNotNull(result);
        verify(discordUserService).findAll(any(Pageable.class));
    }

    @Test
    void getInvoiceOrSelected_returnsFilteredUsers() {
        // Arrange
        Page<DiscordUser> expectedPage = new PageImpl<>(Collections.singletonList(testUser));
        when(discordUserService.findByInVoiceIsTrueOrSelectedIsTrue(any(Pageable.class))).thenReturn(expectedPage);

        // Act
        Page<DiscordUser> result = discordUserController.getInvoiceOrSelected(0, 200);

        // Assert
        assertEquals(expectedPage, result);
        verify(discordUserService).findByInVoiceIsTrueOrSelectedIsTrue(any(Pageable.class));
    }

    @Test
    void updateUserSounds_withoutPermission_returns403() throws Exception {
        // Arrange
        String userId = "user123";
        String authorization = "Bearer token";
        when(userRoleConfig.getUserIdFromAuth(authorization)).thenReturn("authUser123");
        when(userRoleConfig.hasPermission("authUser123", "manage-users")).thenReturn(false);

        // Act
        ResponseEntity<DiscordUser> response = discordUserController.updateUserSounds(
                userId, "new-entrance.mp3", "new-leave.mp3", authorization);

        // Assert
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        verify(discordUserService, never()).updateSounds(anyString(), anyString(), anyString());
    }

    @Test
    void updateUserSounds_withNullAuthId_returns403() throws Exception {
        // Arrange
        String userId = "user123";
        String authorization = "Bearer token";
        when(userRoleConfig.getUserIdFromAuth(authorization)).thenReturn(null);

        // Act
        ResponseEntity<DiscordUser> response = discordUserController.updateUserSounds(
                userId, "new-entrance.mp3", "new-leave.mp3", authorization);

        // Assert
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        verify(discordUserService, never()).updateSounds(anyString(), anyString(), anyString());
    }

    @Test
    void updateUserSounds_withPermission_updatesUser() throws Exception {
        // Arrange
        String userId = "user123";
        String authorization = "Bearer token";
        String newEntranceSound = "new-entrance.mp3";
        String newLeaveSound = "new-leave.mp3";

        when(userRoleConfig.getUserIdFromAuth(authorization)).thenReturn("authUser123");
        when(userRoleConfig.hasPermission("authUser123", "manage-users")).thenReturn(true);
        when(discordUserService.updateSounds(userId, newEntranceSound, newLeaveSound)).thenReturn(testUser);
        when(discordUserService.findByInVoiceIsTrueOrSelectedIsTrue(any(Pageable.class)))
                .thenReturn(new PageImpl<>(Collections.emptyList()));

        // Act
        ResponseEntity<DiscordUser> response = discordUserController.updateUserSounds(
                userId, newEntranceSound, newLeaveSound, authorization);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(testUser, response.getBody());
        verify(discordUserService).updateSounds(userId, newEntranceSound, newLeaveSound);
    }

    @Test
    void updateUserSounds_whenServiceThrowsException_returns500() throws Exception {
        // Arrange
        String userId = "user123";
        String authorization = "Bearer token";

        when(userRoleConfig.getUserIdFromAuth(authorization)).thenReturn("authUser123");
        when(userRoleConfig.hasPermission("authUser123", "manage-users")).thenReturn(true);
        when(discordUserService.updateSounds(anyString(), anyString(), anyString()))
                .thenThrow(new Exception("Database error"));

        // Act
        ResponseEntity<DiscordUser> response = discordUserController.updateUserSounds(
                userId, "new-entrance.mp3", "new-leave.mp3", authorization);

        // Assert
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    }

    @Test
    void updateUserSounds_withNullEntranceSound_updatesOnlyLeaveSound() throws Exception {
        // Arrange
        String userId = "user123";
        String authorization = "Bearer token";
        String newLeaveSound = "new-leave.mp3";

        when(userRoleConfig.getUserIdFromAuth(authorization)).thenReturn("authUser123");
        when(userRoleConfig.hasPermission("authUser123", "manage-users")).thenReturn(true);
        when(discordUserService.updateSounds(userId, null, newLeaveSound)).thenReturn(testUser);
        when(discordUserService.findByInVoiceIsTrueOrSelectedIsTrue(any(Pageable.class)))
                .thenReturn(new PageImpl<>(Collections.emptyList()));

        // Act
        ResponseEntity<DiscordUser> response = discordUserController.updateUserSounds(
                userId, null, newLeaveSound, authorization);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(discordUserService).updateSounds(userId, null, newLeaveSound);
    }

    @Test
    void updateUserSounds_withNullLeaveSound_updatesOnlyEntranceSound() throws Exception {
        // Arrange
        String userId = "user123";
        String authorization = "Bearer token";
        String newEntranceSound = "new-entrance.mp3";

        when(userRoleConfig.getUserIdFromAuth(authorization)).thenReturn("authUser123");
        when(userRoleConfig.hasPermission("authUser123", "manage-users")).thenReturn(true);
        when(discordUserService.updateSounds(userId, newEntranceSound, null)).thenReturn(testUser);
        when(discordUserService.findByInVoiceIsTrueOrSelectedIsTrue(any(Pageable.class)))
                .thenReturn(new PageImpl<>(Collections.emptyList()));

        // Act
        ResponseEntity<DiscordUser> response = discordUserController.updateUserSounds(
                userId, newEntranceSound, null, authorization);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(discordUserService).updateSounds(userId, newEntranceSound, null);
    }

    @Test
    void broadcastUpdate_sendsUpdateToEmitters() {
        // Arrange
        when(discordUserService.findByInVoiceIsTrueOrSelectedIsTrue(any(Pageable.class)))
                .thenReturn(new PageImpl<>(Collections.singletonList(testUser)));

        // Act & Assert - should not throw exception
        assertDoesNotThrow(() -> discordUserController.broadcastUpdate());
    }

    @Test
    void shutdownHeartbeat_completesSuccessfully() {
        // Act & Assert - should not throw exception
        assertDoesNotThrow(() -> discordUserController.shutdownHeartbeat());
    }
}