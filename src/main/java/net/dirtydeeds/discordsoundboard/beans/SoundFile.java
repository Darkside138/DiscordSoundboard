package net.dirtydeeds.discordsoundboard.beans;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.io.File;

/**
 * Class that represents a sound file.
 *
 * @author dfurrer.
 */
@SuppressWarnings("unused")
public class SoundFile {
    
    private final String soundFileId;
    private final String soundFileLocation;
    private final String category;

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
