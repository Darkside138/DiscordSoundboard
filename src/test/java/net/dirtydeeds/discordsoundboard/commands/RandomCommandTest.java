package net.dirtydeeds.discordsoundboard.commands;

import net.dirtydeeds.discordsoundboard.SoundPlaybackException;
import net.dirtydeeds.discordsoundboard.SoundPlayer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.*;

class RandomCommandTest {
    private SoundPlayer soundPlayer;
    private CommandEvent event;

    @BeforeEach
    void setUp() {
        soundPlayer = mock(SoundPlayer.class);
        event = mock(CommandEvent.class);
        when(event.getRequestingUser()).thenReturn("Tester");
    }

    @Test
    void executesHappyPath() throws Exception {
        when(event.getMessageReceivedEvent()).thenReturn(null);
        RandomCommand cmd = new RandomCommand(soundPlayer);

        cmd.execute(event);

        verify(soundPlayer).playRandomSoundFile(eq("Tester"), any());
    }

    @Test
    void handlesPlaybackException() throws Exception {
        when(event.getMessageReceivedEvent()).thenReturn(null);
        RandomCommand cmd = new RandomCommand(soundPlayer);

        doThrow(new SoundPlaybackException("bad"))
                .when(soundPlayer).playRandomSoundFile(eq("Tester"), any());

        cmd.execute(event);

        verify(event).replyByPrivateMessage(contains("Problem playing random file"));
    }
}
