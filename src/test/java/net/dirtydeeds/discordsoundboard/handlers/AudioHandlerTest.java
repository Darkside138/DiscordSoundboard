package net.dirtydeeds.discordsoundboard.handlers;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason;
import com.sedmelluq.discord.lavaplayer.track.playback.AudioFrame;
import net.dirtydeeds.discordsoundboard.service.PlaybackService;
import net.dv8tion.jda.api.entities.Guild;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AudioHandlerTest {

    @Mock
    private PlayerManager playerManager;

    @Mock
    private Guild guild;

    @Mock
    private AudioPlayer audioPlayer;

    @Mock
    private PlaybackService playbackService;

    private AudioHandler handler;

    @BeforeEach
    void setUp() {
        when(guild.getId()).thenReturn("guild-123");
        handler = new AudioHandler(playerManager, guild, audioPlayer, playbackService);
    }

    // ──────────────────────── getGlobalVolume ────────────────────────

    @Test
    void getGlobalVolume_whenNullVolume_returnsDefault75() {
        // globalVolume is null by default (no setter called)
        assertEquals(75, handler.getGlobalVolume());
    }

    @Test
    void getGlobalVolume_whenSetToZero_returnsZero() {
        handler.setGlobalVolume(0);
        assertEquals(0, handler.getGlobalVolume());
    }

    @Test
    void getGlobalVolume_whenSetToHundred_returnsHundred() {
        handler.setGlobalVolume(100);
        assertEquals(100, handler.getGlobalVolume());
    }

    // ──────────────────────── onTrackEnd ────────────────────────

    @Test
    void onTrackEnd_withRepeatTimesGreaterThan1_decrementsAndClonesTrack() {
        AudioTrack track = mock(AudioTrack.class);
        AudioTrack cloned = mock(AudioTrack.class);
        when(track.getUserData()).thenReturn(2);
        when(track.getIdentifier()).thenReturn("/sounds/beep.mp3");
        when(track.makeClone()).thenReturn(cloned);

        handler.onTrackEnd(audioPlayer, track, AudioTrackEndReason.FINISHED);

        verify(track).setUserData(1);
        verify(audioPlayer).playTrack(cloned);
        verify(audioPlayer).setVolume(75);
        verify(playbackService).sendTrackEnd("beep", "guild-123");
    }

    @Test
    void onTrackEnd_withRepeatTimesEqual1_doesNotCloneTrack() {
        AudioTrack track = mock(AudioTrack.class);
        when(track.getUserData()).thenReturn(1);
        when(track.getIdentifier()).thenReturn("/sounds/beep.mp3");

        handler.onTrackEnd(audioPlayer, track, AudioTrackEndReason.FINISHED);

        verify(audioPlayer, never()).playTrack(any());
        verify(audioPlayer).setVolume(75);
        verify(playbackService).sendTrackEnd("beep", "guild-123");
    }

    @Test
    void onTrackEnd_stripsFileExtensionForSendTrackEnd() {
        AudioTrack track = mock(AudioTrack.class);
        when(track.getUserData()).thenReturn(1);
        when(track.getIdentifier()).thenReturn("/path/to/mysound.wav");

        handler.onTrackEnd(audioPlayer, track, AudioTrackEndReason.FINISHED);

        verify(playbackService).sendTrackEnd("mysound", "guild-123");
    }

    @Test
    void onTrackEnd_resetsVolumeToGlobalAfterRepeat() {
        handler.setGlobalVolume(50);
        AudioTrack track = mock(AudioTrack.class);
        AudioTrack cloned = mock(AudioTrack.class);
        when(track.getUserData()).thenReturn(3);
        when(track.getIdentifier()).thenReturn("/sounds/beep.mp3");
        when(track.makeClone()).thenReturn(cloned);

        handler.onTrackEnd(audioPlayer, track, AudioTrackEndReason.FINISHED);

        verify(audioPlayer).setVolume(50);
    }

    // ──────────────────────── onTrackException ────────────────────────

    @Test
    void onTrackException_withExtension_stripsExtension() {
        AudioTrack track = mock(AudioTrack.class);
        when(track.getIdentifier()).thenReturn("/sounds/error-sound.mp3");
        FriendlyException ex = new FriendlyException("Test error", FriendlyException.Severity.COMMON, null);

        handler.onTrackException(audioPlayer, track, ex);

        verify(playbackService).sendTrackEnd("error-sound", "guild-123");
        verify(audioPlayer).setVolume(75);
    }

    @Test
    void onTrackException_withoutExtension_usesFullName() {
        AudioTrack track = mock(AudioTrack.class);
        when(track.getIdentifier()).thenReturn("/sounds/no-extension");
        FriendlyException ex = new FriendlyException("Test error", FriendlyException.Severity.COMMON, null);

        handler.onTrackException(audioPlayer, track, ex);

        verify(playbackService).sendTrackEnd("no-extension", "guild-123");
    }

    // ──────────────────────── canProvide ────────────────────────

    @Test
    void canProvide_whenPlayerProvidesFrame_returnsTrue() {
        AudioFrame frame = mock(AudioFrame.class);
        when(audioPlayer.provide()).thenReturn(frame);

        assertTrue(handler.canProvide());
    }

    @Test
    void canProvide_whenPlayerReturnsNull_returnsFalse() {
        when(audioPlayer.provide()).thenReturn(null);

        assertFalse(handler.canProvide());
    }

    // ──────────────────────── isOpus ────────────────────────

    @Test
    void isOpus_alwaysReturnsTrue() {
        assertTrue(handler.isOpus());
    }
}
