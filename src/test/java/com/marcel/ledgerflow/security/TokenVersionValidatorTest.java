
package com.marcel.ledgerflow.security;

import com.marcel.ledgerflow.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

import java.math.BigDecimal;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TokenVersionValidatorTest {

    private UserRepository userRepository;
    private TokenVersionValidator validator;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        validator = new TokenVersionValidator(userRepository);
    }

    private Jwt createJwt(Object tokenVersion) {
        Instant now = Instant.now();

        return Jwt.withTokenValue("test-token")
                .header("alg", "HS256")
                .subject("5")
                .issuedAt(now)
                .expiresAt(now.plusSeconds(3600))
                .claim("tokenVersion", tokenVersion)
                .build();
    }

    @Test
    void shouldRejectFractionalTokenVersion() {
        OAuth2TokenValidatorResult result =
                validator.validate(createJwt(1.5));

        assertTrue(result.hasErrors());
        verifyNoInteractions(userRepository);
    }

    @Test
    void shouldRejectNegativeTokenVersion() {
        OAuth2TokenValidatorResult result =
                validator.validate(createJwt(-1));

        assertTrue(result.hasErrors());
        verifyNoInteractions(userRepository);
    }

    @Test
    void shouldRejectTokenVersionOutsideLongRange() {
        OAuth2TokenValidatorResult result =
                validator.validate(createJwt(
                        new BigDecimal("9223372036854775808")
                ));

        assertTrue(result.hasErrors());
        verifyNoInteractions(userRepository);
    }

    @Test
    void shouldRejectNonNumericTokenVersion() {
        OAuth2TokenValidatorResult result =
                validator.validate(createJwt("invalid"));

        assertTrue(result.hasErrors());
        verifyNoInteractions(userRepository);
    }
}
