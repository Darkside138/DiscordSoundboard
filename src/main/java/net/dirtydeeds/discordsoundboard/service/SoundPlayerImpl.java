package net.dirtydeeds.discordsoundboard.service;

import net.dirtydeeds.discordsoundboard.*;
import net.dirtydeeds.discordsoundboard.beans.SoundFile;
import net.dirtydeeds.discordsoundboard.beans.User;
import net.dirtydeeds.discordsoundboard.repository.SoundFileRepository;
import net.dv8tion.jda.JDA;
import net.dv8tion.jda.JDABuilder;
import net.dv8tion.jda.OnlineStatus;
import net.dv8tion.jda.Permission;
import net.dv8tion.jda.audio.player.FilePlayer;
import net.dv8tion.jda.entities.*;
import net.dv8tion.jda.events.message.MessageReceivedEvent;
import net.dv8tion.jda.events.voice.VoiceJoinEvent;
import net.dv8tion.jda.events.voice.VoiceLeaveEvent;
import net.dv8tion.jda.managers.AudioManager;
import net.dv8tion.jda.player.MusicPlayer;
import net.dv8tion.jda.player.source.AudioSource;
import net.dv8tion.jda.player.source.LocalSource;
import net.dv8tion.jda.player.source.RemoteSource;
import net.dv8tion.jda.utils.AvatarUtil;
import net.dv8tion.jda.utils.PermissionUtil;
import net.dv8tion.jda.utils.SimpleLog;
import org.springframework.stereotype.Service;
import java.util.concurrent.TimeUnit;
import java.lang.InterruptedException;

import javax.annotation.PreDestroy;
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
 * This class handles moving into channels and playing sounds. Also, it loads the available sound files
 * and the configuration properties.
 */
@Service
public class SoundPlayerImpl implements Observer {

    private static final SimpleLog LOG = SimpleLog.getLog("SoundPlayerImpl");
    
    private Properties appProperties;
    private JDA bot;
    private float playerVolume = (float) .75;
    private final MainWatch mainWatch;
    private boolean initialized = false;
    private boolean isFading = false;
    private MusicPlayer musicPlayer;
    private FilePlayer player;
    private String playerSetting;
    private String soundFileDir;
    private List<String> allowedUsers;
    private List<String> bannedUsers;
    private SoundFileRepository repository;
    private boolean leaveAfterPlayback = false;

    @Inject
    public SoundPlayerImpl(MainWatch mainWatch, SoundFileRepository repository) {
        this.mainWatch = mainWatch;
        this.mainWatch.addObserver(this);
        this.repository = repository;

        setSoundPlayerVolume(75);

        init();
    }

    private void init() {
        loadProperties();
        initializeDiscordBot();
        getFileList();

        playerSetting = appProperties.getProperty("player");
        if (isMusicPlayer()) {
            musicPlayer = new MusicPlayer();
        }

        leaveAfterPlayback = Boolean.valueOf(appProperties.getProperty("leaveAfterPlayback"));

        initialized = true;
    }

    @Override
    public void update(Observable o, Object arg) {
        getFileList();
    }
    
    /**
     * Gets a Map of the loaded sound files.
     * @return Map of sound files that have been loaded.
     */
    public Map<String, SoundFile> getAvailableSoundFiles() {
        Map<String,SoundFile> returnFiles = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        for (SoundFile soundFile : repository.findAll()) {
            returnFiles.put(soundFile.getSoundFileId(), soundFile);
        }
        return returnFiles;
    }

    /**
     * Sets volume of the player.
     * @param volume - The volume value to set.
     */
    public void setSoundPlayerVolume(int volume) {
	    setSoundPlayerVolume(volume, 1000);
    }
    
    /**
     * Sets volume of the player.
     * @param volume - The volume value to set.
     */
    public void setSoundPlayerVolume(int volume, int timeout) {
	    int currentVolume = Math.round(playerVolume * 100);
        
        int volumeDiff = currentVolume - volume;
        if (volumeDiff < 0) {
	        volumeDiff = volumeDiff * -1;
        }
        
        if (volumeDiff != 0 && isFading == false) {
	        boolean isFading = true;
	        int microInterval = Math.round(timeout / volumeDiff);
	        while (currentVolume != volume) {
		        if (currentVolume < volume) {
			        currentVolume++;
		        } else {
			        currentVolume--;
		        }
				playerVolume = (float) currentVolume / 100;
		        if (isMusicPlayer()) {
		            musicPlayer.setVolume(playerVolume);
		        }
		        try {
			        TimeUnit.MILLISECONDS.sleep(microInterval);
		        } catch (InterruptedException e) {
			        // mute...
		        }
	        }
	        isFading = false;
        }
    }

