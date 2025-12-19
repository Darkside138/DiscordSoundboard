package net.dirtydeeds.discordsoundboard.controllers;

import jakarta.servlet.http.HttpServletResponse;
import net.dirtydeeds.discordsoundboard.service.PlaybackService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PlaybackControllerTest {

    @Mock
    private PlaybackService playbackService;

    @Mock
    private HttpServletResponse response;

    private PlaybackController playbackController;

    @BeforeEach
    void setUp() {
        playbackController = new PlaybackController(playbackService);
    }

    @Test
    void streamPlayback_setsResponseHeaders() {
        // Arrange
        SseEmitter expectedEmitter = new SseEmitter();
        when(playbackService.createEmitter()).thenReturn(expectedEmitter);

        // Act
        SseEmitter result = playbackController.streamPlayback(response);

        // Assert
        verify(response).setHeader("Cache-Control", "no-cache");
        verify(response).setHeader("Connection", "keep-alive");
        verify(response).setHeader("X-Accel-Buffering", "no");
    }

    @Test
    void streamPlayback_returnsEmitterFromService() {
        // Arrange
        SseEmitter expectedEmitter = new SseEmitter();
        when(playbackService.createEmitter()).thenReturn(expectedEmitter);

        // Act
        SseEmitter result = playbackController.streamPlayback(response);

        // Assert
        assertSame(expectedEmitter, result);
        verify(playbackService).createEmitter();
    }

    @Test
    void streamPlayback_callsCreateEmitterOnce() {
        // Arrange
        when(playbackService.createEmitter()).thenReturn(new SseEmitter());

        // Act
        playbackController.streamPlayback(response);

        // Assert
        verify(playbackService, times(1)).createEmitter();
    }
}