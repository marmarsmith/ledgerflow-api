
package com.marcel.ledgerflow.integration;

import com.marcel.ledgerflow.dto.LoginResponse;
import com.marcel.ledgerflow.dto.UserResponse;
import com.marcel.ledgerflow.entity.User;
import com.marcel.ledgerflow.service.JwtService;
import com.marcel.ledgerflow.service.UserService;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.OffsetDateTime;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import org.springframework.http.MediaType;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import com.marcel.ledgerflow.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.junit.jupiter.api.AfterEach;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class UserProfileSecurityIntegrationTest {

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
    private JwtService jwtService;

    @MockitoBean
    private UserService userService;
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private final List<Long> createdUserIds = new ArrayList<>();



    private User createTestUser() {
        User user = new User();

        user.setFirstName("Test");
        user.setLastName("User");
        user.setEmail(
                "security-test-" + UUID.randomUUID() + "@example.com"
        );
        user.setPasswordHash(
                passwordEncoder.encode("TestPassword123")
        );

        User savedUser = userRepository.saveAndFlush(user);
        createdUserIds.add(savedUser.getId());

        return savedUser;
    }

    private String generateToken(User user) {
        return jwtService.generateToken(user).accessToken();
    }

    @AfterEach
    void cleanupTestUsers() {
        userRepository.deleteAllById(createdUserIds);
        createdUserIds.clear();
    }



    @Test
    void shouldReturn401WhenNoJwtIsProvided() throws Exception {
        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(userService);
    }

    @Test
    void shouldReturn401WhenJwtIsInvalid() throws Exception {
        mockMvc.perform(get("/api/users/me")
                        .header(
                                HttpHeaders.AUTHORIZATION,
                                "Bearer invalid-jwt-token"
                        ))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(userService);
    }

    @Test
    void shouldReturn200WhenValidJwtIsProvided() throws Exception {
        // Arrange
        User user = createTestUser();
        Long userId = user.getId();
        String token = generateToken(user);

        UserResponse profile = new UserResponse(
                userId,
                "John",
                "Doe",
                "john.doe@example.com",
                OffsetDateTime.parse("2026-10-13T12:00:00Z")
        );

        when(userService.getProfile(userId))
                .thenReturn(profile);

        // Act & Assert
        mockMvc.perform(get("/api/users/me")
                        .header(
                                HttpHeaders.AUTHORIZATION,
                                "Bearer " + token
                        ))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId))
                .andExpect(jsonPath("$.firstName").value("John"))
                .andExpect(jsonPath("$.lastName").value("Doe"))
                .andExpect(jsonPath("$.email")
                        .value("john.doe@example.com"))
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());

        verify(userService).getProfile(userId);
    }


    @Test
    void shouldReturn401WhenJwtReferencesNonexistentUser()
            throws Exception {

        User user = createTestUser();
        String token = generateToken(user);

        Long deletedUserId = user.getId();
        userRepository.deleteById(deletedUserId);
        userRepository.flush();
        createdUserIds.remove(deletedUserId);

        mockMvc.perform(get("/api/users/me")
                        .header(
                                HttpHeaders.AUTHORIZATION,
                                "Bearer " + token
                        ))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(userService);
    }


    @Test
    void shouldOnlyRetrieveAuthenticatedUsersProfile()
            throws Exception {

        // Arrange
        User user = createTestUser();
        Long authenticatedUserId = user.getId();
        String token = generateToken(user);

        UserResponse profile = new UserResponse(
                authenticatedUserId,
                "Jane",
                "Smith",
                "jane.smith@example.com",
                OffsetDateTime.parse("2026-10-13T12:00:00Z")
        );

        when(userService.getProfile(authenticatedUserId))
                .thenReturn(profile);

        // Act & Assert
        mockMvc.perform(get("/api/users/me")
                        .header(
                                HttpHeaders.AUTHORIZATION,
                                "Bearer " + token
                        )
                        .param("userId", "999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(authenticatedUserId));

        verify(userService).getProfile(authenticatedUserId);
        verify(userService, never()).getProfile(999L);
    }

    @Test
    void shouldReturn401WhenUpdatingProfileWithoutJwt()
            throws Exception {

        String request = """
            {
                "firstName": "Jonathan",
                "lastName": "Doe",
                "email": "jonathan@example.com"
            }
            """;

        mockMvc.perform(put("/api/users/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(userService);
    }

    @Test
    void shouldReturn401WhenUpdatingProfileWithInvalidJwt()
            throws Exception {

        String request = """
            {
                "firstName": "Jonathan",
                "lastName": "Doe",
                "email": "jonathan@example.com"
            }
            """;

        mockMvc.perform(put("/api/users/me")
                        .header(
                                HttpHeaders.AUTHORIZATION,
                                "Bearer invalid-jwt-token"
                        )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(userService);
    }

    @Test
    void shouldUpdateProfileWithValidJwt()
            throws Exception {

        // Arrange
        User user = createTestUser();
        Long userId = user.getId();
        String token = generateToken(user);

        UserResponse updatedProfile = new UserResponse(
                userId,
                "Jonathan",
                "Smith",
                "jonathan.smith@example.com",
                OffsetDateTime.parse("2026-10-13T12:00:00Z")
        );

        when(userService.updateProfile(
                eq(userId),
                any(com.marcel.ledgerflow.dto.UpdateProfileRequest.class)
        )).thenReturn(updatedProfile);

        String request = """
            {
                "firstName": "Jonathan",
                "lastName": "Smith",
                "email": "jonathan.smith@example.com"
            }
            """;

        // Act & Assert
        mockMvc.perform(put("/api/users/me")
                        .header(
                                HttpHeaders.AUTHORIZATION,
                                "Bearer " + token
                        )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId))
                .andExpect(jsonPath("$.firstName").value("Jonathan"))
                .andExpect(jsonPath("$.lastName").value("Smith"))
                .andExpect(jsonPath("$.email")
                        .value("jonathan.smith@example.com"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist());

        verify(userService).updateProfile(
                eq(userId),
                any(com.marcel.ledgerflow.dto.UpdateProfileRequest.class)
        );
    }

    @Test
    void shouldOnlyUpdateAuthenticatedUsersProfile()
            throws Exception {

        // Arrange
        User user = createTestUser();
        Long authenticatedUserId = user.getId();
        String token = generateToken(user);

        UserResponse updatedProfile = new UserResponse(
                authenticatedUserId,
                "Jane",
                "Smith",
                "jane.smith@example.com",
                OffsetDateTime.parse("2026-10-13T12:00:00Z")
        );

        when(userService.updateProfile(
                eq(authenticatedUserId),
                any(com.marcel.ledgerflow.dto.UpdateProfileRequest.class)
        )).thenReturn(updatedProfile);

        String request = """
            {
                "firstName": "Jane",
                "lastName": "Smith",
                "email": "jane.smith@example.com"
            }
            """;

        // Act & Assert
        mockMvc.perform(put("/api/users/me")
                        .header(
                                HttpHeaders.AUTHORIZATION,
                                "Bearer " + token
                        )
                        .param("userId", "999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(authenticatedUserId));

        verify(userService).updateProfile(
                eq(authenticatedUserId),
                any(com.marcel.ledgerflow.dto.UpdateProfileRequest.class)
        );

        verify(userService, never()).updateProfile(
                eq(999L),
                any(com.marcel.ledgerflow.dto.UpdateProfileRequest.class)
        );
    }

    @Test
    void shouldReturn401WhenChangingPasswordWithoutJwt()
            throws Exception {

        String request = """
            {
                "currentPassword": "CurrentPass123",
                "newPassword": "NewSecurePass456"
            }
            """;

        mockMvc.perform(put("/api/users/me/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(userService);
    }

    @Test
    void shouldReturn401WhenChangingPasswordWithInvalidJwt()
            throws Exception {

        String request = """
            {
                "currentPassword": "CurrentPass123",
                "newPassword": "NewSecurePass456"
            }
            """;

        mockMvc.perform(put("/api/users/me/password")
                        .header(
                                HttpHeaders.AUTHORIZATION,
                                "Bearer invalid-jwt-token"
                        )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(userService);
    }

    @Test
    void shouldReturn204WhenChangingPasswordWithValidJwt()
            throws Exception {

        // Arrange
        User user = createTestUser();
        Long userId = user.getId();
        String token = generateToken(user);

        String request = """
            {
                "currentPassword": "CurrentPass123",
                "newPassword": "NewSecurePass456"
            }
            """;

        // Act & Assert
        mockMvc.perform(put("/api/users/me/password")
                        .header(
                                HttpHeaders.AUTHORIZATION,
                                "Bearer " + token
                        )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(userService).changePassword(
                eq(userId),
                any(com.marcel.ledgerflow.dto.ChangePasswordRequest.class)
        );
    }

    @Test
    void shouldOnlyChangeAuthenticatedUsersPassword()
            throws Exception {

        // Arrange
        User user = createTestUser();
        Long authenticatedUserId = user.getId();
        String token = generateToken(user);

        String request = """
            {
                "currentPassword": "CurrentPass123",
                "newPassword": "NewSecurePass456"
            }
            """;

        // Act & Assert
        mockMvc.perform(put("/api/users/me/password")
                        .header(
                                HttpHeaders.AUTHORIZATION,
                                "Bearer " + token
                        )
                        .param("userId", "999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isNoContent());

        verify(userService).changePassword(
                eq(authenticatedUserId),
                any(com.marcel.ledgerflow.dto.ChangePasswordRequest.class)
        );

        verify(userService, never()).changePassword(
                eq(999L),
                any(com.marcel.ledgerflow.dto.ChangePasswordRequest.class)
        );
    }

}
