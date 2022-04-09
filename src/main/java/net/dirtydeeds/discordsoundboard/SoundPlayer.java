package net.dirtydeeds.discordsoundboard;

import com.sedmelluq.discord.lavaplayer.natives.ConnectorNativeLibLoader;
import net.dirtydeeds.discordsoundboard.beans.SoundFile;
import net.dirtydeeds.discordsoundboard.beans.User;
import net.dirtydeeds.discordsoundboard.commands.*;
import net.dirtydeeds.discordsoundboard.controllers.response.ChannelResponse;
import net.dirtydeeds.discordsoundboard.listeners.*;
import net.dirtydeeds.discordsoundboard.handlers.AudioHandler;
import net.dirtydeeds.discordsoundboard.service.SoundService;
import net.dirtydeeds.discordsoundboard.service.UserService;
import net.dirtydeeds.discordsoundboard.util.ShutdownManager;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.managers.AudioManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.security.auth.login.LoginException;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;

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

        CommandListener commandListener = new CommandListener(botConfig);
        commandListener.addCommand(new DisconnectCommand(this));
        commandListener.addCommand(new EntranceCommand(this, userService, soundService));
        commandListener.addCommand(new HelpCommand(commandListener, botConfig));
        commandListener.addCommand(new InfoCommand(this, botConfig));
        commandListener.addCommand(new LeaveCommand(this, userService, soundService));
        commandListener.addCommand(new ListCommand(this, botConfig));
        commandListener.addCommand(new PlayCommand(this));
        commandListener.addCommand(new RandomCommand(this));
        commandListener.addCommand(new ReloadCommand(this));
        commandListener.addCommand(new RemoveCommand(this, botConfig, soundService));
        commandListener.addCommand(new StopCommand(this));
        commandListener.addCommand(new URLCommand(this));
        commandListener.addCommand(new UserDetailsCommand(userService, this));
        commandListener.addCommand(new VolumeCommand(this));

        bot.addEventListener(commandListener);
        bot.addEventListener(new EntranceSoundBoardListener(this, userService,
                botConfig.isPlayEntranceOnJoin(), botConfig));
        bot.addEventListener(new LeaveSoundBoardListener(this, userService));
        bot.addEventListener(new MovedChannelListener(this, userService,
                botConfig.isPlayEntranceOnMove(), botConfig));
        bot.addEventListener(new BotLeaveListener());
        bot.addEventListener(new FileAttachmentListener(botConfig));

        ConnectorNativeLibLoader.loadConnectorLibrary();

        mainWatch.watchDirectoryPath(Paths.get(botConfig.getSoundFileDir()));
    }

    /**
     * Logs the discord bot in and adds the ChatSoundBoardListener if the user configured it to be used
     */
