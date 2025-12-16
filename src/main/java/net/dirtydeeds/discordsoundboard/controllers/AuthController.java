package net.dirtydeeds.discordsoundboard.controllers;

import io.jsonwebtoken.Claims;
import net.dirtydeeds.discordsoundboard.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
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

    @GetMapping("/user")
    public ResponseEntity<Map<String, Object>> getUser(@RequestHeader("Authorization") String authorization) {
        try {
            String token = authorization.replace("Bearer ", "");

            if (!jwtUtil.validateToken(token)) {
                return ResponseEntity.status(401).build();
            }

            Claims claims = jwtUtil.getClaimsFromToken(token);

            // Get permissions from the token
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
            Map<String, Boolean> permissions = new HashMap<>();
            permissions.put("upload", permissionsList.contains("upload"));
            permissions.put("delete", permissionsList.contains("delete-sounds"));
            permissions.put("manageUsers", permissionsList.contains("manage-users"));
            permissions.put("editSounds", permissionsList.contains("edit-sounds"));
            permissions.put("playSounds", permissionsList.contains("play-sounds"));
            permissions.put("downloadSounds", permissionsList.contains("download-sounds"));
            permissions.put("updateVolume", permissionsList.contains("update-volume"));

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
}
