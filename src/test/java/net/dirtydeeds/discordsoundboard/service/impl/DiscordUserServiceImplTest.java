package net.dirtydeeds.discordsoundboard.service.impl;

import net.dirtydeeds.discordsoundboard.beans.DiscordUser;
import net.dirtydeeds.discordsoundboard.repository.DiscordUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DiscordUserServiceImplTest {

    @Mock
    private DiscordUserRepository discordUserRepository;

    @InjectMocks
    private DiscordUserServiceImpl discordUserService;

    private DiscordUser discordUser;

    @BeforeEach
    void setUp() {
        discordUser = new DiscordUser();
        discordUser.setId("user123");
        discordUser.setUsername("testuser");
        discordUser.setEntranceSound("entrance.mp3");
        discordUser.setLeaveSound("leave.mp3");
    }

    @Test
    void findById_delegatesToRepository() {
        // Arrange
        String userId = "user123";
        when(discordUserRepository.findById(userId)).thenReturn(Optional.of(discordUser));

        // Act
        Optional<DiscordUser> result = discordUserService.findById(userId);

        // Assert
        assertTrue(result.isPresent());
        assertEquals(discordUser, result.get());
        verify(discordUserRepository).findById(userId);
    }

    @Test
    void findOneByIdOrUsernameIgnoreCase_delegatesToRepository() {
        // Arrange
        String userId = "user123";
        String userName = "testuser";
        when(discordUserRepository.findOneByIdOrUsernameIgnoreCase(userId, userName)).thenReturn(discordUser);

        // Act
        DiscordUser result = discordUserService.findOneByIdOrUsernameIgnoreCase(userId, userName);

        // Assert
        assertEquals(discordUser, result);
        verify(discordUserRepository).findOneByIdOrUsernameIgnoreCase(userId, userName);
    }

    @Test
    void save_delegatesToRepository() {
        // Arrange
        when(discordUserRepository.save(discordUser)).thenReturn(discordUser);

        // Act
        DiscordUser result = discordUserService.save(discordUser);

        // Assert
        assertEquals(discordUser, result);
        verify(discordUserRepository).save(discordUser);
    }

    @Test
    void findAll_delegatesToRepository() {
        // Arrange
        Pageable pageable = Pageable.unpaged();
        Page<DiscordUser> expectedPage = new PageImpl<>(Collections.singletonList(discordUser));
        when(discordUserRepository.findAll(pageable)).thenReturn(expectedPage);

        // Act
        Page<DiscordUser> result = discordUserService.findAll(pageable);

        // Assert
        assertEquals(expectedPage, result);
        verify(discordUserRepository).findAll(pageable);
    }

    @Test
    void delete_delegatesToRepository() {
        // Act
        discordUserService.delete(discordUser);

        // Assert
        verify(discordUserRepository).delete(discordUser);
    }

    @Test
    void findByInVoiceIsTrueOrSelectedIsTrue_delegatesToRepository() {
        // Arrange
        Pageable pageable = Pageable.unpaged();
        Page<DiscordUser> expectedPage = new PageImpl<>(Collections.singletonList(discordUser));
        when(discordUserRepository.findByInVoiceIsTrueOrSelectedIsTrue(pageable)).thenReturn(expectedPage);

        // Act
        Page<DiscordUser> result = discordUserService.findByInVoiceIsTrueOrSelectedIsTrue(pageable);

        // Assert
        assertEquals(expectedPage, result);
        verify(discordUserRepository).findByInVoiceIsTrueOrSelectedIsTrue(pageable);
    }

    @Test
    void updateSounds_whenUserExists_updatesEntranceSound() throws Exception {
        // Arrange
        String userId = "user123";
        String newEntranceSound = "new-entrance.mp3";
        when(discordUserRepository.findById(userId)).thenReturn(Optional.of(discordUser));
        when(discordUserRepository.save(any(DiscordUser.class))).thenReturn(discordUser);

        // Act
        DiscordUser result = discordUserService.updateSounds(userId, newEntranceSound, null);

        // Assert
        assertEquals(newEntranceSound, result.getEntranceSound());
        assertEquals("leave.mp3", result.getLeaveSound()); // Unchanged
        verify(discordUserRepository).save(discordUser);
    }

    @Test
    void updateSounds_whenUserExists_updatesLeaveSound() throws Exception {
        // Arrange
        String userId = "user123";
        String newLeaveSound = "new-leave.mp3";
        when(discordUserRepository.findById(userId)).thenReturn(Optional.of(discordUser));
        when(discordUserRepository.save(any(DiscordUser.class))).thenReturn(discordUser);

        // Act
        DiscordUser result = discordUserService.updateSounds(userId, null, newLeaveSound);

        // Assert
        assertEquals("entrance.mp3", result.getEntranceSound()); // Unchanged
        assertEquals(newLeaveSound, result.getLeaveSound());
        verify(discordUserRepository).save(discordUser);
    }

    @Test
    void updateSounds_whenUserExists_updatesBothSounds() throws Exception {
        // Arrange
        String userId = "user123";
        String newEntranceSound = "new-entrance.mp3";
        String newLeaveSound = "new-leave.mp3";
        when(discordUserRepository.findById(userId)).thenReturn(Optional.of(discordUser));
        when(discordUserRepository.save(any(DiscordUser.class))).thenReturn(discordUser);

        // Act
        DiscordUser result = discordUserService.updateSounds(userId, newEntranceSound, newLeaveSound);

        // Assert
        assertEquals(newEntranceSound, result.getEntranceSound());
        assertEquals(newLeaveSound, result.getLeaveSound());
        verify(discordUserRepository).save(discordUser);
    }

    @Test
    void updateSounds_whenEntranceSoundIsEmptyString_setsToNull() throws Exception {
        // Arrange
        String userId = "user123";
        when(discordUserRepository.findById(userId)).thenReturn(Optional.of(discordUser));
        when(discordUserRepository.save(any(DiscordUser.class))).thenReturn(discordUser);

        // Act
        DiscordUser result = discordUserService.updateSounds(userId, "", null);

        // Assert
        assertNull(result.getEntranceSound());
        verify(discordUserRepository).save(discordUser);
    }

    @Test
    void updateSounds_whenLeaveSoundIsEmptyString_setsToNull() throws Exception {
        // Arrange
        String userId = "user123";
        when(discordUserRepository.findById(userId)).thenReturn(Optional.of(discordUser));
        when(discordUserRepository.save(any(DiscordUser.class))).thenReturn(discordUser);

        // Act
        DiscordUser result = discordUserService.updateSounds(userId, null, "");

        // Assert
        assertNull(result.getLeaveSound());
        verify(discordUserRepository).save(discordUser);
    }

    @Test
    void updateSounds_whenBothSoundsAreEmptyStrings_setsBothToNull() throws Exception {
        // Arrange
        String userId = "user123";
        when(discordUserRepository.findById(userId)).thenReturn(Optional.of(discordUser));
        when(discordUserRepository.save(any(DiscordUser.class))).thenReturn(discordUser);

        // Act
        DiscordUser result = discordUserService.updateSounds(userId, "", "");

        // Assert
        assertNull(result.getEntranceSound());
        assertNull(result.getLeaveSound());
        verify(discordUserRepository).save(discordUser);
    }

    @Test
    void updateSounds_whenBothParametersAreNull_noChanges() throws Exception {
        // Arrange
        String userId = "user123";
        String originalEntrance = discordUser.getEntranceSound();
        String originalLeave = discordUser.getLeaveSound();
        when(discordUserRepository.findById(userId)).thenReturn(Optional.of(discordUser));
        when(discordUserRepository.save(any(DiscordUser.class))).thenReturn(discordUser);

        // Act
        DiscordUser result = discordUserService.updateSounds(userId, null, null);

        // Assert
        assertEquals(originalEntrance, result.getEntranceSound());
        assertEquals(originalLeave, result.getLeaveSound());
        verify(discordUserRepository).save(discordUser);
    }

    @Test
    void updateSounds_whenUserNotFound_throwsException() {
        // Arrange
        String userId = "nonexistent";
        when(discordUserRepository.findById(userId)).thenReturn(Optional.empty());

        // Act & Assert
        Exception exception = assertThrows(Exception.class, () -> {
            discordUserService.updateSounds(userId, "entrance.mp3", "leave.mp3");
        });

        assertEquals("Could not load discord user", exception.getMessage());
        verify(discordUserRepository, never()).save(any());
    }

    @Test
    void updateSounds_returnsTheUpdatedUser() throws Exception {
        // Arrange
        String userId = "user123";
        when(discordUserRepository.findById(userId)).thenReturn(Optional.of(discordUser));
        when(discordUserRepository.save(any(DiscordUser.class))).thenReturn(discordUser);

        // Act
        DiscordUser result = discordUserService.updateSounds(userId, "new.mp3", null);

        // Assert
        assertSame(discordUser, result);
    }
}