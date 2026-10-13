
package com.marcel.ledgerflow.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marcel.ledgerflow.dto.RegisterRequest;
import com.marcel.ledgerflow.dto.UserResponse;
import com.marcel.ledgerflow.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.OffsetDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.marcel.ledgerflow.exception.EmailAlreadyExistsException;
import com.marcel.ledgerflow.exception.GlobalExceptionHandler;

import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.junit.jupiter.api.AfterEach;

class UserControllerTest {

    private MockMvc mockMvc;
    private UserService userService;
    private ObjectMapper objectMapper;
    private LocalValidatorFactoryBean validator;



    @BeforeEach
    void setUp() {
        userService = mock(UserService.class);

        UserController userController = new UserController(userService);

        validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders
                .standaloneSetup(userController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();

        objectMapper = new ObjectMapper();
    }


    @AfterEach
    void tearDown() {
        validator.close();
    }




    @Test
    void shouldReturn201WhenUserRegistersSuccessfully() throws Exception {
        // Arrange
        RegisterRequest request = new RegisterRequest(
                "John",
                "Doe",
                "john.doe@example.com",
                "SecurePass123"
        );

        UserResponse response = new UserResponse(
                1L,
                "John",
                "Doe",
                "john.doe@example.com",
                OffsetDateTime.parse("2026-10-13T12:00:00Z")
        );

        when(userService.registerUser(any(RegisterRequest.class)))
                .thenReturn(response);

        // Act & Assert
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.firstName").value("John"))
                .andExpect(jsonPath("$.lastName").value("Doe"))
                .andExpect(jsonPath("$.email").value("john.doe@example.com"))
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());

        verify(userService).registerUser(any(RegisterRequest.class));
    }


    @Test
    void shouldReturn409WhenEmailAlreadyExists() throws Exception {
        // Arrange
        RegisterRequest request = new RegisterRequest(
                "John",
                "Doe",
                "john.doe@example.com",
                "SecurePass123"
        );

        when(userService.registerUser(any(RegisterRequest.class)))
                .thenThrow(new EmailAlreadyExistsException(
                        "An account with this email already exists"
                ));

        // Act & Assert
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error")
                        .value("An account with this email already exists"));

        verify(userService).registerUser(any(RegisterRequest.class));
    }


    @Test
    void shouldReturn400WhenEmailIsInvalid() throws Exception {
        // Arrange
        RegisterRequest request = new RegisterRequest(
                "John",
                "Doe",
                "not-an-email",
                "SecurePass123"
        );

        // Act & Assert
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());

        verifyNoInteractions(userService);
    }


    @Test
    void shouldReturn400WhenPasswordIsTooShort() throws Exception {
        RegisterRequest request = new RegisterRequest(
                "John",
                "Doe",
                "john.doe@example.com",
                "123"
        );

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());

        verifyNoInteractions(userService);
    }


    @Test
    void shouldReturn400WhenFirstNameIsBlank() throws Exception {
        RegisterRequest request = new RegisterRequest(
                "",
                "Doe",
                "john.doe@example.com",
                "SecurePass123"
        );

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());

        verifyNoInteractions(userService);
    }





}
