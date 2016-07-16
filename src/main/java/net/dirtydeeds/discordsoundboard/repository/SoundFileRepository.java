package net.dirtydeeds.discordsoundboard.repository;

import net.dirtydeeds.discordsoundboard.beans.SoundFile;
import org.springframework.data.repository.CrudRepository;

/**
 * @author dfurrer.
 */
public interface SoundFileRepository extends CrudRepository<SoundFile, String> {
    SoundFile findOneBySoundFileIdIgnoreCase(String name);
}
