
package com.marcel.ledgerflow.service;

import com.marcel.ledgerflow.dto.RegisterRequest;
import com.marcel.ledgerflow.dto.UserResponse;
import com.marcel.ledgerflow.entity.User;
import com.marcel.ledgerflow.exception.EmailAlreadyExistsException;
import com.marcel.ledgerflow.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    @Test
    void shouldRegisterUserSuccessfully() {
        // Arrange
        RegisterRequest request = new RegisterRequest(
                "John",
                "Doe",
                "john.doe@example.com",
                "SecurePass123"
        );

        when(userRepository.existsByEmail("john.doe@example.com"))
                .thenReturn(false);

        when(passwordEncoder.encode("SecurePass123"))
                .thenReturn("hashed-password");

        when(userRepository.saveAndFlush(any(User.class)))
                .thenAnswer(invocation -> {
                    User user = invocation.getArgument(0);
                    user.setId(1L);
                    user.setCreatedAt(OffsetDateTime.now());
                    return user;
                });

        // Act
        UserResponse response = userService.registerUser(request);

        // Assert
        assertNotNull(response);
        assertEquals(1L, response.id());
        assertEquals("John", response.firstName());
        assertEquals("Doe", response.lastName());
        assertEquals("john.doe@example.com", response.email());
        assertNotNull(response.createdAt());

        verify(userRepository).existsByEmail("john.doe@example.com");
        verify(passwordEncoder).encode("SecurePass123");
        verify(userRepository).saveAndFlush(any(User.class));
    }


    @Test
    void shouldRejectRegistrationWhenEmailAlreadyExists() {
        // Arrange
        RegisterRequest request = new RegisterRequest(
                "John",
                "Doe",
                "john.doe@example.com",
                "SecurePass123"
        );

        when(userRepository.existsByEmail("john.doe@example.com"))
                .thenReturn(true);

        // Act & Assert
        EmailAlreadyExistsException exception = assertThrows(
                EmailAlreadyExistsException.class,
                () -> userService.registerUser(request)
        );

        assertEquals(
                "An account with this email already exists",
                exception.getMessage()
        );

        verify(userRepository).existsByEmail("john.doe@example.com");

        verifyNoInteractions(passwordEncoder);

        verify(userRepository, never()).saveAndFlush(any(User.class));
    }


    @Test
    void shouldNormalizeEmailBeforeRegisteringUser() {
        // Arrange
        RegisterRequest request = new RegisterRequest(
                "John",
                "Doe",
                "  JOHN.DOE@EXAMPLE.COM  ",
                "SecurePass123"
        );

        when(userRepository.existsByEmail("john.doe@example.com"))
                .thenReturn(false);

        when(passwordEncoder.encode("SecurePass123"))
                .thenReturn("hashed-password");

        when(userRepository.saveAndFlush(any(User.class)))
                .thenAnswer(invocation -> {
                    User user = invocation.getArgument(0);
                    user.setId(2L);
                    user.setCreatedAt(OffsetDateTime.now());
                    return user;
                });

        // Act
        UserResponse response = userService.registerUser(request);

        // Assert
        assertEquals("john.doe@example.com", response.email());

        verify(userRepository).existsByEmail("john.doe@example.com");

        verify(userRepository).saveAndFlush(argThat(user ->
                "john.doe@example.com".equals(user.getEmail())
        ));
    }


    @Test
    void shouldEncodePasswordBeforeSavingUser() {
        // Arrange
        String rawPassword = "SecurePass123";
        String encodedPassword = "$2a$10$mockEncodedPassword";

        RegisterRequest request = new RegisterRequest(
                "John",
                "Doe",
                "password.test@example.com",
                rawPassword
        );

        when(userRepository.existsByEmail("password.test@example.com"))
                .thenReturn(false);

        when(passwordEncoder.encode(rawPassword))
                .thenReturn(encodedPassword);

        when(userRepository.saveAndFlush(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        userService.registerUser(request);

        // Assert
        verify(passwordEncoder).encode(rawPassword);

        verify(userRepository).saveAndFlush(argThat(user ->
                encodedPassword.equals(user.getPasswordHash())
                        && !rawPassword.equals(user.getPasswordHash())
        ));
    }



}
