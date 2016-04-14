package net.dirtydeeds.discordsoundboard.service;

import net.dirtydeeds.discordsoundboard.ChatSoundBoardListener;
import net.dirtydeeds.discordsoundboard.MainWatch;
import net.dirtydeeds.discordsoundboard.beans.SoundFile;
import net.dirtydeeds.discordsoundboard.beans.User;
import net.dv8tion.jda.JDA;
import net.dv8tion.jda.JDABuilder;
import net.dv8tion.jda.OnlineStatus;
import net.dv8tion.jda.audio.player.FilePlayer;
import net.dv8tion.jda.audio.player.Player;
import net.dv8tion.jda.entities.Guild;
import net.dv8tion.jda.entities.VoiceChannel;
import net.dv8tion.jda.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.managers.AudioManager;
import net.dv8tion.jda.utils.SimpleLog;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import javax.security.auth.login.LoginException;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * @author dfurrer.
 *
 * This class handles moving into channels and playing sounds. Also, it loads the available sound files.
 */
@Service
public class SoundPlayerImpl implements Observer {

    public static final SimpleLog LOG = SimpleLog.getLog("SoundPlayerImpl");
    
    private Properties appProperties;
    private JDA bot;
    private float playerVolume = (float) .75;
    private Map<String, SoundFile> availableSounds;
    private final MainWatch mainWatch;
    private boolean initialized = false;

    @Inject
    public SoundPlayerImpl(MainWatch mainWatch) {
        this.mainWatch = mainWatch;
        this.mainWatch.addObserver(this);
        loadProperties();
        initializeDiscordBot();
        availableSounds = getFileList();
        setSoundPlayerVolume(75);

        initialized = true;
    }
    
    public void setBotListener(ChatSoundBoardListener listener) {
        bot.addEventListener(listener);
    }

    /**
     * Sets volume of the player.
     * @param volume - The volume value to set.
     */
    public void setSoundPlayerVolume(int volume) {
        playerVolume = (float) volume / 100;
    }

