package net.dirtydeeds.discordsoundboard.service;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.FunctionalResultHandler;
import com.sedmelluq.discord.lavaplayer.source.local.LocalAudioSourceManager;
import net.dirtydeeds.discordsoundboard.*;
import net.dirtydeeds.discordsoundboard.beans.SoundFile;
import net.dirtydeeds.discordsoundboard.beans.User;
import net.dirtydeeds.discordsoundboard.repository.SoundFileRepository;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.audio.AudioSendHandler;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.managers.AudioManager;
import org.apache.commons.logging.impl.SimpleLog;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.security.auth.login.LoginException;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * @author dfurrer.
 * <p>
 * This class handles moving into channels and playing sounds. Also, it loads the available sound files
 * and the configuration properties.
 */
@Service
public class SoundPlayerImpl implements Observer {

    private static final SimpleLog LOG = new SimpleLog("SoundPlayerImpl");

    private Properties appProperties;
    private JDA bot;
    private int playerVolume = 75;
    private final MainWatch mainWatch;
    private boolean initialized = false;
    private DefaultAudioPlayerManager playerManager;
    private AudioPlayer musicPlayer;
    private String soundFileDir;
    private List<String> allowedUsers;
    private List<String> bannedUsers;
    private SoundFileRepository repository;
    private boolean leaveAfterPlayback = false;
    private String leaveSuffix = "_leave";

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
        updateFileList();

        playerManager = new DefaultAudioPlayerManager();
        LocalAudioSourceManager localAudioSourceManager = new LocalAudioSourceManager();
        playerManager.registerSourceManager(localAudioSourceManager);

        musicPlayer = playerManager.createPlayer();

        leaveAfterPlayback = Boolean.parseBoolean(appProperties.getProperty("leaveAfterPlayback"));

        initialized = true;
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
                    .setAutoReconnect(true)
                    .setToken(botToken)
                    .build()
                    .awaitReady();

