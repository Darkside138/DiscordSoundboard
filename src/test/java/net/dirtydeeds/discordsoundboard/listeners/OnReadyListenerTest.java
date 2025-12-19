package net.dirtydeeds.discordsoundboard.listeners;

import net.dirtydeeds.discordsoundboard.JDABot;
import net.dirtydeeds.discordsoundboard.handlers.PlayerManager;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Answers;

import java.util.List;

import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.openMocks;

class OnReadyListenerTest {

    @Mock private JDABot jdaBot;
    @Mock private PlayerManager playerManager;
    @Mock private ReadyEvent readyEvent;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS) private JDA jda;
    @Mock private Guild guild1;
    @Mock private Guild guild2;

    private OnReadyListener listener;

    @BeforeEach
    void setUp() {
        openMocks(this);
        when(jdaBot.getPlayerManager()).thenReturn(playerManager);
        listener = new OnReadyListener(jdaBot);

        when(readyEvent.getJDA()).thenReturn(jda);
        when(jda.getGuildCache().isEmpty()).thenReturn(false);
        when(jda.getGuilds()).thenReturn(List.of(guild1, guild2));
    }

    @Test
    void sets_up_handlers_for_all_guilds_on_ready() {
        listener.onReady(readyEvent);

        verify(playerManager, times(1)).setUpHandler(guild1);
        verify(playerManager, times(1)).setUpHandler(guild2);
    }
}
