package net.dirtydeeds.discordsoundboard.util;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = {JwtUtil.class})
@TestPropertySource(properties = {
        // valid 64+ bytes secret
        "jwt.secret=aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
        "jwt.expiration=60000"
})
class JwtUtilInvalidTokenTest {

    @Autowired
    private JwtUtil jwtUtil;

    @Test
    void invalid_signature_token_is_rejected() {
        String token = jwtUtil.generateToken("user-x", Map.of("username", "x"));

        // tamper the token by changing one character
        String tampered = token.substring(0, token.length() - 2) + "ab";

        assertFalse(jwtUtil.validateToken(tampered));
    }

    @Test
    void malformed_token_is_rejected() {
        assertFalse(jwtUtil.validateToken("not.a.jwt"));
    }
}
