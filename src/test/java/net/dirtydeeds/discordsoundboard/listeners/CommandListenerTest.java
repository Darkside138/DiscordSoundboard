package net.dirtydeeds.discordsoundboard.listeners;

import net.dirtydeeds.discordsoundboard.BotConfig;
import net.dirtydeeds.discordsoundboard.SoundPlayer;
import net.dirtydeeds.discordsoundboard.commands.HelpCommand;
import net.dirtydeeds.discordsoundboard.commands.ListCommand;
import net.dirtydeeds.discordsoundboard.commands.PlayCommand;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.openMocks;

class CommandListenerTest {

    @Mock private BotConfig botConfig;
    @Mock SoundPlayer soundPlayer;
    @Mock private Message message;
    @Mock HelpCommand help;
    @Mock PlayCommand play;
    @Mock ListCommand list;
    @Mock MessageReceivedEvent messageReceivedEvent;
    @Mock User user;

    CommandListener commandListener;

    @BeforeEach
    void init() {
        openMocks(this);
        when(messageReceivedEvent.getMessage()).thenReturn(message);
        when(help.getName()).thenReturn("help");
        when(play.getName()).thenReturn("play");
        when(list.getName()).thenReturn("list");
        commandListener = new CommandListener(botConfig);
    }

    @Test
    void onMessageReceivedTestPlay() {
        setupWithNormalCommands();

        when(message.getContentRaw()).thenReturn("?9k");

        commandListener.onMessageReceived(messageReceivedEvent);

        verify(play, times(1)).run(any());
    }

    @Test
    void onMessageReceivedTestList() {
        setupWithNormalCommands();

        when(message.getContentRaw()).thenReturn("?list");

        commandListener.onMessageReceived(messageReceivedEvent);

        verify(list, times(1)).run(any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"?", "?help"})
    void onMessageReceivedTestHelp(String messageInput) {
        setupWithNormalCommands();

        when(message.getContentRaw()).thenReturn(messageInput);

        commandListener.onMessageReceived(messageReceivedEvent);

        verify(help, times(1)).run(any());
    }

    private void setupWithNormalCommands() {
        commandListener.addCommand(help);
        commandListener.addCommand(play);
        commandListener.addCommand(list);

        when(botConfig.isRespondToChatCommands()).thenReturn(true);
        when(messageReceivedEvent.getAuthor()).thenReturn(user);
        when(user.isBot()).thenReturn(false);
        when(botConfig.isRespondToDmsString()).thenReturn(true);
        when(messageReceivedEvent.isFromType(ChannelType.PRIVATE)).thenReturn(true);
        when(botConfig.getCommandCharacter()).thenReturn("?");
        when(soundPlayer.isUserAllowed(any())).thenReturn(true);
        when(soundPlayer.isUserBanned(any())).thenReturn(false);
    }

    @Test
    void ignores_messages_from_bot_users() {
        // Arrange minimal command set
        commandListener.addCommand(help);
        commandListener.addCommand(play);
        when(botConfig.isRespondToChatCommands()).thenReturn(true);
        when(botConfig.isRespondToDmsString()).thenReturn(true);
        when(messageReceivedEvent.isFromType(ChannelType.PRIVATE)).thenReturn(true);
        when(botConfig.getCommandCharacter()).thenReturn("?");
        when(message.getContentRaw()).thenReturn("?help");

        when(messageReceivedEvent.getAuthor()).thenReturn(user);
        when(user.isBot()).thenReturn(true); // bot author

        // Act
        commandListener.onMessageReceived(messageReceivedEvent);

        // Assert: no commands executed
        verify(help, never()).run(any());
        verify(play, never()).run(any());
    }

    @Test
    void does_not_respond_when_chat_commands_disabled() {
        setupWithNormalCommands();
        when(botConfig.isRespondToChatCommands()).thenReturn(false);
        when(message.getContentRaw()).thenReturn("?help");

        commandListener.onMessageReceived(messageReceivedEvent);

        verify(help, never()).run(any());
        verify(play, never()).run(any());
        verify(list, never()).run(any());
    }

    @Test
    void user_not_in_allowed_list_is_blocked() {
        setupWithNormalCommands();
        when(message.getContentRaw()).thenReturn("?help");
        // Simulate allowed users list does NOT contain the requesting user
        when(messageReceivedEvent.getAuthor()).thenReturn(user);
        when(user.getName()).thenReturn("eve");
        when(botConfig.getAllowedUsersList()).thenReturn(java.util.List.of("alice", "bob"));

        commandListener.onMessageReceived(messageReceivedEvent);

        verify(help, never()).run(any());
    }

    @Test
    void banned_user_is_blocked() {
        setupWithNormalCommands();
        when(message.getContentRaw()).thenReturn("?help");
        when(messageReceivedEvent.getAuthor()).thenReturn(user);
        when(user.getName()).thenReturn("mallory");
        when(botConfig.getBannedUsersList()).thenReturn(java.util.List.of("mallory"));

        commandListener.onMessageReceived(messageReceivedEvent);

        verify(help, never()).run(any());
    }
}