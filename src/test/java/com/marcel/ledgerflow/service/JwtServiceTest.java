
package com.marcel.ledgerflow.service;

import com.marcel.ledgerflow.dto.LoginResponse;
import com.marcel.ledgerflow.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;

import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class JwtServiceTest {

    @Test
    void shouldGenerateJwtWithCorrectClaims() {
        // Arrange
        JwtEncoder jwtEncoder = mock(JwtEncoder.class);
        JwtService jwtService = new JwtService(jwtEncoder, 3600);

        User user = new User();
        user.setId(5L);

        Jwt mockJwt = new Jwt(
                "mock-jwt-token",
                Instant.now(),
                Instant.now().plusSeconds(3600),
                Map.of("alg", "HS256"),
                Map.of("sub", "5")
        );

        when(jwtEncoder.encode(any(JwtEncoderParameters.class)))
                .thenReturn(mockJwt);

        // Act
        Instant before = Instant.now();
        LoginResponse response = jwtService.generateToken(user);
        Instant after = Instant.now();

        // Assert response
        assertEquals("mock-jwt-token", response.accessToken());
        assertEquals("Bearer", response.tokenType());
        assertEquals(3600, response.expiresIn());

        // Capture the claims sent to the encoder
        var captor = org.mockito.ArgumentCaptor
                .forClass(JwtEncoderParameters.class);

        verify(jwtEncoder).encode(captor.capture());

        JwtEncoderParameters parameters = captor.getValue();

        assertEquals(
                MacAlgorithm.HS256,
                parameters.getJwsHeader().getAlgorithm()
        );

        assertEquals("5", parameters.getClaims().getSubject());

        assertFalse(parameters.getClaims().getIssuedAt().isBefore(before));
        assertFalse(parameters.getClaims().getIssuedAt().isAfter(after));

        assertEquals(
                3600,
                parameters.getClaims().getExpiresAt().getEpochSecond()
                        - parameters.getClaims().getIssuedAt().getEpochSecond()
        );
    }
}