//    private void initializeDiscordBot() {
//        try {
//            if (bot != null) {
//                bot.shutdown();
//            }
//
//            String botToken = appProperties.getProperty("bot_token");
//            if (botToken == null) {
//                LOG.error("No Discord Token found. Please confirm you have an application.properties file and you have the property bot_token filled with a valid token from https://discord.com/developers/applications");
//                return;
//            }
//
//            bot = JDABuilder.createDefault(botToken)
//                    .setAutoReconnect(true)
//                    .build();
//            bot.awaitReady();
//
//            if (Boolean.parseBoolean(appProperties.getProperty("respond_to_chat_commands"))) {
//                String commandCharacter = appProperties.getProperty("command_character");
//                String messageSizeLimit = appProperties.getProperty("message_size_limit");
//                leaveSuffix = appProperties.getProperty("leave_suffix");
//                String respondToDmsString = appProperties.getProperty("respond_to_dm");
//                boolean respondToDms = true;
//                if (respondToDmsString != null) {
//                    respondToDms = Boolean.parseBoolean(respondToDmsString);
//                }
//                ChatSoundBoardListener chatListener = new ChatSoundBoardListener(this, commandCharacter,
//                        messageSizeLimit, respondToDms, userRepository, soundFileRepository);
//                this.addBotListener(chatListener);
//                EntranceSoundBoardListener entranceSoundBoardListener = new EntranceSoundBoardListener(this,
//                        userRepository, playEntranceOnJoin);
//                LeaveSoundBoardListener leaveSoundBoardListener = new LeaveSoundBoardListener(this, userRepository);
//                MovedChannelListener movedChannelListener = new MovedChannelListener(this, userRepository,
//                        playEntranceOnMove);
//                this.addBotListener(entranceSoundBoardListener);
//                this.addBotListener(leaveSoundBoardListener);
//                this.addBotListener(movedChannelListener);
//            }
//
//            String allowedUsersString = appProperties.getProperty("allowedUsers");
//            if (allowedUsersString != null) {
//                if (!allowedUsersString.isEmpty()) {
//                    String[] allowedUsersArray = allowedUsersString.trim().split(",");
//                    if (allowedUsersArray.length > 0) {
//                        allowedUsers = Arrays.asList(allowedUsersArray);
//                    }
//                }
//            }
//
//            String bannedUsersString = appProperties.getProperty("bannedUsers");
//            if (bannedUsersString != null) {
//                if (!bannedUsersString.isEmpty()) {
//                    String[] bannedUsersArray = bannedUsersString.split(",");
//                    if (bannedUsersArray.length > 0) {
//                        bannedUsers = Arrays.asList(bannedUsersArray);
//                    }
//                }
//            }
//
//            String activityString = appProperties.getProperty("activityString");
//            if (ObjectUtils.isEmpty(activityString)) {
//                bot.getPresence().setActivity(Activity.of(Activity.ActivityType.DEFAULT,
//                        "Type " + appProperties.getProperty("command_character") + "help for a list of commands."));
//            } else {
//                bot.getPresence().setActivity(Activity.of(Activity.ActivityType.DEFAULT, activityString));
//            }
//
//        } catch (IllegalArgumentException e) {
//            LOG.warn("The config was not populated. Please enter an email and password.");
//        } catch (LoginException e) {
//            LOG.warn("The provided bot token was incorrect. Please provide valid details.");
//        } catch (InterruptedException e) {
//            LOG.fatal("Login Interrupted.");
//        }
//    }

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
     * @param voiceChannelId - The voice channel of the guild to set volume for
     */
    public void setSoundPlayerVolume(int volume, String user, String voiceChannelId) {
        Guild guild = getGuildForUserOrChannelId(user, voiceChannelId);
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
    public float getSoundPlayerVolume(String user, String voiceChannelId) {
        Guild guild = getGuildForUserOrChannelId(user, voiceChannelId);
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
                        playForUser(randomValue.getSoundFileId(), requestingUser, 1, null);
                    } else {
                        playFileForEvent(randomValue.getSoundFileId(), event);
                    }
                } else {
                    playForUser(randomValue.getSoundFileId(), requestingUser, 1, null);
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
     * @param fileName - The name of the file to play.
     * @param userName - The name of the user to lookup what VoiceChannel they are in.
     * @param voiceChannelId - The ID of the voice channel to play music in
     */
    public void playForUser(String fileName, String userName, Integer repeatTimes, String voiceChannelId) {
        if (userName == null || userName.isEmpty()) {
            userName = botConfig.getBotOwnerName();
        }
        try {
            Guild guild = getGuildForUserOrChannelId(userName, voiceChannelId);
            joinUsersCurrentChannel(userName, voiceChannelId);

            playFile(fileName, guild, repeatTimes);

            if (botConfig.isLeaveAfterPlayback()) {
                disconnectFromChannel(guild);
            }
        } catch (Exception e) {
            LOG.warn("Could not find requested filename {}", fileName);
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

        playFile(fileName, channel.getGuild(), 1);
        if (botConfig.isLeaveAfterPlayback()) {
            disconnectFromChannel(channel.getGuild());
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

                playFile(fileName, guild, 1);

                if (botConfig.isLeaveAfterPlayback()) {
                    disconnectFromChannel(event.getGuild());
                }
            } else {
                event.getAuthor().openPrivateChannel().complete().sendMessage("Could not find sound to play. Requested sound: " + fileName + ".").queue();
            }
        }
    }

    /**
     * Play file name requested. Will first try to load the file from the map of available sounds.
     *
     * @param fileName - fileName to play.
     */
    private void playFile(String fileName, Guild guild, Integer repeatTimes) {
        SoundFile fileToPlay = soundService.findOneBySoundFileIdIgnoreCase(fileName);
        if (fileToPlay != null) {
            File soundFile = new File(fileToPlay.getSoundFileLocation());
            if (guild == null) {
                LOG.error("Guild is null or you're not in a voice channel the bot has permission to access. Have you added your bot to a guild? https://discord.com/developers/docs/topics/oauth2");
            } else {
                jdaBot.getPlayerManager().loadItem(soundFile.getAbsolutePath(), new FileLoadResultHandler(guild, repeatTimes));
            }
        } else {
            jdaBot.getPlayerManager().loadItem(fileName, new FileLoadResultHandler(guild, repeatTimes));
        }
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
    public boolean stop(String user, String voiceChannelId) {
        Guild guild = getGuildForUserOrChannelId(user, voiceChannelId);
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
                users.add(new net.dirtydeeds.discordsoundboard.beans.User(discordUser.getId(), username, selected, discordUser.getJDA().getStatus()));
            }
        }
        users.sort(Comparator.comparing(User::getUsername));
        userService.saveAll(users);
        return users;
    }

    public net.dv8tion.jda.api.entities.User retrieveUserById(String id) {
        return bot.retrieveUserById(id).complete();
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
     * @param voiceChannelId - The voice channel to return the guild for.
     * @return The voice channel the user is connected to. If user is not connected to a voice channel will return null.
     */
    private Guild getGuildForUserOrChannelId(String userName, String voiceChannelId) {
        if (!botConfig.isControlByChannel() ||  voiceChannelId == null || voiceChannelId.isBlank()) {
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
        } else {
            return Objects.requireNonNull(bot.getVoiceChannelById(voiceChannelId)).getGuild();
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
     * Join the users current channel.
     */
    private void joinUsersCurrentChannel(String userName, String voiceChannelId) {
        if (botConfig.isControlByChannel() && voiceChannelId != null && !voiceChannelId.isBlank()) {
            moveToChannel(bot.getVoiceChannelById(voiceChannelId), bot.getVoiceChannelById(voiceChannelId).getGuild());
        } else {
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
            try {
                while (!audioManager.isConnected()) {
                        wait(waitTime);
                        i++;
                        if (i >= maxIterations) {
                            break; //break out if after 1 second it doesn't get a connection;
                        }

                }
            } catch (InterruptedException e) {
                LOG.warn("Waiting for audio connection was interrupted.");
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

    public List<ChannelResponse> getVoiceChannels() {
        return bot.getVoiceChannels().stream()
                .map(v -> {
                    boolean ownerInChannel = v.getMembers()
                            .stream()
                            .anyMatch(m -> Objects.equals(m.getEffectiveName(), botConfig.botOwnerName));
                    return new ChannelResponse(v.getName(), v.getId(), v.getGuild().getName(),
                            v.getGuild().getId(), ownerInChannel);
                }
        ).collect(Collectors.toList());
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
