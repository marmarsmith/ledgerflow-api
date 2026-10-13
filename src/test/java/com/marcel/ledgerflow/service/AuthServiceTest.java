
package com.marcel.ledgerflow.service;

import com.marcel.ledgerflow.dto.LoginRequest;
import com.marcel.ledgerflow.entity.User;
import com.marcel.ledgerflow.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.marcel.ledgerflow.exception.InvalidCredentialsException;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthService authService;

    @Test
    void shouldAuthenticateUserWithValidCredentials() {
        // Arrange
        LoginRequest request = new LoginRequest(
                "  JOHN.DOE@EXAMPLE.COM  ",
                "SecurePass123"
        );

        User user = new User();
        user.setId(1L);
        user.setEmail("john.doe@example.com");
        user.setPasswordHash("stored-bcrypt-hash");

        when(userRepository.findByEmail("john.doe@example.com"))
                .thenReturn(Optional.of(user));

        when(passwordEncoder.matches(
                "SecurePass123",
                "stored-bcrypt-hash"
        )).thenReturn(true);

        // Act
        User authenticatedUser = authService.authenticate(request);

        // Assert
        assertNotNull(authenticatedUser);
        assertEquals(1L, authenticatedUser.getId());
        assertEquals("john.doe@example.com", authenticatedUser.getEmail());

        verify(userRepository).findByEmail("john.doe@example.com");
        verify(passwordEncoder).matches(
                "SecurePass123",
                "stored-bcrypt-hash"
        );
    }


    @Test
    void shouldRejectIncorrectPassword() {
        // Arrange
        LoginRequest request = new LoginRequest(
                "john.doe@example.com",
                "WrongPassword123"
        );

        User user = new User();
        user.setEmail("john.doe@example.com");
        user.setPasswordHash("stored-bcrypt-hash");

        when(userRepository.findByEmail("john.doe@example.com"))
                .thenReturn(Optional.of(user));

        when(passwordEncoder.matches(
                "WrongPassword123",
                "stored-bcrypt-hash"
        )).thenReturn(false);

        // Act & Assert
        InvalidCredentialsException exception = assertThrows(
                InvalidCredentialsException.class,
                () -> authService.authenticate(request)
        );

        assertEquals(
                "Invalid email or password",
                exception.getMessage()
        );

        verify(userRepository).findByEmail("john.doe@example.com");

        verify(passwordEncoder).matches(
                "WrongPassword123",
                "stored-bcrypt-hash"
        );
    }


    @Test
    void shouldRejectNonexistentEmail() {
        // Arrange
        LoginRequest request = new LoginRequest(
                "unknown@example.com",
                "SecurePass123"
        );

        when(userRepository.findByEmail("unknown@example.com"))
                .thenReturn(Optional.empty());

        // Act & Assert
        InvalidCredentialsException exception = assertThrows(
                InvalidCredentialsException.class,
                () -> authService.authenticate(request)
        );

        assertEquals(
                "Invalid email or password",
                exception.getMessage()
        );

        verify(userRepository).findByEmail("unknown@example.com");

        verifyNoInteractions(passwordEncoder);
    }


}
