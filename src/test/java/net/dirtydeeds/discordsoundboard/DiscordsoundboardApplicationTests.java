package net.dirtydeeds.discordsoundboard;

import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(classes = {MainController.class}, properties = {
        // Avoid trying to auto-configure a real DataSource during context load
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration",
        // Avoid eager creation of heavy beans
        "spring.main.lazy-initialization=true",
        // Provide a valid HS256 key size for any JwtUtil usage during context initialization
        "jwt.secret=0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef",
        "jwt.expiration=1000"
})
class DiscordsoundboardApplicationTests {

    // Prevent heavy Discord/JDA startup during a simple context load test
    @Mock
    private SoundPlayer soundPlayer;

	@Test
	void contextLoads() {
	}

}
