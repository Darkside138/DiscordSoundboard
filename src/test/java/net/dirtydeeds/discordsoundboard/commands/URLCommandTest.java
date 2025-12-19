package net.dirtydeeds.discordsoundboard.commands;

import net.dirtydeeds.discordsoundboard.SoundPlayer;
import org.junit.jupiter.api.Test;

import java.util.LinkedList;

import static org.mockito.Mockito.*;

class URLCommandTest {
    @Test
    void playsWhenArgumentPresent() {
        SoundPlayer sp = mock(SoundPlayer.class);
        CommandEvent event = mock(CommandEvent.class);
        when(event.getArguments()).thenReturn(new LinkedList<>(java.util.List.of("http://example")));
        when(event.getRequestingUser()).thenReturn("Tester");

        URLCommand cmd = new URLCommand(sp);
        cmd.execute(event);

        verify(sp).playForUser("http://example", "Tester", 1, null, "Tester");
    }

    @Test
    void doesNothingWhenNoArgs() {
        SoundPlayer sp = mock(SoundPlayer.class);
        CommandEvent event = mock(CommandEvent.class);
        when(event.getArguments()).thenReturn(new LinkedList<>());

        URLCommand cmd = new URLCommand(sp);
        cmd.execute(event);

        verifyNoInteractions(sp);
    }
}
