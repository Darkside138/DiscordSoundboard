package net.dirtydeeds.discordsoundboard.commands;

import net.dirtydeeds.discordsoundboard.BotConfig;
import net.dirtydeeds.discordsoundboard.listeners.CommandListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Dave Furrer
 * <p>
 * Command list all commands and their help text
 */
public class HelpCommand extends Command {

    private static final Logger LOG = LoggerFactory.getLogger(HelpCommand.class);

    private final CommandListener commandListener;
    private final BotConfig botConfig;

    public HelpCommand(CommandListener commandListener,
                       BotConfig botConfig) {
        this.name = "help";
        this.help = "Lists the commands available in this bot and information about what each does";
        this.commandListener = commandListener;
        this.botConfig = botConfig;
    }

    @Override
    protected void execute(CommandEvent event) {
        LOG.info("Responding to help command. Requested by {}", event.getRequestingUser());
        StringBuilder helpResponse = new StringBuilder("You can type any of the following commands:```");
        commandListener.getCommands().forEach(command -> {
            helpResponse.append("\n");
            helpResponse.append(botConfig.getCommandCharacter());
            helpResponse.append(command.getName());
            helpResponse.append(": ");
            helpResponse.append(command.getHelp());
        });
        helpResponse.append("```");
        event.replyByPrivateMessage(helpResponse.toString());
    }
}
