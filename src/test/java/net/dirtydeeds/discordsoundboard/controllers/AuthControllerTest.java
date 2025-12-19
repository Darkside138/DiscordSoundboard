package net.dirtydeeds.discordsoundboard.controllers;

import io.jsonwebtoken.Claims;
import net.dirtydeeds.discordsoundboard.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.web.csrf.CsrfToken;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private CsrfToken csrfToken;

    @InjectMocks
    private AuthController authController;

    private String validToken;
    private String authorizationHeader;

    @BeforeEach
    void setUp() {
        validToken = "valid.jwt.token";
        authorizationHeader = "Bearer " + validToken;
    }

    @Test
    void getUser_withValidToken_returnsUserInfo() {
        // Arrange
        Claims claims = mock(Claims.class);
        when(claims.getSubject()).thenReturn("user123");
        when(claims.get("username")).thenReturn("testuser");
        when(claims.get("discriminator")).thenReturn("1234");
        when(claims.get("avatar")).thenReturn("avatar_hash");
        when(claims.get("globalName")).thenReturn("Test User");
        when(claims.get("roles", List.class)).thenReturn(Arrays.asList("Admin", "DJ"));
        when(claims.get("permissions", List.class)).thenReturn(Arrays.asList("upload", "delete-sounds", "play-sounds"));

        when(jwtUtil.validateToken(validToken)).thenReturn(true);
        when(jwtUtil.getClaimsFromToken(validToken)).thenReturn(claims);

        // Act
        ResponseEntity<Map<String, Object>> response = authController.getUser(authorizationHeader, csrfToken);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());

        Map<String, Object> userResponse = response.getBody();
        assertEquals("user123", userResponse.get("id"));
        assertEquals("testuser", userResponse.get("username"));
        assertEquals("1234", userResponse.get("discriminator"));
        assertEquals("avatar_hash", userResponse.get("avatar"));
        assertEquals("Test User", userResponse.get("globalName"));

        @SuppressWarnings("unchecked")
        Map<String, Boolean> permissions = (Map<String, Boolean>) userResponse.get("permissions");
        assertTrue(permissions.get("upload"));
        assertTrue(permissions.get("delete"));
        assertTrue(permissions.get("playSounds"));
        assertFalse(permissions.get("manageUsers"));
        assertFalse(permissions.get("editSounds"));
    }

    @Test
    void getUser_withInvalidToken_returns401() {
        // Arrange
        when(jwtUtil.validateToken(validToken)).thenReturn(false);

        // Act
        ResponseEntity<Map<String, Object>> response = authController.getUser(authorizationHeader, csrfToken);

        // Assert
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertNull(response.getBody());
    }

    @Test
    void getUser_whenValidationThrowsException_returns401() {
        // Arrange
        when(jwtUtil.validateToken(validToken)).thenThrow(new RuntimeException("Token validation error"));

        // Act
        ResponseEntity<Map<String, Object>> response = authController.getUser(authorizationHeader, csrfToken);

        // Assert
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertNull(response.getBody());
    }

    @Test
    void getUser_whenGetClaimsThrowsException_returns401() {
        // Arrange
        when(jwtUtil.validateToken(validToken)).thenReturn(true);
        when(jwtUtil.getClaimsFromToken(validToken)).thenThrow(new RuntimeException("Claims error"));

        // Act
        ResponseEntity<Map<String, Object>> response = authController.getUser(authorizationHeader, csrfToken);

        // Assert
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertNull(response.getBody());
    }

    @Test
    void getUser_withNullPermissions_returnsEmptyPermissionsMap() {
        // Arrange
        Claims claims = mock(Claims.class);
        when(claims.getSubject()).thenReturn("user123");
        when(claims.get("username")).thenReturn("testuser");
        when(claims.get("discriminator")).thenReturn("1234");
        when(claims.get("avatar")).thenReturn(null);
        when(claims.get("globalName")).thenReturn(null);
        when(claims.get("roles", List.class)).thenReturn(Arrays.asList("User"));
        when(claims.get("permissions", List.class)).thenReturn(null);

        when(jwtUtil.validateToken(validToken)).thenReturn(true);
        when(jwtUtil.getClaimsFromToken(validToken)).thenReturn(claims);

        // Act
        ResponseEntity<Map<String, Object>> response = authController.getUser(authorizationHeader, csrfToken);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());

        @SuppressWarnings("unchecked")
        Map<String, Boolean> permissions = (Map<String, Boolean>) response.getBody().get("permissions");
        assertFalse(permissions.get("upload"));
        assertFalse(permissions.get("delete"));
        assertFalse(permissions.get("manageUsers"));
        assertFalse(permissions.get("editSounds"));
        assertFalse(permissions.get("playSounds"));
        assertFalse(permissions.get("downloadSounds"));
        assertFalse(permissions.get("updateVolume"));
    }

    @Test
    void getUser_withAllPermissions_returnsAllPermissionsTrue() {
        // Arrange
        Claims claims = mock(Claims.class);
        when(claims.getSubject()).thenReturn("admin123");
        when(claims.get("username")).thenReturn("admin");
        when(claims.get("discriminator")).thenReturn("0001");
        when(claims.get("avatar")).thenReturn("admin_avatar");
        when(claims.get("globalName")).thenReturn("Admin User");
        when(claims.get("roles", List.class)).thenReturn(Arrays.asList("Admin"));
        when(claims.get("permissions", List.class)).thenReturn(Arrays.asList(
                "upload",
                "delete-sounds",
                "manage-users",
                "edit-sounds",
                "play-sounds",
                "download-sounds",
                "update-volume"
        ));

        when(jwtUtil.validateToken(validToken)).thenReturn(true);
        when(jwtUtil.getClaimsFromToken(validToken)).thenReturn(claims);

        // Act
        ResponseEntity<Map<String, Object>> response = authController.getUser(authorizationHeader, csrfToken);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());

        @SuppressWarnings("unchecked")
        Map<String, Boolean> permissions = (Map<String, Boolean>) response.getBody().get("permissions");
        assertTrue(permissions.get("upload"));
        assertTrue(permissions.get("delete"));
        assertTrue(permissions.get("manageUsers"));
        assertTrue(permissions.get("editSounds"));
        assertTrue(permissions.get("playSounds"));
        assertTrue(permissions.get("downloadSounds"));
        assertTrue(permissions.get("updateVolume"));
    }

    @Test
    void getUser_stripsBearerPrefixFromToken() {
        // Arrange
        Claims claims = mock(Claims.class);
        when(claims.getSubject()).thenReturn("user123");
        when(claims.get("username")).thenReturn("testuser");
        when(claims.get("permissions", List.class)).thenReturn(List.of());

        when(jwtUtil.validateToken(validToken)).thenReturn(true);
        when(jwtUtil.getClaimsFromToken(validToken)).thenReturn(claims);

        // Act
        authController.getUser(authorizationHeader, csrfToken);

        // Assert
        verify(jwtUtil).validateToken(validToken); // Verify token without "Bearer " prefix
        verify(jwtUtil).getClaimsFromToken(validToken);
    }

    @Test
    void getUser_withSomePermissions_mapsCorrectly() {
        // Arrange
        Claims claims = mock(Claims.class);
        when(claims.getSubject()).thenReturn("dj123");
        when(claims.get("username")).thenReturn("dj");
        when(claims.get("discriminator")).thenReturn("5678");
        when(claims.get("avatar")).thenReturn("dj_avatar");
        when(claims.get("globalName")).thenReturn("DJ User");
        when(claims.get("roles", List.class)).thenReturn(Arrays.asList("DJ"));
        when(claims.get("permissions", List.class)).thenReturn(Arrays.asList("play-sounds", "download-sounds", "update-volume"));

        when(jwtUtil.validateToken(validToken)).thenReturn(true);
        when(jwtUtil.getClaimsFromToken(validToken)).thenReturn(claims);

        // Act
        ResponseEntity<Map<String, Object>> response = authController.getUser(authorizationHeader, csrfToken);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());

        @SuppressWarnings("unchecked")
        Map<String, Boolean> permissions = (Map<String, Boolean>) response.getBody().get("permissions");
        assertFalse(permissions.get("upload"));
        assertFalse(permissions.get("delete"));
        assertFalse(permissions.get("manageUsers"));
        assertFalse(permissions.get("editSounds"));
        assertTrue(permissions.get("playSounds"));
        assertTrue(permissions.get("downloadSounds"));
        assertTrue(permissions.get("updateVolume"));
    }

    @Test
    void getUser_includesRolesInResponse() {
        // Arrange
        Claims claims = mock(Claims.class);
        when(claims.getSubject()).thenReturn("user123");
        when(claims.get("username")).thenReturn("testuser");
        when(claims.get("discriminator")).thenReturn("1234");
        when(claims.get("avatar")).thenReturn("avatarURL");
        when(claims.get("globalName")).thenReturn("globalName");
        when(claims.get("permissions", List.class)).thenReturn(List.of());
        List<String> roles = Arrays.asList("Moderator", "DJ");
        when(claims.get("roles", List.class)).thenReturn(roles);

        when(jwtUtil.validateToken(validToken)).thenReturn(true);
        when(jwtUtil.getClaimsFromToken(validToken)).thenReturn(claims);

        // Act
        ResponseEntity<Map<String, Object>> response = authController.getUser(authorizationHeader, csrfToken);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());

        @SuppressWarnings("unchecked")
        List<String> responseRoles = (List<String>) response.getBody().get("roles");
        assertEquals(roles, responseRoles);
    }

    @Test
    void getUser_withEmptyPermissionsList_returnsAllPermissionsFalse() {
        // Arrange
        Claims claims = mock(Claims.class);
        when(claims.getSubject()).thenReturn("user123");
        when(claims.get("username")).thenReturn("testuser");
        when(claims.get("discriminator")).thenReturn("1234");
        when(claims.get("permissions", List.class)).thenReturn(List.of());

        when(jwtUtil.validateToken(validToken)).thenReturn(true);
        when(jwtUtil.getClaimsFromToken(validToken)).thenReturn(claims);

        // Act
        ResponseEntity<Map<String, Object>> response = authController.getUser(authorizationHeader, csrfToken);

        // Assert
        @SuppressWarnings("unchecked")
        Map<String, Boolean> permissions = (Map<String, Boolean>) response.getBody().get("permissions");
        assertEquals(7, permissions.size());
        assertFalse(permissions.get("upload"));
        assertFalse(permissions.get("delete"));
        assertFalse(permissions.get("manageUsers"));
        assertFalse(permissions.get("editSounds"));
        assertFalse(permissions.get("playSounds"));
        assertFalse(permissions.get("downloadSounds"));
        assertFalse(permissions.get("updateVolume"));
    }
}