    /**
     * Returns the current volume
     * @return float representing the current volume.
     */
    public float getSoundPlayerVolume() {
        return playerVolume;
    }

    @SuppressWarnings("unchecked")
    public void playRandomSoundFile(String requestingUser, MessageReceivedEvent event) throws SoundPlaybackException {
        try {
            Map<String, SoundFile> sounds = getAvailableSoundFiles();
            List<String> keysAsArray = new ArrayList(sounds.keySet());
            Random r = new Random();
            SoundFile randomValue = sounds.get(keysAsArray.get(r.nextInt(keysAsArray.size())));

            LOG.info("Attempting to play random file: " + randomValue.getSoundFileId() + ", requested by : " + requestingUser);
            try {
                if (event != null) {
                    playFileForEvent(randomValue.getSoundFileId(), event);
                } else {
                    playFileForUser(randomValue.getSoundFileId(), requestingUser);
                }

                if (leaveAfterPlayback) {
                    if (event != null) {
                        disconnectFromChannel(event.getGuild());
                    }
                }
            } catch (Exception e) {
                LOG.fatal("Could not play random file: " + randomValue.getSoundFileId());
            }
        } catch (Exception e) {
            throw new SoundPlaybackException("Problem playing random file.");
        }
    }

    /**
     * Joins the channel of the user provided and then plays a file.
     * @param fileName - The name of the file to play.
     * @param userName - The name of the user to lookup what VoiceChannel they are in.
     */
    public void playFileForUser(String fileName, String userName) throws SoundPlaybackException {
        if (userName == null || userName.isEmpty()) {
            userName = appProperties.getProperty("username_to_join_channel");
        }
        try {
            Guild guild = getUsersGuild(userName);
            joinUsersCurrentChannel(userName);
            
            playFile(fileName, guild);

            if (leaveAfterPlayback) {
                disconnectFromChannel(guild);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void playUrlForUser(String url, String userName) {
        if (userName == null || userName.isEmpty()) {
            userName = appProperties.getProperty("username_to_join_channel");
        }
        try {
            Guild guild = getUsersGuild(userName);
            joinUsersCurrentChannel(userName);

            playUrl(url, guild);

            if (leaveAfterPlayback) {
                disconnectFromChannel(guild);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Plays the fileName requested.
     * @param fileName - The name of the file to play.
     * @param event -  The event that triggered the sound playing request. The event is used to find the channel to play
     *              the sound back in.
     * @throws Exception Throws exception if it couldn't find the file requested or can't join the voice channel
     */
    public void playFileForEvent(String fileName, MessageReceivedEvent event) throws Exception {
	    playFileForEvent(fileName, event, 1);
    }

    /**
     * Plays the fileName requested.
     * @param fileName - The name of the file to play.
     * @param event -  The event that triggered the sound playing request. The event is used to find the channel to play
     *              the sound back in.
     * @param repeatNumber - the number of times to repeat the sound file
     * @throws Exception Throws exception if it couldn't find the file requested or can't join the voice channel
     */
    public void playFileForEvent(String fileName, MessageReceivedEvent event, int repeatNumber) throws Exception {
        SoundFile fileToPlay = getSoundFileById(fileName);
        if (event != null) {
            Guild guild = event.getGuild();
            if (guild == null) {
                guild = getUsersGuild(event.getAuthor().getUsername());
            }
            if (guild != null) {
                if (fileToPlay != null) {
                    try {
                        moveToUserIdsChannel(event, guild);
                    } catch (SoundPlaybackException e) {
                        event.getAuthor().getPrivateChannel().sendMessage(e.getLocalizedMessage());
                    }
                    File soundFile = new File(fileToPlay.getSoundFileLocation());
                    playFile(soundFile, guild, repeatNumber);

                    if (leaveAfterPlayback) {
                        disconnectFromChannel(event.getGuild());
                    }
                } else {
                    event.getAuthor().getPrivateChannel().sendMessage("Could not find sound to play. Requested sound: " + fileName + ".");
                }
            } else {
                event.getAuthor().getPrivateChannel().sendMessage("I can not find a voice channel you are connected to.");
                LOG.warn("no guild to play to.");
            }
        }
    }

    /**
     * Plays the fileName requested for a voice channel entrance.
     * @param fileName - The name of the file to play.
     * @param event -  The even that triggered the sound playing request. The event is used to find the channel to play
     *              the sound back in.
     * @throws Exception Throws exception if entrance sounds couldn't be played
     */
    public void playFileForEntrance(String fileName, VoiceJoinEvent event) throws Exception {
        if (event == null) return;
        try {
            moveToChannel(event.getChannel(), event.getGuild());
            LOG.info("Playing file for entrance of user: " + fileName);
            try {
                playFile(fileName, event.getGuild());
            } catch (SoundPlaybackException e) {
                LOG.info("Could not find any sound to play for entrance of user: " + fileName);
            }
            if (leaveAfterPlayback) {
                disconnectFromChannel(event.getGuild());
            }
        } catch (SoundPlaybackException e) {
            LOG.debug(e.toString());
        }
    }

    /**
     * Plays the fileName requested for a voice channel disconnect.
     * @param fileName - The name of the file to play.
     * @param event -  The even that triggered the sound playing request. The event is used to find the channel to play
     *              the sound back in.
     * @throws Exception Throws exception if disconnect sounds couldn't be played
     */
    public void playFileForDisconnect(String fileName, VoiceLeaveEvent event) throws Exception {
        if (event == null) return;
        try {
            moveToChannel(event.getOldChannel(), event.getGuild());
            LOG.info("Playing file for disconnect of user: " + fileName);
            try {
                playFile(fileName, event.getGuild());
            } catch (SoundPlaybackException e) {
                LOG.info("Could not find any sound to play for disconnect of user: " + fileName);
            }
            if (leaveAfterPlayback) {
                disconnectFromChannel(event.getGuild());
            }
        } catch (SoundPlaybackException e) {
            LOG.debug(e.toString());
        }
    }

    /**
     * Stops sound playback and returns true or false depending on if playback was stopped.
     * @return boolean representing whether playback was stopped.
     */
    public boolean stop() {
	    return stop(0);
    }

    /**
     * Stops sound playback and returns true or false depending on if playback was stopped.
     * @return boolean representing whether playback was stopped.
     */
    public boolean stop(int timeout) {
	    boolean result = false;
	    float originalVolume = playerVolume;
	    
	    
        if (isMusicPlayer()) {
            if (musicPlayer != null && musicPlayer.isPlaying()) {
          	    setSoundPlayerVolume(0, timeout);
                musicPlayer.stop();
                playerVolume = originalVolume;
                return true;
            } else {
                return false;
            }
        } else {
            if (player != null && player.isPlaying()) {
          	    setSoundPlayerVolume(0, timeout);
                player.stop();
                playerVolume = originalVolume;
                return true;
            } else {
                return false;
            }
        }
    }

    /**
     * Get a list of users
     * @return List of soundboard users.
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

    public boolean isUserAllowed(String username) {
        if (allowedUsers == null) {
            return true;
        } else if (allowedUsers.isEmpty()){
            return true;
        } else if (allowedUsers.contains(username)){
            return true;
        } else {
            return false;
        }
    }

    public boolean isUserBanned(String username) {
        return bannedUsers != null && !bannedUsers.isEmpty() && bannedUsers.contains(username);
    }

    /**
     * Get the path the application is using for sound files.
     * @return String representation of the sound file path.
     */
    public String getSoundsPath() {
        return soundFileDir;
    }

    private SoundFile getSoundFileById(String soundFileId) {
        return repository.findOneBySoundFileIdIgnoreCase(soundFileId);
    }

    /**
     * Find the "author" of the event and join the voice channel they are in.
     * @param event - The event
     * @throws Exception Throws exception if the bot couldn't join the users channel.
     */
    private void moveToUserIdsChannel(MessageReceivedEvent event, Guild guild) throws Exception {
        VoiceChannel channel = findUsersChannel(event, guild);

        if (channel == null) {
            event.getAuthor().getPrivateChannel()
                    .sendMessage("Hello @"+ event.getAuthor().getUsername() + "! I can not find you in any Voice Channel. Are you sure you are connected to voice?.");
            LOG.warn("Problem moving to requested users channel. Maybe user, " + event.getAuthor().getUsername() + " is not connected to Voice?");
        } else {
            moveToChannel(channel, guild);
        }
    }

    /**
     * Moves to the specified voice channel.
     * @param channel - The channel specified.
     */
    private void moveToChannel(VoiceChannel channel, Guild guild) throws SoundPlaybackException {
        boolean hasPermissionToSpeak = PermissionUtil.checkPermission(channel, bot.getUserById(bot.getSelfInfo().getId()),
                                                                                    Permission.VOICE_SPEAK);
        if (hasPermissionToSpeak) {
            AudioManager audioManager = bot.getAudioManager(guild);
            if (audioManager.isConnected()) {
                if (audioManager.isAttemptingToConnect()) {
                    audioManager.closeAudioConnection();
                }
                audioManager.moveAudioConnection(channel);
            } else {
                audioManager.openAudioConnection(channel);
            }

            int i = 0;
            int waitTime = 100;
            int maxIterations = 40;
            //Wait for the audio connection to be ready before proceeding.
            synchronized (this) {
                while (!audioManager.isConnected()) {
                    try {
                        wait(waitTime);
                        i++;
                        if (i >= maxIterations) {
                            break; //break out if after 1 second it doesn't get a connection;
                        }
                    } catch (InterruptedException e) {
                        LOG.warn("Waiting for audio connection was interrupted.");
                    }
                }
            }
        } else {
            throw new SoundPlaybackException("The bot does not have permission to speak in the requested channel: " + channel.getName() + ".");
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

    /**
     * Join the users current channel.
     */
    private void joinUsersCurrentChannel(String userName) throws SoundPlaybackException {
        for (Guild guild : bot.getGuilds()) {
            for (VoiceChannel channel : guild.getVoiceChannels()) {
                for (net.dv8tion.jda.entities.User user : channel.getUsers()) {
                    if(user.getUsername().equalsIgnoreCase(userName)) {
                        try {
                            moveToChannel(channel, guild);
                        } catch (SoundPlaybackException e) {
                            LOG.fatal(e.toString());
                            throw e;
                        }
                    }
                }
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
    private void playFile(String fileName, Guild guild) throws SoundPlaybackException {
        SoundFile fileToPlay = getSoundFileById(fileName);
        if (fileToPlay != null) {
            File soundFile = new File(fileToPlay.getSoundFileLocation());
            playFile(soundFile, guild);
        } else {
            throw new SoundPlaybackException("Could not find sound file that was requested.");
        }
    }

    /**
     * Play the provided File object
     * @param audioFile - The File object to play.
     * @param guild - The guild (discord server) the playback is going to happen in.
     */
    private void playFile(File audioFile, Guild guild) {
	    playFile(audioFile, guild, 1);
    }

    /**
     * Play the provided File object
     * @param audioFile - The File object to play.
     * @param guild - The guild (discord server) the playback is going to happen in.
     * @param repeatNumber - The number of times to repeat the audio file.
     */
    private void playFile(File audioFile, Guild guild, int repeatNumber) {
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

				if (repeatNumber > 0) {
	                for (int i = 0; i < repeatNumber; i++) {
	                    musicPlayer.getAudioQueue().add(audioSource);
	                }
					musicPlayer.setRepeat(false);
                } else {
	                musicPlayer.getAudioQueue().add(audioSource);
	                musicPlayer.setRepeat(true);
                }

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

    private void playUrl(String url, Guild guild) throws SoundPlaybackException {
        if (guild == null) {
            LOG.fatal("Guild is null. Have you added your bot to a guild? https://discordapp.com/developers/docs/topics/oauth2");
        } else {
            if (isMusicPlayer()) {
                if (bot.getAudioManager(guild).getSendingHandler() == null) {
                    bot.getAudioManager(guild).setSendingHandler(musicPlayer);
                }

                musicPlayer.stop();
                musicPlayer.getAudioQueue().clear();

                AudioSource audioSource = new RemoteSource(url, guild.getId());
                musicPlayer.getAudioQueue().add(audioSource);

                musicPlayer.setVolume(playerVolume);
                bot.getAudioManager(guild).setConnectTimeout(100L);

                musicPlayer.play();
            } else {
                throw new SoundPlaybackException("URL playback not supported by this player");
            }
        }
    }

    /**
     * Helper method that tells us if we are using MusicPlayer. If false we are using FilePlayer.
     * @return boolean
     */
    private boolean isMusicPlayer() {
        return playerSetting != null && playerSetting.equalsIgnoreCase("musicPlayer");
    }

    /**
     * This method loads the files. This checks if you are running from a .jar file and loads from the /sounds dir relative
     * to the jar file. If not it assumes you are running from code and loads relative to your resource dir.
     * @return Map of the current list of available sound files.
     */
    private Map<String,SoundFile> getFileList() {
        Map<String,SoundFile> returnFiles = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
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
                    repository.save(soundFile);
                    returnFiles.put(fileName, soundFile);
                }
            });
        } catch (IOException e) {
            LOG.fatal(e.toString());
            e.printStackTrace();
        }
        return returnFiles;
    }

    /**
     * Logs the discord bot in and adds the ChatSoundBoardListener if the user configured it to be used
     */
    private void initializeDiscordBot() {
        try {
            if (bot != null) {
                bot.shutdown();
            }

            String botToken = appProperties.getProperty("bot_token");
            bot = new JDABuilder()
                    .setAudioEnabled(true)
                    .setAutoReconnect(true)
                    .setBotToken(botToken)
                    .buildBlocking();

            if (Boolean.valueOf(appProperties.getProperty("respond_to_chat_commands"))) {
                String commandCharacter = appProperties.getProperty("command_character");
                String messageSizeLimit = appProperties.getProperty("message_size_limit");
                String leaveSuffix = appProperties.getProperty("leave_suffix");
                String respondToDmsString = appProperties.getProperty("respond_to_dm");
                Boolean respondToDms = true;
                if (respondToDmsString != null) {
                    respondToDms = Boolean.valueOf(respondToDmsString);
                }
                ChatSoundBoardListener chatListener = new ChatSoundBoardListener(this, commandCharacter,
                                                                                    messageSizeLimit, respondToDms);
                this.addBotListener(chatListener);
                EntranceSoundBoardListener entranceListener = new EntranceSoundBoardListener(this);
                LeaveSoundBoardListener leaveSoundBoardListener = new LeaveSoundBoardListener(this, leaveSuffix);
                this.addBotListener(entranceListener);
                this.addBotListener(leaveSoundBoardListener);
            }

            String allowedUsersString = appProperties.getProperty("allowedUsers");
            if (allowedUsersString != null) {
                String[] allowedUsersArray = allowedUsersString.trim().split(",");
                if (allowedUsersArray.length > 0) {
                    allowedUsers = Arrays.asList(allowedUsersArray);
                }
            }

            String bannedUsersString = appProperties.getProperty("bannedUsers");
            if (bannedUsersString != null) {
                String[] bannedUsersArray = bannedUsersString.split(",");
                if (bannedUsersArray.length > 0) {
                    bannedUsers = Arrays.asList(bannedUsersArray);
                }
            }

            bot.getAccountManager().setGame("Type " + appProperties.getProperty("command_character") + "help for a list of commands.");

//            File avatarFile = new File(System.getProperty("user.dir") + "/avatar.jpg");
//            AvatarUtil.Avatar avatar = AvatarUtil.getAvatar(avatarFile);
//            bot.getAccountManager().setAvatar(avatar).update();
        }
        catch (IllegalArgumentException e) {
            LOG.warn("The config was not populated. Please enter an email and password.");
        }
        catch (LoginException e) {
            LOG.warn("The provided bot token was incorrect. Please provide valid details.");
        } catch (InterruptedException e) {
            LOG.fatal("Login Interrupted.");
        } //catch (UnsupportedEncodingException e) {
//            LOG.warn("Could not update avatar with provided file.");
//        }
    }

    private void disconnectFromChannel(Guild guild) {
        bot.getAudioManager(guild).closeAudioConnection();
        LOG.info("Disconnecting from channel.");
    }

    /**
     * Loads in the properties from the app.properties file
     */
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

    @PreDestroy
    @SuppressWarnings("unused")
    public void cleanUp() throws Exception {
        System.out.println("SoundPlayer is shutting down. Cleaning up.");
        bot.shutdown();
    }

    /**
     * Sets listeners
     * @param listener - The listener object to set.
     */
    private void addBotListener(Object listener) {
        bot.addEventListener(listener);
    }
}
