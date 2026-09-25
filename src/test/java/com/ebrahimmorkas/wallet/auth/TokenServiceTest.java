package com.ebrahimmorkas.wallet.auth;

import com.ebrahimmorkas.wallet.user.Role;
import com.ebrahimmorkas.wallet.user.User;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.spec.SecretKeySpec;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class TokenServiceTest {

    private final SecretKeySpec key = new SecretKeySpec(new byte[32], "HmacSHA256");
    private final Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);
    private final TokenService tokenService = new TokenService(
            new NimbusJwtEncoder(new ImmutableSecret<>(key)),
            new JwtProperties("unused", "test-issuer", Duration.ofMinutes(30)),
            Clock.fixed(now, ZoneOffset.UTC));

    @Test
    void tokenCarriesUserIdRolesAndExpiry() {
        User user = new User("jane@example.com", "hash", "Jane", Role.ADMIN);
        UUID id = UUID.randomUUID();
        ReflectionTestUtils.setField(user, "id", id);

        TokenResponse response = tokenService.issue(user);

        Jwt jwt = NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build()
                .decode(response.accessToken());
        assertThat(jwt.getSubject()).isEqualTo(id.toString());
        assertThat(jwt.getClaimAsStringList("roles")).containsExactly("ADMIN");
        assertThat(jwt.getIssuedAt()).isEqualTo(now);
        assertThat(jwt.getExpiresAt()).isEqualTo(now.plus(Duration.ofMinutes(30)));
        assertThat(response.expiresIn()).isEqualTo(1800);
        assertThat(response.tokenType()).isEqualTo("Bearer");
    }
}
