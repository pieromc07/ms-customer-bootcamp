package com.nttdata.mscustomer.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("jwt")
public record JwtProperties(
        String issuer,
        Duration expiration,
        String privateKeyLocation,
        String publicKeyLocation
) {
}
