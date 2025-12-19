package net.dirtydeeds.discordsoundboard;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BotConfigTest {

    @Test
    void allowedUsers_empty_or_null_returns_empty_list() {
        BotConfig cfg = new BotConfig();
        cfg.allowedUsersString = null;
        assertTrue(cfg.getAllowedUsersList().isEmpty());

        cfg.allowedUsersString = "";
        assertTrue(cfg.getAllowedUsersList().isEmpty());
    }

    @Test
    void allowedUsers_parses_comma_separated_values() {
        BotConfig cfg = new BotConfig();
        cfg.allowedUsersString = "alice,bob,carol";
        List<String> list = cfg.getAllowedUsersList();
        assertEquals(3, list.size());
        assertTrue(list.contains("alice"));
        assertTrue(list.contains("bob"));
        assertTrue(list.contains("carol"));
    }

    @Test
    void bannedUsers_empty_or_null_returns_empty_list() {
        BotConfig cfg = new BotConfig();
        cfg.bannedUsersString = null;
        assertTrue(cfg.getBannedUsersList().isEmpty());

        cfg.bannedUsersString = "";
        assertTrue(cfg.getBannedUsersList().isEmpty());
    }

    @Test
    void bannedUsers_parses_comma_separated_values() {
        BotConfig cfg = new BotConfig();
        cfg.bannedUsersString = "mallory,oscar,peggy";
        List<String> list = cfg.getBannedUsersList();
        assertEquals(3, list.size());
        assertTrue(list.contains("mallory"));
        assertTrue(list.contains("oscar"));
        assertTrue(list.contains("peggy"));
    }

    @Test
    void default_sound_dir_is_user_dir_sounds_when_unset() {
        BotConfig cfg = new BotConfig();
        cfg.soundFileDir = ""; // unset
        String expected = System.getProperty("user.dir") + "/sounds";
        assertEquals(expected, cfg.getSoundFileDir());
    }
}
