package net.dirtydeeds.discordsoundboard.listeners;

import net.dirtydeeds.discordsoundboard.BotConfig;
import net.dirtydeeds.discordsoundboard.commands.Command;
import net.dirtydeeds.discordsoundboard.commands.CommandEvent;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.exceptions.PermissionException;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

/**
 * @author Dave Furrer
 * <p>
 * Class to handle onMessageReceived events from discord
 */
public class CommandListener extends ListenerAdapter {

    private static final Logger LOG = LoggerFactory.getLogger(CommandListener.class);

    private final BotConfig botConfig;
    private final Set<Command> commands = new HashSet<>();

    public CommandListener(BotConfig botConfig) {
        this.botConfig = botConfig;
    }

    public void addCommand(Command command) {
        commands.add(command);
    }

    private Optional<Command> findCommand(String name) {
        return commands.stream()
                .filter(c -> c.getName().equalsIgnoreCase(name))
                .findFirst();
    }

    public Set<Command> getCommands() {
        return commands;
    }

    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        CommandEvent commandEvent = new CommandEvent(event);
        if (botConfig.isRespondToChatCommands() &&
                !event.getAuthor().isBot() &&
                ((botConfig.isRespondToDmsString() && event.isFromType(ChannelType.PRIVATE)) ||
                !event.isFromType(ChannelType.PRIVATE))) {

            String message = commandEvent.getMessage();
            if (message.startsWith(botConfig.getCommandCharacter())) {
                if (isUserAllowed(commandEvent.getRequestingUser()) &&
                        !isUserBanned(commandEvent.getRequestingUser())) {

                    Optional<Command> command = findCommand(commandEvent.getCommandString());

                    if (command.isEmpty()) {
                        if (commandEvent.getCommandString().isEmpty()) {
                            if (!commandEvent.getPrefix().isEmpty()) {
                                command = findCommand("help");
                            }
                        } else if (commandEvent.getMessage().length() > 0) {
                            command = findCommand("play");
                        }
                    }
                    command.ifPresent(c -> c.run(commandEvent));

                    afterMessageReceived(event);
                }
            }
        }
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

    private void afterMessageReceived(@NotNull MessageReceivedEvent event) {
        deleteMessage(event);
    }

    private void deleteMessage(MessageReceivedEvent event) {
        if (!event.isFromType(ChannelType.PRIVATE)) {
            try {
                event.getMessage().delete().queue(null, t -> LOG.debug("RestAction queue returned failure", t));
            } catch (PermissionException e) {
                LOG.warn("Unable to delete message. Does the bot have the correct permissions?");
            }
        }
    }
}
