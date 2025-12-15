package net.dirtydeeds.discordsoundboard.commands;

import net.dirtydeeds.discordsoundboard.SoundPlayer;
import net.dv8tion.jda.api.entities.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

        verify(soundPlayer).playForUser("beep", "Tester", 3, null);
    }

    @Test
    void executesWithDirectFileName() {
        when(event.getCommandString()).thenReturn("boop");
        when(event.getArguments()).thenReturn(new java.util.LinkedList<>());

        playCommand.execute(event);

        verify(soundPlayer).playForUser("boop", "Tester", 1, null);
    }
}
