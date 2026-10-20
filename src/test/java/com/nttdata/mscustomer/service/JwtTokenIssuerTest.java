package com.nttdata.mscustomer.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nttdata.mscustomer.config.JwtProperties;
import com.nttdata.mscustomer.model.AccessToken;
import com.nttdata.mscustomer.model.BusinessDetails;
import com.nttdata.mscustomer.model.Customer;
import com.nttdata.mscustomer.model.CustomerProfile;
import com.nttdata.mscustomer.model.CustomerStatus;
import com.nttdata.mscustomer.model.CustomerType;
import com.nttdata.mscustomer.model.DocumentType;
import com.nttdata.mscustomer.model.PersonalDetails;
import com.nttdata.mscustomer.util.TestRsaKeys;
import java.security.KeyPair;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

class JwtTokenIssuerTest {

    private static final String ISSUER = "ms-customer";
    private static final String CUSTOMER_ID = "665f1c2e8a1b2c3d4e5f6a7b";

    private KeyPair keyPair;
    private NimbusJwtDecoder decoder;
    private JwtTokenIssuer issuer;

    @BeforeEach
    void setUp() {
        keyPair = TestRsaKeys.generate();
        RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
        RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();

        RSAKey rsaKey = new RSAKey.Builder(publicKey).privateKey(privateKey).build();
        JwtEncoder encoder = new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(rsaKey)));
        decoder = NimbusJwtDecoder.withPublicKey(publicKey)
                .signatureAlgorithm(SignatureAlgorithm.RS256)
                .build();

        JwtProperties properties = new JwtProperties(
                ISSUER, Duration.ofHours(1), null, null);
        issuer = new JwtTokenIssuer(encoder, properties);
    }

    @Test
    void issuesRs256TokenVerifiableWithPublicKey() {
        AccessToken token = issuer.issue(customer(CustomerType.PERSONAL));

        Jwt jwt = decoder.decode(token.value());

        assertEquals(ISSUER, jwt.getClaimAsString("iss"));
        assertEquals(CUSTOMER_ID, jwt.getSubject());
        assertEquals("PERSONAL", jwt.getClaimAsString("customerType"));
        assertNotNull(jwt.getIssuedAt());
    }

    @Test
    void tokenExpiresAfterConfiguredLifetime() {
        AccessToken token = issuer.issue(customer(CustomerType.BUSINESS));

        Jwt jwt = decoder.decode(token.value());
        long lifetime = jwt.getExpiresAt().getEpochSecond()
                - jwt.getIssuedAt().getEpochSecond();

        assertEquals(3600L, lifetime);
        assertEquals(3600L, token.expiresInSeconds());
        assertEquals("BUSINESS", jwt.getClaimAsString("customerType"));
    }

    @Test
    void tokenIssuedAtIsRecent() {
        Instant before = Instant.now().minusSeconds(1);

        Jwt jwt = decoder.decode(issuer.issue(customer(CustomerType.PERSONAL)).value());

        assertTrue(jwt.getIssuedAt().isAfter(before));
    }

    private static Customer customer(CustomerType type) {
        boolean personal = type == CustomerType.PERSONAL;
        return new Customer(
                CUSTOMER_ID,
                type,
                CustomerProfile.STANDARD,
                DocumentType.DNI,
                "12345678",
                "ana@example.com",
                "999111222",
                CustomerStatus.ACTIVE,
                personal ? new PersonalDetails("Ana", "Lopez") : null,
                personal ? null : new BusinessDetails("Acme SAC", "Acme"),
                Instant.parse("2024-01-01T00:00:00Z"),
                Instant.parse("2024-01-01T00:00:00Z"));
    }
}
