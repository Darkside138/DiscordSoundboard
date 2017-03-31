package net.dirtydeeds.discordsoundboard.service;

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.local.LocalAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import net.dirtydeeds.discordsoundboard.*;
import net.dirtydeeds.discordsoundboard.beans.SoundFile;
import net.dirtydeeds.discordsoundboard.beans.User;
import net.dirtydeeds.discordsoundboard.repository.SoundFileRepository;
import net.dv8tion.jda.core.*;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.entities.impl.GameImpl;
import net.dv8tion.jda.core.events.guild.voice.GenericGuildVoiceEvent;
import net.dv8tion.jda.core.events.guild.voice.GuildVoiceLeaveEvent;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.exceptions.PermissionException;
import net.dv8tion.jda.core.exceptions.RateLimitedException;
import net.dv8tion.jda.core.managers.AudioManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.security.auth.login.LoginException;
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
 * This class handles moving into channels and playing sounds. Also, it loads the available sound files
 * and the configuration properties.
 */
@Service
public class SoundPlayerImpl implements Observer {

    private final Logger LOG = LoggerFactory.getLogger(this.getClass());
    
    private Properties appProperties;
    private JDA bot;
    private float playerVolume = (float) .75;
    private final MainWatch mainWatch;
    private boolean initialized = false;
    private AudioPlayerManager playerManager;
    private String soundFileDir;
    private List<String> allowedUsers;
    private List<String> bannedUsers;
    private SoundFileRepository repository;
    private boolean leaveAfterPlayback = false;
    private final Map<String, GuildMusicManager> musicManagers;

    @Inject
    public SoundPlayerImpl(MainWatch mainWatch, SoundFileRepository repository) {
        this.musicManagers = new HashMap<>();

        this.mainWatch = mainWatch;
        this.mainWatch.addObserver(this);
        this.repository = repository;

        loadProperties();
        initializeDiscordBot();
        getFileList();

        this.playerManager = new DefaultAudioPlayerManager();
        this.playerManager.registerSourceManager(new LocalAudioSourceManager());
        this.playerManager.registerSourceManager(new YoutubeAudioSourceManager());

        this.leaveAfterPlayback = Boolean.valueOf(appProperties.getProperty("leaveAfterPlayback"));

        this.initialized = true;
    }

