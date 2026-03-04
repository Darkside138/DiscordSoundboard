package net.dirtydeeds.discordsoundboard.service.impl;

import net.dirtydeeds.discordsoundboard.beans.RolePermission;
import net.dirtydeeds.discordsoundboard.repository.RolePermissionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RolePermissionServiceImplTest {

    @Mock
    private RolePermissionRepository rolePermissionRepository;

    @InjectMocks
    private RolePermissionServiceImpl service;

    // ──────────────────────── getPermissionsForRole ────────────────────────

    @Test
    void getPermissionsForRole_emptyResult_returnsEmptyList() {
        when(rolePermissionRepository.findByRole("user")).thenReturn(List.of());

        List<RolePermission> result = service.getPermissionsForRole("user");

        assertTrue(result.isEmpty());
    }

    @Test
    void getPermissionsForRole_withRealPermissions_returnsAll() {
        RolePermission rp = new RolePermission("dj", "play-sounds");
        when(rolePermissionRepository.findByRole("dj")).thenReturn(List.of(rp));

        List<RolePermission> result = service.getPermissionsForRole("dj");

        assertEquals(1, result.size());
        assertEquals("play-sounds", result.getFirst().getPermission());
    }

    @Test
    void getPermissionsForRole_withSentinel_returnsSentinel() {
        RolePermission sentinel = new RolePermission("user", "__EMPTY__");
        when(rolePermissionRepository.findByRole("user")).thenReturn(List.of(sentinel));

        List<RolePermission> result = service.getPermissionsForRole("user");

        assertEquals(1, result.size());
        assertEquals("__EMPTY__", result.getFirst().getPermission());
    }

    // ──────────────────────── getPermissionNamesForRole ────────────────────────

    @Test
    void getPermissionNamesForRole_filtersSentinel() {
        RolePermission sentinel = new RolePermission("user", "__EMPTY__");
        when(rolePermissionRepository.findByRole("user")).thenReturn(List.of(sentinel));

        Set<String> names = service.getPermissionNamesForRole("user");

        assertTrue(names.isEmpty());
    }

    @Test
    void getPermissionNamesForRole_normalPermissionsPassThrough() {
        RolePermission rp1 = new RolePermission("dj", "play-sounds");
        RolePermission rp2 = new RolePermission("dj", "upload");
        when(rolePermissionRepository.findByRole("dj")).thenReturn(List.of(rp1, rp2));

        Set<String> names = service.getPermissionNamesForRole("dj");

        assertEquals(2, names.size());
        assertTrue(names.contains("play-sounds"));
        assertTrue(names.contains("upload"));
    }

    @Test
    void getPermissionNamesForRole_mixedWithSentinel_filtersOutSentinelOnly() {
        RolePermission rp = new RolePermission("dj", "play-sounds");
        RolePermission sentinel = new RolePermission("dj", "__EMPTY__");
        when(rolePermissionRepository.findByRole("dj")).thenReturn(List.of(rp, sentinel));

        Set<String> names = service.getPermissionNamesForRole("dj");

        assertEquals(1, names.size());
        assertTrue(names.contains("play-sounds"));
        assertFalse(names.contains("__EMPTY__"));
    }

    // ──────────────────────── addPermissionToRole ────────────────────────

    @Test
    void addPermissionToRole_newPermission_savesAndReturns() {
        when(rolePermissionRepository.findByRoleAndPermission("dj", "play-sounds")).thenReturn(null);
        RolePermission saved = new RolePermission("dj", "play-sounds");
        when(rolePermissionRepository.save(any(RolePermission.class))).thenReturn(saved);

        RolePermission result = service.addPermissionToRole("dj", "play-sounds", "admin123");

        assertEquals("dj", result.getRole());
        assertEquals("play-sounds", result.getPermission());
        verify(rolePermissionRepository).save(any(RolePermission.class));
    }

    @Test
    void addPermissionToRole_newPermission_setsTimestamp() {
        when(rolePermissionRepository.findByRoleAndPermission("dj", "play-sounds")).thenReturn(null);
        when(rolePermissionRepository.save(any(RolePermission.class))).thenAnswer(inv -> inv.getArgument(0));

        service.addPermissionToRole("dj", "play-sounds", "admin123");

        ArgumentCaptor<RolePermission> captor = ArgumentCaptor.forClass(RolePermission.class);
        verify(rolePermissionRepository).save(captor.capture());
        assertNotNull(captor.getValue().getAssignedAt());
        assertEquals("admin123", captor.getValue().getAssignedBy());
    }

    @Test
    void addPermissionToRole_duplicate_returnsExistingWithoutSaving() {
        RolePermission existing = new RolePermission("dj", "play-sounds");
        when(rolePermissionRepository.findByRoleAndPermission("dj", "play-sounds")).thenReturn(existing);

        RolePermission result = service.addPermissionToRole("dj", "play-sounds", "admin123");

        assertSame(existing, result);
        verify(rolePermissionRepository, never()).save(any());
    }

    @Test
    void addPermissionToRole_withNullAssignedBy_stillSaves() {
        when(rolePermissionRepository.findByRoleAndPermission("dj", "play-sounds")).thenReturn(null);
        when(rolePermissionRepository.save(any(RolePermission.class))).thenAnswer(inv -> inv.getArgument(0));

        RolePermission result = service.addPermissionToRole("dj", "play-sounds", null);

        assertNull(result.getAssignedBy());
        verify(rolePermissionRepository).save(any(RolePermission.class));
    }

    // ──────────────────────── removePermissionFromRole ────────────────────────

    @Test
    void removePermissionFromRole_delegatesToRepository() {
        service.removePermissionFromRole("dj", "play-sounds");

        verify(rolePermissionRepository).deleteByRoleAndPermission("dj", "play-sounds");
    }

    // ──────────────────────── setPermissionsForRole ────────────────────────

    @Test
    void setPermissionsForRole_emptySet_deletesExistingAndSavesSentinel() {
        RolePermission sentinel = new RolePermission("user", "__EMPTY__");
        when(rolePermissionRepository.save(any(RolePermission.class))).thenReturn(sentinel);

        List<RolePermission> result = service.setPermissionsForRole("user", Set.of(), "admin123");

        verify(rolePermissionRepository).deleteByRole("user");
        ArgumentCaptor<RolePermission> captor = ArgumentCaptor.forClass(RolePermission.class);
        verify(rolePermissionRepository).save(captor.capture());
        assertEquals("__EMPTY__", captor.getValue().getPermission());
        assertEquals(1, result.size());
    }

    @Test
    void setPermissionsForRole_emptySet_sentinelHasTimestampAndAssignedBy() {
        when(rolePermissionRepository.save(any(RolePermission.class))).thenAnswer(inv -> inv.getArgument(0));

        service.setPermissionsForRole("user", Set.of(), "admin123");

        ArgumentCaptor<RolePermission> captor = ArgumentCaptor.forClass(RolePermission.class);
        verify(rolePermissionRepository).save(captor.capture());
        assertNotNull(captor.getValue().getAssignedAt());
        assertEquals("admin123", captor.getValue().getAssignedBy());
    }

    @Test
    void setPermissionsForRole_nonEmptySet_deletesExistingAndSavesAll() {
        Set<String> newPerms = Set.of("play-sounds", "upload");
        when(rolePermissionRepository.save(any(RolePermission.class))).thenAnswer(inv -> inv.getArgument(0));

        List<RolePermission> result = service.setPermissionsForRole("dj", newPerms, "admin123");

        verify(rolePermissionRepository).deleteByRole("dj");
        verify(rolePermissionRepository, times(2)).save(any(RolePermission.class));
        assertEquals(2, result.size());
    }

    @Test
    void setPermissionsForRole_nonEmptySet_eachPermissionHasTimestamp() {
        when(rolePermissionRepository.save(any(RolePermission.class))).thenAnswer(inv -> inv.getArgument(0));

        List<RolePermission> result = service.setPermissionsForRole("dj", Set.of("play-sounds"), "admin123");

        assertNotNull(result.getFirst().getAssignedAt());
        assertEquals("admin123", result.getFirst().getAssignedBy());
    }

    // ──────────────────────── hasCustomPermissions ────────────────────────

    @Test
    void hasCustomPermissions_nonEmptyList_returnsTrue() {
        when(rolePermissionRepository.findByRole("dj")).thenReturn(List.of(new RolePermission("dj", "play-sounds")));

        assertTrue(service.hasCustomPermissions("dj"));
    }

    @Test
    void hasCustomPermissions_emptyList_returnsFalse() {
        when(rolePermissionRepository.findByRole("dj")).thenReturn(List.of());

        assertFalse(service.hasCustomPermissions("dj"));
    }

    // ──────────────────────── getAllRolePermissions ────────────────────────

    @Test
    void getAllRolePermissions_delegatesToFindAll() {
        RolePermission rp = new RolePermission("dj", "play-sounds");
        when(rolePermissionRepository.findAll()).thenReturn(List.of(rp));

        List<RolePermission> result = service.getAllRolePermissions();

        assertEquals(1, result.size());
        verify(rolePermissionRepository).findAll();
    }

    // ──────────────────────── resetRoleToDefaults ────────────────────────

    @Test
    void resetRoleToDefaults_callsDeleteByRole() {
        service.resetRoleToDefaults("dj");

        verify(rolePermissionRepository).deleteByRole("dj");
    }

    @Test
    void resetRoleToDefaults_removesIncludingSentinels() {
        // Verify deleteByRole is called (which removes everything including sentinels)
        service.resetRoleToDefaults("user");

        verify(rolePermissionRepository).deleteByRole("user");
        verify(rolePermissionRepository, never()).save(any());
    }
}
