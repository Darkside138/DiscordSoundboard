package net.dirtydeeds.discordsoundboard.listeners;

import net.dirtydeeds.discordsoundboard.BotConfig;
import net.dirtydeeds.discordsoundboard.commands.Command;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.exceptions.PermissionException;
import net.dv8tion.jda.api.requests.restaction.AuditableRestAction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.openMocks;

class CommandListenerDeleteBehaviorTest {

    @Mock private BotConfig botConfig;
    @Mock private MessageReceivedEvent event;
    @Mock private Message message;
    @Mock private User user;
    @Mock private Command someCommand; // will be executed to trigger afterMessageReceived

    private CommandListener listener;

    @BeforeEach
    void init() {
        openMocks(this);
        listener = new CommandListener(botConfig);

        when(event.getMessage()).thenReturn(message);
        when(event.getAuthor()).thenReturn(user);
        when(user.isBot()).thenReturn(false);
        when(botConfig.isRespondToChatCommands()).thenReturn(true);
        when(botConfig.isRespondToDmsString()).thenReturn(false); // we'll use non-private channel
        when(botConfig.getCommandCharacter()).thenReturn("?");

        when(someCommand.getName()).thenReturn("echo");
        listener.addCommand(someCommand);
    }

    @Test
    void deletes_message_in_guild_channels() {
        when(event.isFromType(ChannelType.PRIVATE)).thenReturn(false);
        when(message.getContentRaw()).thenReturn("?echo hi");

        @SuppressWarnings("unchecked")
        AuditableRestAction<Void> restAction = mock(AuditableRestAction.class);
        when(message.delete()).thenReturn(restAction);

        listener.onMessageReceived(event);

        // ensure command executed and deletion queued
        verify(someCommand, times(1)).run(any());
        verify(restAction, times(1)).queue(any(), any());
    }

    @Test
    void permission_exception_during_delete_is_swallowed() {
        when(event.isFromType(ChannelType.PRIVATE)).thenReturn(false);
        when(message.getContentRaw()).thenReturn("?echo hi");

        @SuppressWarnings("unchecked")
        AuditableRestAction<Void> restAction = mock(AuditableRestAction.class);
        when(message.delete()).thenReturn(restAction);
        doThrow(new PermissionException("no perms"))
                .when(restAction).queue(any(), any());

        // Should not throw
        listener.onMessageReceived(event);

        verify(someCommand, times(1)).run(any());
        verify(restAction, times(1)).queue(any(), any());
    }

    @Test
    void does_not_delete_in_private_channels() {
        when(event.isFromType(ChannelType.PRIVATE)).thenReturn(true);
        // allow responding to DMs in this scenario
        when(botConfig.isRespondToDmsString()).thenReturn(true);
        when(message.getContentRaw()).thenReturn("?echo hi");

        listener.onMessageReceived(event);

        verify(someCommand, times(1)).run(any());
        verify(message, never()).delete();
    }
}
