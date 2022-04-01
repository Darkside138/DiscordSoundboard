package net.dirtydeeds.discordsoundboard;

import com.sun.management.OperatingSystemMXBean;
import net.dirtydeeds.discordsoundboard.beans.SoundFile;
import net.dirtydeeds.discordsoundboard.beans.User;
import net.dirtydeeds.discordsoundboard.repository.SoundFileRepository;
import net.dirtydeeds.discordsoundboard.repository.UserRepository;
import net.dirtydeeds.discordsoundboard.service.SoundPlayerImpl;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.exceptions.PermissionException;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.internal.utils.PermissionUtil;
import org.apache.commons.logging.impl.SimpleLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext;

import java.io.File;
import java.io.IOException;
import java.lang.management.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * @author dfurrer.
 * <p>
 * This class handles listening to commands in discord text channels and responding to them.
 */
public class ChatSoundBoardListener extends ListenerAdapter {

    private static final SimpleLog LOG = new SimpleLog("ChatListener");

    private static final int MAX_FILE_SIZE_IN_BYTES = 10000000; // 10 MB
    private final static DecimalFormat df2 = new DecimalFormat("#.##");

    private final SoundPlayerImpl soundPlayer;
    private final boolean respondToDms;
    private final UserRepository userRepository;
    private final SoundFileRepository soundFileRepository;
    private Integer messageSizeLimit = 2000;
    private String commandCharacter = "?";
    private boolean muted;

    public ChatSoundBoardListener(SoundPlayerImpl soundPlayer, String commandCharacter, String messageSizeLimit,
                                  Boolean respondToDms, UserRepository userRepository,
                                  SoundFileRepository soundFileRepository) {
        this.soundPlayer = soundPlayer;
        if (commandCharacter != null && !commandCharacter.isEmpty()) {
            this.commandCharacter = commandCharacter;
        }
        if (messageSizeLimit != null && !messageSizeLimit.isEmpty() && messageSizeLimit.matches("^-?\\d+$")) {
            this.messageSizeLimit = Integer.parseInt(messageSizeLimit);
            if (this.messageSizeLimit > 1994) {
                this.messageSizeLimit = 1994;
            }
        }
        muted = false;
        this.respondToDms = respondToDms;
        this.userRepository = userRepository;
        this.soundFileRepository = soundFileRepository;
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        String requestingUser = event.getAuthor().getName();
        if (!event.getAuthor().isBot() && ((respondToDms && event.isFromType(ChannelType.PRIVATE)) ||
                !event.isFromType(ChannelType.PRIVATE))) {

            String message = event.getMessage().getContentRaw().toLowerCase();
            if (message.startsWith(commandCharacter)) {
                if (soundPlayer.isUserAllowed(requestingUser) && !soundPlayer.isUserBanned(requestingUser)) {
                    final int maxLineLength = messageSizeLimit;

                    //Respond
                    if (message.startsWith(commandCharacter + "list")) {
                        listCommand(event, requestingUser, message, maxLineLength);
                        //If the command is not list and starts with the specified command character try and play that "command" or sound file.
                    } else if (message.startsWith(commandCharacter + "help")) {
                        helpCommand(event, requestingUser);
                    } else if (message.startsWith(commandCharacter + "volume")) {
                        volumeCommand(event, requestingUser, message);
                    } else if (message.startsWith(commandCharacter + "stop")) {
                        stopCommand(event, requestingUser, message);
                    } else if (message.startsWith(commandCharacter + "info")) {
                        infoCommand(event, requestingUser);
                    } else if (message.startsWith(commandCharacter + "remove")) {
                        removeCommand(event, message);
                    } else if (message.startsWith(commandCharacter + "random")) {
                        randomCommand(event, requestingUser);
                    } else if (message.startsWith(commandCharacter + "disconnect")){
                        disconnectFromChannel(event.getGuild());
                    } else if (message.startsWith(commandCharacter + "entrance") ||
                            message.startsWith(commandCharacter + "leave")) {
                        entranceOrLeaveCommand(event, message);
                    } else if (message.startsWith(commandCharacter + "userdetails")) {
                        userDetails(event);
                    } else if (message.startsWith(commandCharacter + "reload")) {
                        reloadSounds(event);
                    } else if (message.startsWith(commandCharacter + "url")) {
                        playFromURL(event, requestingUser);
                    } else if (message.startsWith(commandCharacter) &&
                            message.length() >= (commandCharacter.length() + 1)) {
                        soundFileCommand(event, requestingUser, message);
                    } else {
                        if (message.startsWith(commandCharacter) || event.isFromType(ChannelType.PRIVATE)) {
                            nonRecognizedCommand(event, requestingUser);
                        } else {
                            replyByPrivateMessage(event, "You seem to need help.");
                            helpCommand(event, requestingUser);
                        }
                    }
                } else {
                    if (!soundPlayer.isUserAllowed(requestingUser)) {
                        replyByPrivateMessage(event, "I don't take orders from you.");
                    }
                    if (soundPlayer.isUserBanned(requestingUser)) {
                        replyByPrivateMessage(event, "You've been banned from using this soundboard bot.");
                    }
                }
            } else {
                addAttachedSoundFile(event);
            }
        }
    }

