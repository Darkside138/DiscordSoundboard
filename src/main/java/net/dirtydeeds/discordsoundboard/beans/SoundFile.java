package net.dirtydeeds.discordsoundboard.beans;

import javax.persistence.Entity;
import javax.persistence.Id;

/**
 * Class that represents a sound file.
 *
 * @author dfurrer.
 */
@SuppressWarnings("unused")
@Entity
public class SoundFile {
    
    @Id
    private String soundFileId;
    private String soundFileLocation;
    private String category;
    
    protected SoundFile() {}

    public SoundFile(String soundFileId, String soundFileLocation, String category) {
        this.soundFileId = soundFileId;
        this.soundFileLocation = soundFileLocation;
        this.category = category;
    }

    public String getSoundFileId() {
        return soundFileId;
    }

    public String getSoundFileLocation() {
        return soundFileLocation;
    }

    public String getCategory() {
        return category;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SoundFile soundFile = (SoundFile) o;

        return soundFileId.equals(soundFile.soundFileId);

    }

    @Override
    public int hashCode() {
        return soundFileId.hashCode();
    }
}
