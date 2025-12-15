package net.dirtydeeds.discordsoundboard.commands;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.*;

class PingCommandTest {

    private CommandEvent event;
    private MessageReceivedEvent mre;
    private JDA jda;

    @BeforeEach
    void setUp() {
        event = mock(CommandEvent.class);
        mre = mock(MessageReceivedEvent.class);
        jda = mock(JDA.class);
        when(event.getMessageReceivedEvent()).thenReturn(mre);
        when(mre.getJDA()).thenReturn(jda);
        when(jda.getGatewayPing()).thenReturn(42L);
    }

    @Test
    void executeRepliesWithPing() {
        PingCommand cmd = new PingCommand();
        cmd.execute(event);
        verify(event).replyByPrivateMessage(contains("Web Socket: 42"));
    }
}