    private void reloadSounds(MessageReceivedEvent event) {
        soundPlayer.updateFileList();
        deleteMessage(event);
        replyByPrivateMessage(event, "Sound files reloaded");
    }

    private void disconnectFromChannel(Guild guild) {
        soundPlayer.disconnectFromChannel(guild);
    }

    private void playFromURL(MessageReceivedEvent event, String requestingUser) {
        String[] messageSplit = event.getMessage().getContentRaw().split(" ");
        if (messageSplit.length >= 1) {
            String url = messageSplit[1];
            soundPlayer.playUrlForUser(url, requestingUser);
            deleteMessage(event);
        }
    }

    private void userDetails(MessageReceivedEvent event) {
        String[] messageSplit = event.getMessage().getContentRaw().split(" ");
        if (messageSplit.length >= 2) {
            String userNameOrId = messageSplit[1];
            User user = userRepository.findOneByIdOrUsernameIgnoreCase(userNameOrId, userNameOrId);
            if (user != null) {
                StringBuilder response = new StringBuilder();
                response.append("User details for ").append(userNameOrId).append("```")
                        .append("\nDiscord Id: ").append(user.getId())
                        .append("\nUsername: ").append(user.getUsername())
                        .append("\nEntrance Sound: ");
                if (user.getEntranceSound() != null) {
                    response.append(user.getEntranceSound());
                }
                response.append("\nLeave Sound: ");
                if (user.getLeaveSound() != null) {
                    response.append(user.getLeaveSound());
                }
                response.append("```");
                replyByPrivateMessage(event, response.toString());
            }
        }
    }

    private void entranceOrLeaveCommand(MessageReceivedEvent event, String message) {
        String[] messageSplit = event.getMessage().getContentRaw().split(" ");
        if (messageSplit.length >= 2) {
            String userNameOrId = messageSplit[1];
            String soundFileName = "";
            if (messageSplit.length >= 3) {
                soundFileName = messageSplit[2];
            }
            net.dv8tion.jda.api.entities.User pmUser = event.getAuthor();
            if (userIsAdmin(event) ||
                    (pmUser.getName().equalsIgnoreCase(userNameOrId)
                    || pmUser.getId().equals(userNameOrId))) {
                User user = userRepository.findOneByIdOrUsernameIgnoreCase(userNameOrId, userNameOrId);
                if (user != null) {
                    if (soundFileName.isEmpty()) {
                        if (message.startsWith(commandCharacter + "entrance")) {
                            user.setEntranceSound(null);
                            replyByPrivateMessage(event, "User: " + userNameOrId + " entrance sound cleared");
                        } else {
                            user.setLeaveSound(null);
                            replyByPrivateMessage(event, "User: " + userNameOrId + " leave sound cleared");
                        }
                        userRepository.save(user);
                    } else {
                        SoundFile soundFile = soundFileRepository.findOneBySoundFileIdIgnoreCase(soundFileName);
                        if (soundFile == null) {
                            replyByPrivateMessage(event, "Could not find sound file: " + soundFileName);
                        } else {
                            if (message.startsWith(commandCharacter + "entrance")) {
                                user.setEntranceSound(soundFileName);
                                replyByPrivateMessage(event, "User: " + userNameOrId + " entrance sound set to: " + soundFileName);
                            } else {
                                user.setLeaveSound(soundFileName);
                                replyByPrivateMessage(event, "User: " + userNameOrId + " leave sound set to: " + soundFileName);
                            }
                            userRepository.save(user);
                        }
                    }
                } else {
                    replyByPrivateMessage(event, "Could not find user with id or name: " + userNameOrId);
                }
            } else {
                replyByPrivateMessage(event, "Entrance command incorrect. Required input is " +
                        commandCharacter + "entrance <userid/username> <soundfile>");
            }
        }
    }

