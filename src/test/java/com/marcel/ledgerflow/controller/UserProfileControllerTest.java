
package com.marcel.ledgerflow.controller;

import com.marcel.ledgerflow.dto.ChangePasswordRequest;
import com.marcel.ledgerflow.dto.UserResponse;
import com.marcel.ledgerflow.service.UserService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import com.marcel.ledgerflow.dto.UpdateProfileRequest;
import com.marcel.ledgerflow.exception.GlobalExceptionHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

class UserProfileControllerTest {

    private MockMvc mockMvc;
    private UserService userService;
    private UserProfileController controller;

    @BeforeEach
    void setUp() {
        userService = mock(UserService.class);
        controller = new UserProfileController(userService);

        LocalValidatorFactoryBean validator =
                new LocalValidatorFactoryBean();

        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders
                .standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();
    }

    private Jwt createJwt(Long userId) {
        Instant now = Instant.now();

        return new Jwt(
                "mock-jwt-token",
                now,
                now.plusSeconds(3600),
                Map.of("alg", "HS256"),
                Map.of("sub", userId.toString())
        );
    }

    @Test
    void shouldReturnAuthenticatedUsersProfile() {
        // Arrange
        Long userId = 1L;

        UserResponse expected = new UserResponse(
                userId,
                "John",
                "Doe",
                "john.doe@example.com",
                OffsetDateTime.parse("2026-10-13T12:00:00Z")
        );

        when(userService.getProfile(userId))
                .thenReturn(expected);

        // Act
        ResponseEntity<UserResponse> response =
                controller.getProfile(createJwt(userId));

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(userId, response.getBody().id());
        assertEquals("John", response.getBody().firstName());
        assertEquals("Doe", response.getBody().lastName());
        assertEquals("john.doe@example.com", response.getBody().email());

        verify(userService).getProfile(userId);
    }

    @Test
    void shouldUseAuthenticatedJwtSubjectAsUserId() {
        // Arrange
        Long authenticatedUserId = 42L;

        UserResponse expected = new UserResponse(
                authenticatedUserId,
                "Jane",
                "Smith",
                "jane.smith@example.com",
                OffsetDateTime.parse("2026-10-13T12:00:00Z")
        );

        when(userService.getProfile(authenticatedUserId))
                .thenReturn(expected);

        // Act
        controller.getProfile(createJwt(authenticatedUserId));

        // Assert
        verify(userService).getProfile(authenticatedUserId);
        verifyNoMoreInteractions(userService);
    }

    @Test
    void shouldReturn404WhenUserProfileDoesNotExist() {
        // Arrange
        Long userId = 999L;

        when(userService.getProfile(userId))
                .thenThrow(new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "User profile not found"
                ));

        // Act & Assert
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> controller.getProfile(createJwt(userId))
        );

        assertEquals(
                HttpStatus.NOT_FOUND,
                exception.getStatusCode()
        );

        verify(userService).getProfile(userId);
    }

    @Test
    void shouldNotExposePasswordInformation() {
        // Arrange
        Long userId = 1L;

        UserResponse expected = new UserResponse(
                userId,
                "John",
                "Doe",
                "john.doe@example.com",
                OffsetDateTime.parse("2026-10-13T12:00:00Z")
        );

        when(userService.getProfile(userId))
                .thenReturn(expected);

        // Act
        ResponseEntity<UserResponse> response =
                controller.getProfile(createJwt(userId));

        // Assert
        assertNotNull(response.getBody());

        assertFalse(
                response.getBody().getClass()
                        .isRecord()
                        && java.util.Arrays.stream(
                        response.getBody().getClass()
                                .getRecordComponents()
                ).anyMatch(component ->
                        component.getName().equals("password")
                                || component.getName().equals("passwordHash")
                )
        );
    }

    @Test
    void shouldUpdateAuthenticatedUsersProfile() {
        // Arrange
        Long userId = 5L;

        UpdateProfileRequest request = new UpdateProfileRequest(
                "Jonathan",
                "Smith",
                "jonathan.smith@example.com"
        );

        UserResponse expected = new UserResponse(
                userId,
                "Jonathan",
                "Smith",
                "jonathan.smith@example.com",
                OffsetDateTime.parse("2026-10-13T12:00:00Z")
        );

        when(userService.updateProfile(userId, request))
                .thenReturn(expected);

        // Act
        ResponseEntity<UserResponse> response =
                controller.updateProfile(createJwt(userId), request);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Jonathan", response.getBody().firstName());
        assertEquals("Smith", response.getBody().lastName());
        assertEquals(
                "jonathan.smith@example.com",
                response.getBody().email()
        );

        verify(userService).updateProfile(userId, request);
    }

    @Test
    void shouldRejectProfileUpdateWhenFirstNameIsBlank()
            throws Exception {

        String request = """
            {
                "firstName": "",
                "lastName": "Doe",
                "email": "john.doe@example.com"
            }
            """;

        mockMvc.perform(put("/api/users/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(userService);
    }

    @Test
    void shouldRejectProfileUpdateWhenEmailIsInvalid()
            throws Exception {

        String request = """
            {
                "firstName": "John",
                "lastName": "Doe",
                "email": "invalid-email"
            }
            """;

        mockMvc.perform(put("/api/users/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(userService);
    }

    @Test
    void shouldChangePasswordSuccessfully() {
        // Arrange
        Long userId = 5L;

        ChangePasswordRequest request = new ChangePasswordRequest(
                "CurrentPass123",
                "NewSecurePass456"
        );

        // Act
        ResponseEntity<Void> response =
                controller.changePassword(createJwt(userId), request);

        // Assert
        assertEquals(
                HttpStatus.NO_CONTENT,
                response.getStatusCode()
        );

        assertNull(response.getBody());

        verify(userService).changePassword(userId, request);
    }

    @Test
    void shouldRejectChangePasswordWhenNewPasswordIsTooShort()
            throws Exception {

        String request = """
            {
                "currentPassword": "CurrentPass123",
                "newPassword": "123"
            }
            """;

        mockMvc.perform(put("/api/users/me/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(userService);
    }

    @Test
    void shouldRejectChangePasswordWhenCurrentPasswordIsBlank()
            throws Exception {

        String request = """
            {
                "currentPassword": "",
                "newPassword": "NewSecurePass456"
            }
            """;

        mockMvc.perform(put("/api/users/me/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(userService);
    }

}
