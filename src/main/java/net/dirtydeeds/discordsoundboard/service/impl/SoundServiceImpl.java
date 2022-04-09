package net.dirtydeeds.discordsoundboard.service.impl;

import net.dirtydeeds.discordsoundboard.beans.SoundFile;
import net.dirtydeeds.discordsoundboard.repository.SoundFileRepository;
import net.dirtydeeds.discordsoundboard.service.SoundService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@SuppressWarnings("unused")
public class SoundServiceImpl implements SoundService {

    @Autowired
    SoundFileRepository soundRepository;

    @Override
    public Iterable<SoundFile> findAll() {
        return soundRepository.findAll();
    }

    @Override
    public SoundFile findOneBySoundFileIdIgnoreCase(String fileName) {
        return soundRepository.findOneBySoundFileIdIgnoreCase(fileName);
    }

    @Override
    public void deleteAll() {
        soundRepository.deleteAll();
    }

    @Override
    public boolean existsById(String fileName) {
        return soundRepository.existsById(fileName);
    }

    @Override
    public SoundFile save(SoundFile soundFile) {
        return soundRepository.save(soundFile);
    }

    @Override
    public void delete(SoundFile soundFile) {
        soundRepository.delete(soundFile);
    }
}
