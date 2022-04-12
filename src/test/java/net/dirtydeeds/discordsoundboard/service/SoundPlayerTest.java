package net.dirtydeeds.discordsoundboard.service;

import net.dirtydeeds.discordsoundboard.MainWatch;
import net.dirtydeeds.discordsoundboard.SoundPlayer;
import net.dirtydeeds.discordsoundboard.beans.SoundFile;
import net.dirtydeeds.discordsoundboard.repository.SoundFileRepository;
import net.dirtydeeds.discordsoundboard.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.any;
import static org.mockito.MockitoAnnotations.openMocks;


@ExtendWith(MockitoExtension.class)
class SoundPlayerTest {
    @Mock private SoundFile soundFile1;
    @Mock private SoundFile soundFile2;
    @Mock private MainWatch watch;
    @Mock private SoundFileRepository soundFileRepository;
    @Mock private UserRepository userRepository;

    private SoundPlayer soundPlayer;

    @BeforeEach
    void init() {
        openMocks(this);
    }

    @Test
    void updateFileListTest() {
    }
}