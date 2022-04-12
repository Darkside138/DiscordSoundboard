package net.dirtydeeds.discordsoundboard.commands;

import lombok.Getter;
import net.dirtydeeds.discordsoundboard.util.BotUtils;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.exceptions.PermissionException;
import net.dv8tion.jda.internal.utils.PermissionUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.LinkedList;

@Getter
public class CommandEvent {

    private static final Logger LOG = LoggerFactory.getLogger(CommandEvent.class);

    private final MessageReceivedEvent messageReceivedEvent;
    private LinkedList<String> arguments = new LinkedList<>();
    private String commandString = "";
    private String prefix = "";

    public CommandEvent(MessageReceivedEvent messageReceivedEvent) {
        this.messageReceivedEvent = messageReceivedEvent;
        String input = messageReceivedEvent.getMessage().getContentRaw();
        if (!input.isEmpty()) {
            this.prefix = input.substring(0, 1);
            String theRest = input.substring(1);
            LinkedList<String> messageSplit = new LinkedList<>(Arrays.asList(theRest.split(" ")));
            if (messageSplit.size() == 1) {
                this.commandString = theRest;
                this.arguments = new LinkedList<>();
            } else if (messageSplit.size() > 1) {
                this.commandString = messageSplit.getFirst();
                messageSplit.remove(0);
                this.arguments = messageSplit;
            }
        }
    }

    public String getCommandString() {
        return this.commandString;
    }

    public String getMessage() {
        return messageReceivedEvent.getMessage().getContentRaw().trim();
    }

    public String getRequestingUser() {
        return messageReceivedEvent.getAuthor().getName();
    }

    public User getAuthor() {
        return messageReceivedEvent.getAuthor();
    }

    public boolean userIsAdmin() {
        return BotUtils.userIsAdmin(messageReceivedEvent);
    }

    public void replyByPrivateMessage(String message) {
        BotUtils.replyByPrivateMessage(messageReceivedEvent, message);
    }
}
