package net.dirtydeeds.discordsoundboard.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = {JwtUtil.class})
@TestPropertySource(properties = {
        // 64+ chars for HS256 HMAC key
        "jwt.secret=0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef",
        "jwt.expiration=1000"
})
class JwtUtilTest {

    @Autowired
    private JwtUtil jwtUtil;

    @Test
    void generate_and_parse_token_roundtrip() {
        Map<String, Object> claims = new HashMap<>();
        claims.put("username", "alice");
        claims.put("discriminator", "1234");
        claims.put("avatar", "abc");

        String token = jwtUtil.generateToken("user-1", claims);
        assertNotNull(token);
        assertTrue(jwtUtil.validateToken(token));

        String subject = jwtUtil.getUserIdFromToken(token);
        assertEquals("user-1", subject);

        Claims parsed = jwtUtil.getClaimsFromToken(token);
        assertEquals("alice", parsed.get("username"));
        assertEquals("1234", parsed.get("discriminator"));
        assertEquals("abc", parsed.get("avatar"));
    }

    @Test
    void expired_token_is_invalid() throws InterruptedException {
        Map<String, Object> claims = new HashMap<>();
        String token = jwtUtil.generateToken("user-2", claims);

        // wait past expiration (configured to 200ms)
        Thread.sleep(1100);

        assertFalse(jwtUtil.validateToken(token));
    }

    @Test
    void token_with_invalid_signature_fails_validation() {
        // Create a token signed with a DIFFERENT key
        String differentSecret = "fedcba9876543210fedcba9876543210fedcba9876543210fedcba9876543210";
        String badToken = Jwts.builder()
                .setSubject("user-3")
                .signWith(Keys.hmacShaKeyFor(differentSecret.getBytes()), SignatureAlgorithm.HS256)
                .compact();

        assertFalse(jwtUtil.validateToken(badToken));
        assertThrows(JwtException.class, () -> jwtUtil.getClaimsFromToken(badToken));
        assertThrows(JwtException.class, () -> jwtUtil.getUserIdFromToken(badToken));
    }

    @Test
    void malformed_token_is_rejected() {
        String malformed = "not-a-jwt-token";

        assertFalse(jwtUtil.validateToken(malformed));
        assertThrows(JwtException.class, () -> jwtUtil.getClaimsFromToken(malformed));
        assertThrows(JwtException.class, () -> jwtUtil.getUserIdFromToken(malformed));
    }

    @Test
    void token_without_subject_returns_null_subject() {
        // Build a token without subject but with the app's signing key via JwtUtil
        // We cannot access the secret directly, so we generate a proper token via JwtUtil,
        // then rebuild a new one with same claims but no subject by parsing and re-signing.
        Map<String, Object> claims = new HashMap<>();
        claims.put("k", "v");
        String valid = jwtUtil.generateToken("temp-user", claims);

        // Extract claims and reissue without subject using the same signing key indirectly isn't possible,
        // so instead create a minimal valid token via JwtUtil and verify subject retrieval works,
        // then ensure that a token explicitly built without subject parses to null subject using the Jwt library
        // but still with a valid signature from a separate builder using the SAME key length requirements.

        // Build no-subject token signed with the same algorithm but we cannot reuse JwtUtil key directly; however,
        // validateToken only checks signature against JwtUtil's key, so this will fail validation. We'll focus on
        // behavior of getUserIdFromToken for a structurally valid JWT without subject using the same key as JwtUtil
        // by leveraging jwtUtil.getClaimsFromToken on a token it created and checking null when subject is removed
        // is not feasible without key. Therefore, assert library behavior locally: subject can be null.

        String noSubToken = Jwts.builder()
                .claim("k", "v")
                // no subject
                .signWith(Keys.hmacShaKeyFor(
                        ("0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef").getBytes()
                ), SignatureAlgorithm.HS256)
                .compact();

        // Since we used the same secret as the test context, JwtUtil should validate this token,
        // and because there is no subject, getUserIdFromToken should return null.
        assertTrue(jwtUtil.validateToken(noSubToken));
        assertNull(jwtUtil.getUserIdFromToken(noSubToken));

        // Also sanity check our earlier valid token still works end-to-end
        assertTrue(jwtUtil.validateToken(valid));
        assertEquals("temp-user", jwtUtil.getUserIdFromToken(valid));
    }

    @Test
    void null_token_is_invalid_and_parsing_throws() {
        String token = null;
        assertFalse(jwtUtil.validateToken(token));
        assertThrows(IllegalArgumentException.class, () -> jwtUtil.getClaimsFromToken(token));
        assertThrows(IllegalArgumentException.class, () -> jwtUtil.getUserIdFromToken(token));
    }

    @Test
    void empty_token_is_invalid_and_parsing_throws() {
        String token = "";
        assertFalse(jwtUtil.validateToken(token));
        assertThrows(IllegalArgumentException.class, () -> jwtUtil.getClaimsFromToken(token));
        assertThrows(IllegalArgumentException.class, () -> jwtUtil.getUserIdFromToken(token));
    }
}
