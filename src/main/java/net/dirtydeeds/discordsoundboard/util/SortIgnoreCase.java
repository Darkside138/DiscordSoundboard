package net.dirtydeeds.discordsoundboard.util;

import net.dirtydeeds.discordsoundboard.beans.SoundFile;

import java.util.Comparator;

/**
 * Class used to sort SoundFile Object
 *
 * Created by Dave on 4/2/2016.
 */
public class SortIgnoreCase implements Comparator<SoundFile> {
    public int compare(SoundFile o1, SoundFile o2) {
        String s1 = o1.getSoundFileId();
        String s2 = o2.getSoundFileId();
        return s1.toLowerCase().compareTo(s2.toLowerCase());
    }
}
