package net.dirtydeeds.discordsoundboard.commands;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;

import java.util.LinkedList;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

class CommandEventTest {

    @Mock private MessageReceivedEvent event;
    @Mock private Message message;

    @BeforeEach
    void init() {
        openMocks(this);
        when(event.getMessage()).thenReturn(message);
    }

    @ParameterizedTest
    @ValueSource(strings = {"?botCommand arg1 arg2", "?botCommand", "?botCommand arg1 arg2     "})
    void getCommandWithArgs(String messageInput) {
        when(message.getContentRaw()).thenReturn(messageInput);

        CommandEvent commandEvent = new CommandEvent(event);

        assertEquals("botCommand", commandEvent.getCommandString());
    }

    @Test
    void getCommandEmpty() {
        when(message.getContentRaw()).thenReturn("?");

        CommandEvent commandEvent = new CommandEvent(event);

        assertEquals("", commandEvent.getCommandString());
    }

    @ParameterizedTest
    @ValueSource(strings = {"?botCommand arg1 arg2", "?botCommand arg1 arg2     "})
    void getArguments2(String messageInput) {
        when(message.getContentRaw()).thenReturn(messageInput);

        CommandEvent commandEvent = new CommandEvent(event);
        LinkedList<String> args = commandEvent.getArguments();

        assertNotNull(args);
        assertEquals(2, args.size());
    }

    @ParameterizedTest
    @ValueSource(strings = {"?botCommand arg1 arg2", "?botCommand", "?botCommand arg1 arg2     ", "?"})
    void getPrefix(String messageInput) {
        when(message.getContentRaw()).thenReturn(messageInput);

        CommandEvent commandEvent = new CommandEvent(event);

        assertEquals("?", commandEvent.getPrefix());
    }
}