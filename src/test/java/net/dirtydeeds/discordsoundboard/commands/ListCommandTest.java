package net.dirtydeeds.discordsoundboard.commands;

import net.dirtydeeds.discordsoundboard.BotConfig;
import net.dirtydeeds.discordsoundboard.SoundPlayer;
import net.dirtydeeds.discordsoundboard.beans.SoundFile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;

import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.*;

class ListCommandTest {

    private SoundPlayer soundPlayer;
    private BotConfig botConfig;
    private CommandEvent event;
    private ListCommand listCommand;

    @BeforeEach
    void setUp() {
        soundPlayer = mock(SoundPlayer.class);
        botConfig = mock(BotConfig.class);
        event = mock(CommandEvent.class);
        listCommand = new ListCommand(soundPlayer, botConfig);
        when(event.getPrefix()).thenReturn("?");
        when(botConfig.getCommandCharacter()).thenReturn("?");
        when(botConfig.getMessageSizeLimit()).thenReturn(100);
    }

    private Map<String, SoundFile> mapWith(String... ids) {
        Map<String, SoundFile> map = new LinkedHashMap<>();
        for (String id : ids) {
            map.put(id, new SoundFile(id, "/tmp/" + id + ".mp3", "default", 0, null, false, id, null));
        }
        return map;
    }

    @Test
    void repliesWithFirstPageWhenNoArgsAndShort() {
        when(soundPlayer.getAvailableSoundFiles()).thenReturn(mapWith("a", "b", "c"));
        when(event.getArguments()).thenReturn(new LinkedList<>());

        listCommand.execute(event);

        verify(event).replyByPrivateMessage(contains("Type any of the following"));
    }

    @Test
    void repliesWithSpecificPageWhenPageNumberProvided() {
        // Create many entries to force multiple pages
        String[] ids = new String[30];
        for (int i = 0; i < ids.length; i++) ids[i] = "snd" + i;
        when(soundPlayer.getAvailableSoundFiles()).thenReturn(mapWith(ids));
        when(event.getArguments()).thenReturn(new LinkedList<>(java.util.List.of("1")));

        listCommand.execute(event);

        verify(event).replyByPrivateMessage(contains("```"));
    }

    @Test
    void invalidPageNumberHandled() {
        when(soundPlayer.getAvailableSoundFiles()).thenReturn(mapWith("a"));
        when(event.getArguments()).thenReturn(new LinkedList<>(java.util.List.of("99")));

        listCommand.execute(event);

        verify(event).replyByPrivateMessage(contains("not valid"));
    }

    @Test
    void nonNumericPageArgumentHandled() {
        when(soundPlayer.getAvailableSoundFiles()).thenReturn(mapWith("a"));
        when(event.getArguments()).thenReturn(new LinkedList<>(java.util.List.of("abc")));

        listCommand.execute(event);

        verify(event).replyByPrivateMessage(contains("must be a number"));
    }
}