    private GuildMusicManager getGuildAudioPlayer(Guild guild) {
        String guildId = guild.getId();
        GuildMusicManager mng = musicManagers.get(guildId);
        if (mng == null) {
            synchronized (musicManagers) {
                mng = musicManagers.computeIfAbsent(guildId, k -> new GuildMusicManager(playerManager));
            }
        }
        return mng;
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
    public void setSoundPlayerVolume(int volume, String username) {
        playerVolume = (float) volume / 100;
        Guild guild = getUsersGuild(username);
        GuildMusicManager gmm = getGuildAudioPlayer(guild);
        gmm.player.setVolume(volume);
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
                LOG.error("Could not play random file: " + randomValue.getSoundFileId());
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

            playFile(url, guild, 0);

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
    private void playFileForEvent(String fileName, MessageReceivedEvent event) throws Exception {
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
                guild = getUsersGuild(event.getAuthor().getName());
            }
            if (guild != null) {
                if (fileToPlay != null) {
                    try {
                        moveToUserIdsChannel(event, guild);
                    } catch (SoundPlaybackException e) {
                        event.getAuthor().getPrivateChannel().sendMessage(e.getLocalizedMessage());
                    }
                    File soundFile = new File(fileToPlay.getSoundFileLocation());
                    playFile(soundFile.getAbsolutePath(), guild, repeatNumber);

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
     * This doesn't play anything, but since all of these actions are currently in the soundplayer service,
     * we keep this code here. The Bot will switch channels and stop.
     * @param event
     * @throws Exception
     */
    public void playNothingForEvent(MessageReceivedEvent event) throws Exception {
        if (event != null) {
            Guild guild = event.getGuild();
            if (guild == null) {
                guild = getUsersGuild(event.getAuthor().getName());
            }
            if (guild != null) {
                    try {
                        moveToUserIdsChannel(event, guild);
                    } catch (SoundPlaybackException e) {
                        event.getAuthor().getPrivateChannel().sendMessage(e.getLocalizedMessage());
                    }
            } else {
                event.getAuthor().getPrivateChannel().sendMessage("I can not find a voice channel you are connected to.");
                LOG.warn("no guild to join.");
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
    public void playFileForEntrance(String fileName, GenericGuildVoiceEvent event, VoiceChannel channel) throws Exception {
        if (event == null) return;
        try {
            moveToChannel(channel, event.getGuild());
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
    public void playFileForDisconnect(String fileName, GuildVoiceLeaveEvent event) throws Exception {
        if (event == null) return;
        try {
            moveToChannel(event.getChannelLeft(), event.getGuild());
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
    public boolean stop(String requestingUser) {
        Guild guild = getUsersGuild(requestingUser);
        GuildMusicManager mng = getGuildAudioPlayer(guild);
        AudioPlayer player = mng.player;
        player.stopTrack();
        return true;
    }

    /**
     * Get a list of users
     * @return List of soundboard users.
     */
    public List<net.dirtydeeds.discordsoundboard.beans.User> getUsers() {
        String userNameToSelect = appProperties.getProperty("username_to_join_channel");
        List<User> users = new ArrayList<>();
        for (Guild guild : bot.getGuilds()) {
            for (Member member : guild.getMembers()) {
                boolean selected = false;
                String username = member.getUser().getName();
                if (userNameToSelect.equals(username)) {
                    selected = true;
                }
                users.add(new net.dirtydeeds.discordsoundboard.beans.User(member.getUser().getId(), username, member.getOnlineStatus().name(), selected));
            }
        }
        Comparator<User> c = Comparator.comparing(User::getUsernameLowerCase);
        users.sort(c);
        return users;
    }

    public boolean isUserAllowed(String username) {
        if (allowedUsers == null) {
            return true;
        } else if (!allowedUsers.isEmpty() && allowedUsers.contains(username)){
            return true;
        } else {
            return true;
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
                    .sendMessage("Hello @"+ event.getAuthor().getName() + "! I can not find you in any Voice Channel. Are you sure you are connected to voice?.");
            LOG.warn("Problem moving to requested users channel. Maybe user, " + event.getAuthor().getName() + " is not connected to Voice?");
        } else {
            moveToChannel(channel, guild);
        }
    }

    /**
     * Moves to the specified voice channel.
     * @param channel - The channel specified.
     */
    private void moveToChannel(VoiceChannel channel, Guild guild) throws SoundPlaybackException {
        GuildMusicManager mng = getGuildAudioPlayer(guild);

        AudioManager audioManager = guild.getAudioManager();
        audioManager.setSendingHandler(mng.sendHandler);

        if (!audioManager.isAttemptingToConnect()) {
            try {
                guild.getAudioManager().openAudioConnection(channel);
            } catch (PermissionException e) {
                if (e.getPermission() == Permission.VOICE_CONNECT) {
                    throw new SoundPlaybackException("The bot does not have permission to speak in the requested channel: " + channel.getName() + ".");
                }
            }

            int i = 0;
            int waitTime = 100;
            int maxIterations = 80;
            //Wait for the audio connection to be ready before proceeding.
            synchronized (this) {
                while (!audioManager.isConnected() && audioManager.isAttemptingToConnect()) {
                    try {
                        wait(waitTime);
                        i++;
                        if (i >= maxIterations) {
                            break; //break out if after some time if it doesn't get a connection;
                        }
                    } catch (InterruptedException e) {
                        LOG.warn("Waiting for audio connection was interrupted.");
                    }
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
            for (Member user : channel1.getMembers()) {
                if (user.getUser().getId().equals(event.getAuthor().getId())) {
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
                for (Member user : channel.getMembers()) {
                    if(user.getUser().getName().equalsIgnoreCase(userName)) {
                        try {
                            moveToChannel(channel, guild);
                        } catch (SoundPlaybackException e) {
                            LOG.error(e.toString());
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
                for (Member user : channel.getMembers()) {
                    if (user.getUser().getName().equalsIgnoreCase(userName)) {
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
	    playFile(audioFile.getAbsolutePath(), guild, 1);
    }

    /**
     * Play the provided File object
     * @param audioFile - The File object to play.
     * @param guild - The guild (discord server) the playback is going to happen in.
     * @param repeatNumber - The number of times to repeat the audio file.
     */
    @Async
    private void playFile(String audioFile, Guild guild, int repeatNumber) {
        if (guild == null) {
            LOG.error("Guild is null. Have you added your bot to a guild? https://discordapp.com/developers/docs/topics/oauth2");
        } else {
            GuildMusicManager mng = getGuildAudioPlayer(guild);
            playerManager.loadItemOrdered(mng, audioFile, new AudioLoadResultHandler() {
                @Override
                public void trackLoaded(AudioTrack track) {
                    if (repeatNumber > 1) {
                        for (int i = 0; i <= repeatNumber - 1; i++) {
                            if (i == 0) {
                                mng.scheduler.playNow(track);
                            } else {
                                LOG.info("Queuing additional play of track.");
                                mng.scheduler.queue(track.makeClone());
                            }
                        }
                    } else if (repeatNumber < 0) {
                        mng.scheduler.playNow(track);
                        mng.scheduler.setRepeating(true);
                    } else {
                        mng.scheduler.playNow(track);
                    }
                }

                @Override
                public void playlistLoaded(AudioPlaylist playlist) {
                    AudioTrack firstTrack = playlist.getSelectedTrack();

                    if (firstTrack == null) {
                        firstTrack = playlist.getSelectedTrack();
                    }

                    mng.scheduler.playNow(firstTrack);
                }

                @Override
                public void noMatches() {
                    // Notify the user that we've got nothing
                    LOG.debug("Could not find file");
                }

                @Override
                public void loadFailed(FriendlyException throwable) {
                    // Notify the user that everything exploded
                    LOG.error(throwable.getMessage());
                }
            });
        }
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
                    LOG.error("Could not create directory: " + soundFilePath.toFile().toString());
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
                    SoundFile existing = repository.findOneBySoundFileIdIgnoreCase(fileName);
                    if (existing != null) {
                        repository.delete(existing);
                    }
                    repository.save(soundFile);
                    returnFiles.put(fileName, soundFile);
                }
            });
        } catch (IOException e) {
            LOG.error(e.toString());
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
            bot = new JDABuilder(AccountType.BOT)
                    .setAudioEnabled(true)
                    .setAutoReconnect(true)
                    .setToken(botToken)
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
                EntranceSoundBoardListener entranceListener = new EntranceSoundBoardListener(this);
                LeaveSoundBoardListener leaveSoundBoardListener = new LeaveSoundBoardListener(this, leaveSuffix);

                this.addBotListener(chatListener);
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

            Game game = new GameImpl("Type " + appProperties.getProperty("command_character") + "help for a list of commands.", "", Game.GameType.DEFAULT);
            bot.getPresence().setGame(game);

            try {
                File avatarFile = new File(System.getProperty("user.dir") + "/avatar.jpg");
                Icon icon = Icon.from(avatarFile);
                bot.getSelfUser().getManager().setAvatar(icon).queue();
            } catch (IllegalArgumentException e) {
                LOG.warn("Could not find avatar file " + System.getProperty("user.dir") + "/avatar.jpg");
            }
        }
        catch (IllegalArgumentException e) {
            LOG.warn("Something was configured incorrectly.", e);
        }
        catch (LoginException e) {
            LOG.warn("The provided bot token was incorrect. Please provide valid details.", e);
        } catch (InterruptedException e) {
            LOG.error("Login Interrupted.", e);
        } //catch (UnsupportedEncodingException e) {
        catch (RateLimitedException e) {
            LOG.error("Login rate exceeded.", e);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void disconnectFromChannel(Guild guild) {
        guild.getAudioManager().setSendingHandler(null);
        guild.getAudioManager().closeAudioConnection();
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
                    LOG.error("You do not have an app.properties file. Please create one.");
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
        playerManager.shutdown();
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