            if (Boolean.parseBoolean(appProperties.getProperty("respond_to_chat_commands"))) {
                String commandCharacter = appProperties.getProperty("command_character");
                String messageSizeLimit = appProperties.getProperty("message_size_limit");
                leaveSuffix = appProperties.getProperty("leave_suffix");
                String respondToDmsString = appProperties.getProperty("respond_to_dm");
                Boolean respondToDms = true;
                if (respondToDmsString != null) {
                    respondToDms = Boolean.valueOf(respondToDmsString);
                }
                ChatSoundBoardListener chatListener = new ChatSoundBoardListener(this, commandCharacter,
                        messageSizeLimit, respondToDms);
                this.addBotListener(chatListener);
                EntranceSoundBoardListener entranceListener = new EntranceSoundBoardListener(this);
                LeaveSoundBoardListener leaveSoundBoardListener = new LeaveSoundBoardListener(this);
                MovedChannelListener movedChannelListener = new MovedChannelListener(this);
//                this.addBotListener(entranceListener);
//                this.addBotListener(leaveSoundBoardListener);
                this.addBotListener(movedChannelListener);
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

            String activityString = appProperties.getProperty("activityString");
            if (StringUtils.isEmpty(activityString)) {
                bot.getPresence().setActivity(Activity.of(Activity.ActivityType.DEFAULT,
                        "Type " + appProperties.getProperty("command_character") + "help for a list of commands."));
            } else {
                bot.getPresence().setActivity(Activity.of(Activity.ActivityType.DEFAULT, activityString));
            }

        } catch (IllegalArgumentException e) {
            LOG.warn("The config was not populated. Please enter an email and password.");
        } catch (LoginException e) {
            LOG.warn("The provided bot token was incorrect. Please provide valid details.");
        } catch (InterruptedException e) {
            LOG.fatal("Login Interrupted.");
        }
    }

    @Override
    public void update(Observable o, Object arg) {
        updateFileList();
    }

    /**
     * Gets a Map of the loaded sound files.
     *
     * @return Map of sound files that have been loaded.
     */
    public Map<String, SoundFile> getAvailableSoundFiles() {
        Map<String, SoundFile> returnFiles = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        for (SoundFile soundFile : repository.findAll()) {
            returnFiles.put(soundFile.getSoundFileId(), soundFile);
        }
        return returnFiles;
    }

    /**
     * Sets volume of the player.
     *
     * @param volume - The volume value to set.
     */
    public void setSoundPlayerVolume(int volume) {
        setSoundPlayerVolume(volume, 1000);
    }

    /**
     * Sets volume of the player.
     *
     * @param volume - The volume value to set.
     */
    public void setSoundPlayerVolume(int volume, int timeout) {
        int currentVolume = playerVolume;

        int volumeDiff = currentVolume - volume;
        if (volumeDiff < 0) {
            volumeDiff = volumeDiff * -1;
        }

        if (volumeDiff != 0) {
            int microInterval = Math.round(timeout / volumeDiff);
            boolean goingUp = false;
            if (currentVolume < volume) {
                goingUp = true;
            }
            while (currentVolume != volume) {
                if (currentVolume < volume) {
                    currentVolume++;
                } else {
                    currentVolume--;
                }
                playerVolume = currentVolume;
                musicPlayer.setVolume(currentVolume);
                LOG.info("Setting player volume to: " + currentVolume);

                if (goingUp && currentVolume >= volume) {
                    break;
                } else if (!goingUp && currentVolume <= volume) {
                    break;
                }
                try {
                    TimeUnit.MILLISECONDS.sleep(microInterval);
                } catch (InterruptedException e) {
                    // mute...
                }
            }
        }
    }

    /**
     * Returns the current volume
     *
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
                    if (event.getChannelType().equals(ChannelType.PRIVATE)) {
                        playFileForUser(randomValue.getSoundFileId(), requestingUser);
                    } else {
                        playFileForEvent(randomValue.getSoundFileId(), event);
                    }
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
     *
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
     *
     * @param fileName - The name of the file to play.
     * @param event    -  The event that triggered the sound playing request. The event is used to find the channel to play
     *                 the sound back in.
     */
    private void playFileForEvent(String fileName, MessageReceivedEvent event) {
        playFileForEvent(fileName, event, 1);
    }

    /**
     * Plays the fileName requested.
     *
     * @param fileName     - The name of the file to play.
     * @param event        -  The event that triggered the sound playing request. The event is used to find the channel to play
     *                     the sound back in.
     * @param repeatNumber - the number of times to repeat the sound file
     */
    private void playFileForEvent(String fileName, MessageReceivedEvent event, int repeatNumber) {
        SoundFile fileToPlay = getSoundFileById(fileName);
        if (event != null) {
            Guild guild = event.getGuild();
            if (fileToPlay != null) {
                moveToUserIdsChannel(event, guild);

                File soundFile = new File(fileToPlay.getSoundFileLocation());
                playFile(soundFile, guild, repeatNumber);

                if (leaveAfterPlayback) {
                    disconnectFromChannel(event.getGuild());
                }
            } else {
                event.getAuthor().openPrivateChannel().complete().sendMessage("Could not find sound to play. Requested sound: " + fileName + ".").queue();
            }
        }
    }

    /**
     * Plays the fileName requested in the requested channel.
     *
     * @param fileName - The name of the file to play.
     * @param channel  -  The channel to play the file in
     */
    public void playFileInChannel(String fileName, VoiceChannel channel) {
        if (channel == null) return;
        moveToChannel(channel, channel.getGuild());
        LOG.info("Playing file for user: " + fileName + " in channel: " + channel.getName());
        try {
            playFile(fileName, channel.getGuild());
        } catch (SoundPlaybackException e) {
            LOG.info("Could not find any sound to play for channel movement of user: " + fileName);
        }
        if (leaveAfterPlayback) {
            disconnectFromChannel(channel.getGuild());
        }
    }

    /**
     * Stops sound playback and returns true or false depending on if playback was stopped.
     *
     * @return boolean representing whether playback was stopped.
     */
    public boolean stop() {
        musicPlayer.stopTrack();

        return true;
    }

    /**
     * Get a list of users
     *
     * @return List of soundboard users.
     */
    public List<net.dirtydeeds.discordsoundboard.beans.User> getUsers() {
        String userNameToSelect = appProperties.getProperty("username_to_join_channel");
        List<User> users = new ArrayList<>();
        for (net.dv8tion.jda.api.entities.User user : bot.getUsers()) {
            if (user.getJDA().getStatus().equals(JDA.Status.CONNECTED)) {
                boolean selected = false;
                String username = user.getName();
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
        } else if (allowedUsers.isEmpty()) {
            return true;
        } else return !allowedUsers.contains(username);
    }

    public boolean isUserBanned(String username) {
        return bannedUsers != null && !bannedUsers.isEmpty() && bannedUsers.contains(username);
    }

    /**
     * Get the path the application is using for sound files.
     *
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
     *
     * @param event - The event
     */
    private void moveToUserIdsChannel(MessageReceivedEvent event, Guild guild) {
        VoiceChannel channel = findUsersChannel(event, guild);

        if (channel == null) {
            event.getAuthor().openPrivateChannel().complete()
                    .sendMessage("Hello @" + event.getAuthor().getName() + "! I can not find you in any Voice Channel. Are you sure you are connected to voice?.").queue();
            LOG.warn("Problem moving to requested users channel. Maybe user, " + event.getAuthor().getName() + " is not connected to Voice?");
        } else {
            moveToChannel(channel, guild);
        }
    }

    /**
     * Moves to the specified voice channel.
     *
     * @param channel - The channel specified.
     */
    private void moveToChannel(VoiceChannel channel, Guild guild) {
//        boolean hasPermissionToSpeak = PermissionUtil.checkPermission(bot.getSelfUser(), Permission.VOICE_SPEAK);
//        if (hasPermissionToSpeak) {
        AudioManager audioManager = guild.getAudioManager();
        if (audioManager.isConnected()) {
            if (audioManager.isAttemptingToConnect()) {
                audioManager.closeAudioConnection();
            }
            audioManager.openAudioConnection(channel);
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
//        } else {
//            throw new SoundPlaybackException("The bot does not have permission to speak in the requested channel: " + channel.getName() + ".");
//        }
    }

    /**
     * Finds a users voice channel based on event and what guild to look in.
     *
     * @param event - The event that triggered this search. This is used to get th events author.
     * @param guild - The guild (discord server) to look in for the author.
     * @return The VoiceChannel if one is found. Otherwise return null.
     */
    private VoiceChannel findUsersChannel(MessageReceivedEvent event, Guild guild) {
        VoiceChannel channel = null;

        outerloop:
        for (VoiceChannel channel1 : guild.getVoiceChannels()) {
            for (Member user : channel1.getMembers()) {
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
    private void joinUsersCurrentChannel(String userName) {
        for (Guild guild : bot.getGuilds()) {
            for (VoiceChannel channel : guild.getVoiceChannels()) {
                for (Member user : channel.getMembers()) {
                    if (user.getEffectiveName().equalsIgnoreCase(userName)) {
                        moveToChannel(channel, guild);
                    }
                }
            }
        }
    }

    /**
     * Looks through all the guilds the bot has access to and returns the VoiceChannel the requested user is connected to.
     *
     * @param userName - The username to look for.
     * @return The voice channel the user is connected to. If user is not connected to a voice channel will return null.
     */
    private Guild getUsersGuild(String userName) {
        for (Guild guild : bot.getGuilds()) {
            for (VoiceChannel channel : guild.getVoiceChannels()) {
                for (Member user : channel.getMembers()) {
                    if (user.getEffectiveName().equalsIgnoreCase(userName)) {
                        return guild;
                    }
                }
            }
        }
        return null;
    }

    /**
     * Play file name requested. Will first try to load the file from the map of available sounds.
     *
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
     *
     * @param audioFile - The File object to play.
     * @param guild     - The guild (discord server) the playback is going to happen in.
     */
    private void playFile(File audioFile, Guild guild) {
        playFile(audioFile, guild, 1);
    }

    /**
     * Play the provided File object
     *
     * @param audioFile    - The File object to play.
     * @param guild        - The guild (discord server) the playback is going to happen in.
     * @param repeatNumber - The number of times to repeat the audio file.
     */
    private void playFile(File audioFile, Guild guild, int repeatNumber) {
        if (guild == null) {
            LOG.fatal("Guild is null. Have you added your bot to a guild? https://discordapp.com/developers/docs/topics/oauth2");
        } else {
            AudioManager audioManager = guild.getAudioManager();
            AudioSendHandler audioSendHandler = new MyAudioSendHandler(musicPlayer);
            audioManager.setSendingHandler(audioSendHandler);

            musicPlayer.setVolume(playerVolume);

            playerManager.loadItem(audioFile.getAbsolutePath(),
                    new FunctionalResultHandler(track -> musicPlayer.playTrack(track),
                            null, null, null));

        }
    }

    private void playUrl(String url, Guild guild) {
//        if (guild == null) {
//            LOG.fatal("Guild is null. Have you added your bot to a guild? https://discordapp.com/developers/docs/topics/oauth2");
//        } else {
//            if (isMusicPlayer()) {
//                if (bot.getAudioManager(guild).getSendingHandler() == null) {
//                    bot.getAudioManager(guild).setSendingHandler(musicPlayer);
//                }
//
//                musicPlayer.stop();
//                musicPlayer.getAudioQueue().clear();
//
//                AudioSource audioSource = new RemoteSource(url, guild.getId());
//                musicPlayer.getAudioQueue().add(audioSource);
//
//                musicPlayer.setVolume(playerVolume);
//                bot.getAudioManager(guild).setConnectTimeout(100L);
//
//                musicPlayer.play();
//            } else {
//                throw new SoundPlaybackException("URL playback not supported by this player");
//            }
//        }
    }

    public String getFileForUser(String userName, boolean entrance) {
        Set<Map.Entry<String, SoundFile>> entrySet = getAvailableSoundFiles().entrySet();
        String fileToPlay = "";
        if (entrySet.size() > 0) {
            for (Map.Entry entry : entrySet) {
                String fileEntry = (String) entry.getKey();
                if (entrance) {
                    if (userName.toLowerCase().startsWith(fileEntry.toLowerCase())
                            && fileEntry.length() > fileToPlay.length())
                        fileToPlay = fileEntry;
                } else {
                    if (fileEntry.toLowerCase().startsWith(userName.toLowerCase()) &&
                            fileEntry.toLowerCase().endsWith(leaveSuffix.toLowerCase())
                            && fileEntry.length() > fileToPlay.length()) {
                        fileToPlay = fileEntry;
                    }
                }
            }
        }
        return fileToPlay;
    }

    /**
     * This method loads the files. This checks if you are running from a .jar file and loads from the /sounds dir relative
     * to the jar file. If not it assumes you are running from code and loads relative to your resource dir.
     */
    private void updateFileList() {
        try {

            soundFileDir = appProperties.getProperty("sounds_directory");
            if (soundFileDir == null || soundFileDir.isEmpty()) {
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
                } catch (SecurityException se) {
                    LOG.fatal("Could not create directory: " + soundFilePath.toFile().toString());
                }
                if (result) {
                    LOG.info("DIR: " + soundFilePath.toFile().toString() + " created.");
                }
            }

            repository.deleteAll();

            Files.walk(soundFilePath).forEach(filePath -> {
                if (Files.isRegularFile(filePath)) {
                    String fileName = filePath.getFileName().toString();
                    fileName = fileName.substring(fileName.indexOf("/") + 1);
                    fileName = fileName.substring(0, fileName.indexOf("."));
                    LOG.info(fileName);
                    File file = filePath.toFile();
                    String parent = file.getParentFile().getName();
                    if (!repository.exists(fileName)) {
                        SoundFile soundFile = new SoundFile(fileName, filePath.toString(), parent);
                        repository.save(soundFile);
                    }
                }
            });
        } catch (IOException e) {
            LOG.fatal(e.toString());
            e.printStackTrace();
        }
    }


    private void disconnectFromChannel(Guild guild) {
        if (guild != null) {
            guild.getAudioManager().closeAudioConnection();
            LOG.info("Disconnecting from channel.");
        }
    }

    /**
     * Loads in the properties from the application.properties file
     */
    private void loadProperties() {
        appProperties = new Properties();
        InputStream stream = null;
        try {
            stream = new FileInputStream(System.getProperty("user.dir") + "/application.properties");
            appProperties.load(stream);
            stream.close();
            return;
        } catch (FileNotFoundException e) {
            LOG.warn("Could not find application.properties file.");
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (stream == null) {
            LOG.warn("Loading application.properties file from resources folder");
            try {
                stream = this.getClass().getResourceAsStream("/application.properties");
                if (stream != null) {
                    appProperties.load(stream);
                    stream.close();
                } else {
                    //TODO: Would be nice if we could auto create a default application.properties here.
                    LOG.fatal("You do not have an application.properties file. Please create one.");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @PreDestroy
    @SuppressWarnings("unused")
    public void cleanUp() {
        System.out.println("SoundPlayer is shutting down. Cleaning up.");
        bot.shutdown();
    }

    /**
     * Sets listeners
     *
     * @param listener - The listener object to set.
     */
    private void addBotListener(Object listener) {
        bot.addEventListener(listener);
    }
}
