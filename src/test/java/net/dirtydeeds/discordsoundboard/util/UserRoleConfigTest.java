package net.dirtydeeds.discordsoundboard.util;

import io.jsonwebtoken.Claims;
import net.dirtydeeds.discordsoundboard.beans.DiscordUser;
import net.dirtydeeds.discordsoundboard.service.DiscordUserService;
import net.dirtydeeds.discordsoundboard.service.RolePermissionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserRoleConfigTest {

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private DiscordUserService discordUserService;

    @Mock
    private RolePermissionService rolePermissionService;

    @InjectMocks
    private UserRoleConfig userRoleConfig;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(userRoleConfig, "adminUserList", new ArrayList<>(List.of("admin-user-id")));
        ReflectionTestUtils.setField(userRoleConfig, "moderatorUserList", new ArrayList<>(List.of("mod-user-id")));
        ReflectionTestUtils.setField(userRoleConfig, "djUserList", new ArrayList<>(List.of("dj-user-id")));
        userRoleConfig.setRoles(new HashMap<>());
        userRoleConfig.setPermissions(new HashMap<>());
    }

    // ──────────────────────── getUserIdFromAuth ────────────────────────

    @Test
    void getUserIdFromAuth_withNullHeader_returnsNull() {
        assertNull(userRoleConfig.getUserIdFromAuth(null));
    }

    @Test
    void getUserIdFromAuth_withoutBearerPrefix_returnsNull() {
        assertNull(userRoleConfig.getUserIdFromAuth("token123"));
    }

    @Test
    void getUserIdFromAuth_withOnlyBearerPrefix_returnsNull() {
        when(jwtUtil.validateToken("")).thenReturn(false);
        assertNull(userRoleConfig.getUserIdFromAuth("Bearer "));
    }

    @Test
    void getUserIdFromAuth_withValidToken_returnsUserId() {
        String token = "valid.jwt.token";
        Claims claims = mock(Claims.class);
        when(jwtUtil.validateToken(token)).thenReturn(true);
        when(jwtUtil.getClaimsFromToken(token)).thenReturn(claims);
        when(claims.getSubject()).thenReturn("user-123");

        assertEquals("user-123", userRoleConfig.getUserIdFromAuth("Bearer " + token));
    }

    @Test
    void getUserIdFromAuth_withInvalidToken_returnsNull() {
        String token = "invalid.jwt.token";
        when(jwtUtil.validateToken(token)).thenReturn(false);

        assertNull(userRoleConfig.getUserIdFromAuth("Bearer " + token));
    }

    @Test
    void getUserIdFromAuth_whenJwtThrowsException_returnsNull() {
        String token = "malformed";
        when(jwtUtil.validateToken(token)).thenThrow(new RuntimeException("parse error"));

        assertNull(userRoleConfig.getUserIdFromAuth("Bearer " + token));
    }

    // ──────────────────────── getUserRoles ────────────────────────

    @Test
    void getUserRoles_nullUserId_doesNotGetDefaultUserRole() {
        // null userId → DB returns null (NPE caught) → falls through → null check prevents "user" default
        List<String> roles = userRoleConfig.getUserRoles(null);
        assertFalse(roles.contains("user"));
    }

    @Test
    void getUserRoles_emptyUserId_doesNotGetDefaultUserRole() {
        when(discordUserService.findById("")).thenReturn(Optional.empty());
        List<String> roles = userRoleConfig.getUserRoles("");
        assertFalse(roles.contains("user"));
    }

    @Test
    void getUserRoles_dbHasRole_returnsDbRoleAndIgnoresProperties() {
        DiscordUser dbUser = new DiscordUser();
        dbUser.setId("dj-user-id");
        dbUser.setAssignedRole("admin"); // DB says admin, even though dj-user-id is in djUserList
        when(discordUserService.findById("dj-user-id")).thenReturn(Optional.of(dbUser));

        List<String> roles = userRoleConfig.getUserRoles("dj-user-id");

        assertEquals(List.of("admin"), roles);
        verify(discordUserService).findById("dj-user-id");
    }

    @Test
    void getUserRoles_dbThrows_fallsBackToAdminProperties() {
        when(discordUserService.findById("admin-user-id")).thenThrow(new RuntimeException("db error"));

        List<String> roles = userRoleConfig.getUserRoles("admin-user-id");

        assertTrue(roles.contains("admin"));
    }

    @Test
    void getUserRoles_moderatorInProperties_returnsModerator() {
        when(discordUserService.findById("mod-user-id")).thenReturn(Optional.empty());

        List<String> roles = userRoleConfig.getUserRoles("mod-user-id");

        assertTrue(roles.contains("moderator"));
    }

    @Test
    void getUserRoles_djInProperties_returnsDj() {
        when(discordUserService.findById("dj-user-id")).thenReturn(Optional.empty());

        List<String> roles = userRoleConfig.getUserRoles("dj-user-id");

        assertTrue(roles.contains("dj"));
    }

    @Test
    void getUserRoles_authenticatedUserWithNoRoles_getsDefaultUserRole() {
        when(discordUserService.findById("regular-user")).thenReturn(Optional.empty());

        List<String> roles = userRoleConfig.getUserRoles("regular-user");

        assertEquals(List.of("user"), roles);
    }

    @Test
    void getUserRoles_yamlMapRole_returnsYamlRole() {
        Map<String, List<String>> rolesMap = new HashMap<>();
        rolesMap.put("yaml-user", List.of("moderator"));
        userRoleConfig.setRoles(rolesMap);
        when(discordUserService.findById("yaml-user")).thenReturn(Optional.empty());

        List<String> roles = userRoleConfig.getUserRoles("yaml-user");

        assertTrue(roles.contains("moderator"));
    }

    @Test
    void getUserRoles_dbUserWithNullRole_fallsBackToProperties() {
        DiscordUser dbUser = new DiscordUser();
        dbUser.setId("admin-user-id");
        dbUser.setAssignedRole(null); // DB user exists but no role assigned
        when(discordUserService.findById("admin-user-id")).thenReturn(Optional.of(dbUser));

        List<String> roles = userRoleConfig.getUserRoles("admin-user-id");

        // Falls through to properties → finds admin-user-id in adminUserList
        assertTrue(roles.contains("admin"));
    }

    // ──────────────────────── getUserPermissions ────────────────────────

    @Test
    void getUserPermissions_nullUserId_withCustomDbDefaultPerms_returnsDbPerms() {
        when(rolePermissionService.hasCustomPermissions("default")).thenReturn(true);
        when(rolePermissionService.getPermissionNamesForRole("default"))
                .thenReturn(Set.of("play-sounds", "download-sounds"));

        Set<String> perms = userRoleConfig.getUserPermissions(null);

        assertTrue(perms.contains("play-sounds"));
        assertTrue(perms.contains("download-sounds"));
    }

    @Test
    void getUserPermissions_nullUserId_dbThrows_fallsBackToYaml() {
        when(rolePermissionService.hasCustomPermissions("default")).thenThrow(new RuntimeException("db error"));
        Map<String, List<String>> permissions = new HashMap<>();
        permissions.put("default-permissions", List.of("play-sounds"));
        userRoleConfig.setPermissions(permissions);

        Set<String> perms = userRoleConfig.getUserPermissions(null);

        assertTrue(perms.contains("play-sounds"));
    }

    @Test
    void getUserPermissions_nullUserId_noDbAndNoYaml_returnsEmpty() {
        when(rolePermissionService.hasCustomPermissions("default")).thenReturn(false);

        Set<String> perms = userRoleConfig.getUserPermissions(null);

        assertTrue(perms.isEmpty());
    }

    @Test
    void getUserPermissions_adminUser_getsAdminPermsFromYamlNotDb() {
        DiscordUser dbUser = new DiscordUser();
        dbUser.setId("admin-user-id");
        dbUser.setAssignedRole("admin");
        when(discordUserService.findById("admin-user-id")).thenReturn(Optional.of(dbUser));

        Map<String, List<String>> permissions = new HashMap<>();
        permissions.put("admin", List.of("upload", "delete-sounds", "manage-users"));
        userRoleConfig.setPermissions(permissions);

        Set<String> perms = userRoleConfig.getUserPermissions("admin-user-id");

        assertTrue(perms.contains("upload"));
        assertTrue(perms.contains("delete-sounds"));
        assertTrue(perms.contains("manage-users"));
        // Admin role does NOT call DB permission service
        verify(rolePermissionService, never()).hasCustomPermissions("admin");
    }

    @Test
    void getUserPermissions_roleWithCustomDbPerms_returnsDbPerms() {
        when(discordUserService.findById("dj-user-id")).thenReturn(Optional.empty());
        when(rolePermissionService.hasCustomPermissions("dj")).thenReturn(true);
        when(rolePermissionService.getPermissionNamesForRole("dj"))
                .thenReturn(Set.of("play-sounds", "upload"));

        Set<String> perms = userRoleConfig.getUserPermissions("dj-user-id");

        assertTrue(perms.contains("play-sounds"));
        assertTrue(perms.contains("upload"));
    }

    @Test
    void getUserPermissions_roleWithNoCustomDbPerms_fallsBackToYaml() {
        when(discordUserService.findById("dj-user-id")).thenReturn(Optional.empty());
        when(rolePermissionService.hasCustomPermissions("dj")).thenReturn(false);

        Map<String, List<String>> permissions = new HashMap<>();
        permissions.put("dj", List.of("play-sounds", "upload"));
        userRoleConfig.setPermissions(permissions);

        Set<String> perms = userRoleConfig.getUserPermissions("dj-user-id");

        assertTrue(perms.contains("play-sounds"));
        assertTrue(perms.contains("upload"));
    }

    @Test
    void getUserPermissions_roleDbThrows_fallsBackToYaml() {
        when(discordUserService.findById("dj-user-id")).thenReturn(Optional.empty());
        when(rolePermissionService.hasCustomPermissions("dj")).thenThrow(new RuntimeException("db error"));

        Map<String, List<String>> permissions = new HashMap<>();
        permissions.put("dj", List.of("play-sounds"));
        userRoleConfig.setPermissions(permissions);

        Set<String> perms = userRoleConfig.getUserPermissions("dj-user-id");

        assertTrue(perms.contains("play-sounds"));
    }

    // ──────────────────────── hasPermission ────────────────────────

    @Test
    void hasPermission_permissionInSet_returnsTrue() {
        when(discordUserService.findById("dj-user-id")).thenReturn(Optional.empty());
        when(rolePermissionService.hasCustomPermissions("dj")).thenReturn(false);

        Map<String, List<String>> permissions = new HashMap<>();
        permissions.put("dj", List.of("play-sounds", "upload"));
        userRoleConfig.setPermissions(permissions);

        assertTrue(userRoleConfig.hasPermission("dj-user-id", "play-sounds"));
    }

    @Test
    void hasPermission_permissionNotInSet_returnsFalse() {
        when(discordUserService.findById("regular-user")).thenReturn(Optional.empty());
        when(rolePermissionService.hasCustomPermissions("user")).thenReturn(false);

        Map<String, List<String>> permissions = new HashMap<>();
        permissions.put("user", List.of("play-sounds"));
        userRoleConfig.setPermissions(permissions);

        assertFalse(userRoleConfig.hasPermission("regular-user", "delete-sounds"));
    }

    // ──────────────────────── hasRole ────────────────────────

    @Test
    void hasRole_roleInList_returnsTrue() {
        DiscordUser dbUser = new DiscordUser();
        dbUser.setId("admin-user-id");
        dbUser.setAssignedRole("admin");
        when(discordUserService.findById("admin-user-id")).thenReturn(Optional.of(dbUser));

        assertTrue(userRoleConfig.hasRole("admin-user-id", "admin"));
    }

    @Test
    void hasRole_roleNotInList_returnsFalse() {
        when(discordUserService.findById("regular-user")).thenReturn(Optional.empty());

        assertFalse(userRoleConfig.hasRole("regular-user", "admin"));
    }
}
