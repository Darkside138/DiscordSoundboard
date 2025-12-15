package net.dirtydeeds.discordsoundboard.commands;

import net.dirtydeeds.discordsoundboard.SoundPlayer;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.*;

class DisconnectCommandTest {
    @Test
    void executeCallsDisconnect() {
        SoundPlayer sp = mock(SoundPlayer.class);
        CommandEvent event = mock(CommandEvent.class);
        MessageReceivedEvent mre = mock(MessageReceivedEvent.class);
        Guild guild = mock(Guild.class);
        when(event.getMessageReceivedEvent()).thenReturn(mre);
        when(mre.getGuild()).thenReturn(guild);

        DisconnectCommand cmd = new DisconnectCommand(sp);
        cmd.execute(event);

        verify(sp).disconnectFromChannel(guild);
    }
}
