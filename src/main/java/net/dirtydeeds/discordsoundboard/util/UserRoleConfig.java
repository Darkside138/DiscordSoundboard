package net.dirtydeeds.discordsoundboard.util;

import io.jsonwebtoken.Claims;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.*;

@Configuration
@ConfigurationProperties(prefix = "app.users")
@SuppressWarnings("unused")
public class UserRoleConfig {

    // Map of user ID to their roles
    @Setter
    @Getter
    private Map<String, List<String>> roles = new HashMap<>();

    @Value("${admin_user_list}")
    private List<String> adminUserList = new ArrayList<>();

    @Value("moderator_user_list")
    private List<String> moderatorUserList = new ArrayList<>();

    @Value("dj_user_list")
    private List<String> djUserList = new ArrayList<>();

    // Map of roles to their permissions
    @Setter
    @Getter
    private Map<String, List<String>> permissions = new HashMap<>();

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    @org.springframework.context.annotation.Lazy
    private net.dirtydeeds.discordsoundboard.service.DiscordUserService discordUserService;

    @Autowired
    @org.springframework.context.annotation.Lazy
    private net.dirtydeeds.discordsoundboard.service.RolePermissionService rolePermissionService;

    /**
     * Get roles for a specific user
     * Database roles take precedence over configuration file roles
     */
    public List<String> getUserRoles(String userId) {
        List<String> returnRoles = new ArrayList<>();

        // 1. Check database FIRST - takes precedence
        try {
            java.util.Optional<net.dirtydeeds.discordsoundboard.beans.DiscordUser> dbUser = discordUserService.findById(userId);
            if (dbUser.isPresent() && dbUser.get().getAssignedRole() != null) {
                returnRoles.add(dbUser.get().getAssignedRole());
                return returnRoles;  // DB role wins, ignore properties
            }
        } catch (Exception e) {
            // If database lookup fails, fall back to properties
        }

        // 2. Fallback to properties-based roles
        if (adminUserList.contains(userId)) {
            returnRoles.add("admin");
        }
        if (moderatorUserList.contains(userId)) {
            returnRoles.add("moderator");
        }
        if (djUserList.contains(userId)) {
            returnRoles.add("dj");
        }

        // 3. Fallback to YAML roles map
        returnRoles.addAll(roles.getOrDefault(userId, new ArrayList<>()));

        return returnRoles;
    }

    /**
     * Get all permissions for a user based on their roles
     * Database permissions take precedence over YAML configuration
     * Admin role always has all permissions and cannot be customized
     * Unauthenticated users (null/empty userId) get "default" role permissions
     */
    public Set<String> getUserPermissions(String userId) {
        Set<String> userPermissions = new HashSet<>();

        // Handle unauthenticated users - check for custom default permissions
        if (userId == null || userId.isEmpty()) {
            try {
                if (rolePermissionService.hasCustomPermissions("default")) {
                    userPermissions.addAll(rolePermissionService.getPermissionNamesForRole("default"));
                    return userPermissions;
                }
            } catch (Exception e) {
                // If database lookup fails, fall back to YAML
            }

            // Fallback to YAML default-permissions
            List<String> defaultPerms = permissions.get("default-permissions");
            if (defaultPerms != null) {
                userPermissions.addAll(defaultPerms);
            }
            return userPermissions;
        }

        List<String> userRoles = getUserRoles(userId);

        for (String role : userRoles) {
            // Admin always gets all permissions, cannot be customized
            if ("admin".equals(role)) {
                List<String> adminPerms = permissions.get("admin");
                if (adminPerms != null) {
                    userPermissions.addAll(adminPerms);
                }
                continue;
            }

            // 1. Check database for custom permissions first
            try {
                if (rolePermissionService.hasCustomPermissions(role)) {
                    userPermissions.addAll(rolePermissionService.getPermissionNamesForRole(role));
                    continue; // Skip YAML config for this role
                }
            } catch (Exception e) {
                // If database lookup fails, fall back to YAML
            }

            // 2. Fallback to YAML permissions
            List<String> rolePerms = permissions.get(role);
            if (rolePerms != null) {
                userPermissions.addAll(rolePerms);
            }
        }

        return userPermissions;
    }

    /**
     * Check if the user has a specific permission
     */
    public boolean hasPermission(String userId, String permission) {
        Set<String> userPermissions = getUserPermissions(userId);
        return userPermissions.contains(permission);
    }

    /**
     * Check if the user has a specific role
     */
    public boolean hasRole(String userId, String role) {
        List<String> userRoles = getUserRoles(userId);
        return userRoles.contains(role);
    }

    /**
     * Extract user ID from JWT token
     */
    public String getUserIdFromAuth(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return null;
        }

        try {
            String token = authorization.replace("Bearer ", "");

            if (!jwtUtil.validateToken(token)) {
                return null;
            }

            Claims claims = jwtUtil.getClaimsFromToken(token);
            return claims.getSubject();
        } catch (Exception e) {
            return null;
        }
    }
}
