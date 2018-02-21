package net.dirtydeeds.discordsoundboard;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import net.dv8tion.jda.core.entities.Guild;

public class AudioInfo {

    private final AudioTrack track;
    private final Guild guild;

    public AudioInfo(AudioTrack track, Guild guild) {
        this.track = track;
        this.guild = guild;
    }

    public AudioTrack getTrack() {
        return track;
    }

    public Guild getGuild() {
        return guild;
    }
}
