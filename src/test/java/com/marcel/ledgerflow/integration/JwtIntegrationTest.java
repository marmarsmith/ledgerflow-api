
package com.marcel.ledgerflow.integration;

import com.marcel.ledgerflow.dto.LoginResponse;
import com.marcel.ledgerflow.entity.User;
import com.marcel.ledgerflow.service.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;

import static org.junit.jupiter.api.Assertions.*;

import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import com.marcel.ledgerflow.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;

@SpringBootTest
@Testcontainers
class JwtIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("ledgerflow_test")
                    .withUsername("test")
                    .withPassword("test");

    @Autowired
    private JwtService jwtService;

    @Autowired
    private JwtDecoder jwtDecoder;
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private JwtEncoder jwtEncoder;


    @Test
    void shouldSignAndVerifyJwt() {
        // Arrange
        User user = new User();
        user.setFirstName("John");
        user.setLastName("Doe");
        user.setEmail("jwt-test@example.com");
        user.setPasswordHash(
                passwordEncoder.encode("TestPassword123")
        );

        User savedUser = userRepository.saveAndFlush(user);

        // Act
        LoginResponse response = jwtService.generateToken(savedUser);
        Jwt decodedJwt = jwtDecoder.decode(response.accessToken());

        // Assert
        assertNotNull(response.accessToken());
        assertEquals("Bearer", response.tokenType());
        assertEquals(3600, response.expiresIn());

        assertEquals(
                savedUser.getId().toString(),
                decodedJwt.getSubject()
        );

        assertEquals(
                0L,
                ((Number) decodedJwt.getClaim("tokenVersion")).longValue()
        );

        assertNotNull(decodedJwt.getIssuedAt());
        assertNotNull(decodedJwt.getExpiresAt());

        assertTrue(
                decodedJwt.getExpiresAt().isAfter(
                        decodedJwt.getIssuedAt()
                )
        );
    }

    @Test
    void shouldRejectPreviouslyIssuedJwtAfterTokenVersionChanges() {
        // Arrange
        User user = new User();
        user.setFirstName("John");
        user.setLastName("Doe");
        user.setEmail("jwt-revocation@example.com");
        user.setPasswordHash(
                passwordEncoder.encode("CurrentPass123")
        );

        User savedUser = userRepository.saveAndFlush(user);

        String oldToken = jwtService
                .generateToken(savedUser)
                .accessToken();

        // Verify the original token works
        Jwt originalJwt = jwtDecoder.decode(oldToken);

        assertEquals(
                0L,
                ((Number) originalJwt.getClaim("tokenVersion")).longValue()
        );

        // Simulate a successful password change
        savedUser.setPasswordHash(
                passwordEncoder.encode("NewSecurePass456")
        );

        savedUser.setTokenVersion(
                savedUser.getTokenVersion() + 1
        );

        userRepository.saveAndFlush(savedUser);

        // Assert: old token is revoked
        assertThrows(
                JwtValidationException.class,
                () -> jwtDecoder.decode(oldToken)
        );

        // Generate a new token after the version change
        String newToken = jwtService
                .generateToken(savedUser)
                .accessToken();

        Jwt newJwt = jwtDecoder.decode(newToken);

        assertEquals(
                1L,
                ((Number) newJwt.getClaim("tokenVersion")).longValue()
        );
    }

    @Test
    void shouldRejectJwtWithoutTokenVersion() {
        // Arrange
        User user = new User();
        user.setFirstName("Jane");
        user.setLastName("Doe");
        user.setEmail("jwt-missing-version@example.com");
        user.setPasswordHash(
                passwordEncoder.encode("TestPassword123")
        );

        User savedUser = userRepository.saveAndFlush(user);

        Instant now = Instant.now();

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(savedUser.getId().toString())
                .issuedAt(now)
                .expiresAt(now.plusSeconds(3600))
                .build();

        JwsHeader header = JwsHeader
                .with(MacAlgorithm.HS256)
                .build();

        String token = jwtEncoder.encode(
                JwtEncoderParameters.from(header, claims)
        ).getTokenValue();

        // Act & Assert
        assertThrows(
                JwtValidationException.class,
                () -> jwtDecoder.decode(token)
        );
    }

}
