package net.dirtydeeds.discordsoundboard.service.impl;

import net.dirtydeeds.discordsoundboard.beans.SoundFile;
import net.dirtydeeds.discordsoundboard.repository.SoundFileRepository;
import net.dirtydeeds.discordsoundboard.service.SoundService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.ZonedDateTime;

@Service
@SuppressWarnings("unused")
public class SoundServiceImpl implements SoundService {

    @Autowired
    SoundFileRepository soundRepository;

    @Override
    public Iterable<SoundFile> findAll(Pageable pageable) {
        return soundRepository.findAll(pageable);
    }

    @Override
    public SoundFile findOneBySoundFileIdIgnoreCase(String fileName) {
        return soundRepository.findOneBySoundFileIdIgnoreCase(fileName);
    }

    @Override
    public SoundFile save(SoundFile soundFile) {
        return soundRepository.save(soundFile);
    }

    @Override
    public void delete(SoundFile soundFile) {
        soundRepository.delete(soundFile);
    }

    @Override
    public SoundFile updateSoundPlayed(SoundFile soundFile) {
        if (soundFile.getTimesPlayed() == null) {
            soundFile.setTimesPlayed(1);
        } else {
            soundFile.setTimesPlayed(soundFile.getTimesPlayed() + 1);
        }
        soundFile = initializeDateAdded(soundFile);
        return soundFile;
    }

    @Override
    public SoundFile initializeDateAdded(SoundFile soundFile) {
        if (soundFile.getDateAdded() == null) {
            soundFile.setDateAdded(ZonedDateTime.now());
        }
        return soundFile;
    }
}
