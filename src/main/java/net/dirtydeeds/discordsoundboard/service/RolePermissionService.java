package net.dirtydeeds.discordsoundboard.service;

import net.dirtydeeds.discordsoundboard.beans.RolePermission;

import java.util.List;
import java.util.Set;

/**
 * Service for managing role-permission mappings.
 *
 * @author dfurrer
 */
public interface RolePermissionService {

    /**
     * Get all permissions for a role
     */
    List<RolePermission> getPermissionsForRole(String role);

    /**
     * Get permission names for a role (just the permission strings)
     */
    Set<String> getPermissionNamesForRole(String role);

    /**
     * Add a permission to a role
     */
    RolePermission addPermissionToRole(String role, String permission, String assignedBy);

    /**
     * Remove a permission from a role
     */
    void removePermissionFromRole(String role, String permission);

    /**
     * Set all permissions for a role (replaces existing)
     */
    List<RolePermission> setPermissionsForRole(String role, Set<String> permissions, String assignedBy);

    /**
     * Check if a role has any custom permissions defined in the database
     */
    boolean hasCustomPermissions(String role);

    /**
     * Get all role-permission mappings
     */
    List<RolePermission> getAllRolePermissions();

    /**
     * Remove all custom permissions for a role (reset to YAML defaults)
     */
    void resetRoleToDefaults(String role);
}
