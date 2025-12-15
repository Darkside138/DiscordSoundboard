package net.dirtydeeds.discordsoundboard.commands;

import net.dirtydeeds.discordsoundboard.SoundPlayer;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.*;

class ReloadCommandTest {
    @Test
    void executeUpdatesFilesAndReplies() {
        SoundPlayer sp = mock(SoundPlayer.class);
        CommandEvent event = mock(CommandEvent.class);
        ReloadCommand cmd = new ReloadCommand(sp);

        cmd.execute(event);

        verify(sp).updateFileList();
        verify(event).replyByPrivateMessage(contains("reloaded"));
    }
}
