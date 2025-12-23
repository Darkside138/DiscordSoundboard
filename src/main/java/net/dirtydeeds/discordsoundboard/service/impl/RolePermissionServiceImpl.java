package net.dirtydeeds.discordsoundboard.service.impl;

import net.dirtydeeds.discordsoundboard.beans.RolePermission;
import net.dirtydeeds.discordsoundboard.repository.RolePermissionRepository;
import net.dirtydeeds.discordsoundboard.service.RolePermissionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Implementation of RolePermissionService.
 *
 * @author dfurrer
 */
@Service
public class RolePermissionServiceImpl implements RolePermissionService {

    @Autowired
    private RolePermissionRepository rolePermissionRepository;

    @Override
    public List<RolePermission> getPermissionsForRole(String role) {
        return rolePermissionRepository.findByRole(role);
    }

    @Override
    public Set<String> getPermissionNamesForRole(String role) {
        return rolePermissionRepository.findByRole(role)
                .stream()
                .map(RolePermission::getPermission)
                .filter(permission -> !"__EMPTY__".equals(permission)) // Filter out sentinel value
                .collect(Collectors.toSet());
    }

    @Override
    @Transactional
    public RolePermission addPermissionToRole(String role, String permission, String assignedBy) {
        // Check if permission already exists
        RolePermission existing = rolePermissionRepository.findByRoleAndPermission(role, permission);
        if (existing != null) {
            return existing;
        }

        // Create new permission
        RolePermission rolePermission = new RolePermission();
        rolePermission.setRole(role);
        rolePermission.setPermission(permission);
        rolePermission.setAssignedAt(Instant.now());
        rolePermission.setAssignedBy(assignedBy);

        return rolePermissionRepository.save(rolePermission);
    }

    @Override
    @Transactional
    public void removePermissionFromRole(String role, String permission) {
        rolePermissionRepository.deleteByRoleAndPermission(role, permission);
    }

    @Override
    @Transactional
    public List<RolePermission> setPermissionsForRole(String role, Set<String> permissions, String assignedBy) {
        // Delete all existing permissions for this role
        rolePermissionRepository.deleteByRole(role);

        // If empty permissions, save a sentinel record to mark as "explicitly configured as empty"
        if (permissions.isEmpty()) {
            RolePermission sentinel = new RolePermission();
            sentinel.setRole(role);
            sentinel.setPermission("__EMPTY__"); // Sentinel value to indicate explicit empty configuration
            sentinel.setAssignedAt(Instant.now());
            sentinel.setAssignedBy(assignedBy);
            return List.of(rolePermissionRepository.save(sentinel));
        }

        // Add new permissions
        return permissions.stream()
                .map(permission -> {
                    RolePermission rp = new RolePermission();
                    rp.setRole(role);
                    rp.setPermission(permission);
                    rp.setAssignedAt(Instant.now());
                    rp.setAssignedBy(assignedBy);
                    return rolePermissionRepository.save(rp);
                })
                .collect(Collectors.toList());
    }

    @Override
    public boolean hasCustomPermissions(String role) {
        return !rolePermissionRepository.findByRole(role).isEmpty();
    }

    @Override
    public List<RolePermission> getAllRolePermissions() {
        return rolePermissionRepository.findAll();
    }

    @Override
    @Transactional
    public void resetRoleToDefaults(String role) {
        // Delete all permissions including sentinel - this removes all customization
        rolePermissionRepository.deleteByRole(role);
    }
}
