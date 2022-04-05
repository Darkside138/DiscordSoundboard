package net.dirtydeeds.discordsoundboard.beans;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.persistence.Entity;
import javax.persistence.Id;

/**
 * Class that represents a sound file.
 *
 * @author dfurrer.
 */
@Entity
@Getter
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
public class SoundFile {
    
    @Id
    private String soundFileId;
    private String soundFileLocation;
    private String category;

}
