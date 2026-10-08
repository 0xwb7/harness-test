package com.group.harnesstest;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import java.util.UUID;

@SpringBootTest
class HarnessTestApplicationTests {
    private static final String TEST_SECRET = UUID.randomUUID().toString() + UUID.randomUUID();

    @DynamicPropertySource
    static void jwtSecret(DynamicPropertyRegistry properties) {
        properties.add("app.jwt.secret", () -> TEST_SECRET);
    }

    @Test
    void contextLoads() {
    }

}
