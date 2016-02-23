package net.dirtydeeds.discordsoundboard;

import com.sun.javaws.Launcher;
import net.dv8tion.jda.JDA;
import net.dv8tion.jda.audio.player.FilePlayer;
import net.dv8tion.jda.audio.player.Player;
import net.dv8tion.jda.entities.Guild;
import net.dv8tion.jda.entities.User;
import net.dv8tion.jda.entities.VoiceChannel;
import net.dv8tion.jda.events.message.guild.GuildMessageReceivedEvent;

import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

/**
 * @author dfurrer.
 */
public class SoundPlayer {
    private JDA bot;
    private Player player;
    private Map<String, File> availableSounds;
    
    private final String resourceDir = "sounds";

    public SoundPlayer(Player player) {
        this.player = player;
    }

    public void setBot(JDA bot) {
        this.bot = bot;
    }
    
    public void playFileForUser(String fileName, String userName) {
        try {
            joinCurrentChannel(userName);
            
            File fileToPlay = availableSounds.get(fileName);
            if (fileToPlay != null) {
                playFile(fileToPlay);
            } else {
                playFile(fileName);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void playFileForEvent(String fileName, GuildMessageReceivedEvent event) throws Exception {
        if (event != null) {
            moveToUserIdsChannel(event);
            playFile(fileName);
        }
    }
    
    public void playFile(String fileName) {
        URL url = Thread.currentThread().getContextClassLoader().getResource(resourceDir + "/" + fileName);
        if( url == null ){
            url = Thread.currentThread().getContextClassLoader().getResource(fileName);
        }
        if( url == null ){
            throw new RuntimeException( "Cannot find resource on classpath: '" + fileName + "'" );
        } else {
            System.out.printf("Found file " + url);
        }
        fileName = url.getFile();

        File audioFile = new File(fileName);
        
        playFile(audioFile);
    }
    
    public void playFile(File audioFile) {
        try {
            player = new FilePlayer(audioFile);

            //Provide the handler to send audio.
            //NOTE: You don't have to set the handler each time you create an audio connection with the
            // AudioManager. Handlers persist between audio connections. Furthermore, handler playback is also
            // paused when a connection is severed (closeAudioConnection), however it would probably be better
            // to pause the play back yourself before severing the connection (If you are using a player class
            // you could just call the pause() method. Otherwise, make canProvide() return false).
            // Once again, you don't HAVE to pause before severing an audio connection,
            // but it probably would be good to do.
            bot.getAudioManager().setSendingHandler(player);

            //Start playback. This will only start after the AudioConnection has completely connected.
            //NOTE: "completely connected" is not just joining the VoiceChannel. Think about when your Discord
            // client joins a VoiceChannel. You appear in the channel lobby immediately, but it takes a few
            // moments before you can start communicating.
            player.play();
        }
        catch (IOException | UnsupportedAudioFileException e) {
            e.printStackTrace();
        }
    }

    private void joinCurrentChannel(String userName) {
        for (Guild guild : bot.getGuilds()) {
            for (VoiceChannel channel : guild.getVoiceChannels()) {
                channel.getUsers().stream().filter(user -> user.getUsername()
                        .equalsIgnoreCase(userName)).forEach(user -> moveToChannel(channel));
            }
        }
    }
    
    public void moveToChannel(VoiceChannel channel){
        if (bot.getAudioManager().isConnected()) {
            bot.getAudioManager().moveAudioConnection(channel);
        } else {
            bot.getAudioManager().openAudioConnection(channel);
        }
    }

    public void moveToUserIdsChannel(GuildMessageReceivedEvent event) throws Exception {
        VoiceChannel channel = null;
        
        outerloop:
        for (VoiceChannel channel1 : event.getGuild().getVoiceChannels()) {
            for (User user : channel1.getUsers()) {
                if (user.getId().equals(event.getAuthor().getId())) {
                    channel = channel1;
                    break outerloop;
                }
            }
        }

        if (channel == null) {
            event.getChannel().sendMessage("There isn't a VoiceChannel in this Guild with the name: event.getMessage().getChannelId() ");
            throw new Exception("Problem moving to requested channel");
        }
        
        moveToChannel(channel);
    }
    
    public Map<String, File> getAvailableSoundFiles() {
        availableSounds = getFileList();
        return availableSounds;
    }
    
    private Map<String,File> getFileList() {
        Map<String,File> returnFiles = new HashMap<>();
        try {
            final File jarFile = new File(getClass().getProtectionDomain().getCodeSource().getLocation().getPath());

            if(jarFile.isFile()) {  // Run with JAR file
                System.out.println("Loading from " + System.getProperty("user.dir") + "/sounds");
                Files.walk(Paths.get(System.getProperty("user.dir") + "/sounds")).forEach(filePath -> {
                    if (Files.isRegularFile(filePath)) {
                        String fileName = filePath.getFileName().toString();
                        fileName = fileName.substring(fileName.indexOf("/") + 1, fileName.length());
                        fileName = fileName.substring(0, fileName.indexOf("."));
                        System.out.println(fileName);
                        returnFiles.put(fileName, filePath.toFile());
                    }
                });
            } else {
                System.out.println("Loading from classpath resources /" + resourceDir);
                final URL url = Launcher.class.getResource("/" + resourceDir);
                if (url != null) {
                    try {
                        final File apps = new File(url.toURI());
                        for (File app : apps.listFiles()) {
                            if (returnFiles.get(app.getName()) == null) {
                                String fileName = app.getName();
                                fileName = fileName.substring(0, fileName.indexOf("."));
                                returnFiles.put(fileName, app);
                                System.out.println(app);
                            }
                        }
                    } catch (URISyntaxException ex) {
                        // never happens
                    }
                }
            }
        } catch (IOException e) {
            System.out.println(e.toString());
            e.printStackTrace();
        }
        return returnFiles;
    }
}
