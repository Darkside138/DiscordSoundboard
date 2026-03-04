package net.dirtydeeds.discordsoundboard;

import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import net.dirtydeeds.discordsoundboard.handlers.AudioHandler;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.managers.AudioManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FileLoadResultHandlerTest {

    @Mock
    private Guild guild;

    @Mock
    private AudioManager audioManager;

    @Mock
    private AudioHandler audioHandler;

    private void setupGuildAudioManager() {
        when(guild.getAudioManager()).thenReturn(audioManager);
        when(audioManager.getSendingHandler()).thenReturn(audioHandler);
    }

    // ──────────────────────── trackLoaded ────────────────────────

    @Test
    void trackLoaded_setsUserDataAndAddsTrack() {
        setupGuildAudioManager();
        FileLoadResultHandler resultHandler = new FileLoadResultHandler(guild, 3);
        AudioTrack track = mock(AudioTrack.class);

        resultHandler.trackLoaded(track);

        verify(track).setUserData(3);
        verify(audioHandler).addTrack(track);
    }

    @Test
    void trackLoaded_withRepeatTimesOne_setsUserDataToOne() {
        setupGuildAudioManager();
        FileLoadResultHandler resultHandler = new FileLoadResultHandler(guild, 1);
        AudioTrack track = mock(AudioTrack.class);

        resultHandler.trackLoaded(track);

        verify(track).setUserData(1);
        verify(audioHandler).addTrack(track);
    }

    @Test
    void trackLoaded_whenHandlerIsNull_doesNotThrow() {
        when(guild.getAudioManager()).thenReturn(audioManager);
        when(audioManager.getSendingHandler()).thenReturn(null);
        FileLoadResultHandler resultHandler = new FileLoadResultHandler(guild, 1);
        AudioTrack track = mock(AudioTrack.class);

        assertDoesNotThrow(() -> resultHandler.trackLoaded(track));
        verify(track).setUserData(1);
    }

    // ──────────────────────── playlistLoaded ────────────────────────

    @Test
    void playlistLoaded_singleTrack_noSelectedTrack_usesFirstTrack() {
        setupGuildAudioManager();
        FileLoadResultHandler resultHandler = new FileLoadResultHandler(guild, 2);
        AudioTrack track = mock(AudioTrack.class);
        AudioPlaylist playlist = mock(AudioPlaylist.class);
        when(playlist.getTracks()).thenReturn(List.of(track));
        // size==1 short-circuits, isSearchResult() never called
        when(playlist.getSelectedTrack()).thenReturn(null);

        resultHandler.playlistLoaded(playlist);

        verify(track).setUserData(2);
        verify(audioHandler).addTrack(track);
    }

    @Test
    void playlistLoaded_singleTrack_withSelectedTrack_usesSelectedTrack() {
        setupGuildAudioManager();
        FileLoadResultHandler resultHandler = new FileLoadResultHandler(guild, 1);
        AudioTrack track = mock(AudioTrack.class);
        AudioTrack selected = mock(AudioTrack.class);
        AudioPlaylist playlist = mock(AudioPlaylist.class);
        when(playlist.getTracks()).thenReturn(List.of(track));
        // size==1 short-circuits isSearchResult, so getSelectedTrack is checked
        when(playlist.getSelectedTrack()).thenReturn(selected);

        resultHandler.playlistLoaded(playlist);

        verify(selected).setUserData(1);
        verify(audioHandler).addTrack(selected);
        verify(audioHandler, never()).addTrack(track);
    }

    @Test
    void playlistLoaded_isSearchResult_true_withNullSelectedTrack_usesFirstTrack() {
        setupGuildAudioManager();
        FileLoadResultHandler resultHandler = new FileLoadResultHandler(guild, 1);
        AudioTrack first = mock(AudioTrack.class);
        AudioTrack second = mock(AudioTrack.class);
        AudioPlaylist playlist = mock(AudioPlaylist.class);
        when(playlist.getTracks()).thenReturn(List.of(first, second));
        when(playlist.isSearchResult()).thenReturn(true);
        when(playlist.getSelectedTrack()).thenReturn(null);

        resultHandler.playlistLoaded(playlist);

        verify(first).setUserData(1);
        verify(audioHandler).addTrack(first);
        verify(audioHandler, never()).addTrack(second);
    }

    @Test
    void playlistLoaded_isSearchResult_true_withSelectedTrack_usesSelectedTrack() {
        setupGuildAudioManager();
        FileLoadResultHandler resultHandler = new FileLoadResultHandler(guild, 1);
        AudioTrack selected = mock(AudioTrack.class);
        AudioPlaylist playlist = mock(AudioPlaylist.class);
        when(playlist.getTracks()).thenReturn(List.of(mock(AudioTrack.class), mock(AudioTrack.class)));
        when(playlist.isSearchResult()).thenReturn(true);
        when(playlist.getSelectedTrack()).thenReturn(selected);

        resultHandler.playlistLoaded(playlist);

        verify(selected).setUserData(1);
        verify(audioHandler).addTrack(selected);
    }

    @Test
    void playlistLoaded_multipleTracksWithSelectedTrack_notSearchResult_playsSelectedOnly() {
        setupGuildAudioManager();
        FileLoadResultHandler resultHandler = new FileLoadResultHandler(guild, 1);
        AudioTrack selected = mock(AudioTrack.class);
        AudioPlaylist playlist = mock(AudioPlaylist.class);
        when(playlist.getTracks()).thenReturn(List.of(mock(AudioTrack.class), mock(AudioTrack.class)));
        when(playlist.isSearchResult()).thenReturn(false);
        when(playlist.getSelectedTrack()).thenReturn(selected);

        resultHandler.playlistLoaded(playlist);

        verify(audioHandler).addTrack(selected);
    }

    @Test
    void playlistLoaded_multipleTracksNoSelectedTrackNotSearchResult_noTracksPlayed() {
        // No guild/audioManager stubs needed — addTrack should never be called
        FileLoadResultHandler resultHandler = new FileLoadResultHandler(guild, 1);
        AudioPlaylist playlist = mock(AudioPlaylist.class);
        when(playlist.getTracks()).thenReturn(List.of(mock(AudioTrack.class), mock(AudioTrack.class)));
        when(playlist.isSearchResult()).thenReturn(false);
        when(playlist.getSelectedTrack()).thenReturn(null);

        resultHandler.playlistLoaded(playlist);

        verify(audioHandler, never()).addTrack(any());
    }

    // ──────────────────────── noMatches / loadFailed ────────────────────────

    @Test
    void noMatches_doesNotThrow() {
        FileLoadResultHandler resultHandler = new FileLoadResultHandler(guild, 1);

        assertDoesNotThrow(resultHandler::noMatches);
    }

    @Test
    void loadFailed_doesNotThrow() {
        FileLoadResultHandler resultHandler = new FileLoadResultHandler(guild, 1);
        FriendlyException ex = new FriendlyException("fail", FriendlyException.Severity.COMMON, null);

        assertDoesNotThrow(() -> resultHandler.loadFailed(ex));
    }
}
