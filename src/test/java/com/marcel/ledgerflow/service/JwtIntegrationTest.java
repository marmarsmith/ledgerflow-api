
package com.marcel.ledgerflow.service;

import com.marcel.ledgerflow.dto.LoginResponse;
import com.marcel.ledgerflow.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;

import static org.junit.jupiter.api.Assertions.*;

import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

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

    @Test
    void shouldSignAndVerifyJwt() {
        // Arrange
        User user = new User();
        user.setId(5L);

        // Act
        LoginResponse response = jwtService.generateToken(user);
        Jwt decodedJwt = jwtDecoder.decode(response.accessToken());

        // Assert
        assertNotNull(response.accessToken());
        assertEquals("Bearer", response.tokenType());
        assertEquals(3600, response.expiresIn());

        assertEquals("5", decodedJwt.getSubject());
        assertNotNull(decodedJwt.getIssuedAt());
        assertNotNull(decodedJwt.getExpiresAt());

        assertTrue(decodedJwt.getExpiresAt().isAfter(decodedJwt.getIssuedAt()));
    }
}
