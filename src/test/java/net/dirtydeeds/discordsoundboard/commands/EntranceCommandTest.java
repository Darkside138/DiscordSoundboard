package net.dirtydeeds.discordsoundboard.commands;

import net.dirtydeeds.discordsoundboard.SoundPlayer;
import net.dirtydeeds.discordsoundboard.beans.DiscordUser;
import net.dirtydeeds.discordsoundboard.beans.SoundFile;
import net.dirtydeeds.discordsoundboard.service.DiscordUserService;
import net.dirtydeeds.discordsoundboard.service.SoundService;
import net.dv8tion.jda.api.entities.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import java.util.LinkedList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.*;

class EntranceCommandTest {

    @Mock private EntranceCommand entranceCommand;
    @Mock private SoundPlayer soundPlayer;
    @Mock private DiscordUserService discordUserService;
    @Mock private SoundService soundService;
    @Mock private CommandEvent event;
    @Mock private User author;

    @BeforeEach
    void setUp() {
        soundPlayer = mock(SoundPlayer.class);
        discordUserService = mock(DiscordUserService.class);
        soundService = mock(SoundService.class);
        event = mock(CommandEvent.class);
        author = mock(User.class);

        entranceCommand = new EntranceCommand(soundPlayer, discordUserService, soundService);

        when(event.getAuthor()).thenReturn(author);
        when(author.getName()).thenReturn("testUser");
        when(author.getId()).thenReturn("123");
    }

    @Test
    void executeSetsEntranceSoundSuccessfully() {
        String userName = "testUser";
        String soundName = "welcome";
        when(event.getArguments()).thenReturn(new LinkedList<>(List.of(userName, soundName)));
        when(event.userIsAdmin()).thenReturn(false);

        DiscordUser discordUser = new DiscordUser("123", userName, false, null, null);
        when(discordUserService.findOneByIdOrUsernameIgnoreCase(userName, userName)).thenReturn(discordUser);
        when(soundService.findOneBySoundFileIdIgnoreCase(soundName)).thenReturn(new SoundFile(soundName, "path", "category"));

        entranceCommand.execute(event);

        assertEquals(soundName, discordUser.getEntranceSound());
        verify(discordUserService).save(discordUser);
        verify(event).replyByPrivateMessage(contains("entrance sound set to: welcome"));
    }

    @Test
    void executeClearsEntranceSoundWhenNoSoundProvided() {
        String userName = "testUser";
        when(event.getArguments()).thenReturn(new LinkedList<>(List.of(userName)));
        when(event.userIsAdmin()).thenReturn(false);

        DiscordUser discordUser = new DiscordUser("123", userName, false, null, null);
        discordUser.setEntranceSound("oldSound");
        when(discordUserService.findOneByIdOrUsernameIgnoreCase(userName, userName)).thenReturn(discordUser);

        entranceCommand.execute(event);

        assertNull(discordUser.getEntranceSound());
        verify(discordUserService).save(discordUser);
        verify(event).replyByPrivateMessage(contains("entrance sound cleared"));
    }

    @Test
    void executeFailsIfSoundNotFound() {
        String userName = "testUser";
        String soundName = "nonexistent";
        when(event.getArguments()).thenReturn(new LinkedList<>(List.of(userName, soundName)));
        when(event.userIsAdmin()).thenReturn(true);

        when(discordUserService.findOneByIdOrUsernameIgnoreCase(userName, userName))
                .thenReturn(new DiscordUser("123", userName, false, null, null));
        when(soundService.findOneBySoundFileIdIgnoreCase(soundName)).thenReturn(null);

        entranceCommand.execute(event);

        verify(event).replyByPrivateMessage(contains("Could not find sound file"));
        verify(discordUserService, never()).save(any());
    }

    @Test
    void executeFailsIfUserNotAdminAndEditingOtherUser() {
        when(event.getArguments()).thenReturn(new LinkedList<>(List.of("otherUser", "sound")));
        when(event.userIsAdmin()).thenReturn(false);
        when(event.getPrefix()).thenReturn("!");

        entranceCommand.execute(event);

        verify(event).replyByPrivateMessage(contains("You must have admin on the server"));
        verify(discordUserService, never()).save(any());
    }

    @Test
    void executeFailsIfUserNotFound() {
        String userName = "unknown";
        when(event.getArguments()).thenReturn(new LinkedList<>(List.of(userName)));
        when(event.userIsAdmin()).thenReturn(true);
        when(discordUserService.findOneByIdOrUsernameIgnoreCase(userName, userName)).thenReturn(null);
        when(soundPlayer.retrieveUserById(userName)).thenReturn(null);

        entranceCommand.execute(event);

        verify(event).replyByPrivateMessage(contains("Could not find user"));
    }
}