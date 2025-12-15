package net.dirtydeeds.discordsoundboard.listeners;

import net.dirtydeeds.discordsoundboard.BotConfig;
import net.dirtydeeds.discordsoundboard.SoundPlayer;
import net.dirtydeeds.discordsoundboard.beans.DiscordUser;
import net.dirtydeeds.discordsoundboard.beans.SoundFile;
import net.dirtydeeds.discordsoundboard.controllers.DiscordUserController;
import net.dirtydeeds.discordsoundboard.service.DiscordUserService;
import net.dirtydeeds.discordsoundboard.service.SoundService;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.unions.AudioChannelUnion;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceUpdateEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.openMocks;

class MovedChannelListenerTest {

    @Mock private SoundPlayer bot;
    @Mock private DiscordUserService discordUserService;
    @Mock private SoundService soundService;
    @Mock private BotConfig botConfig;
    @Mock private DiscordUserController discordUserController;
    @Mock private GuildVoiceUpdateEvent event;
    @Mock private Member member;
    @Mock private User user;
    @Mock private AudioChannelUnion joined;
    @Mock private AudioChannelUnion left;

    private MovedChannelListener listenerEnabled;
    private MovedChannelListener listenerDisabled;

    @BeforeEach
    void setUp() {
        openMocks(this);
        listenerEnabled = new MovedChannelListener(bot, discordUserService, soundService, true, botConfig, discordUserController);
        listenerDisabled = new MovedChannelListener(bot, discordUserService, soundService, false, botConfig, discordUserController);
        when(event.getMember()).thenReturn(member);
        when(member.getUser()).thenReturn(user);
        when(user.isBot()).thenReturn(false);
        when(event.getChannelLeft()).thenReturn(left);
        when(event.getChannelJoined()).thenReturn(joined);
        when(member.getEffectiveName()).thenReturn("alice");
        when(member.getId()).thenReturn("u1");
    }

    @Test
    void plays_global_entrance_when_configured() {
        when(botConfig.getEntranceForAll()).thenReturn("global");

        listenerEnabled.onGuildVoiceUpdate(event);

        verify(discordUserController, times(1)).broadcastUpdate();
        verify(bot, times(1)).playFileInChannel(eq("global"), eq(joined), any());
    }

    @Test
    void does_nothing_when_disabled() {
        listenerDisabled.onGuildVoiceUpdate(event);
        verify(bot, never()).playFileInChannel(any(), any(), any());
    }

    @Test
    void falls_back_to_disconnect_when_only_leave_sound_available() {
        // No global entrance
        when(botConfig.getEntranceForAll()).thenReturn("");

        DiscordUser du = new DiscordUser();
        du.setUsername("alice");
        du.setLeaveSound("bye");
        when(discordUserService.findOneByIdOrUsernameIgnoreCase("u1", "alice")).thenReturn(du);

        listenerEnabled.onGuildVoiceUpdate(event);

        verify(bot, times(1)).playFileInChannel("bye", left, du);
        verify(bot, never()).playFileInChannel(eq("bye"), eq(joined), eq(du));
    }
}
