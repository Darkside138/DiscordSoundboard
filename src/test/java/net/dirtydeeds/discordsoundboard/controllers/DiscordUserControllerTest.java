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
import java.util.List;

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
        when(discordUserService.findByInVoiceIsTrue(any(Pageable.class))).thenReturn(expectedPage);

        // Act
        Page<DiscordUser> result = discordUserController.getInvoice(0, 200);

        // Assert
        assertEquals(expectedPage, result);
        verify(discordUserService).findByInVoiceIsTrue(any(Pageable.class));
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
        when(discordUserService.findByInVoiceIsTrue(any(Pageable.class)))
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
        when(discordUserService.findByInVoiceIsTrue(any(Pageable.class)))
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
        when(discordUserService.findByInVoiceIsTrue(any(Pageable.class)))
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
        when(discordUserService.findByInVoiceIsTrue(any(Pageable.class)))
                .thenReturn(new PageImpl<>(Collections.singletonList(testUser)));

        // Act & Assert - should not throw exception
        assertDoesNotThrow(() -> discordUserController.broadcastUpdate());
    }

    @Test
    void shutdownHeartbeat_completesSuccessfully() {
        // Act & Assert - should not throw exception
        assertDoesNotThrow(() -> discordUserController.shutdownHeartbeat());
    }

    // ──────────────────────── Role management endpoints ────────────────────────

    @Test
    void getRoles_withPermission_returnsUsersPage() {
        // Arrange
        String authorization = "Bearer token";
        when(userRoleConfig.getUserIdFromAuth(authorization)).thenReturn("admin123");
        when(userRoleConfig.hasPermission("admin123", "manage-users")).thenReturn(true);
        Page<DiscordUser> expectedPage = new PageImpl<>(List.of(testUser));
        when(discordUserService.findAll(any(Pageable.class))).thenReturn(expectedPage);

        // Act
        ResponseEntity<Page<DiscordUser>> response = discordUserController.getUsersWithRoles(0, 50, authorization);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(expectedPage, response.getBody());
    }

    @Test
    void getRoles_withoutPermission_returns403() {
        // Arrange
        String authorization = "Bearer token";
        when(userRoleConfig.getUserIdFromAuth(authorization)).thenReturn("user123");
        when(userRoleConfig.hasPermission("user123", "manage-users")).thenReturn(false);

        // Act
        ResponseEntity<Page<DiscordUser>> response = discordUserController.getUsersWithRoles(0, 50, authorization);

        // Assert
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        verify(discordUserService, never()).findAll(any(Pageable.class));
    }

    @Test
    void assignRole_withoutPermission_returns403() {
        // Arrange
        String authorization = "Bearer token";
        when(userRoleConfig.getUserIdFromAuth(authorization)).thenReturn("user123");
        when(userRoleConfig.hasPermission("user123", "manage-users")).thenReturn(false);

        // Act
        ResponseEntity<DiscordUser> response = discordUserController.assignRole("other-user", "dj", authorization);

        // Assert
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }

    @Test
    void assignRole_withPermission_assignsRoleAndReturnsUser() throws Exception {
        // Arrange
        String authorization = "Bearer token";
        when(userRoleConfig.getUserIdFromAuth(authorization)).thenReturn("admin123");
        when(userRoleConfig.hasPermission("admin123", "manage-users")).thenReturn(true);
        // admin123 != user123, no self-demotion check needed
        when(discordUserService.assignRole("user123", "dj", "admin123")).thenReturn(testUser);
        when(discordUserService.findByInVoiceIsTrue(any(Pageable.class)))
                .thenReturn(new PageImpl<>(Collections.emptyList()));

        // Act
        ResponseEntity<DiscordUser> response = discordUserController.assignRole("user123", "dj", authorization);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(testUser, response.getBody());
        verify(discordUserService).assignRole("user123", "dj", "admin123");
    }

    @Test
    void removeRole_withoutPermission_returns403() {
        // Arrange
        String authorization = "Bearer token";
        when(userRoleConfig.getUserIdFromAuth(authorization)).thenReturn("user123");
        when(userRoleConfig.hasPermission("user123", "manage-users")).thenReturn(false);

        // Act
        ResponseEntity<DiscordUser> response = discordUserController.removeRole("other-user", authorization);

        // Assert
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }

    @Test
    void removeRole_withPermission_removesRoleAndReturnsUser() throws Exception {
        // Arrange
        String authorization = "Bearer token";
        when(userRoleConfig.getUserIdFromAuth(authorization)).thenReturn("admin123");
        when(userRoleConfig.hasPermission("admin123", "manage-users")).thenReturn(true);
        // admin123 != user123, so self-removal guard never fires
        when(discordUserService.removeRole("user123", "admin123")).thenReturn(testUser);
        when(discordUserService.findByInVoiceIsTrue(any(Pageable.class)))
                .thenReturn(new PageImpl<>(Collections.emptyList()));

        // Act
        ResponseEntity<DiscordUser> response = discordUserController.removeRole("user123", authorization);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(testUser, response.getBody());
    }

    @Test
    void getInvoice_whenNoUsersInVoice_returnsEmptyPage() {
        // Arrange
        when(discordUserService.findByInVoiceIsTrue(any(Pageable.class)))
                .thenReturn(new PageImpl<>(Collections.emptyList()));

        // Act
        Page<DiscordUser> result = discordUserController.getInvoice(0, 200);

        // Assert
        assertEquals(0, result.getTotalElements());
        assertTrue(result.getContent().isEmpty());
    }
}