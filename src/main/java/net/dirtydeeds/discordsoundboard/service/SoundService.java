package net.dirtydeeds.discordsoundboard.service;

import net.dirtydeeds.discordsoundboard.beans.SoundFile;

public interface SoundService {
    Iterable<SoundFile> findAll();

    SoundFile findOneBySoundFileIdIgnoreCase(String fileName);

    void deleteAll();

    boolean existsById(String fileName);

    SoundFile save(SoundFile soundFile);

    void delete(SoundFile soundFile);
}
