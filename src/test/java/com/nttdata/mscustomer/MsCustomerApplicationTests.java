package com.nttdata.mscustomer;

import com.nttdata.mscustomer.util.TestRsaKeys;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest
class MsCustomerApplicationTests {

    @DynamicPropertySource
    static void jwtKeys(DynamicPropertyRegistry registry) {
        KeyPair keyPair = TestRsaKeys.generate();
        registry.add("jwt.private-key-location",
                () -> writeTempFile(TestRsaKeys.privatePem(keyPair.getPrivate())));
        registry.add("jwt.public-key-location",
                () -> writeTempFile(TestRsaKeys.publicPem(keyPair.getPublic())));
    }

    @Test
    void contextLoads() {
    }

    private static String writeTempFile(String content) {
        try {
            Path path = Files.createTempFile("ms-customer-jwt", ".pem");
            path.toFile().deleteOnExit();
            Files.writeString(path, content, StandardCharsets.UTF_8);
            return path.toUri().toString();
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot write test JWT key", exception);
        }
    }
}