    private boolean userIsAdmin(MessageReceivedEvent event) {
        if (event.getMember() == null) {
            return false;
        }
        return PermissionUtil.checkPermission(event.getMember(), Permission.MANAGE_SERVER);
    }

    private void addAttachedSoundFile(MessageReceivedEvent event) {
        List<Message.Attachment> attachments = event.getMessage().getAttachments();
        if (attachments.size() > 0 && event.isFromType(ChannelType.PRIVATE)) {
            for (Message.Attachment attachment : attachments) {
                String name = attachment.getFileName();
                String extension = name.substring(name.indexOf(".") + 1);
                if (extension.equals("wav") || extension.equals("mp3")) {
                    if (attachment.getSize() < MAX_FILE_SIZE_IN_BYTES) {
                        if (!Files.exists(Paths.get(soundPlayer.getSoundsPath() + "/" + name))) {
                            File newSoundFile = new File(soundPlayer.getSoundsPath(), name);
                            attachment.downloadToFile(newSoundFile).getNow(null);
                            event.getChannel().sendMessage("Downloaded file `" + name + "` and added to list of sounds " + event.getAuthor().getAsMention() + ".").queue();
                        } else {
                            if (event.getMember() != null) {
                                boolean hasManageServerPerm = userIsAdmin(event);
                                if (event.getAuthor().getName().equalsIgnoreCase(name.substring(0, name.indexOf(".")))
                                        || hasManageServerPerm) {
                                    try {
                                        Files.deleteIfExists(Paths.get(soundPlayer.getSoundsPath() + "/" + name));
                                        File newSoundFile = new File(soundPlayer.getSoundsPath(), name);
                                        attachment.downloadToFile().getNow(newSoundFile);
                                        event.getChannel().sendMessage("Downloaded file `" + name + "` and updated list of sounds " + event.getAuthor().getAsMention() + ".").queue();
                                    } catch (IOException e1) {
                                        LOG.fatal("Problem deleting and re-adding sound file: " + name);
                                    }
                                } else {
                                    event.getChannel().sendMessage("The file '" + name + "' already exists. Only " + name.substring(0, name.indexOf(".")) + " can change update this sound.").queue();
                                }
                            }
                        }
                    } else {
                        replyByPrivateMessage(event, "File `" + name + "` is too large to add to library.");
                    }
                }
            }
        }
    }

