package com.nttdata.mscustomer.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.security.KeyPair;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import org.junit.jupiter.api.Test;

class PemKeyLoaderTest {

    private final KeyPair keyPair = TestRsaKeys.generate();

    @Test
    void loadsPrivateKeyFromPkcs8Pem() {
        RSAPrivateKey key = PemKeyLoader.loadPrivateKey(
                TestRsaKeys.privatePem(keyPair.getPrivate()));

        assertArrayEquals(keyPair.getPrivate().getEncoded(), key.getEncoded());
    }

    @Test
    void loadsPublicKeyFromX509Pem() {
        RSAPublicKey key = PemKeyLoader.loadPublicKey(
                TestRsaKeys.publicPem(keyPair.getPublic()));

        assertArrayEquals(keyPair.getPublic().getEncoded(), key.getEncoded());
    }

    @Test
    void rejectsPublicPemWhenLoadingPrivateKey() {
        String publicPem = TestRsaKeys.publicPem(keyPair.getPublic());

        assertThrows(IllegalArgumentException.class,
                () -> PemKeyLoader.loadPrivateKey(publicPem));
    }

    @Test
    void rejectsNonBase64Content() {
        String invalid = "-----BEGIN PUBLIC KEY-----\n%%%\n-----END PUBLIC KEY-----";

        assertThrows(IllegalArgumentException.class,
                () -> PemKeyLoader.loadPublicKey(invalid));
    }

    @Test
    void rejectsBase64ThatIsNotAKey() {
        String invalid = "-----BEGIN PRIVATE KEY-----\nAAAA\n-----END PRIVATE KEY-----";

        assertThrows(IllegalArgumentException.class,
                () -> PemKeyLoader.loadPrivateKey(invalid));
    }
}
