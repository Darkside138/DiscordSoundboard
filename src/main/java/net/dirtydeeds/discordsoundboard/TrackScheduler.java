package net.dirtydeeds.discordsoundboard;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.VoiceChannel;

import java.util.Collections;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * @author dave_f
 */
public class TrackScheduler extends AudioEventAdapter {

    private boolean repeating = false;
    private final AudioPlayer player;
    private final Queue<AudioInfo> queue;
    private final boolean leaveAfterPlayback;

    TrackScheduler(AudioPlayer player, boolean leaveAfterPlayback) {
        this.player = player;
        this.queue = new LinkedBlockingQueue<>();
        this.leaveAfterPlayback = leaveAfterPlayback;
    }

    /**
     * Add the next track to queue or play right away if nothing is in the queue.
     *
     * @param track The track to play or add to queue.
     */
    public void queue(AudioTrack track, Guild guild) {
        AudioInfo info = new AudioInfo(track, guild);
        queue.add(info);

        if (player.getPlayingTrack() == null) {
            player.playTrack(track);
        }
    }

    public void playNow(AudioTrack track, Guild guild) {
        if (player.startTrack(track, false)) {
            AudioInfo info = new AudioInfo(track, guild);
            queue.add(info);
        }
    }

    @Override
    public void onTrackEnd(AudioPlayer player, AudioTrack track, AudioTrackEndReason endReason) {
        Guild guild = queue.poll().getGuild();

        if (repeating) {
            playNow(track.makeClone(), guild);
        }

        if (queue.isEmpty()) {
            if (leaveAfterPlayback) {
                VoiceChannel afkChannel = guild.getAfkChannel();
                if (afkChannel != null) {
                    guild.getAudioManager().openAudioConnection(afkChannel);
                }
            }
        } else {
           //playNow(queue.element().getTrack(), guild);
        }
    }

    /**
     * Start the next track, stopping the current one if it is playing.
     */
    public void nextTrack(Guild guild) {
        // Start the next track, regardless of if something is already playing or not. In case queue was empty, we are
        // giving null to startTrack, which is a valid argument and will simply stop the player.
        playNow(queue.poll().getTrack(), guild);
    }

    public boolean isRepeating() {
        return repeating;
    }

    public void setRepeating(boolean repeating) {
        this.repeating = repeating;
    }

    public void shuffle() {
        Collections.shuffle((List<?>) queue);
    }
}