    private void soundFileCommand(MessageReceivedEvent event, String requestingUser, String message) {
        if (!muted) {
            try {
                int repeatNumber = 1;
                String fileNameRequested = message.substring(1);

                // If there is the repeat character (~) then cut up the message string.
                int repeatIndex = message.indexOf('~');
                if (repeatIndex > -1) {
                    fileNameRequested = message.substring(1, repeatIndex - 1); // -1 to ignore the previous space
                    if (repeatIndex + 1 == message.length()) { // If there is only a ~ then repeat-infinite
                        repeatNumber = -1;
                    } else { // If there is something after the ~ then repeat for that value
                        repeatNumber = Integer.parseInt(message.substring(repeatIndex + 1)); // +1 to ignore the ~ character
                    }
                }
                LOG.info("Attempting to play file: " + fileNameRequested + " " + repeatNumber + " times. Requested by " + requestingUser + ".");

                soundPlayer.playFileForUser(fileNameRequested, event.getAuthor().getName());
                deleteMessage(event);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            replyByPrivateMessage(event, "I seem to be muted! Try " + commandCharacter + "help");
            LOG.info("Attempting to play a sound file while muted. Requested by " + requestingUser + ".");
        }
    }

    private void randomCommand(MessageReceivedEvent event, String requestingUser) {
        try {
            soundPlayer.playRandomSoundFile(requestingUser, event);
            deleteMessage(event);
        } catch (SoundPlaybackException e) {
            replyByPrivateMessage(event, "Problem playing random file:" + e);
        }
    }

    private void removeCommand(MessageReceivedEvent event, String message) {
        if (event.getMember() != null) {
            boolean hasManageServerPerm = userIsAdmin(event);
            String[] messageSplit = message.split(" ");
            String soundToRemove = messageSplit[1];
            if (event.getAuthor().getName().equalsIgnoreCase(soundToRemove)
                    || hasManageServerPerm) {
                SoundFile soundFileToRemove = soundPlayer.getAvailableSoundFiles().get(soundToRemove);
                if (soundFileToRemove != null) {
                    try {
                        boolean fileRemoved = Files.deleteIfExists(Paths.get(soundFileToRemove.getSoundFileLocation()));
                        if (fileRemoved) {
                            replyByPrivateMessage(event, "Sound file " + soundToRemove + " was removed.");
                        } else {
                            replyByPrivateMessage(event, "Could not find sound file: " + soundToRemove + ".");
                        }
                    } catch (IOException e) {
                        LOG.fatal("Could not remove sound file " + soundToRemove);
                    }
                }
            } else {
                replyByPrivateMessage(event, "You do not have permission to remove sound file: " + soundToRemove + ".");
            }
        }
    }

    private void infoCommand(MessageReceivedEvent event, String requestingUser) {
        LOG.info("Responding to info request by " + requestingUser + ".");

        OperatingSystemMXBean operatingSystemMXBean = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
        RuntimeMXBean runtimeMXBean = ManagementFactory.getRuntimeMXBean();
        int availableProcessors = operatingSystemMXBean.getAvailableProcessors();
        long prevUpTime = runtimeMXBean.getUptime();
        long prevProcessCpuTime = operatingSystemMXBean.getProcessCpuTime();
        double cpuUsage;
        try {
            Thread.sleep(500);
        } catch (Exception ignored) {
        }

        long upTime = runtimeMXBean.getUptime();
        long processCpuTime = operatingSystemMXBean.getProcessCpuTime();
        long elapsedCpu = processCpuTime - prevProcessCpuTime;
        long elapsedTime = upTime - prevUpTime;

        cpuUsage = Math.min(99F, elapsedCpu / (elapsedTime * 10000F * availableProcessors));

        List<MemoryPoolMXBean> memoryPools = new ArrayList<>(ManagementFactory.getMemoryPoolMXBeans());
        long usedHeapMemoryAfterLastGC = 0;
        for (MemoryPoolMXBean memoryPool : memoryPools) {
            if (memoryPool.getType().equals(MemoryType.HEAP)) {
                MemoryUsage poolCollectionMemoryUsage = memoryPool.getCollectionUsage();
                usedHeapMemoryAfterLastGC += poolCollectionMemoryUsage.getUsed();
            }
        }

        Package thisPackage = getClass().getPackage();
        String version = null;
        if (thisPackage != null) {
            version = getClass().getPackage().getImplementationVersion();
        }
        if (version == null) {
            version = "DEVELOPMENT";
        }

        long uptimeDays = TimeUnit.DAYS.convert(upTime, TimeUnit.MILLISECONDS);
        long uptimeHours = TimeUnit.HOURS.convert(upTime, TimeUnit.MILLISECONDS) - TimeUnit.DAYS.toHours(TimeUnit.MILLISECONDS.toDays(upTime));
        long uptimeMinutes = TimeUnit.MINUTES.convert(upTime, TimeUnit.MILLISECONDS) - TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(upTime));
        long upTimeSeconds = TimeUnit.MILLISECONDS.toSeconds(upTime) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(upTime));

