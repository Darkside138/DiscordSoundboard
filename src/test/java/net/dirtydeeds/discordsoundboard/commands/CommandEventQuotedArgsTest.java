package net.dirtydeeds.discordsoundboard.commands;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import java.util.LinkedList;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

class CommandEventQuotedArgsTest {

    @Mock private MessageReceivedEvent event;
    @Mock private Message message;

    @BeforeEach
    void init() {
        openMocks(this);
        when(event.getMessage()).thenReturn(message);
    }

    @Test
    void quoted_argument_with_spaces_is_parsed_as_single_arg() {
        when(message.getContentRaw()).thenReturn("?play \"file name with spaces\" --loop");

        CommandEvent ce = new CommandEvent(event);
        assertEquals("play", ce.getCommandString());
        LinkedList<String> args = ce.getArguments();
        assertEquals(2, args.size());
        assertEquals("file name with spaces", args.get(0));
        assertEquals("--loop", args.get(1));
    }

    @Test
    void multiple_spaces_and_trailing_whitespace_are_ignored() {
        when(message.getContentRaw()).thenReturn("?play    sound   another   ");

        CommandEvent ce = new CommandEvent(event);
        assertEquals("play", ce.getCommandString());
        LinkedList<String> args = ce.getArguments();
        assertEquals(2, args.size());
        assertEquals("sound", args.get(0));
        assertEquals("another", args.get(1));
    }
}
