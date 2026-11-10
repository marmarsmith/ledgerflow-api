
package com.marcel.ledgerflow.service;

import com.marcel.ledgerflow.dto.RegisterRequest;
import com.marcel.ledgerflow.dto.UserResponse;
import com.marcel.ledgerflow.entity.User;
import com.marcel.ledgerflow.exception.EmailAlreadyExistsException;
import com.marcel.ledgerflow.repository.UserRepository;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import com.marcel.ledgerflow.dto.ChangePasswordRequest;
import com.marcel.ledgerflow.exception.InvalidCurrentPasswordException;

import java.sql.SQLException;
import java.util.Optional;
import com.marcel.ledgerflow.dto.UpdateProfileRequest;
import org.springframework.dao.DataIntegrityViolationException;

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


    @Test
    void shouldGetUserProfileSuccessfully() {
        // Arrange
        Long userId = 1L;
        OffsetDateTime createdAt =
                OffsetDateTime.parse("2026-10-13T12:00:00Z");

        User user = new User();
        user.setId(userId);
        user.setFirstName("John");
        user.setLastName("Doe");
        user.setEmail("john.doe@example.com");
        user.setPasswordHash("hashed-password");
        user.setCreatedAt(createdAt);

        when(userRepository.findById(userId))
                .thenReturn(Optional.of(user));

        // Act
        UserResponse response = userService.getProfile(userId);

        // Assert
        assertNotNull(response);
        assertEquals(userId, response.id());
        assertEquals("John", response.firstName());
        assertEquals("Doe", response.lastName());
        assertEquals("john.doe@example.com", response.email());
        assertEquals(createdAt, response.createdAt());

        verify(userRepository).findById(userId);
        verifyNoMoreInteractions(userRepository);
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void shouldReturn404WhenUserProfileDoesNotExist() {
        // Arrange
        Long userId = 999L;

        when(userRepository.findById(userId))
                .thenReturn(Optional.empty());

        // Act & Assert
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> userService.getProfile(userId)
        );

        assertEquals(
                HttpStatus.NOT_FOUND,
                exception.getStatusCode()
        );

        assertEquals(
                "User profile not found",
                exception.getReason()
        );

        verify(userRepository).findById(userId);
        verifyNoMoreInteractions(userRepository);
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void shouldNotModifyUserWhenRetrievingProfile() {
        // Arrange
        Long userId = 5L;

        User user = new User();
        user.setId(userId);
        user.setFirstName("Jane");
        user.setLastName("Smith");
        user.setEmail("jane.smith@example.com");
        user.setPasswordHash("original-hashed-password");
        user.setCreatedAt(
                OffsetDateTime.parse("2026-10-13T12:00:00Z")
        );

        when(userRepository.findById(userId))
                .thenReturn(Optional.of(user));

        // Act
        userService.getProfile(userId);

        // Assert
        assertEquals("Jane", user.getFirstName());
        assertEquals("Smith", user.getLastName());
        assertEquals("jane.smith@example.com", user.getEmail());
        assertEquals(
                "original-hashed-password",
                user.getPasswordHash()
        );

        verify(userRepository).findById(userId);
        verify(userRepository, never()).save(any(User.class));
        verify(userRepository, never()).saveAndFlush(any(User.class));
        verifyNoInteractions(passwordEncoder);
    }


    @Test
    void shouldUpdateUserProfileSuccessfully() {
        // Arrange
        Long userId = 1L;

        User user = new User();
        user.setId(userId);
        user.setFirstName("John");
        user.setLastName("Doe");
        user.setEmail("john.doe@example.com");

        UpdateProfileRequest request = new UpdateProfileRequest(
                "Jonathan",
                "Smith",
                "jonathan.smith@example.com"
        );

        when(userRepository.findById(userId))
                .thenReturn(Optional.of(user));

        when(userRepository.existsByEmail(
                "jonathan.smith@example.com"))
                .thenReturn(false);

        when(userRepository.saveAndFlush(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        UserResponse response = userService.updateProfile(
                userId,
                request
        );

        // Assert
        assertEquals("Jonathan", response.firstName());
        assertEquals("Smith", response.lastName());
        assertEquals(
                "jonathan.smith@example.com",
                response.email()
        );

        verify(userRepository).findById(userId);
        verify(userRepository)
                .existsByEmail("jonathan.smith@example.com");
        verify(userRepository).saveAndFlush(user);
    }

    @Test
    void shouldAllowUserToKeepExistingEmail() {
        // Arrange
        Long userId = 1L;

        User user = new User();
        user.setId(userId);
        user.setFirstName("John");
        user.setLastName("Doe");
        user.setEmail("john.doe@example.com");

        UpdateProfileRequest request = new UpdateProfileRequest(
                "Jonathan",
                "Doe",
                "john.doe@example.com"
        );

        when(userRepository.findById(userId))
                .thenReturn(Optional.of(user));

        when(userRepository.saveAndFlush(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        UserResponse response = userService.updateProfile(
                userId,
                request
        );

        // Assert
        assertEquals("Jonathan", response.firstName());
        assertEquals(
                "john.doe@example.com",
                response.email()
        );

        verify(userRepository, never())
                .existsByEmail(anyString());

        verify(userRepository).saveAndFlush(user);
    }

    @Test
    void shouldNormalizeProfileInformationBeforeSaving() {
        // Arrange
        Long userId = 1L;

        User user = new User();
        user.setId(userId);
        user.setFirstName("John");
        user.setLastName("Doe");
        user.setEmail("john.doe@example.com");

        UpdateProfileRequest request = new UpdateProfileRequest(
                "  Jonathan  ",
                "  Smith  ",
                "  JONATHAN.SMITH@EXAMPLE.COM  "
        );

        when(userRepository.findById(userId))
                .thenReturn(Optional.of(user));

        when(userRepository.saveAndFlush(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        UserResponse response = userService.updateProfile(
                userId,
                request
        );

        // Assert
        assertEquals("Jonathan", response.firstName());
        assertEquals("Smith", response.lastName());
        assertEquals(
                "jonathan.smith@example.com",
                response.email()
        );

        verify(userRepository).existsByEmail(
                "jonathan.smith@example.com"
        );

        verify(userRepository).saveAndFlush(user);
    }

    @Test
    void shouldRejectProfileUpdateWhenEmailAlreadyExists() {
        // Arrange
        Long userId = 1L;

        User user = new User();
        user.setId(userId);
        user.setFirstName("John");
        user.setLastName("Doe");
        user.setEmail("john.doe@example.com");

        UpdateProfileRequest request = new UpdateProfileRequest(
                "John",
                "Doe",
                "existing@example.com"
        );

        when(userRepository.findById(userId))
                .thenReturn(Optional.of(user));

        when(userRepository.existsByEmail(
                "existing@example.com"))
                .thenReturn(true);

        // Act & Assert
        EmailAlreadyExistsException exception = assertThrows(
                EmailAlreadyExistsException.class,
                () -> userService.updateProfile(userId, request)
        );

        assertEquals(
                "An account with this email already exists",
                exception.getMessage()
        );

        verify(userRepository, never())
                .saveAndFlush(any(User.class));

        // Existing profile must remain unchanged
        assertEquals("John", user.getFirstName());
        assertEquals("Doe", user.getLastName());
        assertEquals("john.doe@example.com", user.getEmail());
    }

    @Test
    void shouldReturn404WhenUpdatingNonexistentUser() {
        // Arrange
        Long userId = 999L;

        UpdateProfileRequest request = new UpdateProfileRequest(
                "John",
                "Doe",
                "john.doe@example.com"
        );

        when(userRepository.findById(userId))
                .thenReturn(Optional.empty());

        // Act & Assert
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> userService.updateProfile(userId, request)
        );

        assertEquals(
                HttpStatus.NOT_FOUND,
                exception.getStatusCode()
        );

        verify(userRepository).findById(userId);
        verify(userRepository, never())
                .saveAndFlush(any(User.class));
    }


    @Test
    void shouldRejectProfileUpdateWhenDatabaseDetectsDuplicateEmail() {
        // Arrange
        Long userId = 1L;

        User user = new User();
        user.setId(userId);
        user.setFirstName("John");
        user.setLastName("Doe");
        user.setEmail("john.doe@example.com");

        UpdateProfileRequest request = new UpdateProfileRequest(
                "Jonathan",
                "Doe",
                "taken@example.com"
        );

        when(userRepository.findById(userId))
                .thenReturn(Optional.of(user));

        when(userRepository.existsByEmail("taken@example.com"))
                .thenReturn(false);

        SQLException sqlException = new SQLException(
                "Duplicate key value violates unique constraint",
                "23505"
        );

        ConstraintViolationException constraintException =
                new ConstraintViolationException(
                        "Unique constraint violation",
                        sqlException,
                        "users_email_key"
                );

        when(userRepository.saveAndFlush(any(User.class)))
                .thenThrow(new DataIntegrityViolationException(
                        "Database constraint violation",
                        constraintException
                ));

        // Act & Assert
        EmailAlreadyExistsException exception = assertThrows(
                EmailAlreadyExistsException.class,
                () -> userService.updateProfile(userId, request)
        );

        assertEquals(
                "An account with this email already exists",
                exception.getMessage()
        );

        verify(userRepository).findById(userId);
        verify(userRepository).existsByEmail("taken@example.com");
        verify(userRepository).saveAndFlush(user);
    }

    @Test
    void shouldNotMisclassifyUnrelatedDatabaseErrorsAsDuplicateEmail() {
        // Arrange
        Long userId = 1L;

        User user = new User();
        user.setId(userId);
        user.setEmail("john.doe@example.com");

        UpdateProfileRequest request = new UpdateProfileRequest(
                "John",
                "Doe",
                "new@example.com"
        );

        when(userRepository.findById(userId))
                .thenReturn(Optional.of(user));

        when(userRepository.saveAndFlush(any(User.class)))
                .thenThrow(new DataIntegrityViolationException(
                        "Unrelated database constraint violation"
                ));

        // Act & Assert
        assertThrows(
                DataIntegrityViolationException.class,
                () -> userService.updateProfile(userId, request)
        );

        verify(userRepository).saveAndFlush(user);
    }

    @Test
    void shouldChangePasswordSuccessfully() {
        // Arrange
        Long userId = 1L;

        User user = new User();
        user.setId(userId);
        user.setPasswordHash("old-hashed-password");

        ChangePasswordRequest request = new ChangePasswordRequest(
                "CurrentPass123",
                "NewSecurePass456"
        );

        when(userRepository.findById(userId))
                .thenReturn(Optional.of(user));

        when(passwordEncoder.matches(
                "CurrentPass123",
                "old-hashed-password"
        )).thenReturn(true);

        when(passwordEncoder.matches(
                "NewSecurePass456",
                "old-hashed-password"
        )).thenReturn(false);

        when(passwordEncoder.encode("NewSecurePass456"))
                .thenReturn("new-hashed-password");

        when(userRepository.saveAndFlush(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        userService.changePassword(userId, request);

        assertEquals(1L, user.getTokenVersion());

        // Assert
        assertEquals(
                "new-hashed-password",
                user.getPasswordHash()
        );

        verify(passwordEncoder).matches(
                "CurrentPass123",
                "old-hashed-password"
        );

        verify(passwordEncoder).encode("NewSecurePass456");
        verify(userRepository).saveAndFlush(user);
    }

    @Test
    void shouldRejectChangePasswordWhenCurrentPasswordIsIncorrect() {
        // Arrange
        Long userId = 1L;

        User user = new User();
        user.setId(userId);
        user.setPasswordHash("original-hashed-password");

        ChangePasswordRequest request = new ChangePasswordRequest(
                "WrongPassword123",
                "NewSecurePass456"
        );

        when(userRepository.findById(userId))
                .thenReturn(Optional.of(user));

        when(passwordEncoder.matches(
                "WrongPassword123",
                "original-hashed-password"
        )).thenReturn(false);

        // Act & Assert
        InvalidCurrentPasswordException exception = assertThrows(
                InvalidCurrentPasswordException.class,
                () -> userService.changePassword(userId, request)
        );

        assertEquals(0L, user.getTokenVersion());
        assertEquals(
                "Current password is incorrect",
                exception.getMessage()
        );

        assertEquals(
                "original-hashed-password",
                user.getPasswordHash()
        );

        verify(passwordEncoder, never()).encode(anyString());
        verify(userRepository, never()).saveAndFlush(any(User.class));
    }

    @Test
    void shouldRejectChangePasswordWhenNewPasswordMatchesCurrentPassword() {
        // Arrange
        Long userId = 1L;

        User user = new User();
        user.setId(userId);
        user.setPasswordHash("original-hashed-password");

        ChangePasswordRequest request = new ChangePasswordRequest(
                "CurrentPass123",
                "CurrentPass123"
        );

        when(userRepository.findById(userId))
                .thenReturn(Optional.of(user));

        when(passwordEncoder.matches(
                "CurrentPass123",
                "original-hashed-password"
        )).thenReturn(true);

        // Act & Assert
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> userService.changePassword(userId, request)
        );

        assertEquals(0L, user.getTokenVersion());
        assertEquals(
                HttpStatus.BAD_REQUEST,
                exception.getStatusCode()
        );

        assertEquals(
                "New password must be different from current password",
                exception.getReason()
        );

        verify(passwordEncoder, never()).encode(anyString());
        verify(userRepository, never()).saveAndFlush(any(User.class));
    }

    @Test
    void shouldReturn404WhenChangingPasswordForNonexistentUser() {
        // Arrange
        Long userId = 999L;

        ChangePasswordRequest request = new ChangePasswordRequest(
                "CurrentPass123",
                "NewSecurePass456"
        );

        when(userRepository.findById(userId))
                .thenReturn(Optional.empty());

        // Act & Assert
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> userService.changePassword(userId, request)
        );
        assertEquals(
                HttpStatus.NOT_FOUND,
                exception.getStatusCode()
        );

        verify(userRepository).findById(userId);
        verifyNoInteractions(passwordEncoder);
        verify(userRepository, never()).saveAndFlush(any(User.class));
    }

    @Test
    void shouldStoreEncodedPasswordInsteadOfRawPassword() {
        // Arrange
        Long userId = 1L;

        User user = new User();
        user.setId(userId);
        user.setPasswordHash("old-hashed-password");

        ChangePasswordRequest request = new ChangePasswordRequest(
                "CurrentPass123",
                "NewSecurePass456"
        );

        when(userRepository.findById(userId))
                .thenReturn(Optional.of(user));

        when(passwordEncoder.matches(
                "CurrentPass123",
                "old-hashed-password"
        )).thenReturn(true);

        when(passwordEncoder.encode("NewSecurePass456"))
                .thenReturn("$2a$10$mockNewEncodedPassword");

        when(userRepository.saveAndFlush(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        userService.changePassword(userId, request);

        // Assert
        assertNotEquals(
                request.newPassword(),
                user.getPasswordHash()
        );

        assertEquals(
                "$2a$10$mockNewEncodedPassword",
                user.getPasswordHash()
        );

        verify(passwordEncoder).encode(request.newPassword());
        verify(userRepository).saveAndFlush(user);
    }



}
