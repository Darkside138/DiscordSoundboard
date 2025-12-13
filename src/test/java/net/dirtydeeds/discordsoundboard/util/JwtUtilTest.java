package net.dirtydeeds.discordsoundboard.util;

import io.jsonwebtoken.Claims;
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
}
