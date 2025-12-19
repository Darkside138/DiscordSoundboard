package net.dirtydeeds.discordsoundboard.controllers;

import net.dirtydeeds.discordsoundboard.SoundPlaybackException;
import net.dirtydeeds.discordsoundboard.SoundPlayer;
import net.dirtydeeds.discordsoundboard.beans.SoundFile;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.times;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = BotCommandController.class)
@AutoConfigureMockMvc(addFilters = false)
class BotCommandControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SoundPlayer soundPlayer;

    @MockBean
    private BotVolumeController botVolumeController;

    @MockBean
    private net.dirtydeeds.discordsoundboard.util.UserRoleConfig userRoleConfig;

    @MockBean
    private net.dirtydeeds.discordsoundboard.service.DiscordUserService discordUserService;

    @Test
    @DisplayName("POST /bot/playFile returns 200 and calls soundPlayer.playForUser")
    void playFile_ok() throws Exception {
        mockMvc.perform(post("/bot/playFile")
                        .param("soundFileId", "file-1")
                        .param("username", "alice")
                        .param("repeatTimes", "2")
                        .param("voiceChannelId", "vc1"))
                .andExpect(status().isOk());

        Mockito.verify(soundPlayer, times(1))
                .playForUser("file-1", "alice", 2, "vc1", "anonymous");
    }

    @Test
    @DisplayName("POST /bot/playUrl returns 200 and calls soundPlayer.playForUser with URL")
    void playUrl_ok() throws Exception {
        mockMvc.perform(post("/bot/playUrl")
                        .param("url", "http://example.com/a.mp3")
                        .param("username", "bob"))
                .andExpect(status().isOk());

        Mockito.verify(soundPlayer, times(1))
                .playForUser("http://example.com/a.mp3", "bob", 1, "", "anonymous");
    }

    @Test
    @DisplayName("POST /bot/random returns 200 on success, 500 on SoundPlaybackException")
    void random_status_codes() throws Exception {
        // success path
        Mockito.when(soundPlayer.playRandomSoundFile(anyString(), isNull(), anyString()))
                .thenReturn(Mockito.mock(SoundFile.class));

        mockMvc.perform(post("/bot/random").param("username", "carol"))
                .andExpect(status().isOk());

        // error path
        Mockito.when(soundPlayer.playRandomSoundFile(anyString(), isNull(), anyString()))
                .thenThrow(new SoundPlaybackException("boom"));

        mockMvc.perform(post("/bot/random").param("username", "carol"))
                .andExpect(status().is5xxServerError());
    }

    @Test
    @DisplayName("POST /bot/stop returns 200 and calls soundPlayer.stop")
    void stop_ok() throws Exception {
        Mockito.when(soundPlayer.stop(anyString(), anyString())).thenReturn("some-id");

        mockMvc.perform(post("/bot/stop")
                        .param("username", "dave")
                        .param("voiceChannelId", "vc2"))
                .andExpect(status().isOk());

        Mockito.verify(soundPlayer).stop("dave", "vc2");
    }

    @Test
    @DisplayName("POST /bot/volume calls setGlobalVolume and broadcasts update")
    void volume_set_ok() throws Exception {
        mockMvc.perform(post("/bot/volume")
                        .param("volume", "75")
                        .param("username", "erin"))
                .andExpect(status().isOk());

        Mockito.verify(soundPlayer).setGlobalVolume(75, "erin", null);
        Mockito.verify(botVolumeController).broadcastUpdate("erin");
    }

    @Test
    @DisplayName("GET /bot/volume returns float value from soundPlayer")
    void volume_get_ok() throws Exception {
        Mockito.when(soundPlayer.getGlobalVolume(anyString(), anyString())).thenReturn(0.42f);

        mockMvc.perform(get("/bot/volume").param("username", "frank"))
                .andExpect(status().isOk())
                .andExpect(content().string("0.42"));
    }

    @Test
    @DisplayName("GET /bot/channels returns list from soundPlayer")
    void channels_ok() throws Exception {
        Mockito.when(soundPlayer.getVoiceChannels()).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/bot/channels").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }
}
