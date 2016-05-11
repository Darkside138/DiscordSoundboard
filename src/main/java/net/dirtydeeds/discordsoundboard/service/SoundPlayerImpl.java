package net.dirtydeeds.discordsoundboard.service;

import net.dirtydeeds.discordsoundboard.ChatSoundBoardListener;
import net.dirtydeeds.discordsoundboard.MainWatch;
import net.dirtydeeds.discordsoundboard.beans.SoundFile;
import net.dirtydeeds.discordsoundboard.beans.User;
import net.dv8tion.jda.JDA;
import net.dv8tion.jda.JDABuilder;
import net.dv8tion.jda.OnlineStatus;
import net.dv8tion.jda.audio.player.FilePlayer;
import net.dv8tion.jda.entities.*;
import net.dv8tion.jda.events.message.MessageReceivedEvent;
import net.dv8tion.jda.managers.AudioManager;
import net.dv8tion.jda.player.MusicPlayer;
import net.dv8tion.jda.player.source.AudioSource;
import net.dv8tion.jda.player.source.LocalSource;
import net.dv8tion.jda.utils.AvatarUtil;
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
import java.io.UnsupportedEncodingException;
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

    private static final SimpleLog LOG = SimpleLog.getLog("SoundPlayerImpl");
    
    private Properties appProperties;
    private JDA bot;
    private float playerVolume = (float) .75;
    private Map<String, SoundFile> availableSounds;
    private final MainWatch mainWatch;
    private boolean initialized = false;
    private MusicPlayer musicPlayer;
    private FilePlayer player;
    private String playerSetting;
    private String soundFileDir;

    @Inject
    public SoundPlayerImpl(MainWatch mainWatch) {
        this.mainWatch = mainWatch;
        this.mainWatch.addObserver(this);
        loadProperties();
        initializeDiscordBot();
        availableSounds = getFileList();
        setSoundPlayerVolume(75);

        playerSetting = appProperties.getProperty("player");
        if (isMusicPlayer()) {
            musicPlayer = new MusicPlayer();
        }

        initialized = true;
    }

    @Override
    public void update(Observable o, Object arg) {
        availableSounds = getFileList();
    }
    
    /**
     * Gets a Map of the loaded sound files.
     * @return Map of sound files that have been loaded.
     */
    public Map<String, SoundFile> getAvailableSoundFiles() {
        return availableSounds;
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
    public void playFileForEvent(String fileName, MessageReceivedEvent event) throws Exception {
        if (event != null) {
            Guild guild = event.getGuild();
            if (guild == null) {
                guild = getUsersGuild(event.getAuthor().getUsername());
            }
            if (guild != null) {
                moveToUserIdsChannel(event, guild);
                playFile(fileName, guild);
            } else {
                event.getAuthor().getPrivateChannel().sendMessage("I can not find a voice channel you are connected to.");
                LOG.warn("no guild to play to.");
            }
        }
    }

    /**
     * Stops sound playback and returns true or false depending on if playback was stopped.
     * @return boolean representing whether playback was stopped.
     */
    public boolean stop() {
        if (isMusicPlayer()) {
            if (musicPlayer != null && musicPlayer.isPlaying()) {
                musicPlayer.stop();
                return true;
            } else {
                return false;
            }
        } else {
            if (player != null && player.isPlaying()) {
                player.stop();
                return true;
            } else {
                return false;
            }
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

    /**
     * Get the path the application is using for sound files.
     * @return String representation of the sound file path.
     */
    public String getSoundsPath() {
        return soundFileDir;
    }

    /**
     * Find the "author" of the event and join the voice channel they are in.
     * @param event - The event
     * @throws Exception
     */
    private void moveToUserIdsChannel(MessageReceivedEvent event, Guild guild) throws Exception {
        VoiceChannel channel = findUsersChannel(event, guild);

        if (channel == null) {
            event.getAuthor().getPrivateChannel().sendMessage("Hello @"+ event.getAuthor().getUsername() +"! I can not find you in any Voice Channel. Are you sure you are connected to voice?.");
            LOG.warn("Problem moving to requested users channel. Maybe user, " + event.getAuthor().getUsername() + " is not connected to Voice?");
        } else {
            moveToChannel(channel, guild);
        }
    }

    /**
     * Moves to the specified voice channel.
     * @param channel - The channel specified.
     */
    private void moveToChannel(VoiceChannel channel, Guild guild){
        AudioManager audioManager = bot.getAudioManager(guild);
        if (audioManager.isConnected()) {
            if (audioManager.isAttemptingToConnect()) {
                audioManager.closeAudioConnection();
            }
            audioManager.moveAudioConnection(channel);
        } else {
            audioManager.openAudioConnection(channel);
        }

        //Wait for the audio connection to be ready before proceeding.
        synchronized (this) {
            while(audioManager.isAttemptingToConnect()) {
                try {
                    wait(100);
                } catch (InterruptedException e) {
                    LOG.warn("Waiting for audio connection was interrupted.");
                }
            }
        }
    }

    /**
     * Finds a users voice channel based on event and what guild to look in.
     * @param event - The event that triggered this search. This is used to get th events author.
     * @param guild - The guild (discord server) to look in for the author.
     * @return The VoiceChannel if one is found. Otherwise return null.
     */
    private VoiceChannel findUsersChannel(MessageReceivedEvent event, Guild guild) {
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

        return channel;
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

    /**
     * Looks through all the guilds the bot has access to and returns the VoiceChannel the requested user is connected to.
     * @param userName - The username to look for.
     * @return The voice channel the user is connected to. If user is not connected to a voice channel will return null.
     */
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

    /**
     * Play file name requested. Will first try to load the file from the map of available sounds.
     * @param fileName - fileName to play.
     */
    private void playFile(String fileName, Guild guild) {
        SoundFile fileToPlay = availableSounds.get(fileName);
        if (fileToPlay != null) {
            File soundFile = new File(fileToPlay.getSoundFileLocation());
            playFile(soundFile, guild);
        }
    }

    /**
     * Play the provided File object
     * @param audioFile - The File object to play.
     * @param guild - The guild (discord server) the playback is going to happen in.
     */
    private void playFile(File audioFile, Guild guild) {
        if (guild == null) {
            LOG.fatal("Guild is null. Have you added your bot to a guild? https://discordapp.com/developers/docs/topics/oauth2");
        } else {
            if (isMusicPlayer()) {
                if (bot.getAudioManager(guild).getSendingHandler() == null) {
                    bot.getAudioManager(guild).setSendingHandler(musicPlayer);
                }

                musicPlayer.stop();
                musicPlayer.getAudioQueue().clear();

                AudioSource audioSource = new LocalSource(audioFile);
                musicPlayer.getAudioQueue().add(audioSource);

                musicPlayer.setVolume(playerVolume);
                bot.getAudioManager(guild).setConnectTimeout(100L);

                musicPlayer.play();
            } else {
                try {
                    player = new FilePlayer(audioFile);
                    
                    bot.getAudioManager(guild).setSendingHandler(player);

                    player.stop();

                    player.setVolume(playerVolume);
                    bot.getAudioManager(guild).setConnectTimeout(100L);

                    player.play();
                } catch (IOException | UnsupportedAudioFileException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Helper method that tells us if we are using MusicPlayer. If false we are using FilePlayer.
     * @return boolean
     */
    private boolean isMusicPlayer() {
        return playerSetting != null && playerSetting.equals("musicPlayer");
    }

    //This method loads the files. This checks if you are running from a .jar file and loads from the /sounds dir relative
    //to the jar file. If not it assumes you are running from code and loads relative to your resource dir.
    private Map<String,SoundFile> getFileList() {
        Map<String,SoundFile> returnFiles = new TreeMap<>();
        try {
            
            soundFileDir = appProperties.getProperty("sounds_directory");
            if (soundFileDir == null || soundFileDir.isEmpty())  {
                soundFileDir = System.getProperty("user.dir") + "/sounds";
            }
            LOG.info("Loading from " + soundFileDir);
            Path soundFilePath = Paths.get(soundFileDir);

            if (!initialized) {
                mainWatch.watchDirectoryPath(soundFilePath);
            }

            if (!soundFilePath.toFile().exists()) {
                System.out.println("creating directory: " + soundFilePath.toFile().toString());
                boolean result = false;

                try {
                    result = soundFilePath.toFile().mkdir();
                } catch(SecurityException se) {
                    LOG.fatal("Could not create directory: " + soundFilePath.toFile().toString());
                }
                if(result) {
                    LOG.info("DIR: " + soundFilePath.toFile().toString() + " created.");
                }
            }

            Files.walk(soundFilePath).forEach(filePath -> {
                if (Files.isRegularFile(filePath)) {
                    String fileName = filePath.getFileName().toString();
                    fileName = fileName.substring(fileName.indexOf("/") + 1, fileName.length());
                    fileName = fileName.substring(0, fileName.indexOf("."));
                    LOG.info(fileName);
                    File file = filePath.toFile();
                    String parent = file.getParentFile().getName();
                    SoundFile soundFile = new SoundFile(fileName, filePath.toString(), parent);
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
                String messageSizeLimit = appProperties.getProperty("message_size_limit");
                ChatSoundBoardListener chatListener = new ChatSoundBoardListener(this, commandCharacter, messageSizeLimit);
                this.setBotListener(chatListener);
            }
            
            File avatarFile = new File(System.getProperty("user.dir") + "/avatar.jpg");
            AvatarUtil.Avatar avatar = AvatarUtil.getAvatar(avatarFile);
            bot.getAccountManager().setAvatar(avatar).update();
        }
        catch (IllegalArgumentException e) {
            LOG.warn("The config was not populated. Please enter an email and password.");
        }
        catch (LoginException e) {
            LOG.warn("The provided email / password combination was incorrect. Please provide valid details.");
        } catch (InterruptedException e) {
            LOG.fatal("Login Interrupted.");
        } catch (UnsupportedEncodingException e) {
            LOG.warn("Could not update avatar with provided file.");
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
                if (stream != null) {
                    appProperties.load(stream);
                    stream.close();
                } else {
                    //TODO: Would be nice if we could auto create a default app.properties here.
                    LOG.fatal("You do not have an app.properties file. Please create one.");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    //Sets listeners
    private void setBotListener(ChatSoundBoardListener listener) {
        bot.addEventListener(listener);
    }
}
