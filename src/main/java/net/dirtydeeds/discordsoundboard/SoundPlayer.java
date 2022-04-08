package net.dirtydeeds.discordsoundboard;

import com.sedmelluq.discord.lavaplayer.natives.ConnectorNativeLibLoader;
import net.dirtydeeds.discordsoundboard.beans.SoundFile;
import net.dirtydeeds.discordsoundboard.beans.User;
import net.dirtydeeds.discordsoundboard.listeners.ChatSoundBoardListener;
import net.dirtydeeds.discordsoundboard.listeners.EntranceSoundBoardListener;
import net.dirtydeeds.discordsoundboard.listeners.LeaveSoundBoardListener;
import net.dirtydeeds.discordsoundboard.listeners.MovedChannelListener;
import net.dirtydeeds.discordsoundboard.repository.SoundFileRepository;
import net.dirtydeeds.discordsoundboard.handlers.AudioHandler;
import net.dirtydeeds.discordsoundboard.service.SoundService;
import net.dirtydeeds.discordsoundboard.service.UserService;
import net.dirtydeeds.discordsoundboard.util.ShutdownManager;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.managers.AudioManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext;
import org.springframework.stereotype.Component;

import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.*;
import java.nio.file.*;
import java.util.*;

import static java.util.Map.*;

/**
 * @author dfurrer.
 * <p>
 * This class handles moving into channels and playing sounds. Also, it loads the available sound files
 * and the configuration properties.
 */
@Component
@Singleton
public class SoundPlayer {

    private static final Logger LOG = LoggerFactory.getLogger(SoundPlayer.class);

    private final SoundService soundService;
    private final UserService userService;
    private final MainWatch mainWatch;
    @Value("${spring.application.version:unknown}")
    @SuppressWarnings("unused")
    private String applicationVersion;
    @SuppressWarnings("unused")
    @Autowired
    private ServletWebServerApplicationContext webServerAppCtxt;
    private final ShutdownManager shutdownManager;
    private final BotConfig botConfig;
    private JDA bot;
    private JDABot jdaBot;

    @Inject
    public SoundPlayer(MainWatch mainWatch, SoundService soundService,
                       UserService userService, ShutdownManager shutdownManager, BotConfig botConfig) {
        this.mainWatch = mainWatch;
        this.mainWatch.setSoundPlayer(this);
        this.soundService = soundService;
        this.userService = userService;
        this.shutdownManager = shutdownManager;
        this.botConfig = botConfig;

        init();
    }

    private void init() {
        jdaBot = new JDABot(botConfig);
        bot = jdaBot.getJda();
        if (bot == null) {
            shutdownManager.initiateShutdown(0);
            return;
        }
        updateFileList();
        getUsers();

        if (botConfig.isRespondToChatCommands()) {
            boolean respondToDms = true;
            if (botConfig.getRespondToDmsString() != null) {
                respondToDms = Boolean.parseBoolean(botConfig.getRespondToDmsString());
            }
            bot.addEventListener(new ChatSoundBoardListener(this, botConfig, respondToDms,
                    userService, soundService));
        }

        bot.addEventListener(new EntranceSoundBoardListener(this, userService,
                botConfig.isPlayEntranceOnJoin(), botConfig));
        bot.addEventListener(new LeaveSoundBoardListener(this, userService));
        bot.addEventListener(new MovedChannelListener(this, userService,
                botConfig.isPlayEntranceOnMove(), botConfig));

        ConnectorNativeLibLoader.loadConnectorLibrary();

        mainWatch.watchDirectoryPath(Paths.get(botConfig.getSoundFileDir()));
    }

    public String getApplicationVersion() {
        return applicationVersion;
    }

    public ServletWebServerApplicationContext getApplicationContext() {
        return webServerAppCtxt;
    }

