package net.dirtydeeds.discordsoundboard.commands;

import net.dirtydeeds.discordsoundboard.SoundPlayer;
import net.dv8tion.jda.api.entities.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class PlayCommandTest {

    private SoundPlayer soundPlayer;
    private CommandEvent event;
    private PlayCommand playCommand;

    @BeforeEach
    void setUp() {
        soundPlayer = mock(SoundPlayer.class);
        event = mock(CommandEvent.class);
        playCommand = new PlayCommand(soundPlayer);

        User author = mock(User.class);
        when(author.getName()).thenReturn("Tester");
        when(event.getAuthor()).thenReturn(author);
        when(event.getRequestingUser()).thenReturn("Tester");
    }

    @Test
    void executesWithExplicitCommandNameAndRepeat() {
        when(event.getCommandString()).thenReturn("Play");
        when(event.getArguments()).thenReturn(new java.util.LinkedList<>(java.util.List.of("beep~3")));

        playCommand.execute(event);

        verify(soundPlayer).playForUser("beep", "Tester", 3, null, "Tester");
    }

    @Test
    void executesWithDirectFileName() {
        when(event.getCommandString()).thenReturn("boop");
        when(event.getArguments()).thenReturn(new java.util.LinkedList<>());

        playCommand.execute(event);

        verify(soundPlayer).playForUser("boop", "Tester", 1, null, "Tester");
    }

    @Test
    void executesWithRepeatCountZero_passesZeroToSoundPlayer() {
        when(event.getCommandString()).thenReturn("Play");
        when(event.getArguments()).thenReturn(new java.util.LinkedList<>(java.util.List.of("beep~0")));

        playCommand.execute(event);

        verify(soundPlayer).playForUser("beep", "Tester", 0, null, "Tester");
    }

    @Test
    void executesWithNegativeRepeatCount_passesNegativeToSoundPlayer() {
        when(event.getCommandString()).thenReturn("Play");
        when(event.getArguments()).thenReturn(new java.util.LinkedList<>(java.util.List.of("beep~-1")));

        playCommand.execute(event);

        verify(soundPlayer).playForUser("beep", "Tester", -1, null, "Tester");
    }

    @Test
    void executesWithNonNumericRepeatSuffix_doesNotInvokeSoundPlayer() {
        // Integer.parseInt("abc") throws → caught by outer try-catch → soundPlayer never called
        when(event.getCommandString()).thenReturn("Play");
        when(event.getArguments()).thenReturn(new java.util.LinkedList<>(java.util.List.of("beep~abc")));

        playCommand.execute(event);

        verify(soundPlayer, never()).playForUser(anyString(), anyString(), anyInt(), any(), anyString());
    }

    @Test
    void executesWithEmptySoundName_playsWithEmptyName() {
        when(event.getCommandString()).thenReturn("Play");
        when(event.getArguments()).thenReturn(new java.util.LinkedList<>(java.util.List.of("")));

        playCommand.execute(event);

        // Empty string with no ~ is passed directly to soundPlayer with repeatNumber=1
        verify(soundPlayer).playForUser("", "Tester", 1, null, "Tester");
    }

    @Test
    void executesWithRepeatCountAtLargeNumber_playsWithThatCount() {
        when(event.getCommandString()).thenReturn("Play");
        when(event.getArguments()).thenReturn(new java.util.LinkedList<>(java.util.List.of("beep~999")));

        playCommand.execute(event);

        verify(soundPlayer).playForUser("beep", "Tester", 999, null, "Tester");
    }
}
