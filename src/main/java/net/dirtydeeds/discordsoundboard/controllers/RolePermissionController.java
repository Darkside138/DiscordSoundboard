package net.dirtydeeds.discordsoundboard.controllers;

import io.swagger.v3.oas.annotations.Hidden;
import net.dirtydeeds.discordsoundboard.beans.RolePermission;
import net.dirtydeeds.discordsoundboard.service.RolePermissionService;
import net.dirtydeeds.discordsoundboard.util.UserRoleConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * REST controller for managing role-permission mappings.
 *
 * @author dfurrer
 */
@Hidden
@RestController
@RequestMapping("/api/rolePermissions")
@SuppressWarnings("unused")
public class RolePermissionController {

    @Autowired
    private RolePermissionService rolePermissionService;

    @Autowired
    private UserRoleConfig userRoleConfig;

    /**
     * Get list of roles that have been explicitly configured in database
     */
    @GetMapping("/configured")
    public ResponseEntity<Set<String>> getConfiguredRoles(
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        String authId = userRoleConfig.getUserIdFromAuth(authorization);
        if (!userRoleConfig.hasPermission(authId, "manage-users")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        List<RolePermission> allDbPermissions = rolePermissionService.getAllRolePermissions();
        Set<String> configuredRoles = new HashSet<>();
        for (RolePermission rp : allDbPermissions) {
            configuredRoles.add(rp.getRole());
        }

        return ResponseEntity.ok(configuredRoles);
    }

    /**
     * Get all role-permission mappings
     * Returns database records plus YAML defaults for roles without database customization
     */
    @GetMapping()
    public ResponseEntity<List<RolePermission>> getAllRolePermissions(
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        String authId = userRoleConfig.getUserIdFromAuth(authorization);
        if (!userRoleConfig.hasPermission(authId, "manage-users")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        List<RolePermission> rolePermissions = new ArrayList<>();

        // Get all database records
        List<RolePermission> allDbPermissions = rolePermissionService.getAllRolePermissions();

        // Get roles that have custom permissions in database (including those with sentinel)
        Set<String> rolesWithCustomPermissions = new HashSet<>();
        for (RolePermission rp : allDbPermissions) {
            rolesWithCustomPermissions.add(rp.getRole());
        }

        // Filter out sentinel values from response
        List<RolePermission> dbPermissions = allDbPermissions.stream()
                .filter(rp -> !"__EMPTY__".equals(rp.getPermission()))
                .collect(java.util.stream.Collectors.toList());
        rolePermissions.addAll(dbPermissions);

        // Add YAML defaults for roles without database customization
        List<String> allRoles = List.of("default", "admin", "dj", "moderator", "user");
        for (String role : allRoles) {
            if (!rolesWithCustomPermissions.contains(role)) {
                // This role has no database customization, add YAML defaults
                Set<String> yamlPerms = getYamlPermissionsForRole(role);
                for (String perm : yamlPerms) {
                    RolePermission yamlPermission = new RolePermission(role, perm);
                    rolePermissions.add(yamlPermission);
                }
            }
        }

        return ResponseEntity.ok(rolePermissions);
    }

    /**
     * Get all permissions for a specific role
     * Returns database permissions if available, otherwise returns YAML configuration
     */
    @GetMapping("/{role}")
    public ResponseEntity<Set<String>> getPermissionsForRole(
            @PathVariable String role,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        String authId = userRoleConfig.getUserIdFromAuth(authorization);
        if (!userRoleConfig.hasPermission(authId, "manage-users")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        Set<String> permissions;

        try {
            // Check if role has custom permissions in database
            if (rolePermissionService.hasCustomPermissions(role)) {
                // Return database permissions
                permissions = rolePermissionService.getPermissionNamesForRole(role);
            } else {
                // Return YAML configured permissions
                permissions = getYamlPermissionsForRole(role);
            }
        } catch (Exception e) {
            // If there's any error, fall back to YAML
            permissions = getYamlPermissionsForRole(role);
        }

        return ResponseEntity.ok(permissions);
    }

    /**
     * Helper method to get YAML configured permissions for a role
     */
    private Set<String> getYamlPermissionsForRole(String role) {
        Set<String> yamlPermissions = new HashSet<>();

        if (role == null) {
            return yamlPermissions;
        }

        // Get permissions from UserRoleConfig's YAML map
        Map<String, List<String>> permissionsMap = userRoleConfig.getPermissions();

        if (permissionsMap == null || permissionsMap.isEmpty()) {
            return yamlPermissions;
        }

        // For "default" role, check "default-permissions" key in YAML
        String yamlKey = "default".equals(role) ? "default-permissions" : role;

        List<String> rolePerms = permissionsMap.get(yamlKey);
        if (rolePerms != null && !rolePerms.isEmpty()) {
            yamlPermissions.addAll(rolePerms);
        }

        return yamlPermissions;
    }

    /**
     * Set all permissions for a role (replaces existing permissions)
     * Admin role permissions cannot be modified
     */
    @PutMapping("/{role}")
    public ResponseEntity<List<RolePermission>> setPermissionsForRole(
            @PathVariable String role,
            @RequestBody Set<String> permissions,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        String authId = userRoleConfig.getUserIdFromAuth(authorization);
        if (!userRoleConfig.hasPermission(authId, "manage-users")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        // Prevent modifying admin role permissions
        if ("admin".equals(role)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }

        try {
            List<RolePermission> rolePermissions = rolePermissionService.setPermissionsForRole(role, permissions, authId);
            return ResponseEntity.ok(rolePermissions);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Add a single permission to a role
     * Admin role permissions cannot be modified
     */
    @PostMapping("/{role}/permissions")
    public ResponseEntity<RolePermission> addPermissionToRole(
            @PathVariable String role,
            @RequestBody Map<String, String> body,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        String authId = userRoleConfig.getUserIdFromAuth(authorization);
        if (!userRoleConfig.hasPermission(authId, "manage-users")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        // Prevent modifying admin role permissions
        if ("admin".equals(role)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }

        String permission = body.get("permission");
        if (permission == null || permission.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        try {
            RolePermission rolePermission = rolePermissionService.addPermissionToRole(role, permission, authId);
            return ResponseEntity.ok(rolePermission);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Remove a specific permission from a role
     * Admin role permissions cannot be modified
     */
    @DeleteMapping("/{role}/permissions/{permission}")
    public ResponseEntity<Void> removePermissionFromRole(
            @PathVariable String role,
            @PathVariable String permission,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        String authId = userRoleConfig.getUserIdFromAuth(authorization);
        if (!userRoleConfig.hasPermission(authId, "manage-users")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        // Prevent modifying admin role permissions
        if ("admin".equals(role)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }

        try {
            rolePermissionService.removePermissionFromRole(role, permission);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Delete all permissions for a role
     * Admin role permissions cannot be modified
     */
    @DeleteMapping("/{role}")
    public ResponseEntity<Void> deleteAllPermissionsForRole(
            @PathVariable String role,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        String authId = userRoleConfig.getUserIdFromAuth(authorization);
        if (!userRoleConfig.hasPermission(authId, "manage-users")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        // Prevent modifying admin role permissions
        if ("admin".equals(role)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }

        try {
            rolePermissionService.setPermissionsForRole(role, Set.of(), authId);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Reset a role to default YAML permissions (remove all database customization)
     * Admin role permissions cannot be modified
     */
    @PostMapping("/{role}/reset")
    public ResponseEntity<Void> resetRoleToDefaults(
            @PathVariable String role,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        String authId = userRoleConfig.getUserIdFromAuth(authorization);
        if (!userRoleConfig.hasPermission(authId, "manage-users")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        // Prevent modifying admin role permissions
        if ("admin".equals(role)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }

        try {
            rolePermissionService.resetRoleToDefaults(role);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}
