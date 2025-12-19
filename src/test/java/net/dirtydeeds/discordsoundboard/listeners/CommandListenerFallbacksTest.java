package net.dirtydeeds.discordsoundboard.listeners;

import net.dirtydeeds.discordsoundboard.BotConfig;
import net.dirtydeeds.discordsoundboard.commands.Command;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.openMocks;

class CommandListenerFallbacksTest {

    @Mock private BotConfig botConfig;
    @Mock private MessageReceivedEvent event;
    @Mock private Message message;
    @Mock private User user;
    @Mock private Command help;
    @Mock private Command play;

    private CommandListener listener;

    @BeforeEach
    void init() {
        openMocks(this);
        listener = new CommandListener(botConfig);

        when(event.getMessage()).thenReturn(message);
        when(event.getAuthor()).thenReturn(user);
        when(user.isBot()).thenReturn(false);
        when(botConfig.isRespondToChatCommands()).thenReturn(true);
        when(botConfig.isRespondToDmsString()).thenReturn(true);
        when(event.isFromType(ChannelType.PRIVATE)).thenReturn(true);
        when(botConfig.getCommandCharacter()).thenReturn("?");

        when(help.getName()).thenReturn("help");
        when(play.getName()).thenReturn("play");

        listener.addCommand(help);
        listener.addCommand(play);
    }

    @Test
    void unknown_command_falls_back_to_play() {
        when(message.getContentRaw()).thenReturn("?unknown song");

        listener.onMessageReceived(event);

        verify(play, times(1)).run(any());
        verify(help, never()).run(any());
    }

    @Test
    void only_prefix_falls_back_to_help() {
        when(message.getContentRaw()).thenReturn("?");

        listener.onMessageReceived(event);

        verify(help, times(1)).run(any());
        verify(play, never()).run(any());
    }
}
