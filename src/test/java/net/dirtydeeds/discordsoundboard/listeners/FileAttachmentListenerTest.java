package net.dirtydeeds.discordsoundboard.listeners;

import net.dirtydeeds.discordsoundboard.BotConfig;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.openMocks;

class FileAttachmentListenerTest {

    @Mock private BotConfig botConfig;
    @Mock private MessageReceivedEvent event;
    @Mock private Message message;
    @Mock private MessageChannelUnion channel;
    @Mock private User author;

    private FileAttachmentListener listener;

    @BeforeEach
    void setUp() {
        openMocks(this);
        listener = new FileAttachmentListener(botConfig);

        when(event.getMessage()).thenReturn(message);
        when(event.getChannel()).thenReturn(channel);
        when(event.getAuthor()).thenReturn(author);
    }

    @Test
    void no_attachments_results_in_no_action() {
        when(message.getAttachments()).thenReturn(Collections.emptyList());

        listener.onMessageReceived(event);

        // Ensure nothing else attempted
        verify(channel, never()).sendMessage(any(CharSequence.class));
    }

    @Test
    void attachments_in_guild_channel_are_ignored() {
        when(message.getAttachments()).thenReturn(List.of(mock(Message.Attachment.class)));
        when(event.isFromType(ChannelType.PRIVATE)).thenReturn(false);

        listener.onMessageReceived(event);

        verify(channel, never()).sendMessage(any(CharSequence.class));
    }
}
