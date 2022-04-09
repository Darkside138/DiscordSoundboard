package net.dirtydeeds.discordsoundboard.commands;

import net.dirtydeeds.discordsoundboard.BotConfig;
import net.dirtydeeds.discordsoundboard.listeners.CommandListener;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import java.util.HashSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

class HelpCommandTest {

    @Mock private CommandEvent event;
    @Mock HelpCommand help;
    @Mock PlayCommand play;
    @Mock ListCommand list;
    @Mock CommandListener commandListener;
    @Mock BotConfig botConfig;

    HelpCommand helpCommand;

    @BeforeEach
    void init() {
        openMocks(this);
        helpCommand = new HelpCommand(commandListener, botConfig);
    }

    @Test
    void execute() {
        when(event.getRequestingUser()).thenReturn("UserTest");
        when(commandListener.getCommands()).thenReturn(Stream.of(help, play, list)
                .collect(Collectors.toCollection(HashSet::new)));
        when(help.getName()).thenReturn("help");
        when(help.getHelp()).thenReturn("Returns command list and help text for all commands");
        when(play.getName()).thenReturn("play");
        when(play.getHelp()).thenReturn("Plays the requested sound file");
        when(list.getName()).thenReturn("list");
        when(list.getHelp()).thenReturn("Lists all sound files in the bot");
        when(botConfig.getCommandCharacter()).thenReturn("?");

        helpCommand.execute(event);

        final ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);

        verify(event).replyByPrivateMessage(captor.capture());

        final String argument = captor.getValue();

        assertNotNull(argument);
    }
}