        replyByPrivateMessage(event, "DiscordSoundboard info: ```" +
                "CPU: " + df2.format(cpuUsage) + "%" +
                "\nMemory: " + humanReadableByteCount(usedHeapMemoryAfterLastGC) +
                "\nUptime: Days: " + uptimeDays + " Hours: " + uptimeHours + " Minutes: " + uptimeMinutes + " Seconds: " + upTimeSeconds +
                "\nVersion: " + version +
                "\nSoundFiles: " + soundPlayer.getAvailableSoundFiles().size() +
                "\nCommand Prefix: " + commandCharacter +
                "\nSound File Path: " + soundPlayer.getSoundsPath() +
                "\nSoundboard Version: " + soundPlayer.getApplicationVersion() +
                "\nWeb UI URL: localhost:" + soundPlayer.getApplicationContext().getWebServer().getPort() +
                "\nSwagger URL: localhost:" + soundPlayer.getApplicationContext().getWebServer().getPort() + "/swagger-ui/index.html" +
                "```");
    }

    private void stopCommand(MessageReceivedEvent event, String requestingUser, String message) {
        int fadeoutIndex = message.indexOf('~');
        int fadeoutTimeout = 0;
        if (fadeoutIndex > -1) {
            fadeoutTimeout = Integer.parseInt(message.substring(fadeoutIndex + 1));
        }
        LOG.info("Stop requested by " + requestingUser + " with a fadeout of " + fadeoutTimeout + " seconds");
        if (soundPlayer.stop()) {
            replyByPrivateMessage(event, "Playback stopped.");
        } else {
            replyByPrivateMessage(event, "Nothing was playing.");
        }
    }

    private void volumeCommand(MessageReceivedEvent event, String requestingUser, String message) {
        int fadeoutIndex = message.indexOf('~');
        int newVol = Integer.parseInt(message.substring(8, (fadeoutIndex > -1) ? fadeoutIndex - 1 : message.length()));

        if (newVol >= 1 && newVol <= 100) {
            muted = false;
            soundPlayer.setSoundPlayerVolume(newVol);
            replyByPrivateMessage(event, "*Volume set to " + newVol + "%*");
            LOG.info("Volume set to " + newVol + "% by " + requestingUser + ".");
        } else if (newVol == 0) {
            muted = true;
            soundPlayer.setSoundPlayerVolume(newVol);
            replyByPrivateMessage(event, requestingUser + " muted me.");
            LOG.info("Bot muted by " + requestingUser + ".");
        }
    }

    private void helpCommand(MessageReceivedEvent event, String requestingUser) {
        LOG.info("Responding to help command. Requested by " + requestingUser + ".");
        replyByPrivateMessage(event, "You can type any of the following commands:" +
                "\n```" + commandCharacter + "list             - Returns a list of available sound files." +
                "\n" + commandCharacter + "soundFileName    - Plays the specified sound from the list." +
                "\n" + commandCharacter + "reload           - Reloads the sound files from disk." +
                "\n" + commandCharacter + "random           - Plays a random sound from the list." +
                "\n" + commandCharacter + "volume 0-100     - Sets the playback volume." +
                "\n" + commandCharacter + "stop             - Stops the sound that is currently playing." +
                "\n" + commandCharacter + "disconnect       - Disconnects the bot from the current channel." +
                "\n" + commandCharacter + "info             - Returns info about the bot." +
                "\n" + commandCharacter + "url              - Play file from URL (Youtube, Vimeo, Soundcloud)." +
                "\n" + commandCharacter + "entrance userName soundFileName - Sets entrance sound for user. Leave soundFileName empty to remove." +
                "\n" + commandCharacter + "leave userName soundFileName - Sets leave sound for user. Leave soundFileName empty to remove." +
                "\n" + commandCharacter + "userDetails userName - Get details for user```");
    }

    private void listCommand(MessageReceivedEvent event, String requestingUser, String message, int maxLineLength) {
        StringBuilder commandString = getCommandListString();
        List<String> soundList = getCommandList(commandString);

        LOG.info("Responding to list command. Requested by " + requestingUser + ".");
        if (message.equals(commandCharacter + "list")) {
            if (commandString.length() > maxLineLength) {
                replyByPrivateMessage(event, "You have " + soundList.size() + " pages of soundFiles. Reply: ```" + commandCharacter + "list pageNumber``` to request a specific page of results.");
            } else {
                replyByPrivateMessage(event, "Type any of the following into the chat to play the sound:"+
				"\n"+soundList.get(0));
            }
        } else {
            String[] messageSplit = message.split(" ");
            try {
                int pageNumber = Integer.parseInt(messageSplit[1]);
                replyByPrivateMessage(event, soundList.get(pageNumber - 1));
            } catch (IndexOutOfBoundsException e) {
                replyByPrivateMessage(event, "The page number you entered is not valid.");
            } catch (NumberFormatException e) {
                replyByPrivateMessage(event, "The page number argument must be a number.");
            }
        }
    }

    private void nonRecognizedCommand(MessageReceivedEvent event, String requestingUser) {
        replyByPrivateMessage(event, "Hello @" + requestingUser + ". I don't know how to respond to this message!");
        replyByPrivateMessage(event, "You can type " + commandCharacter + "help to see a list of recognized commands.");
        LOG.info("Responding to PM of " + requestingUser + ". Unknown Command. Sending help text.");
    }

    private List<String> getCommandList(StringBuilder commandString) {
        final int maxLineLength = messageSizeLimit;
        List<String> soundFiles = new ArrayList<>();

        //if text has \n, \r or \t symbols it's better to split by \s+
        final String SPLIT_REGEXP = "(?<=[ \\n])";

        String[] tokens = commandString.toString().split(SPLIT_REGEXP);
        int lineLen = 0;
        StringBuilder output = new StringBuilder();
        output.append("```\n");
        for (int i = 0; i < tokens.length; i++) {
            String word = tokens[i];

            if (lineLen + (word).length() > maxLineLength) {
                if (i > 0) {
                    output.append("```\n");
                    soundFiles.add(output.toString());

                    output = new StringBuilder(maxLineLength);
                    output.append("```");
                }
                lineLen = 0;
            }
            output.append(word);
            lineLen += word.length();
        }
        if (output.length() > 0) {
            output.append("```");
        }
        soundFiles.add(output.toString());
        return soundFiles;
    }

    private StringBuilder getCommandListString() {
        StringBuilder sb = new StringBuilder();

        Set<Map.Entry<String, SoundFile>> entrySet = soundPlayer.getAvailableSoundFiles().entrySet();

        if (entrySet.size() > 0) {
            entrySet.forEach(entry -> sb.append(commandCharacter).append(entry.getKey()).append("\n"));
        }
        return sb;
    }

    private void replyByPrivateMessage(MessageReceivedEvent event, String message) {
        event.getAuthor().openPrivateChannel().complete().sendMessage(message).queue();
        deleteMessage(event);
    }

    private static String humanReadableByteCount(long bytes) {
        int unit = 1000;
        if (bytes < unit) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(unit));
        String pre = ("kMGTPE").charAt(exp - 1) + ("");
        return String.format("%.1f %sB", bytes / Math.pow(unit, exp), pre);
    }

    private void deleteMessage(MessageReceivedEvent event) {
        if (!event.isFromType(ChannelType.PRIVATE)) {
            try {
                event.getMessage().delete().queue();
            } catch (PermissionException e) {
                LOG.warn("Unable to delete message");
            }
        }
    }
}
