package net.dirtydeeds.discordsoundboard.controllers;

import net.dirtydeeds.discordsoundboard.beans.RolePermission;
import net.dirtydeeds.discordsoundboard.service.RolePermissionService;
import net.dirtydeeds.discordsoundboard.util.UserRoleConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RolePermissionControllerTest {

    @Mock
    private RolePermissionService rolePermissionService;

    @Mock
    private UserRoleConfig userRoleConfig;

    @InjectMocks
    private RolePermissionController controller;

    private static final String AUTH = "Bearer valid-token";
    private static final String ADMIN_ID = "admin123";

    @BeforeEach
    void setUp() {
        when(userRoleConfig.getUserIdFromAuth(AUTH)).thenReturn(ADMIN_ID);
        when(userRoleConfig.hasPermission(ADMIN_ID, "manage-users")).thenReturn(true);
    }

    // ──────────────────────── GET /configured ────────────────────────

    @Test
    void getConfiguredRoles_withoutPermission_returns403() {
        when(userRoleConfig.hasPermission(ADMIN_ID, "manage-users")).thenReturn(false);

        ResponseEntity<Set<String>> response = controller.getConfiguredRoles(AUTH);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        verify(rolePermissionService, never()).getAllRolePermissions();
    }

    @Test
    void getConfiguredRoles_emptyDb_returnsEmptySet() {
        when(rolePermissionService.getAllRolePermissions()).thenReturn(List.of());

        ResponseEntity<Set<String>> response = controller.getConfiguredRoles(AUTH);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().isEmpty());
    }

    @Test
    void getConfiguredRoles_multipleRoles_returnsDistinctRoles() {
        RolePermission rp1 = new RolePermission("dj", "play-sounds");
        RolePermission rp2 = new RolePermission("dj", "upload");
        RolePermission rp3 = new RolePermission("user", "__EMPTY__");
        when(rolePermissionService.getAllRolePermissions()).thenReturn(List.of(rp1, rp2, rp3));

        ResponseEntity<Set<String>> response = controller.getConfiguredRoles(AUTH);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        Set<String> roles = response.getBody();
        assertEquals(2, roles.size());
        assertTrue(roles.contains("dj"));
        assertTrue(roles.contains("user"));
    }

    // ──────────────────────── GET / ────────────────────────

    @Test
    void getAllRolePermissions_withoutPermission_returns403() {
        when(userRoleConfig.hasPermission(ADMIN_ID, "manage-users")).thenReturn(false);

        ResponseEntity<List<RolePermission>> response = controller.getAllRolePermissions(AUTH);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }

    @Test
    void getAllRolePermissions_dbHasCustomPerms_filtersSentinelFromResponse() {
        RolePermission real = new RolePermission("dj", "play-sounds");
        RolePermission sentinel = new RolePermission("user", "__EMPTY__");
        when(rolePermissionService.getAllRolePermissions()).thenReturn(List.of(real, sentinel));
        when(userRoleConfig.getPermissions()).thenReturn(Map.of()); // No YAML defaults

        ResponseEntity<List<RolePermission>> response = controller.getAllRolePermissions(AUTH);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        List<RolePermission> body = response.getBody();
        // Sentinel is filtered out, real permission is included
        // Plus YAML defaults for roles without DB customization (but YAML map is empty)
        assertTrue(body.stream().noneMatch(rp -> "__EMPTY__".equals(rp.getPermission())));
        assertTrue(body.stream().anyMatch(rp -> "play-sounds".equals(rp.getPermission())));
    }

    @Test
    void getAllRolePermissions_yamlFillsInRolesWithoutDbCustomization() {
        // All DB permissions are for "dj" only, so other roles get YAML defaults
        RolePermission rp = new RolePermission("dj", "play-sounds");
        when(rolePermissionService.getAllRolePermissions()).thenReturn(List.of(rp));
        Map<String, List<String>> yamlPerms = Map.of(
                "user", List.of("play-sounds", "download-sounds")
        );
        when(userRoleConfig.getPermissions()).thenReturn(yamlPerms);

        ResponseEntity<List<RolePermission>> response = controller.getAllRolePermissions(AUTH);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        // "user" role (no DB customization) should get YAML defaults
        long userPermsCount = response.getBody().stream()
                .filter(rp2 -> "user".equals(rp2.getRole()))
                .count();
        assertEquals(2, userPermsCount);
    }

    @Test
    void getAllRolePermissions_defaultRoleMappedFromDefaultPermissionsKey() {
        when(rolePermissionService.getAllRolePermissions()).thenReturn(List.of());
        Map<String, List<String>> yamlPerms = Map.of(
                "default-permissions", List.of("play-sounds")
        );
        when(userRoleConfig.getPermissions()).thenReturn(yamlPerms);

        ResponseEntity<List<RolePermission>> response = controller.getAllRolePermissions(AUTH);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        long defaultPermsCount = response.getBody().stream()
                .filter(rp -> "default".equals(rp.getRole()))
                .count();
        assertEquals(1, defaultPermsCount);
    }

    // ──────────────────────── GET /{role} ────────────────────────

    @Test
    void getPermissionsForRole_withoutPermission_returns403() {
        when(userRoleConfig.hasPermission(ADMIN_ID, "manage-users")).thenReturn(false);

        ResponseEntity<Set<String>> response = controller.getPermissionsForRole("dj", AUTH);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }

    @Test
    void getPermissionsForRole_dbHasCustomPerms_returnsDbResult() {
        when(rolePermissionService.hasCustomPermissions("dj")).thenReturn(true);
        when(rolePermissionService.getPermissionNamesForRole("dj")).thenReturn(Set.of("play-sounds", "upload"));

        ResponseEntity<Set<String>> response = controller.getPermissionsForRole("dj", AUTH);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().contains("play-sounds"));
        assertTrue(response.getBody().contains("upload"));
    }

    @Test
    void getPermissionsForRole_noDbPerms_returnsYamlDefaults() {
        when(rolePermissionService.hasCustomPermissions("user")).thenReturn(false);
        when(userRoleConfig.getPermissions()).thenReturn(Map.of("user", List.of("play-sounds", "download-sounds")));

        ResponseEntity<Set<String>> response = controller.getPermissionsForRole("user", AUTH);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().contains("play-sounds"));
    }

    @Test
    void getPermissionsForRole_serviceThrows_fallsBackToYaml() {
        when(rolePermissionService.hasCustomPermissions("dj")).thenThrow(new RuntimeException("db error"));
        when(userRoleConfig.getPermissions()).thenReturn(Map.of("dj", List.of("play-sounds")));

        ResponseEntity<Set<String>> response = controller.getPermissionsForRole("dj", AUTH);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().contains("play-sounds"));
    }

    // ──────────────────────── PUT /{role} ────────────────────────

    @Test
    void setPermissionsForRole_withoutPermission_returns403() {
        when(userRoleConfig.hasPermission(ADMIN_ID, "manage-users")).thenReturn(false);

        ResponseEntity<List<RolePermission>> response = controller.setPermissionsForRole(
                "dj", Set.of("play-sounds"), AUTH);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }

    @Test
    void setPermissionsForRole_adminRole_returns400() {
        ResponseEntity<List<RolePermission>> response = controller.setPermissionsForRole(
                "admin", Set.of("play-sounds"), AUTH);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verify(rolePermissionService, never()).setPermissionsForRole(any(), any(), any());
    }

    @Test
    void setPermissionsForRole_emptyPermissionSet_acceptsAndSavesSentinel() {
        when(rolePermissionService.setPermissionsForRole(eq("dj"), eq(Set.of()), eq(ADMIN_ID)))
                .thenReturn(List.of(new RolePermission("dj", "__EMPTY__")));

        ResponseEntity<List<RolePermission>> response = controller.setPermissionsForRole(
                "dj", Set.of(), AUTH);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void setPermissionsForRole_nonEmptySet_returnsOk() {
        Set<String> perms = Set.of("play-sounds", "upload");
        when(rolePermissionService.setPermissionsForRole(eq("dj"), eq(perms), eq(ADMIN_ID)))
                .thenReturn(List.of(new RolePermission("dj", "play-sounds"), new RolePermission("dj", "upload")));

        ResponseEntity<List<RolePermission>> response = controller.setPermissionsForRole("dj", perms, AUTH);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(2, response.getBody().size());
    }

    @Test
    void setPermissionsForRole_serviceThrows_returns500() {
        when(rolePermissionService.setPermissionsForRole(any(), any(), any()))
                .thenThrow(new RuntimeException("db error"));

        ResponseEntity<List<RolePermission>> response = controller.setPermissionsForRole(
                "dj", Set.of("play-sounds"), AUTH);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    }

    // ──────────────────────── POST /{role}/permissions ────────────────────────

    @Test
    void addPermissionToRole_withoutPermission_returns403() {
        when(userRoleConfig.hasPermission(ADMIN_ID, "manage-users")).thenReturn(false);

        ResponseEntity<RolePermission> response = controller.addPermissionToRole(
                "dj", Map.of("permission", "play-sounds"), AUTH);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }

    @Test
    void addPermissionToRole_adminRole_returns400() {
        ResponseEntity<RolePermission> response = controller.addPermissionToRole(
                "admin", Map.of("permission", "play-sounds"), AUTH);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void addPermissionToRole_missingPermissionKey_returns400() {
        ResponseEntity<RolePermission> response = controller.addPermissionToRole(
                "dj", Map.of(), AUTH);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void addPermissionToRole_emptyPermissionValue_returns400() {
        ResponseEntity<RolePermission> response = controller.addPermissionToRole(
                "dj", Map.of("permission", ""), AUTH);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void addPermissionToRole_success_returns200() {
        RolePermission saved = new RolePermission("dj", "play-sounds");
        when(rolePermissionService.addPermissionToRole("dj", "play-sounds", ADMIN_ID)).thenReturn(saved);

        ResponseEntity<RolePermission> response = controller.addPermissionToRole(
                "dj", Map.of("permission", "play-sounds"), AUTH);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("play-sounds", response.getBody().getPermission());
    }

    @Test
    void addPermissionToRole_serviceThrows_returns500() {
        when(rolePermissionService.addPermissionToRole(any(), any(), any()))
                .thenThrow(new RuntimeException("db error"));

        ResponseEntity<RolePermission> response = controller.addPermissionToRole(
                "dj", Map.of("permission", "play-sounds"), AUTH);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    }

    @Test
    void addPermissionToRole_duplicatePermission_returns200WithExisting() {
        RolePermission existing = new RolePermission("dj", "play-sounds");
        // Service returns existing (no save) for duplicates
        when(rolePermissionService.addPermissionToRole("dj", "play-sounds", ADMIN_ID)).thenReturn(existing);

        ResponseEntity<RolePermission> response = controller.addPermissionToRole(
                "dj", Map.of("permission", "play-sounds"), AUTH);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    // ──────────────────────── DELETE /{role}/permissions/{perm} ────────────────────────

    @Test
    void removePermissionFromRole_withoutPermission_returns403() {
        when(userRoleConfig.hasPermission(ADMIN_ID, "manage-users")).thenReturn(false);

        ResponseEntity<Void> response = controller.removePermissionFromRole("dj", "play-sounds", AUTH);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }

    @Test
    void removePermissionFromRole_adminRole_returns400() {
        ResponseEntity<Void> response = controller.removePermissionFromRole("admin", "play-sounds", AUTH);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void removePermissionFromRole_success_returns200() {
        doNothing().when(rolePermissionService).removePermissionFromRole("dj", "play-sounds");

        ResponseEntity<Void> response = controller.removePermissionFromRole("dj", "play-sounds", AUTH);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void removePermissionFromRole_serviceThrows_returns500() {
        doThrow(new RuntimeException("db error")).when(rolePermissionService)
                .removePermissionFromRole("dj", "play-sounds");

        ResponseEntity<Void> response = controller.removePermissionFromRole("dj", "play-sounds", AUTH);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    }

    // ──────────────────────── DELETE /{role} ────────────────────────

    @Test
    void deleteAllPermissionsForRole_withoutPermission_returns403() {
        when(userRoleConfig.hasPermission(ADMIN_ID, "manage-users")).thenReturn(false);

        ResponseEntity<Void> response = controller.deleteAllPermissionsForRole("dj", AUTH);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }

    @Test
    void deleteAllPermissionsForRole_adminRole_returns400() {
        ResponseEntity<Void> response = controller.deleteAllPermissionsForRole("admin", AUTH);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void deleteAllPermissionsForRole_success_createsSentinelAndReturns200() {
        when(rolePermissionService.setPermissionsForRole(eq("dj"), eq(Set.of()), eq(ADMIN_ID)))
                .thenReturn(List.of(new RolePermission("dj", "__EMPTY__")));

        ResponseEntity<Void> response = controller.deleteAllPermissionsForRole("dj", AUTH);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(rolePermissionService).setPermissionsForRole("dj", Set.of(), ADMIN_ID);
    }

    @Test
    void deleteAllPermissionsForRole_serviceThrows_returns500() {
        when(rolePermissionService.setPermissionsForRole(any(), any(), any()))
                .thenThrow(new RuntimeException("db error"));

        ResponseEntity<Void> response = controller.deleteAllPermissionsForRole("dj", AUTH);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    }

    // ──────────────────────── POST /{role}/reset ────────────────────────

    @Test
    void resetRoleToDefaults_withoutPermission_returns403() {
        when(userRoleConfig.hasPermission(ADMIN_ID, "manage-users")).thenReturn(false);

        ResponseEntity<Void> response = controller.resetRoleToDefaults("dj", AUTH);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }

    @Test
    void resetRoleToDefaults_adminRole_returns400() {
        ResponseEntity<Void> response = controller.resetRoleToDefaults("admin", AUTH);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void resetRoleToDefaults_success_returns200() {
        doNothing().when(rolePermissionService).resetRoleToDefaults("dj");

        ResponseEntity<Void> response = controller.resetRoleToDefaults("dj", AUTH);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(rolePermissionService).resetRoleToDefaults("dj");
    }

    @Test
    void resetRoleToDefaults_serviceThrows_returns500() {
        doThrow(new RuntimeException("db error")).when(rolePermissionService).resetRoleToDefaults("dj");

        ResponseEntity<Void> response = controller.resetRoleToDefaults("dj", AUTH);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    }
}
