package net.dirtydeeds.discordsoundboard.handlers;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.sedmelluq.discord.lavaplayer.track.playback.AudioFrame;
import lombok.Setter;
import net.dirtydeeds.discordsoundboard.service.PlaybackService;
import net.dv8tion.jda.api.audio.AudioSendHandler;
import net.dv8tion.jda.api.entities.Guild;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.nio.ByteBuffer;
import java.util.List;

@SuppressWarnings("unused")
public class AudioHandler extends AudioEventAdapter implements AudioSendHandler {

    private static final Logger LOG = LoggerFactory.getLogger(AudioHandler.class);

    private final PlayerManager manager;
    private final AudioPlayer audioPlayer;
    private final String guildId;
    private AudioFrame lastFrame;
    @Setter
    private Integer globalVolume;
    private final PlaybackService playbackService;

    protected AudioHandler(PlayerManager manager, Guild guild, AudioPlayer player, PlaybackService playbackService)
    {
        this.manager = manager;
        this.audioPlayer = player;
        this.guildId = guild.getId();
        this.playbackService = playbackService;
    }

    public Integer getGlobalVolume() {
        return (globalVolume != null) ? globalVolume : 75;
    }

    public int addTrack(AudioTrack track) {
        audioPlayer.playTrack(track);
        return -1;
    }

    public int addTracks(List<AudioTrack> tracks) {
        return -1;
    }

    public AudioPlayer getPlayer()
    {
        return audioPlayer;
    }

    @Override
    public void onTrackEnd(AudioPlayer player, AudioTrack track, AudioTrackEndReason endReason) {
        int repeatTimes = (int)track.getUserData();
        if (repeatTimes > 1) {
            track.setUserData(--repeatTimes);
            audioPlayer.playTrack(track.makeClone());
        }
        audioPlayer.setVolume(getGlobalVolume());
        File file = new File(track.getIdentifier());
        playbackService.sendTrackEnd(file.getName().substring(0, file.getName().lastIndexOf('.')), guildId);
    }

    @Override
    public void onTrackException(AudioPlayer player, AudioTrack track, FriendlyException exception) {
        File file = new File(track.getIdentifier());
        String trackName = file.getName();
        if (trackName.contains(".")) {
            trackName = trackName.substring(0, trackName.lastIndexOf('.'));
        }
        LOG.warn("Track exception for {}: {}", trackName, exception.getMessage());
        // Send track end event so UI updates properly
        playbackService.sendTrackEnd(trackName, guildId);
        audioPlayer.setVolume(getGlobalVolume());
    }

    @Override
    public boolean canProvide() {
        lastFrame = audioPlayer.provide();
        return lastFrame != null;
    }

    @Nullable
    @Override
    public ByteBuffer provide20MsAudio() {
        return ByteBuffer.wrap(lastFrame.getData());
    }

    @Override
    public boolean isOpus() {
        return true;
    }
}
