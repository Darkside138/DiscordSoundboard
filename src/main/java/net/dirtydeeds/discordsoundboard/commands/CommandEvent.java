package net.dirtydeeds.discordsoundboard.commands;

import lombok.Getter;
import net.dirtydeeds.discordsoundboard.util.BotUtils;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
            Matcher m = Pattern.compile("([^\"]\\S*|\".+?\")\\s*").matcher(theRest);
            while (m.find()) {
                this.arguments.add(m.group(1).replace("\"", "")); // Add .replace("\"", "") to remove surrounding quotes.
            }
            if (this.arguments.size() > 0) {
                commandString = this.arguments.get(0);
                this.arguments.remove(0);
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
