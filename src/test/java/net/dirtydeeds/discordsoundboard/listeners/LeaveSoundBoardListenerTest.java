package net.dirtydeeds.discordsoundboard.listeners;

import net.dirtydeeds.discordsoundboard.BotConfig;
import net.dirtydeeds.discordsoundboard.SoundPlayer;
import net.dirtydeeds.discordsoundboard.beans.DiscordUser;
import net.dirtydeeds.discordsoundboard.beans.SoundFile;
import net.dirtydeeds.discordsoundboard.controllers.DiscordUserController;
import net.dirtydeeds.discordsoundboard.service.DiscordUserService;
import net.dirtydeeds.discordsoundboard.service.SoundService;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.unions.AudioChannelUnion;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceUpdateEvent;
import net.dv8tion.jda.api.managers.AudioManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.openMocks;

class LeaveSoundBoardListenerTest {

    @Mock private SoundPlayer bot;
    @Mock private DiscordUserService discordUserService;
    @Mock private SoundService soundService;
    @Mock private BotConfig botConfig;
    @Mock private DiscordUserController discordUserController;
    @Mock private GuildVoiceUpdateEvent event;
    @Mock private Member member;
    @Mock private User user;
    @Mock private AudioChannelUnion leftChannel;
    @Mock private SoundPlayer soundPlayer;
    @Mock private Guild guild;
    @Mock private AudioChannelUnion joinedChannel;
    @Mock private AudioManager audioManager;

    private LeaveSoundBoardListener listener;

    @BeforeEach
    void setUp() {
        openMocks(this);
        listener = new LeaveSoundBoardListener(bot, discordUserService, soundService, botConfig,
                                                discordUserController, soundPlayer);
        when(event.getMember()).thenReturn(member);
        when(member.getUser()).thenReturn(user);
        when(event.getChannelJoined()).thenReturn(null);
        when(event.getChannelLeft()).thenReturn(leftChannel);
        when(member.getEffectiveName()).thenReturn("alice");
        when(member.getId()).thenReturn("u1");
        when(event.getGuild()).thenReturn(guild);
        when(guild.getAudioManager()).thenReturn(audioManager);
        when(guild.getAudioManager().getConnectedChannel()).thenReturn(joinedChannel);
    }

    @Test
    void plays_user_specific_leave_sound_when_set() {
        DiscordUser du = new DiscordUser();
        du.setUsername("alice");
        du.setLeaveSound("bye");
        when(discordUserService.findOneByIdOrUsernameIgnoreCase("u1", "alice")).thenReturn(du);

        listener.onGuildVoiceUpdate(event);

        verify(discordUserController, times(1)).broadcastUpdate();
        verify(bot, times(1)).playFileInChannel("bye", leftChannel, du);
    }

    @Test
    void falls_back_to_username_plus_suffix_when_no_user_sound() {
        DiscordUser du = new DiscordUser();
        du.setUsername("alice");
        when(discordUserService.findOneByIdOrUsernameIgnoreCase("u1", "alice")).thenReturn(du);
        when(botConfig.getLeaveSuffix()).thenReturn("_leave");
        SoundFile sf = new SoundFile();
        sf.setSoundFileId("alice_leave");
        when(soundService.findOneBySoundFileIdIgnoreCase("alice_leave")).thenReturn(sf);

        listener.onGuildVoiceUpdate(event);

        verify(bot, times(1)).playFileInChannel("alice_leave", leftChannel, du);
    }

    @Test
    void does_nothing_when_no_files_found() {
        DiscordUser du = new DiscordUser();
        du.setUsername("alice");
        when(discordUserService.findOneByIdOrUsernameIgnoreCase("u1", "alice")).thenReturn(du);
        when(botConfig.getLeaveSuffix()).thenReturn("_leave");

        listener.onGuildVoiceUpdate(event);

        verify(bot, never()).playFileInChannel(any(), any(), any());
    }
}
