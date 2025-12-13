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

    // Map of role to their permissions
    @Setter
    @Getter
    private Map<String, List<String>> permissions = new HashMap<>();

    @Autowired
    private JwtUtil jwtUtil;

    /**
     * Get roles for a specific user
     */
    public List<String> getUserRoles(String userId) {
        List<String> returnRoles = new ArrayList<>();
        if (adminUserList.contains(userId)) {
            returnRoles.add("admin");
        }
        if (moderatorUserList.contains(userId)) {
            returnRoles.add("moderator");
        }
        if (djUserList.contains(userId)) {
            returnRoles.add("dj");
        }
        returnRoles.addAll(roles.getOrDefault(userId, new ArrayList<>()));
        return returnRoles;
    }

    /**
     * Get all permissions for a user based on their roles
     */
    public Set<String> getUserPermissions(String userId) {
        Set<String> userPermissions = new HashSet<>();
        List<String> userRoles = getUserRoles(userId);

        for (String role : userRoles) {
            List<String> rolePerms = permissions.get(role);
            if (rolePerms != null) {
                userPermissions.addAll(rolePerms);
            }
        }

        return userPermissions;
    }

    /**
     * Check if user has a specific permission
     */
    public boolean hasPermission(String userId, String permission) {
        Set<String> userPermissions = getUserPermissions(userId);
        return userPermissions.contains(permission);
    }

    /**
     * Check if user has a specific role
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
