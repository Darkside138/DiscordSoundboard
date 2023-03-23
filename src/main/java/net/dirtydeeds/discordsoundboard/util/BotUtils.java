package net.dirtydeeds.discordsoundboard.util;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.exceptions.PermissionException;
import net.dv8tion.jda.internal.utils.PermissionUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.validation.constraints.NotNull;

public class BotUtils {

    private static final Logger LOG = LoggerFactory.getLogger(BotUtils.class);

    public static boolean userIsAdmin(@NotNull MessageReceivedEvent messageReceivedEvent) {
        if (messageReceivedEvent.getMember() == null) {
            return false;
        }
        return PermissionUtil.checkPermission(messageReceivedEvent.getMember(), Permission.MANAGE_SERVER);
    }

    public static void replyByPrivateMessage(@NotNull MessageReceivedEvent messageReceivedEvent, String message) {
        if (message.length() < Message.MAX_CONTENT_LENGTH) {
            messageReceivedEvent.getAuthor().openPrivateChannel().complete().sendMessage(message).queue();
        } else {
            LOG.error("Could not reply to message. Message was more than max length");
        }
        deleteMessage(messageReceivedEvent);
    }

    public static void deleteMessage(MessageReceivedEvent event) {
        if (!event.isFromType(ChannelType.PRIVATE)) {
            try {
                event.getMessage().delete().queue();
            } catch (PermissionException e) {
                LOG.warn("Unable to delete message");
            }
        }
    }
}
