package net.dirtydeeds.discordsoundboard.service;

import net.dirtydeeds.discordsoundboard.beans.SoundFile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface SoundService {

    Page<SoundFile> findAll(Pageable pageable);

    SoundFile findOneBySoundFileIdIgnoreCase(String fileName);

    SoundFile save(SoundFile soundFile);

    void delete(SoundFile soundFile);

    SoundFile updateSoundPlayed(SoundFile soundFile);

    SoundFile initializeDateAdded(SoundFile soundFile);
}
