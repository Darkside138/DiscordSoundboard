package net.dirtydeeds.discordsoundboard.listeners;

import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.sun.management.OperatingSystemMXBean;
import net.dirtydeeds.discordsoundboard.DiscordSoundboardProperties;
import net.dirtydeeds.discordsoundboard.SoundPlaybackException;
import net.dirtydeeds.discordsoundboard.beans.PlayEventFilenameCount;
import net.dirtydeeds.discordsoundboard.beans.PlayEventUsernameCount;
import net.dirtydeeds.discordsoundboard.beans.PlayEventUsernameFilenameCount;
import net.dirtydeeds.discordsoundboard.beans.SoundFile;
import net.dirtydeeds.discordsoundboard.repository.PlayEventRepository;
import net.dirtydeeds.discordsoundboard.repository.SoundFileRepository;
import net.dirtydeeds.discordsoundboard.service.SoundPlayerImpl;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.ChannelType;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.events.message.MessageEmbedEvent;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.exceptions.PermissionException;
import net.dv8tion.jda.core.hooks.ListenerAdapter;
import net.dv8tion.jda.core.utils.PermissionUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.lang.management.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author dfurrer.
 * <p>
 * This class handles listening to commands in discord text channels and responding to them.
 */
public class ChatSoundBoardListener extends ListenerAdapter {

    private final Logger LOG = LoggerFactory.getLogger(this.getClass());

    private SoundPlayerImpl soundPlayer;
    private DiscordSoundboardProperties appProperties;
    private PlayEventRepository playEventRepository;
    private SoundFileRepository soundFileRepository;
    private boolean muted;
    private static DecimalFormat df2 = new DecimalFormat("#.##");
    private static final int MAX_FILE_SIZE_IN_BYTES = 15000000; // 15 MB


    public ChatSoundBoardListener(SoundPlayerImpl soundPlayer, DiscordSoundboardProperties appProperties, PlayEventRepository playEventRepository, SoundFileRepository soundFileRepository) {
        this.soundPlayer = soundPlayer;
        this.appProperties = appProperties;
        this.playEventRepository = playEventRepository;
        this.soundFileRepository = soundFileRepository;
        muted = false;
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        String requestingUser = event.getAuthor().getName();
        String requestingUserId = event.getAuthor().getId();
        if (event.getAuthor().isBot() || !((appProperties.isRespondToDm() && event.isFromType(ChannelType.PRIVATE)) || !event.isFromType(ChannelType.PRIVATE))) {
            super.onMessageReceived(event);
            return;
        }
        String originalMessage = event.getMessage().getContent().trim();
        String message = originalMessage.toLowerCase();

        if (!soundPlayer.isUserAllowed(requestingUser, requestingUserId)) {
            replyByPrivateMessage(event, "I don't take orders from you.");
            super.onMessageReceived(event);
            return;
        }
        if (soundPlayer.isUserBanned(requestingUser, requestingUserId)) {
            replyByPrivateMessage(event, "You've been banned from using this soundboard bot.");
            super.onMessageReceived(event);
            return;
        }

        if (!message.startsWith(appProperties.getCommandCharacter())) {
            uploadCommand(event, originalMessage);
            super.onMessageReceived(event);
            return;
        }

        //Respond
        if (message.startsWith(appProperties.getCommandCharacter() + "list")) {
            listCommand(event, requestingUser, requestingUserId, message);
        } else if (message.startsWith(appProperties.getCommandCharacter() + "help")) {
            helpCommand(event, requestingUser, requestingUserId);
        } else if (message.startsWith(appProperties.getCommandCharacter() + "volume")) {
            volumeCommand(event, requestingUser, requestingUserId, message);
        } else if (message.startsWith(appProperties.getCommandCharacter() + "stop")) {
            stopCommand(event, requestingUser);
        } else if (message.startsWith(appProperties.getCommandCharacter() + "info")) {
            infoCommand(event, requestingUser, requestingUserId);
        } else if (message.startsWith(appProperties.getCommandCharacter() + "remove")) {
            removeCommand(event, message);
        } else if (message.startsWith(appProperties.getCommandCharacter() + "random")) {
            randomCommand(event, requestingUser);
        } else if (message.startsWith(appProperties.getCommandCharacter() + "summon")) {
            summonCommand(event);
        } else if (message.startsWith(appProperties.getCommandCharacter() + "yt")) {
            youtubeCommand(event, originalMessage);
        } else if (message.startsWith(appProperties.getCommandCharacter() + "stats")) {
            statsCommand(event);
        } else if (message.startsWith(appProperties.getCommandCharacter()) && message.length() >= 2) {
            playSoundCommand(event, requestingUser, requestingUserId, message);
        } else {
            if (event.isFromType(ChannelType.PRIVATE)) {
                nonRecognizedCommand(event, requestingUser);
            }
        }

        super.onMessageReceived(event);
    }

