package net.dirtydeeds.discordsoundboard.commands;

import net.dirtydeeds.discordsoundboard.SoundPlayer;
import org.junit.jupiter.api.Test;

import java.util.LinkedList;

import static org.mockito.Mockito.*;

class StopCommandTest {

    @Test
    void stopWithFadeoutAndSomethingWasPlaying() {
        SoundPlayer sp = mock(SoundPlayer.class);
        CommandEvent event = mock(CommandEvent.class);
        when(event.getArguments()).thenReturn(new LinkedList<>(java.util.List.of("3")));
        when(event.getRequestingUser()).thenReturn("Tester");
        when(sp.stop(anyString(), isNull())).thenReturn("prev-file");

        StopCommand cmd = new StopCommand(sp);
        cmd.execute(event);

        verify(sp).stop(eq("Tester"), isNull());
        verify(event).replyByPrivateMessage(contains("Playback stopped"));
    }

    @Test
    void stopWhenNothingPlaying() {
        SoundPlayer sp = mock(SoundPlayer.class);
        CommandEvent event = mock(CommandEvent.class);
        when(event.getArguments()).thenReturn(new LinkedList<>());
        when(event.getRequestingUser()).thenReturn("Tester");
        when(sp.stop(anyString(), isNull())).thenReturn(null);

        StopCommand cmd = new StopCommand(sp);
        cmd.execute(event);

        verify(event).replyByPrivateMessage(contains("Nothing was playing"));
    }
}
