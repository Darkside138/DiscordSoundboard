package net.dirtydeeds.discordsoundboard.beans;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.*;
import org.springframework.lang.Nullable;

import java.time.ZonedDateTime;

/**
 * Class that represents a sound file.
 *
 * @author dfurrer.
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@RequiredArgsConstructor
public class SoundFile {

    @Id
    @NonNull
    private String soundFileId;
    @NonNull
    private String soundFileLocation;
    @NonNull
    private String category;
    @Nullable
    private Integer timesPlayed;
    @Nullable
    private ZonedDateTime dateAdded;
    @Nullable
    private Boolean favorite;
    @Nullable
    private String displayName;
}