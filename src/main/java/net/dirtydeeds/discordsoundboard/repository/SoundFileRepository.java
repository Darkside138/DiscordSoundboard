package net.dirtydeeds.discordsoundboard.repository;

import net.dirtydeeds.discordsoundboard.beans.SoundFile;
import org.apache.commons.io.FilenameUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author dfurrer.
 */
public interface SoundFileRepository extends CrudRepository<SoundFile, String> {
    SoundFile findOneBySoundFileIdIgnoreCase(String name);
    Page<SoundFile> findAll(Pageable pageable);

    default SoundFile findRandom() {
        long count = count();
        int random = (int) (Math.random() * count);

        Page<SoundFile> page = findAll(PageRequest.of(random, 1));

        if (page.hasContent()) {
            return page.getContent().get(0);
        }
        return null;
    }

    @Query(value = "select s.soundFileLocation as soundFileLocation from SoundFile s")
    Collection<String> getSoundFileLocations();

    default Set<String> getSoundFileNames() {
        Collection<String> soundFileLocations = getSoundFileLocations();
        return soundFileLocations.stream()
                .map(FilenameUtils::getBaseName)
                .collect(Collectors.toSet());
    }
}
