package com.nttdata.mscustomer.service;

import com.nttdata.mscustomer.config.JwtProperties;
import com.nttdata.mscustomer.model.AccessToken;
import com.nttdata.mscustomer.model.Customer;
import java.time.Duration;
import java.time.Instant;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.stereotype.Component;

@Component
public class JwtTokenIssuer {

    private final JwtEncoder jwtEncoder;
    private final JwtProperties properties;

    public JwtTokenIssuer(JwtEncoder jwtEncoder, JwtProperties properties) {
        this.jwtEncoder = jwtEncoder;
        this.properties = properties;
    }

    public AccessToken issue(Customer customer) {
        Instant issuedAt = Instant.now();
        Duration lifetime = properties.expiration();

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(properties.issuer())
                .subject(customer.getId())
                .issuedAt(issuedAt)
                .expiresAt(issuedAt.plus(lifetime))
                .claim("customerType", customer.getCustomerType().name())
                .build();
        JwsHeader header = JwsHeader.with(SignatureAlgorithm.RS256).build();

        Jwt jwt = jwtEncoder.encode(JwtEncoderParameters.from(header, claims));
        return new AccessToken(jwt.getTokenValue(), lifetime.toSeconds());
    }
}
