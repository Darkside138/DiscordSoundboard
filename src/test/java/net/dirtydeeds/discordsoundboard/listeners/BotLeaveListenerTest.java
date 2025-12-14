package net.dirtydeeds.discordsoundboard.listeners;

import net.dirtydeeds.discordsoundboard.BotConfig;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.managers.AudioManager;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.unions.AudioChannelUnion;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceUpdateEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import java.util.List;

import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.openMocks;

class BotLeaveListenerTest {

    @Mock private BotConfig botConfig;
    @Mock private Guild guild;
    @Mock private AudioManager audioManager;
    @Mock private AudioChannelUnion voiceChannel;
    @Mock private Member humanMember;
    @Mock private Member botMember;
    @Mock private User humanUser;
    @Mock private User botUser;
    @Mock private GuildVoiceUpdateEvent event;

    private BotLeaveListener listener;

    @BeforeEach
    void setUp() {
        openMocks(this);
        listener = new BotLeaveListener(botConfig);
        when(event.getGuild()).thenReturn(guild);
        when(guild.getAudioManager()).thenReturn(audioManager);
        when(audioManager.getConnectedChannel()).thenReturn(voiceChannel);
        when(humanMember.getUser()).thenReturn(humanUser);
        when(botMember.getUser()).thenReturn(botUser);
    }

    @Test
    void closes_connection_when_alone_and_enabled() {
        when(botConfig.isLeaveOnEmptyChannel()).thenReturn(true);
        when(humanUser.isBot()).thenReturn(true); // none human
        when(humanUser.isSystem()).thenReturn(true);
        when(botUser.isBot()).thenReturn(true);
        when(voiceChannel.getMembers()).thenReturn(List.of(botMember));

        listener.onGuildVoiceUpdate(event);

        verify(audioManager, times(1)).closeAudioConnection();
    }

    @Test
    void does_not_close_when_humans_present() {
        when(botConfig.isLeaveOnEmptyChannel()).thenReturn(true);
        when(humanUser.isBot()).thenReturn(false);
        when(humanUser.isSystem()).thenReturn(false);
        when(voiceChannel.getMembers()).thenReturn(List.of(humanMember));

        listener.onGuildVoiceUpdate(event);

        verify(audioManager, never()).closeAudioConnection();
    }

    @Test
    void does_not_close_when_feature_disabled() {
        when(botConfig.isLeaveOnEmptyChannel()).thenReturn(false);
        when(voiceChannel.getMembers()).thenReturn(List.of());

        listener.onGuildVoiceUpdate(event);

        verify(audioManager, never()).closeAudioConnection();
    }

    @Test
    void does_nothing_when_not_connected() {
        when(audioManager.getConnectedChannel()).thenReturn(null);
        when(botConfig.isLeaveOnEmptyChannel()).thenReturn(true);

        listener.onGuildVoiceUpdate(event);

        verify(audioManager, never()).closeAudioConnection();
    }
}