    /**
     * Joins the channel of the user provided and then plays a file.
     * @param fileName - The name of the file to play.
     * @param userName - The name of the user to lookup what VoiceChannel they are in.
     */
    public void playFileForUser(String fileName, String userName) {
        if (userName == null || userName.isEmpty()) {
            userName = appProperties.getProperty("username_to_join_channel");
        }
        try {
            Guild guild = getUsersGuild(userName);
            joinUsersCurrentChannel(userName);
            
            playFile(fileName, guild);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Plays the fileName requested.
     * @param fileName - The name of the file to play.
     * @param event -  The even that triggered the sound playing request. The event is used to find the channel to play
     *              the sound back in.
     * @throws Exception
     */
    public void playFileForEvent(String fileName, GuildMessageReceivedEvent event) throws Exception {
        if (event != null) {
            Guild guild = event.getGuild();
            moveToUserIdsChannel(event, guild);
            playFile(fileName, guild);
        }
    }

    /**
     * Moves to the specified voice channel.
     * @param channel - The channel specified.
     */
    public void moveToChannel(VoiceChannel channel, Guild guild){
        AudioManager audioManager = bot.getAudioManager(guild);
        if (audioManager.isConnected()) {
            if (audioManager.isAttemptingToConnect()) {
                audioManager.closeAudioConnection();
            }
            audioManager.moveAudioConnection(channel);
        } else {
            audioManager.openAudioConnection(channel);
        }
    }

    /**
     * Find the "author" of the event and join the voice channel they are in.
     * @param event - The event
     * @throws Exception
     */
    public void moveToUserIdsChannel(GuildMessageReceivedEvent event, Guild guild) throws Exception {
        VoiceChannel channel = null;

        outerloop:
        for (VoiceChannel channel1 : guild.getVoiceChannels()) {
            for (net.dv8tion.jda.entities.User user : channel1.getUsers()) {
                if (user.getId().equals(event.getAuthor().getId())) {
                    channel = channel1;
                    break outerloop;
                }
            }
        }

        if (channel == null) {
            event.getChannel().sendMessage("There isn't a VoiceChannel in this Guild with the name: event.getMessage().getChannelId() ");
            throw new Exception("Problem moving to requested users channel" + event.getAuthor().getId());
        }

        moveToChannel(channel, guild);
    }

    /**
     * Gets a Map of the loaded sound files.
     * @return Map of sound files that have been loaded.
     */
    public Map<String, SoundFile> getAvailableSoundFiles() {
        return availableSounds;
    }

    /**
     * Play file name requested. Will first try to load the file from the map of available sounds.
     * @param fileName - fileName to play.
     */
    public void playFile(String fileName, Guild guild) {
        SoundFile fileToPlay = availableSounds.get(fileName);
        if (fileToPlay != null) {
            playFile(fileToPlay.getSoundFile(), guild);
        }
    }

    /**
     * Get a list of users
     */
    public List<net.dirtydeeds.discordsoundboard.beans.User> getUsers() {
        String userNameToSelect = appProperties.getProperty("username_to_join_channel");
        List<User> users = new ArrayList<>();
        for (net.dv8tion.jda.entities.User user : bot.getUsers()) {
            if (user.getOnlineStatus().equals(OnlineStatus.ONLINE)) {
                boolean selected = false;
                String username = user.getUsername();
                if (userNameToSelect.equals(username)) {
                    selected = true;
                }
                users.add(new net.dirtydeeds.discordsoundboard.beans.User(user.getId(), username, selected));
            }
        }
        return users;
    }

    //Play the file provided.
    private void playFile(File audioFile, Guild guild) {
        try {
            Player player = new FilePlayer(audioFile);

            //Provide the handler to send audio.
            //NOTE: You don't have to set the handler each time you create an audio connection with the
            // AudioManager. Handlers persist between audio connections. Furthermore, handler playback is also
            // paused when a connection is severed (closeAudioConnection), however it would probably be better
            // to pause the play back yourself before severing the connection (If you are using a player class
            // you could just call the pause() method. Otherwise, make canProvide() return false).
            // Once again, you don't HAVE to pause before severing an audio connection,
            // but it probably would be good to do.
            bot.getAudioManager(guild).setSendingHandler(player);

            //Start playback. This will only start after the AudioConnection has completely connected.
            //NOTE: "completely connected" is not just joining the VoiceChannel. Think about when your Discord
            // client joins a VoiceChannel. You appear in the channel lobby immediately, but it takes a few
            // moments before you can start communicating.
            player.play();
            player.setVolume(playerVolume);
        }
        catch (IOException | UnsupportedAudioFileException e) {
            e.printStackTrace();
        }
    }
    
    private Guild getUsersGuild(String userName) {
        for (Guild guild : bot.getGuilds()) {
            for (VoiceChannel channel : guild.getVoiceChannels()) {
                for (net.dv8tion.jda.entities.User user : channel.getUsers()) {
                    if (user.getUsername().equalsIgnoreCase(userName)) {
                        return guild;
                    }
                }
            }
        }
        return null;
    }

    //Join the users current channel.
    private void joinUsersCurrentChannel(String userName) {
        for (Guild guild : bot.getGuilds()) {
            for (VoiceChannel channel : guild.getVoiceChannels()) {
                channel.getUsers().stream().filter(user -> user.getUsername()
                        .equalsIgnoreCase(userName)).forEach(user -> moveToChannel(channel, guild));
            }
        }
    }

    //This method loads the files. This checks if you are running from a .jar file and loads from the /sounds dir relative
    //to the jar file. If not it assumes you are running from code and loads relative to your resource dir.
    private Map<String,SoundFile> getFileList() {
        Map<String,SoundFile> returnFiles = new TreeMap<>();
        try {
            LOG.info("Loading from " + System.getProperty("user.dir") + "/sounds");
            Path soundFilePath = Paths.get(System.getProperty("user.dir") + "/sounds");

            if (!initialized) {
                mainWatch.watchDirectoryPath(soundFilePath);
            }

            if (!soundFilePath.toFile().exists()) {
                System.out.println("creating directory: " + soundFilePath.toFile().toString());
                boolean result = false;

                try{
                    result = soundFilePath.toFile().mkdir();
                }
                catch(SecurityException se){
                    LOG.fatal("Could not create directory: " + soundFilePath.toFile().toString());
                }
                if(result) {
                    LOG.info("DIR: " + soundFilePath.toFile().toString() + " created.");
                }
            }

            mainWatch.watchDirectoryPath(soundFilePath);

            Files.walk(soundFilePath).forEach(filePath -> {
                if (Files.isRegularFile(filePath)) {
                    String fileName = filePath.getFileName().toString();
                    fileName = fileName.substring(fileName.indexOf("/") + 1, fileName.length());
                    fileName = fileName.substring(0, fileName.indexOf("."));
                    LOG.info(fileName);
                    File file = filePath.toFile();
                    String parent = file.getParentFile().getName();
                    SoundFile soundFile = new SoundFile(fileName, filePath.toFile(), parent);
                    returnFiles.put(fileName, soundFile);
                }
            });
        } catch (IOException e) {
            LOG.fatal(e.toString());
            e.printStackTrace();
        }
        return returnFiles;
    }

    //Logs the discord bot in and adds the ChatSoundBoardListener if the user configured it to be used
    private void initializeDiscordBot() {
        try {
            String botToken = appProperties.getProperty("bot_token");
            bot = new JDABuilder()
                    .setAudioEnabled(true)
                    .setAutoReconnect(true)
                    .setBotToken(botToken)
                    .buildBlocking();

            if (Boolean.valueOf(appProperties.getProperty("respond_to_chat_commands"))) {
                String commandCharacter = appProperties.getProperty("command_character");
                String mesageSizeLimit = appProperties.getProperty("message_size_limit");
                ChatSoundBoardListener chatListener = new ChatSoundBoardListener(this, commandCharacter, mesageSizeLimit);
                this.setBotListener(chatListener);
            }
        }
        catch (IllegalArgumentException e) {
            LOG.warn("The config was not populated. Please enter an email and password.");
        }
        catch (LoginException e) {
            LOG.warn("The provided email / password combination was incorrect. Please provide valid details.");
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    //Loads in the properties from the app.properties file
    private void loadProperties() {
        appProperties = new Properties();
        InputStream stream = null;
        try {
            stream = new FileInputStream(System.getProperty("user.dir") + "/app.properties");
            appProperties.load(stream);
            stream.close();
            return;
        } catch (FileNotFoundException e) {
            LOG.warn("Could not find app.properties file.");
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (stream == null) {
            LOG.warn("Loading app.properties file from resources folder");
            try {
                stream = this.getClass().getResourceAsStream("/app.properties");
                appProperties.load(stream);
                stream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void update(Observable o, Object arg) {
        availableSounds = getFileList();
    }
}