    /**
     * Gets a Map of the loaded sound files.
     *
     * @return Map of sound files that have been loaded.
     */
    public Map<String, SoundFile> getAvailableSoundFiles() {
        Map<String, SoundFile> returnFiles = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        for (SoundFile soundFile : soundService.findAll()) {
            returnFiles.put(soundFile.getSoundFileId(), soundFile);
        }
        return returnFiles;
    }

    /**
     * Sets volume of the player.
     *
     * @param volume - The volume value to set.
     */
    public void setSoundPlayerVolume(int volume, String user) {
        Guild guild = getUsersGuild(user);
        if (guild != null) {
            AudioHandler handler = (AudioHandler) guild.getAudioManager().getSendingHandler();
            if (handler != null) {
                handler.getPlayer().setVolume(volume);
            }
        }
    }

    /**
     * Returns the current volume
     *
     * @return float representing the current volume.
     */
    public float getSoundPlayerVolume(String user) {
        Guild guild = getUsersGuild(user);
        if (guild != null) {
            AudioHandler handler = (AudioHandler) guild.getAudioManager().getSendingHandler();
            if (handler != null) {
                return handler.getPlayer().getVolume();

            }
        }
        return 0;
    }

    public void playRandomSoundFile(String requestingUser, MessageReceivedEvent event) throws SoundPlaybackException {
        try {
            Map<String, SoundFile> sounds = getAvailableSoundFiles();
            List<String> keysAsArray = new ArrayList<>(sounds.keySet());
            Random r = new Random();
            SoundFile randomValue = sounds.get(keysAsArray.get(r.nextInt(keysAsArray.size())));

            LOG.info("Attempting to play random file: " + randomValue.getSoundFileId() + ", requested by : " + requestingUser);
            try {
                if (event != null) {
                    if (event.getChannelType().equals(ChannelType.PRIVATE)) {
                        playFileForUser(randomValue.getSoundFileId(), requestingUser, 1);
                    } else {
                        playFileForEvent(randomValue.getSoundFileId(), event);
                    }
                } else {
                    playFileForUser(randomValue.getSoundFileId(), requestingUser, 1);
                }

                if (botConfig.isLeaveAfterPlayback()) {
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
     *
     * @param fileName - The name of the file to play.
     * @param userName - The name of the user to lookup what VoiceChannel they are in.
     */
    public void playFileForUser(String fileName, String userName, Integer repeatTimes) {
        if (userName == null || userName.isEmpty()) {
            userName = botConfig.getBotOwnerName();
        }
        try {
            Guild guild = getUsersGuild(userName);
            joinUsersCurrentChannel(userName);

            playFile(fileName, guild, repeatTimes);

            if (botConfig.isLeaveAfterPlayback()) {
                disconnectFromChannel(guild);
            }
        } catch (Exception e) {
            LOG.warn("Could not find requested filename {}", fileName);
        }
    }

    public void playUrlForUser(String url, String userName) {
        if (userName == null || userName.isEmpty()) {
            userName = botConfig.getBotOwnerName();
        }
        try {
            Guild guild = getUsersGuild(userName);
            joinUsersCurrentChannel(userName);

            playFileString(url, guild, 1);

            if (botConfig.isLeaveAfterPlayback()) {
                disconnectFromChannel(guild);
            }
        } catch (Exception e) {
            LOG.warn("Could not find requested url {}", url);
        }
    }

    /**
     * Plays the fileName requested.
     *
     * @param fileName     - The name of the file to play.
     * @param event        -  The event that triggered the sound playing request. The event is used to find the channel to play
     *                     the sound back in.
     */
    private void playFileForEvent(String fileName, MessageReceivedEvent event) {
        SoundFile fileToPlay = soundService.findOneBySoundFileIdIgnoreCase(fileName);
        if (event != null) {
            Guild guild = event.getGuild();
            if (fileToPlay != null) {
                moveToUserIdsChannel(event, guild);

                File soundFile = new File(fileToPlay.getSoundFileLocation());
                playFile(soundFile, guild, 1);

                if (botConfig.isLeaveAfterPlayback()) {
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
            playFile(fileName, channel.getGuild(), 1);
        } catch (SoundPlaybackException e) {
            LOG.info("Could not find any sound to play for channel movement of user: " + fileName);
        }
        if (botConfig.isLeaveAfterPlayback()) {
            disconnectFromChannel(channel.getGuild());
        }
    }

    /**
     * Play file name requested. Will first try to load the file from the map of available sounds.
     *
     * @param fileName - fileName to play.
     */
    private void playFile(String fileName, Guild guild, Integer repeatTimes) throws SoundPlaybackException {
        SoundFile fileToPlay = soundService.findOneBySoundFileIdIgnoreCase(fileName);
        if (fileToPlay != null) {
            File soundFile = new File(fileToPlay.getSoundFileLocation());
            playFile(soundFile, guild, repeatTimes);
        } else {
            throw new SoundPlaybackException("Could not find sound file that was requested.");
        }
    }

    /**
     * Play the provided File object
     *
     * @param audioFile    - The File object to play.
     * @param guild        - The guild (discord server) the playback is going to happen in.
     * @param repeatTimes - The number of times to repeat the audio file.
     */
    private void playFile(File audioFile, Guild guild, int repeatTimes) {
        if (guild == null) {
            LOG.error("Guild is null or you're not in a voice channel the bot has permission to access. Have you added your bot to a guild? https://discord.com/developers/docs/topics/oauth2");
        } else {
            playFileString(audioFile.getAbsolutePath(), guild, repeatTimes);
        }
    }

    private void playFileString(String whatToPlay, Guild guild, int repeatTimes) {
        jdaBot.getPlayerManager().loadItem(whatToPlay, new FileLoadResultHandler(guild, repeatTimes));
    }

    public String getFileForUser(String userName, boolean entrance) {
        Set<Map.Entry<String, SoundFile>> entrySet = getAvailableSoundFiles().entrySet();
        String fileToPlay = "";
        if (entrySet.size() > 0) {
            for (Entry entry : entrySet) {
                String fileEntry = (String) entry.getKey();
                if (entrance) {
                    if (userName.toLowerCase().startsWith(fileEntry.toLowerCase())
                            && fileEntry.length() > fileToPlay.length())
                        fileToPlay = fileEntry;
                } else {
                    if (fileEntry.toLowerCase().startsWith(userName.toLowerCase()) &&
                            fileEntry.toLowerCase().endsWith(botConfig.getLeaveSuffix())
                            && fileEntry.length() > fileToPlay.length()) {
                        fileToPlay = fileEntry;
                    }
                }
            }
        }
        return fileToPlay;
    }

    /**
     * Stops sound playback and returns true or false depending on if playback was stopped.
     *
     * @return boolean representing whether playback was stopped.
     */
    public boolean stop(String user) {
        Guild guild = getUsersGuild(user);
        if (guild != null) {
            AudioHandler handler = (AudioHandler) guild.getAudioManager().getSendingHandler();
            if (handler != null) {
                handler.getPlayer().stopTrack();
                return true;
            }
        }

        return false;
    }

    /**
     * Get a list of users
     *
     * @return List of soundboard users.
     */
    public List<net.dirtydeeds.discordsoundboard.beans.User> getUsers() {
        String userNameToSelect = botConfig.getBotOwnerName();
        List<User> users = new ArrayList<>();
        for (net.dv8tion.jda.api.entities.User discordUser : bot.getUsers()) {
            if (discordUser.getJDA().getStatus().equals(JDA.Status.CONNECTED)) {
                boolean selected = false;
                String username = discordUser.getName();
                if (userNameToSelect != null && userNameToSelect.equals(username)) {
                    selected = true;
                }
                Optional<User> optionalUser = userService.findById(discordUser.getId());
                if (optionalUser.isPresent()) {
                    User user = optionalUser.get();
                    user.setSelected(selected);
                    users.add(user);
                } else {
                    users.add(new net.dirtydeeds.discordsoundboard.beans.User(discordUser.getId(), username, selected));
                }
            }
        }
        users.sort(Comparator.comparing(User::getUsername));
        userService.saveAll(users);
        return users;
    }

    public boolean isUserAllowed(String username) {
        if (botConfig.getAllowedUsersList() == null) {
            return true;
        } else if (botConfig.getAllowedUsersList().isEmpty()) {
            return true;
        } else return botConfig.getAllowedUsersList().contains(username);
    }

    public boolean isUserBanned(String username) {
        return botConfig.getBannedUsersList() != null && !botConfig.getBannedUsersList().isEmpty()
                && botConfig.getBannedUsersList().contains(username);
    }

    /**
     * Get the path the application is using for sound files.
     *
     * @return String representation of the sound file path.
     */
    public String getSoundsPath() {
        return botConfig.getSoundFileDir();
    }

    /**
     * This method loads the files. This checks if you are running from a .jar file and loads from the /sounds dir relative
     * to the jar file. If not it assumes you are running from code and loads relative to your resource dir.
     */
    public void updateFileList() {
        try {
            String soundFileDir = botConfig.getSoundFileDir();
            if (soundFileDir == null || soundFileDir.isEmpty()) {
                soundFileDir = System.getProperty("user.dir") + "/sounds";
            }
            LOG.info("Loading from " + soundFileDir);
            Path soundFilePath = Paths.get(soundFileDir);

            if (!soundFilePath.toFile().exists()) {
                System.out.println("creating directory: " + soundFilePath.toFile());
                boolean result = false;

                try {
                    result = soundFilePath.toFile().mkdir();
                } catch (SecurityException se) {
                    LOG.error("Could not create directory: " + soundFilePath.toFile());
                }
                if (result) {
                    LOG.info("DIR: " + soundFilePath.toFile() + " created.");
                }
            }

            soundService.deleteAll();

            Files.walk(soundFilePath).forEach(filePath -> {
                if (Files.isRegularFile(filePath)) {
                    String fileName = filePath.getFileName().toString();
                    fileName = fileName.substring(fileName.indexOf("/") + 1);
                    int fileExtensionPeriodIndex = fileName.lastIndexOf(".");
                    if (fileExtensionPeriodIndex > 0) {
                        fileName = fileName.substring(0, fileExtensionPeriodIndex);
                        LOG.info(fileName);
                        File file = filePath.toFile();
                        String parent = file.getParentFile().getName();
                        if (!soundService.existsById(fileName)) {
                            SoundFile soundFile = new SoundFile(fileName, filePath.toString(), parent);
                            soundService.save(soundFile);
                        }
                    }
                }
            });
        } catch (IOException e) {
            LOG.error(e.toString());
            e.printStackTrace();
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
                    if (user.getEffectiveName().equalsIgnoreCase(userName)
                            || user.getUser().getName().equalsIgnoreCase(userName)) {
                        return guild;
                    }
                }
            }
        }
        return null;
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
        AudioManager audioManager = guild.getAudioManager();

        audioManager.openAudioConnection(channel);

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
                    if (user.getEffectiveName().equalsIgnoreCase(userName)
                            || user.getUser().getName().equalsIgnoreCase(userName)) {
                        moveToChannel(channel, guild);
                    }
                }
            }
        }
    }

    public void disconnectFromChannel(Guild guild) {
        if (guild != null) {
            guild.getAudioManager().closeAudioConnection();
            LOG.info("Disconnecting from channel.");
        }
    }

    @PreDestroy
    @SuppressWarnings("unused")
    public void cleanUp() {
        System.out.println("SoundPlayer is shutting down. Cleaning up.");
        bot.shutdown();
    }
}
