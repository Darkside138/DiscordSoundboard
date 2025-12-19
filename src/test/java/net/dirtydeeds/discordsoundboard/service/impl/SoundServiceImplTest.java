package net.dirtydeeds.discordsoundboard.service.impl;

import net.dirtydeeds.discordsoundboard.beans.SoundFile;
import net.dirtydeeds.discordsoundboard.repository.SoundFileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.ZonedDateTime;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SoundServiceImplTest {

    @Mock
    private SoundFileRepository soundRepository;

    @InjectMocks
    private SoundServiceImpl soundService;

    private SoundFile soundFile;

    @BeforeEach
    void setUp() {
        soundFile = new SoundFile();
        soundFile.setSoundFileId("test-sound");
        soundFile.setSoundFileLocation("test.mp3");
    }

    @Test
    void findAll_delegatesToRepository() {
        // Arrange
        Pageable pageable = Pageable.unpaged();
        Page<SoundFile> expectedPage = new PageImpl<>(Collections.singletonList(soundFile));
        when(soundRepository.findAll(pageable)).thenReturn(expectedPage);

        // Act
        Page<SoundFile> result = soundService.findAll(pageable);

        // Assert
        assertEquals(expectedPage, result);
        verify(soundRepository).findAll(pageable);
    }

    @Test
    void findOneBySoundFileIdIgnoreCase_delegatesToRepository() {
        // Arrange
        String fileName = "test-sound";
        when(soundRepository.findOneBySoundFileIdIgnoreCase(fileName)).thenReturn(soundFile);

        // Act
        SoundFile result = soundService.findOneBySoundFileIdIgnoreCase(fileName);

        // Assert
        assertEquals(soundFile, result);
        verify(soundRepository).findOneBySoundFileIdIgnoreCase(fileName);
    }

    @Test
    void save_delegatesToRepository() {
        // Arrange
        when(soundRepository.save(soundFile)).thenReturn(soundFile);

        // Act
        SoundFile result = soundService.save(soundFile);

        // Assert
        assertEquals(soundFile, result);
        verify(soundRepository).save(soundFile);
    }

    @Test
    void delete_delegatesToRepository() {
        // Act
        soundService.delete(soundFile);

        // Assert
        verify(soundRepository).delete(soundFile);
    }

    @Test
    void updateSoundPlayed_whenTimesPlayedIsNull_setsToOne() {
        // Arrange
        soundFile.setTimesPlayed(null);
        soundFile.setDateAdded(ZonedDateTime.now());

        // Act
        SoundFile result = soundService.updateSoundPlayed(soundFile);

        // Assert
        assertEquals(1, result.getTimesPlayed());
    }

    @Test
    void updateSoundPlayed_whenTimesPlayedExists_incrementsByOne() {
        // Arrange
        soundFile.setTimesPlayed(5);
        soundFile.setDateAdded(ZonedDateTime.now());

        // Act
        SoundFile result = soundService.updateSoundPlayed(soundFile);

        // Assert
        assertEquals(6, result.getTimesPlayed());
    }

    @Test
    void updateSoundPlayed_callsInitializeDateAdded() {
        // Arrange
        soundFile.setTimesPlayed(0);
        soundFile.setDateAdded(null);

        // Act
        SoundFile result = soundService.updateSoundPlayed(soundFile);

        // Assert
        assertNotNull(result.getDateAdded(), "dateAdded should be initialized");
    }

    @Test
    void initializeDateAdded_whenDateAddedIsNull_setsToNow() {
        // Arrange
        soundFile.setDateAdded(null);
        ZonedDateTime before = ZonedDateTime.now().minusSeconds(1);

        // Act
        SoundFile result = soundService.initializeDateAdded(soundFile);
        ZonedDateTime after = ZonedDateTime.now().plusSeconds(1);

        // Assert
        assertNotNull(result.getDateAdded());
        assertTrue(result.getDateAdded().isAfter(before));
        assertTrue(result.getDateAdded().isBefore(after));
    }

    @Test
    void initializeDateAdded_whenDateAddedExists_doesNotChange() {
        // Arrange
        ZonedDateTime existingDate = ZonedDateTime.now().minusDays(5);
        soundFile.setDateAdded(existingDate);

        // Act
        SoundFile result = soundService.initializeDateAdded(soundFile);

        // Assert
        assertEquals(existingDate, result.getDateAdded());
    }

    @Test
    void updateSoundPlayed_returnsTheSameSoundFile() {
        // Arrange
        soundFile.setTimesPlayed(10);
        soundFile.setDateAdded(ZonedDateTime.now());

        // Act
        SoundFile result = soundService.updateSoundPlayed(soundFile);

        // Assert
        assertSame(soundFile, result, "Should return the same SoundFile instance");
    }

    @Test
    void initializeDateAdded_returnsTheSameSoundFile() {
        // Arrange
        soundFile.setDateAdded(ZonedDateTime.now());

        // Act
        SoundFile result = soundService.initializeDateAdded(soundFile);

        // Assert
        assertSame(soundFile, result, "Should return the same SoundFile instance");
    }
}