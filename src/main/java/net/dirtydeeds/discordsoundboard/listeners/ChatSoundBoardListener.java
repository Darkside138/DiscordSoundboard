package net.dirtydeeds.discordsoundboard.listeners;

import com.sun.management.OperatingSystemMXBean;
import net.dirtydeeds.discordsoundboard.SoundPlaybackException;
import net.dirtydeeds.discordsoundboard.beans.SoundFile;
import net.dirtydeeds.discordsoundboard.service.SoundPlayerImpl;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.ChannelType;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.exceptions.PermissionException;
import net.dv8tion.jda.core.hooks.ListenerAdapter;
import net.dv8tion.jda.core.utils.PermissionUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryType;
import java.lang.management.MemoryUsage;
import java.lang.management.RuntimeMXBean;
import java.text.DecimalFormat;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * @author dfurrer.
 *
 * This class handles listening to commands in discord text channels and responding to them.
 */
public class ChatSoundBoardListener extends ListenerAdapter {

	private final Logger LOG = LoggerFactory.getLogger(this.getClass());

    private SoundPlayerImpl soundPlayer;
    private String commandCharacter = "?";
    private Integer messageSizeLimit = 2000;
    private boolean respondToDms = true;
    private boolean muted;
    private static DecimalFormat df2 = new DecimalFormat("#.##");
    private static final int MAX_FILE_SIZE_IN_BYTES = 15000000; // 15 MB