    private void statsCommand(MessageReceivedEvent event) {
        String message = event.getMessage().getContent().trim().toLowerCase();

        // g1 stats
        // g3 which stats query
        // g5 period for stats
        Pattern pattern = Pattern.compile("\\?(\\w+)(\\s(\\w+))?(\\s(\\d+))?");
        Matcher matcher = pattern.matcher(message);

        if (!matcher.matches()) {
            return;
        }

        String statsQuery = matcher.group(3);
        String statsPeriod = matcher.group(5); // TODO date range for stats

        if (Strings.isNullOrEmpty(statsQuery)) {
            statsQuery = "top";
        }

        StringBuilder sb = new StringBuilder();
        Set<String> soundFileLocations = soundFileRepository.getSoundFileNames();
        switch (statsQuery) {
            case "usersounds":
                Collection<PlayEventUsernameFilenameCount> usernameFilenameCount = playEventRepository.getUsernameFilenameCount();
                sb.append("Username, Filename, Count\n");
                if (usernameFilenameCount.isEmpty()) {
                    sb.append("No Results\n");
                }
                usernameFilenameCount.stream()
                        .filter(ufc -> soundFileLocations.contains(ufc.getFilename()))
                        .limit(50)
                        .forEach(ufc -> sb.append(ufc.getUsername()).append(", ").append(ufc.getFilename()).append(", ").append(ufc.getCount()).append("\n"));
                break;
            case "users":
                Collection<PlayEventUsernameCount> usernameCount = playEventRepository.getUsernameCount();
                sb.append("Username, Count\n");
                if (usernameCount.isEmpty()) {
                    sb.append("No Results\n");
                }
                usernameCount.forEach(e -> sb.append(e.getUsername()).append(", ").append(e.getCount()).append("\n"));
                sb.append("\n");
                break;
            case "sounds":
            default:
                Collection<PlayEventFilenameCount> filenameCount = playEventRepository.getFilenameCount();
                sb.append("Filename, Count\n");
                if (filenameCount.isEmpty()) {
                    sb.append("No Results\n");
                }
                filenameCount.stream()
                        .filter(fc -> soundFileLocations.contains(fc.getFilename()))
                        .forEach(fc -> sb.append(fc.getFilename()).append(", ").append(fc.getCount()).append("\n"));
                sb.append("\n");
                break;
        }

        String output = sb.toString();
        if (output.length() < appProperties.getMessageSizeLimit()) {
            replyByPrivateMessage(event, output);
        } else {
            Splitter splitter = Splitter.fixedLength(appProperties.getMessageSizeLimit());
            for (String s : splitter.split(output)) {
                replyByPrivateMessage(event, s);
            }
        }
    }

    private void listCommand(MessageReceivedEvent event, String requestingUser, String requestingUserId, String message) {
        StringBuilder commandString = getCommandListString();
        List<String> soundList = getCommandList(commandString);

        LOG.info("Responding to list command. Requested by " + requestingUser + ". ID: " + requestingUserId);
        if (message.equals(appProperties.getCommandCharacter() + "list")) {
            if (commandString.length() > appProperties.getMessageSizeLimit()) {
                replyByPrivateMessage(event, "You have " + soundList.size() + " pages of soundFiles. Reply: ```" + appProperties.getCommandCharacter() + "list pageNumber``` to request a specific page of results.");
            } else {
                replyByPrivateMessage(event, "Type any of the following into the chat to play the sound:");
                replyByPrivateMessage(event, soundList.get(0));
            }
        } else {
            String[] messageSplit = message.split(" ");
            try {
                Integer pageNumber = Integer.parseInt(messageSplit[1]);
                replyByPrivateMessage(event, soundList.get(pageNumber - 1));
            } catch (IndexOutOfBoundsException e) {
                replyByPrivateMessage(event, "The page number you entered is not valid.");
            } catch (NumberFormatException e) {
                replyByPrivateMessage(event, "The page number argument must be a number.");
            }
        }
        deleteMessage(event);
        //If the command is not list and starts with the specified command character try and play that "command" or sound file.
    }

