package net.dirtydeeds.discordsoundboard;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEvent;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventListener;

public class TrackScheduler implements AudioEventListener {

    private AudioPlayer player;
    private Integer repeatNumber;

    public TrackScheduler(AudioPlayer musicPlayer) {
        this.player = musicPlayer;
    }

    @Override
    public void onEvent(AudioEvent event) {

    }

    public void setRepeat(int repeatNumber) {
        this.repeatNumber = repeatNumber;
    }
}