    public ChatSoundBoardListener(SoundPlayerImpl soundPlayer, String commandCharacter, String messageSizeLimit,
                                  Boolean respondToDms) {
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
        muted=false;
        this.respondToDms = respondToDms;
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        String requestingUser = event.getAuthor().getName();
        String requestingUserId = event.getAuthor().getId();
        if (!event.getAuthor().isBot() && ((respondToDms && event.isFromType(ChannelType.PRIVATE))
											|| !event.isFromType(ChannelType.PRIVATE))) {
            String originalMessage = event.getMessage().getContent();
	        String message = originalMessage.toLowerCase();
            if (message.startsWith(commandCharacter)) {
	            if (soundPlayer.isUserAllowed(requestingUser, requestingUserId) && !soundPlayer.isUserBanned(requestingUser, requestingUserId)) {
	                final int maxLineLength = messageSizeLimit;
	
	                //Respond
	                if (message.startsWith(commandCharacter + "list")) {
	                    StringBuilder commandString = getCommandListString();
	                    List<String> soundList = getCommandList(commandString);
	
	                    LOG.info("Responding to list command. Requested by " + requestingUser + ". ID: " + requestingUserId);
	                    if (message.equals(commandCharacter + "list")) {
	                        if (commandString.length() > maxLineLength) {
	                            replyByPrivateMessage(event, "You have " + soundList.size() + " pages of soundFiles. Reply: ```" + commandCharacter + "list pageNumber``` to request a specific page of results.");
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
	                } else if (message.startsWith(commandCharacter + "help")) {
	                    LOG.info("Responding to help command. Requested by " + requestingUser + ". ID: " + requestingUserId);
	                    replyByPrivateMessage(event, "You can type any of the following commands:" +
	                            "\n```" + commandCharacter + "list             - Returns a list of available sound files." +
	                            "\n" + commandCharacter + "soundFileName    - Plays the specified sound from the list." +
                                "\n" + commandCharacter + "yt youtubeLink   - Plays the youtube link specified." +
	                            "\n" + commandCharacter + "random           - Plays a random sound from the list." +
	                            "\n" + commandCharacter + "volume 0-100     - Sets the playback volume." +
								"\n" + commandCharacter + "stop             - Stops the sound that is currently playing." +
	                            "\n" + commandCharacter + "summon           - Summon the bot to your channel." +
	                            "\n" + commandCharacter + "info             - Returns info about the bot.```");
						deleteMessage(event);
	                } else if (message.startsWith(commandCharacter + "volume")) {
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
	                } else if (message.startsWith(commandCharacter + "stop")) {
	                    LOG.info("Stop requested by " + requestingUser + ".");
	                    if (soundPlayer.stop(requestingUser)) {
	                        replyByPrivateMessage(event, "Playback stopped.");
	                    } else {
	                        replyByPrivateMessage(event, "Nothing was playing.");
	                    }
						deleteMessage(event);
	                } else if (message.startsWith(commandCharacter + "info")) {
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
	                            "\nCommand Prefix: " + commandCharacter +
	                            "\nSound File Path: " + soundPlayer.getSoundsPath() +
	                            "```");
	                } else if (message.startsWith(commandCharacter + "remove")) {
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
	                } else if (message.startsWith(commandCharacter + "random")) {
						try {
							soundPlayer.playRandomSoundFile(requestingUser, event);
							deleteMessage(event);
						} catch (SoundPlaybackException e) {
							replyByPrivateMessage(event, "Problem playing random file:" + e);
						}
					} else if (message.startsWith(commandCharacter + "summon")) {
						try {
							soundPlayer.playNothingForEvent(event);
							deleteMessage(event);
						} catch (Exception e) {
							replyByPrivateMessage(event, "Problem being summoned to user:" + e);
						}
					} else if (message.startsWith(commandCharacter + "yt")) {
	                	try {
	                		int split = originalMessage.indexOf(" ");
							String url = originalMessage.substring(split + 1, originalMessage.length());
							soundPlayer.playUrlForUser(url, event.getAuthor().getName());
							deleteMessage(event);
						}catch (Exception e) {
							e.printStackTrace();
							LOG.error(e.getMessage());
						}
	                } else if (message.startsWith(commandCharacter) && message.length() >= 2) {
	                    if (!muted) {
	                        try {
	                            int repeatNumber = 1;
	                            String fileNameRequested = message.substring(1, message.length());
								String repeatString = "";

		                        // If there is the repeat character (space) then cut up the message string.
	                            int repeatIndex = message.indexOf(' ');
	                            if (repeatIndex > -1) {
									fileNameRequested = message.substring(1, repeatIndex);
									fileNameRequested = fileNameRequested.trim();
									if (fileNameRequested.endsWith("~")) {
									    fileNameRequested = fileNameRequested.substring(0, fileNameRequested.length() - 1);
                                    }
	                            	if (repeatIndex + 1 == message.length()) { // If there is only a ~ then repeat-infinite
		                            	repeatNumber = -1;
	                            	} else { // If there is something after the ~ then repeat for that value
										try {
											// Get the number string, +1 to ignore the ~ character. Still needs work. GJF.
											repeatString = message.substring(repeatIndex + 1, message.length());
											repeatNumber = Integer.parseInt(repeatString);
										} catch( NumberFormatException ex) {
											replyByPrivateMessage(event, "Repeat argument should be a number, but you sent me: " + repeatString);
											repeatNumber = 1;
										}
	                            	}
	                            }
	                            LOG.info("Attempting to play file: " + fileNameRequested + " " + repeatNumber + " times. Requested by " + requestingUser + ". ID: " + requestingUserId);
	
	                            soundPlayer.playFileForEvent(fileNameRequested, event, repeatNumber);
	                            deleteMessage(event);
	                        } catch (Exception e) {
	                            e.printStackTrace();
	                        }
	                    } else {
	                        replyByPrivateMessage(event, "I seem to be muted! Try " + commandCharacter + "help");
	                        LOG.info("Attempting to play a sound file while muted. Requested by " + requestingUser + ". ID: " + requestingUserId);
	                    }
	                } else {
	                    List<Message.Attachment> attachments = event.getMessage().getAttachments();
	                    if (attachments.size() > 0 && event.isFromType(ChannelType.PRIVATE)) {
	                        for (Message.Attachment attachment : attachments) {
	                            String name = attachment.getFileName();
	                            String extension = name.substring(name.indexOf(".") + 1);
	                            if (extension.equals("wav") || extension.equals("mp3")) {
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
													replyByPrivateMessage(event,"Downloaded file `" + name + "` and updated list of sounds " + event.getAuthor().getAsMention() + ".");
	                                            } catch (IOException e1) {
	                                                LOG.error("Problem deleting and re-adding sound file: " + name);
	                                            }
	                                        } else {
												replyByPrivateMessage(event,"The file '" + name + "' already exists. Only " + name.substring(0, name.indexOf(".")) + " can change update this sound.");
	                                        }
	                                    }
	                                } else {
	                                    replyByPrivateMessage(event, "File `" + name + "` is too large to add to library.");
	                                }
	                            }
	                        }
	                    } else {
	                        if (message.startsWith(commandCharacter) || event.isFromType(ChannelType.PRIVATE)) {
	                            nonRecognizedCommand(event, requestingUser);
	                        }
	                    }
	                }
	            } else {
	                if (!soundPlayer.isUserAllowed(requestingUser, requestingUserId)) {
	                    replyByPrivateMessage(event, "I don't take orders from you.");
	                }
	                if (soundPlayer.isUserBanned(requestingUser, requestingUserId)) {
	                    replyByPrivateMessage(event, "You've been banned from using this soundboard bot.");
	                }
	            }
	        }
        }

        super.onMessageReceived(event);
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
        final String SPLIT_REGEXP= "(?<=[ \\n])";

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
            for (Map.Entry entry : entrySet) {
                sb.append(commandCharacter).append(entry.getKey()).append("\n");
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
        String pre = ("kMGTPE").charAt(exp-1) + ("");
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
