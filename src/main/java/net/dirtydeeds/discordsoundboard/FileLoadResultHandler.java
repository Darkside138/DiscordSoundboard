package net.dirtydeeds.discordsoundboard;

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import net.dirtydeeds.discordsoundboard.handlers.AudioHandler;
import net.dv8tion.jda.api.entities.Guild;

public class FileLoadResultHandler implements AudioLoadResultHandler {
    private final Guild guild;
    private final int repeatTimes;

    public FileLoadResultHandler(Guild guild, int repeatTimes) {
        this.guild = guild;
        this.repeatTimes = repeatTimes;
    }

    private void loadSingle(AudioTrack track, AudioPlaylist playlist) {
        AudioHandler handler = (AudioHandler)guild.getAudioManager().getSendingHandler();

        track.setUserData(repeatTimes);
        handler.addTrack(track);
    }

    private int loadPlaylist(AudioPlaylist playlist, AudioTrack exclude) {
        int[] count = {0};
        playlist.getTracks().forEach((track) -> {
            AudioHandler handler = (AudioHandler)guild.getAudioManager().getSendingHandler();
            assert handler != null;
            handler.addTrack(track);
            count[0]++;
        });
        return count[0];
    }

    @Override
    public void trackLoaded(AudioTrack track) {
        loadSingle(track, null);
    }

    @Override
    public void playlistLoaded(AudioPlaylist playlist) {
        if (playlist.getTracks().size()==1 || playlist.isSearchResult()) {
            AudioTrack single = playlist.getSelectedTrack()==null ? playlist.getTracks().get(0) : playlist.getSelectedTrack();
            loadSingle(single, null);
        }
        else if (playlist.getSelectedTrack() != null) {
            AudioTrack single = playlist.getSelectedTrack();
            loadSingle(single, playlist);
        }
    }

    @Override
    public void noMatches() {

    }

    @Override
    public void loadFailed(FriendlyException throwable) {

    }
}