    private void helpCommand(MessageReceivedEvent event, String requestingUser, String requestingUserId) {
        LOG.info("Responding to help command. Requested by " + requestingUser + ". ID: " + requestingUserId);
        replyByPrivateMessage(event, "You can type any of the following commands:" +
                "\n```" + appProperties.getCommandCharacter() + "list             - Returns a list of available sound files." +
                "\n" + appProperties.getCommandCharacter() + "soundFileName    - Plays the specified sound from the list." +
                "\n" + appProperties.getCommandCharacter() + "yt youtubeLink   - Plays the youtube link specified." +
                "\n" + appProperties.getCommandCharacter() + "random           - Plays a random sound from the list." +
                "\n" + appProperties.getCommandCharacter() + "volume 0-100     - Sets the playback volume." +
                "\n" + appProperties.getCommandCharacter() + "stop             - Stops the sound that is currently playing." +
                "\n" + appProperties.getCommandCharacter() + "summon           - Summon the bot to your channel." +
                "\n" + appProperties.getCommandCharacter() + "info             - Returns info about the bot." +
                "\n" + appProperties.getCommandCharacter() + "stats            - Returns statistics about which sounds are popular.```");
        deleteMessage(event);
    }

    private void volumeCommand(MessageReceivedEvent event, String requestingUser, String requestingUserId, String message) {
        int newVol = Integer.parseInt(message.substring(8));
        if (newVol >= 1 && newVol <= 100) {
            muted = false;
            soundPlayer.setSoundPlayerVolume(newVol, requestingUser);
            replyByPrivateMessage(event, "*Volume set to " + newVol + "%*");
            LOG.info("Volume set to " + newVol + "% by " + requestingUser + ". ID: " + requestingUserId);
        } else if (newVol == 0) {
            muted = true;
            soundPlayer.setSoundPlayerVolume(newVol, requestingUser);
            replyByPrivateMessage(event, requestingUser + " muted me.");
            LOG.info("Bot muted by " + requestingUser + ".");
        }
        deleteMessage(event);
    }

    private void stopCommand(MessageReceivedEvent event, String requestingUser) {
        LOG.info("Stop requested by " + requestingUser + ".");
        if (soundPlayer.stop(requestingUser)) {
            replyByPrivateMessage(event, "Playback stopped.");
        } else {
            replyByPrivateMessage(event, "Nothing was playing.");
        }
        deleteMessage(event);
    }

    private void uploadCommand(MessageReceivedEvent event, String originalMessage) {
        List<Message.Attachment> attachments = event.getMessage().getAttachments();

        if (attachments.size() > 0 && event.isFromType(ChannelType.PRIVATE)) {
            for (Message.Attachment attachment : attachments) {
                String name = attachment.getFileName();
                String extension = name.substring(name.indexOf(".") + 1);
                if (!originalMessage.isEmpty()) {
                    name = originalMessage + "." + extension;
                }
                if (extension.equals("wav") || extension.equals("mp3") || extension.equals("ogg")
                        || extension.equals("flac") || extension.equals("mp4") || extension.equals("m4a")) {
                    if (attachment.getSize() < MAX_FILE_SIZE_IN_BYTES) {
                        if (!Files.exists(Paths.get(soundPlayer.getSoundsPath() + "/" + name))) {
                            File newSoundFile = new File(soundPlayer.getSoundsPath(), name);
                            attachment.download(newSoundFile);
                            replyByPrivateMessage(event, "Downloaded file `" + name + "` and added to list of sounds " + event.getAuthor().getAsMention() + ".");
                        } else {
                            boolean hasManageServerPerm = PermissionUtil.checkPermission(event.getMember(), Permission.MANAGE_SERVER);
                            if (event.getAuthor().getName().equalsIgnoreCase(name.substring(0, name.indexOf(".")))
                                    || hasManageServerPerm) {
                                try {
                                    Files.deleteIfExists(Paths.get(soundPlayer.getSoundsPath() + "/" + name));
                                    File newSoundFile = new File(soundPlayer.getSoundsPath(), name);
                                    attachment.download(newSoundFile);
                                    replyByPrivateMessage(event, "Downloaded file `" + name + "` and updated list of sounds " + event.getAuthor().getAsMention() + ".");
                                } catch (IOException e1) {
                                    LOG.error("Problem deleting and re-adding sound file: " + name);
                                }
                            } else {
                                replyByPrivateMessage(event, "The file '" + name + "' already exists. Only " + name.substring(0, name.indexOf(".")) + " can change update this sound.");
                            }
                        }
                        soundPlayer.getFileList();
                    } else {
                        replyByPrivateMessage(event, "File `" + name + "` is too large to add to library.");
                    }
                }
            }
        }
    }

