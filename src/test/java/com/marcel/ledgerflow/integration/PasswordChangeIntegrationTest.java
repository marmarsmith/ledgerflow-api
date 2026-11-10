
package com.marcel.ledgerflow.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marcel.ledgerflow.entity.User;
import com.marcel.ledgerflow.repository.UserRepository;
import com.marcel.ledgerflow.service.JwtService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class PasswordChangeIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("ledgerflow_test")
                    .withUsername("test")
                    .withPassword("test");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;
    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldRevokeOldJwtAfterSuccessfulPasswordChange()
            throws Exception {

        // Arrange: create a real user
        User user = new User();
        user.setFirstName("Test");
        user.setLastName("User");
        user.setEmail(
                "password-change-" + UUID.randomUUID()
                        + "@example.com"
        );
        user.setPasswordHash(
                passwordEncoder.encode("CurrentPass123")
        );

        User savedUser = userRepository.saveAndFlush(user);
        Long userId = savedUser.getId();

        try {
            String oldToken = jwtService
                    .generateToken(savedUser)
                    .accessToken();

            String request = """
                    {
                        "currentPassword": "CurrentPass123",
                        "newPassword": "NewSecurePass456"
                    }
                    """;

            // Act: change the password
            mockMvc.perform(put("/api/users/me/password")
                            .header(
                                    HttpHeaders.AUTHORIZATION,
                                    "Bearer " + oldToken
                            )
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(request))
                    .andExpect(status().isNoContent());

            // Verify the database was updated
            User updatedUser = userRepository.findById(userId)
                    .orElseThrow();

            assertEquals(1L, updatedUser.getTokenVersion());

            assertTrue(passwordEncoder.matches(
                    "NewSecurePass456",
                    updatedUser.getPasswordHash()
            ));

            assertFalse(passwordEncoder.matches(
                    "CurrentPass123",
                    updatedUser.getPasswordHash()
            ));

            // Old token should no longer work
            mockMvc.perform(get("/api/users/me")
                            .header(
                                    HttpHeaders.AUTHORIZATION,
                                    "Bearer " + oldToken
                            ))
                    .andExpect(status().isUnauthorized());

            // Generate a new token for the updated user
            String newToken = jwtService
                    .generateToken(updatedUser)
                    .accessToken();

            // New token should work
            mockMvc.perform(get("/api/users/me")
                            .header(
                                    HttpHeaders.AUTHORIZATION,
                                    "Bearer " + newToken
                            ))
                    .andExpect(status().isOk());

        } finally {
            userRepository.deleteById(userId);
        }
    }

    @Test
    void shouldRequireNewPasswordForLoginAfterPasswordChange()
            throws Exception {

        User user = new User();
        user.setFirstName("Test");
        user.setLastName("User");
        user.setEmail(
                "password-login-" + UUID.randomUUID()
                        + "@example.com"
        );
        user.setPasswordHash(
                passwordEncoder.encode("CurrentPass123")
        );

        User savedUser = userRepository.saveAndFlush(user);
        Long userId = savedUser.getId();

        try {
            String oldToken = jwtService
                    .generateToken(savedUser)
                    .accessToken();

            // Change the password using the authenticated endpoint
            mockMvc.perform(put("/api/users/me/password")
                            .header(
                                    HttpHeaders.AUTHORIZATION,
                                    "Bearer " + oldToken
                            )
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                {
                                    "currentPassword": "CurrentPass123",
                                    "newPassword": "NewSecurePass456"
                                }
                                """))
                    .andExpect(status().isNoContent());

            // Old password should no longer authenticate
            String oldLoginRequest = """
                {
                    "email": "%s",
                    "password": "CurrentPass123"
                }
                """.formatted(savedUser.getEmail());

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(oldLoginRequest))
                    .andExpect(status().isUnauthorized());

            // New password should authenticate successfully
            String newLoginRequest = """
                {
                    "email": "%s",
                    "password": "NewSecurePass456"
                }
                """.formatted(savedUser.getEmail());

            MvcResult loginResult =
                    mockMvc.perform(post("/api/auth/login")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(newLoginRequest))
                            .andExpect(status().isOk())
                            .andExpect(jsonPath("$.accessToken").exists())
                            .andReturn();

            JsonNode response = objectMapper.readTree(
                    loginResult.getResponse().getContentAsString()
            );

            String newToken = response
                    .get("accessToken")
                    .asText();

            // JWT issued through login should work
            mockMvc.perform(get("/api/users/me")
                            .header(
                                    HttpHeaders.AUTHORIZATION,
                                    "Bearer " + newToken
                            ))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(userId));

            // Previously issued JWT should remain invalid
            mockMvc.perform(get("/api/users/me")
                            .header(
                                    HttpHeaders.AUTHORIZATION,
                                    "Bearer " + oldToken
                            ))
                    .andExpect(status().isUnauthorized());

        } finally {
            userRepository.deleteById(userId);
        }
    }

}
