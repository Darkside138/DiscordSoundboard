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

class EntranceSoundBoardListenerTest {

    @Mock private SoundPlayer bot;
    @Mock private DiscordUserService discordUserService;
    @Mock private SoundService soundService;
    @Mock private BotConfig botConfig;
    @Mock private DiscordUserController discordUserController;
    @Mock private GuildVoiceUpdateEvent event;
    @Mock private Member member;
    @Mock private User user;
    @Mock private AudioChannelUnion joinedChannel;

    private EntranceSoundBoardListener listener;

    @BeforeEach
    void setUp() {
        openMocks(this);
        listener = new EntranceSoundBoardListener(bot, discordUserService, soundService, true, botConfig, discordUserController);
        when(event.getMember()).thenReturn(member);
        when(member.getUser()).thenReturn(user);
        when(user.isBot()).thenReturn(false);
        when(event.getChannelLeft()).thenReturn(null);
        when(event.getChannelJoined()).thenReturn(joinedChannel);
        when(member.getEffectiveName()).thenReturn("alice");
        when(member.getId()).thenReturn("u1");
    }

    @Test
    void plays_user_specific_entrance_sound_when_set() {
        DiscordUser du = new DiscordUser();
        du.setUsername("alice");
        du.setEntranceSound("hello");
        when(discordUserService.findOneByIdOrUsernameIgnoreCase("u1", "alice")).thenReturn(du);

        listener.onGuildVoiceUpdate(event);

        verify(discordUserController, times(1)).broadcastUpdate();
        verify(bot, times(1)).playFileInChannel("hello", joinedChannel, du);
    }

    @Test
    void falls_back_to_global_entrance_when_user_has_none() {
        DiscordUser du = new DiscordUser();
        du.setUsername("alice");
        when(discordUserService.findOneByIdOrUsernameIgnoreCase("u1", "alice")).thenReturn(du);
        when(botConfig.getEntranceForAll()).thenReturn("global");

        listener.onGuildVoiceUpdate(event);

        verify(bot, times(1)).playFileInChannel("global", joinedChannel, du);
    }

    @Test
    void falls_back_to_username_file_when_no_db_or_global() {
        DiscordUser du = new DiscordUser();
        du.setUsername("alice");
        when(discordUserService.findOneByIdOrUsernameIgnoreCase("u1", "alice")).thenReturn(du);
        when(botConfig.getEntranceForAll()).thenReturn("");
        SoundFile sf = new SoundFile();
        sf.setSoundFileId("alice");
        when(soundService.findOneBySoundFileIdIgnoreCase("alice")).thenReturn(sf);

        listener.onGuildVoiceUpdate(event);

        verify(bot, times(1)).playFileInChannel("alice", joinedChannel, du);
    }

    @Test
    void ignores_bots() {
        when(user.isBot()).thenReturn(true);
        listener.onGuildVoiceUpdate(event);
        verify(bot, never()).playFileInChannel(any(), any(), any());
    }
}
