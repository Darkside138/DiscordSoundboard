package net.dirtydeeds.discordsoundboard.listeners;

import net.dirtydeeds.discordsoundboard.BotConfig;
import net.dirtydeeds.discordsoundboard.SoundPlayer;
import net.dirtydeeds.discordsoundboard.util.BotUtils;
import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class FileAttachmentListener extends ListenerAdapter {

    private static final Logger LOG = LoggerFactory.getLogger(FileAttachmentListener.class);

    private final BotConfig botConfig;

    public FileAttachmentListener(BotConfig botConfig) {
        this.botConfig = botConfig;
    }

    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        if (!event.getMessage().getAttachments().isEmpty()) {
            addAttachment(event);
        }
    }

    private void addAttachment(@NotNull MessageReceivedEvent event) {
        List<Message.Attachment> attachments = event.getMessage().getAttachments();
        if (attachments.size() > 0 && event.isFromType(ChannelType.PRIVATE)) {
            for (Message.Attachment attachment : attachments) {
                String message = event.getMessage().getContentRaw().trim();
                String attachmentFileName = attachment.getFileName();
                String fileName = attachmentFileName;
                String extension = attachmentFileName.substring(attachmentFileName.indexOf(".") + 1);
                if(!message.isBlank()) {
                    fileName = message + "." + extension;
                }
                if (extension.equals("wav") || extension.equals("mp3")) {
                    if (attachment.getSize() < botConfig.getMaxFileSizeInBytes()) {
                        if (!Files.exists(Paths.get(botConfig.getSoundFileDir() + "/" + fileName))) {
                            File newSoundFile = new File(botConfig.getSoundFileDir(), fileName);
                            attachment.downloadToFile(newSoundFile).getNow(null);
                            event.getChannel().sendMessage("Downloaded file `" + fileName + "` and added to list of sounds " + event.getAuthor().getAsMention() + ".").queue();
                        } else {
                            if (event.getMember() != null) {
                                boolean hasManageServerPerm = BotUtils.userIsAdmin(event);
                                if (event.getAuthor().getName().equalsIgnoreCase(fileName.substring(0, fileName.indexOf(".")))
                                        || hasManageServerPerm) {
                                    try {
                                        Files.deleteIfExists(Paths.get(botConfig.getSoundFileDir() + "/" + fileName));
                                        File newSoundFile = new File(botConfig.getSoundFileDir(), fileName);
                                        attachment.downloadToFile().getNow(newSoundFile);
                                        event.getChannel().sendMessage("Downloaded file `" + fileName + "` and updated list of sounds " + event.getAuthor().getAsMention() + ".").queue();
                                    } catch (IOException e1) {
                                        LOG.error("Problem deleting and re-adding sound file: {}", fileName);
                                    }
                                } else {
                                    event.getChannel().sendMessage("The file '" + fileName + "' already exists. Only " + fileName.substring(0, fileName.indexOf(".")) + " can change update this sound.").queue();
                                }
                            }
                        }
                    } else {
                        BotUtils.replyByPrivateMessage(event, "File `" + fileName + "` is too large to add to library.");
                    }
                }
            }
        }
    }
}
