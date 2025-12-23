package net.dirtydeeds.discordsoundboard.controllers;

import io.jsonwebtoken.Claims;
import net.dirtydeeds.discordsoundboard.util.JwtUtil;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("api/auth")
public class AuthController {

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private net.dirtydeeds.discordsoundboard.util.UserRoleConfig userRoleConfig;

    @GetMapping("/user")
    public ResponseEntity<Map<String, Object>> getUser(
            @RequestHeader("Authorization") String authorization,
            CsrfToken csrfToken) { // Accessing CsrfToken triggers cookie generation
        try {
            String token = authorization.replace("Bearer ", "");

            if (!jwtUtil.validateToken(token)) {
                return ResponseEntity.status(401).build();
            }

            Claims claims = jwtUtil.getClaimsFromToken(token);

            // Get permissions from the token
            @SuppressWarnings("unchecked")
            List<String> permissionsList = claims.get("permissions", List.class);
            if (permissionsList == null) {
                permissionsList = new ArrayList<>();
            }

            // Build response with user info and permissions
            Map<String, Object> userResponse = new HashMap<>();
            userResponse.put("id", claims.getSubject());
            userResponse.put("username", claims.get("username"));
            userResponse.put("discriminator", claims.get("discriminator"));
            userResponse.put("avatar", claims.get("avatar"));
            userResponse.put("globalName", claims.get("globalName"));
            userResponse.put("roles", claims.get("roles", List.class));

            // Convert the permissions list to a boolean map
            Map<String, Boolean> permissions = getStringBooleanMap(permissionsList);

            userResponse.put("permissions", permissions);

            return ResponseEntity.ok(userResponse);
        } catch (Exception e) {
            return ResponseEntity.status(401).build();
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestHeader("Authorization") String authorization) {
        return ResponseEntity.ok().build();
    }

    @PostMapping("/refresh")
    public ResponseEntity<Map<String, Object>> refreshToken(
            @RequestHeader("Authorization") String authorization) {
        try {
            String token = authorization.replace("Bearer ", "");

            if (!jwtUtil.validateToken(token)) {
                return ResponseEntity.status(401).build();
            }

            Claims claims = jwtUtil.getClaimsFromToken(token);
            String userId = claims.getSubject();

            // Re-fetch roles from UserRoleConfig (which now checks DB)
            java.util.List<String> roles = userRoleConfig.getUserRoles(userId);
            java.util.Set<String> permissions = userRoleConfig.getUserPermissions(userId);

            // Preserve existing claims, update roles/permissions
            Map<String, Object> newClaims = new HashMap<>();
            newClaims.put("username", claims.get("username"));
            newClaims.put("discriminator", claims.get("discriminator"));
            newClaims.put("avatar", claims.get("avatar"));
            newClaims.put("globalName", claims.get("globalName"));
            newClaims.put("roles", roles);
            newClaims.put("permissions", new ArrayList<>(permissions));

            // Generate new JWT with updated permissions
            String newToken = jwtUtil.generateToken(userId, newClaims);

            Map<String, Object> response = new HashMap<>();
            response.put("token", newToken);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(401).build();
        }
    }

    @GetMapping("/default-permissions")
    public ResponseEntity<Map<String, Object>> getDefaultPermissions(CsrfToken csrfToken) {
        // Get permissions for unauthenticated users (default role)
        java.util.Set<String> permissions = userRoleConfig.getUserPermissions(null);

        // Convert to boolean map
        Map<String, Boolean> permissionsMap = getStringBooleanMap(new ArrayList<>(permissions));

        Map<String, Object> response = new HashMap<>();
        response.put("permissions", permissionsMap);

        return ResponseEntity.ok(response);
    }

    @NotNull
    private static Map<String, Boolean> getStringBooleanMap(List<String> permissionsList) {
        Map<String, Boolean> permissions = new HashMap<>();
        permissions.put("upload", permissionsList.contains("upload"));
        permissions.put("delete", permissionsList.contains("delete-sounds"));
        permissions.put("manageUsers", permissionsList.contains("manage-users"));
        permissions.put("editSounds", permissionsList.contains("edit-sounds"));
        permissions.put("playSounds", permissionsList.contains("play-sounds"));
        permissions.put("downloadSounds", permissionsList.contains("download-sounds"));
        permissions.put("updateVolume", permissionsList.contains("update-volume"));
        return permissions;
    }
}