    private void playSoundCommand(MessageReceivedEvent event, String requestingUser, String requestingUserId, String message) {
        if (muted) {
            LOG.info("Attempting to play a sound file while muted. Requested by " + requestingUser + ". ID: " + requestingUserId);
            replyByPrivateMessage(event, "I seem to be muted! Try " + appProperties.getCommandCharacter() + "help");
            deleteMessage(event);
            return;
        }

        Pattern pattern = Pattern.compile("\\?(\\w+)(\\s(\\d+)|)");
        Matcher matcher = pattern.matcher(message);
        if (!matcher.matches()) {
            return;
        }

        String fileNameRequested = matcher.group(1);
        String repeatString = matcher.group(3);

        if (fileNameRequested == null) {
            LOG.info("No filename recognized for message: {}", message);
            replyByPrivateMessage(event, "I didn't recognize the filename in your message! Try " + appProperties.getCommandCharacter() + "help");
            deleteMessage(event);
            return;
        }

        int repeatNumber = 1;
        if (repeatString != null) {
            try {
                repeatNumber = Integer.parseInt(repeatString);
            } catch (NumberFormatException e) {
                replyByPrivateMessage(event, "Repeat argument should be a number, but you sent me: " + repeatString);
            }
        }

        LOG.info("Attempting to play file: " + fileNameRequested + " " + repeatNumber + " times. Requested by " + requestingUser + ". ID: " + requestingUserId);
        try {
            soundPlayer.playFileForEvent(fileNameRequested, event, repeatNumber);
            deleteMessage(event);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void youtubeCommand(MessageReceivedEvent event, String originalMessage) {
        try {
            int split = originalMessage.indexOf(" ");
            String url = originalMessage.substring(split + 1, originalMessage.length());
            soundPlayer.playUrlForUser(url, event.getAuthor().getName());
            deleteMessage(event);
        } catch (Exception e) {
            e.printStackTrace();
            LOG.error(e.getMessage());
        }
    }

    private void summonCommand(MessageReceivedEvent event) {
        try {
            soundPlayer.playNothingForEvent(event);
            deleteMessage(event);
        } catch (Exception e) {
            replyByPrivateMessage(event, "Problem being summoned to user:" + e);
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
        String[] messageSplit = message.split(" ");
        String soundToRemove = messageSplit[1];
        boolean hasManageServerPerm = PermissionUtil.checkPermission(event.getMember(), Permission.MANAGE_SERVER);
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
                    LOG.error("Could not remove sound file " + soundToRemove);
                }
            }
        } else {
            replyByPrivateMessage(event, "You do not have permission to remove sound file: " + soundToRemove + ".");
        }
    }

    private void infoCommand(MessageReceivedEvent event, String requestingUser, String requestingUserId) {
        LOG.info("Responding to info request by " + requestingUser + ". ID: " + requestingUserId);

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
                "\nCommand Prefix: " + appProperties.getCommandCharacter() +
                "\nSound File Path: " + soundPlayer.getSoundsPath() +
                "```");
    }

    @Override
    public void onMessageEmbed(MessageEmbedEvent event) {
        LOG.info(event.toString());
    }

    private void nonRecognizedCommand(MessageReceivedEvent event, String requestingUser) {
        replyByPrivateMessage(event, "Hello @" + requestingUser + ". I don't know how to respond to this message!");
        replyByPrivateMessage(event, "You can type " + appProperties.getCommandCharacter() + "help to see a list of recognized commands.");
        LOG.info("Responding to PM of " + requestingUser + ". Unknown Command. Sending help text.");
    }

    private List<String> getCommandList(StringBuilder commandString) {
        final int maxLineLength = appProperties.getMessageSizeLimit();
        List<String> soundFiles = new ArrayList<>();

        //if text has \n, \r or \t symbols it's better to split by \s+
        final String SPLIT_REGEXP = "(?<=[ \\n])";

        String[] tokens = commandString.toString().split(SPLIT_REGEXP);
        int lineLen = 0;
        StringBuilder output = new StringBuilder();
        output.append("```");
        for (int i = 0; i < tokens.length; i++) {
            String word = tokens[i];

            if (lineLen + (word).length() > maxLineLength) {
                if (i > 0) {
                    output.append("```");
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
            for (Map.Entry entry : entrySet) {
                sb.append(appProperties.getCommandCharacter()).append(entry.getKey()).append("\n");
            }
        }
        return sb;
    }

    private void replyByPrivateMessage(MessageReceivedEvent event, String message) {
        try {
            soundPlayer.sendPrivateMessage(event, message);
        } catch (InterruptedException e) {
            LOG.warn("Message reply async action interrupted");
        } catch (ExecutionException e) {
            LOG.warn("Could not send private message reply");
        } catch (TimeoutException e) {
            LOG.warn("Sending of private message timed out");
        }
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
