package net.dirtydeeds.discordsoundboard.handlers;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import com.sedmelluq.discord.lavaplayer.source.local.LocalAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.soundcloud.SoundCloudAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.vimeo.VimeoAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeAudioSourceManager;
import net.dirtydeeds.discordsoundboard.JDABot;
import net.dirtydeeds.discordsoundboard.handlers.AudioHandler;
import net.dv8tion.jda.api.entities.Guild;

public class PlayerManager extends DefaultAudioPlayerManager {

    private final JDABot bot;

    public PlayerManager(JDABot bot) {
        this.bot = bot;
    }

    public void init() {
//        TransformativeAudioSourceManager.createTransforms(bot.getConfig().getTransforms()).forEach(t -> registerSourceManager(t));
//        AudioSourceManagers.registerRemoteSources(this);
//        AudioSourceManagers.registerLocalSource(this);
//        source(YoutubeAudioSourceManager.class).setPlaylistPageCount(10);

        registerSourceManager(new LocalAudioSourceManager());
        registerSourceManager(new YoutubeAudioSourceManager());
        registerSourceManager(new VimeoAudioSourceManager());
        registerSourceManager(SoundCloudAudioSourceManager.createDefault());
    }

    public JDABot getBot()
    {
        return bot;
    }

    public boolean hasHandler(Guild guild)
    {
        return guild.getAudioManager().getSendingHandler()!=null;
    }

    public AudioHandler setUpHandler(Guild guild) {
        AudioHandler handler;
        if(guild.getAudioManager().getSendingHandler()==null) {
            AudioPlayer player = createPlayer();
            player.setVolume(75);
            handler = new AudioHandler(this, guild, player);
            player.addListener(handler);
            guild.getAudioManager().setSendingHandler(handler);
        }
        else
            handler = (AudioHandler) guild.getAudioManager().getSendingHandler();
        return handler;
    }
}