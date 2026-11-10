
package com.marcel.ledgerflow.service;

import com.marcel.ledgerflow.dto.RegisterRequest;
import com.marcel.ledgerflow.dto.UpdateProfileRequest;
import com.marcel.ledgerflow.dto.UserResponse;
import com.marcel.ledgerflow.entity.User;
import com.marcel.ledgerflow.exception.EmailAlreadyExistsException;
import com.marcel.ledgerflow.repository.UserRepository;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.hibernate.exception.ConstraintViolationException;
import java.sql.SQLException;
import com.marcel.ledgerflow.dto.ChangePasswordRequest;
import com.marcel.ledgerflow.exception.InvalidCurrentPasswordException;

import java.util.Locale;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public UserResponse registerUser(RegisterRequest request) {

        String email = request.email().trim().toLowerCase();

        if (userRepository.existsByEmail(email)) {
            throw new EmailAlreadyExistsException(
                    "An account with this email already exists"
            );
        }

        User user = new User();
        user.setFirstName(request.firstName().trim());
        user.setLastName(request.lastName().trim());
        user.setEmail(email);

        user.setPasswordHash(
                passwordEncoder.encode(request.password())
        );

        User savedUser = userRepository.saveAndFlush(user);

        return new UserResponse(
                savedUser.getId(),
                savedUser.getFirstName(),
                savedUser.getLastName(),
                savedUser.getEmail(),
                savedUser.getCreatedAt()
        );
    }


    @Transactional(readOnly = true)
    public UserResponse getProfile(Long userId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "User profile not found"
                ));

        return new UserResponse(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.getCreatedAt()
        );
    }


    @Transactional
    public UserResponse updateProfile(
            Long userId,
            UpdateProfileRequest request
    ) {
        // Retrieve the authenticated user
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "User profile not found"
                ));

        // Normalize incoming profile information
        String firstName = request.firstName().trim();
        String lastName = request.lastName().trim();
        String email = request.email()
                .trim()
                .toLowerCase(Locale.ROOT);

        // Check whether the email is being changed
        boolean emailChanged = !user.getEmail().equals(email);

        // Prevent another user from using the same email
        if (emailChanged && userRepository.existsByEmail(email)) {
            throw new EmailAlreadyExistsException(
                    "An account with this email already exists"
            );
        }

        // Update profile information
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setEmail(email);

        // Persist changes
        User updatedUser;


        try {
            updatedUser = userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException exception) {

            if (isEmailUniqueConstraintViolation(exception)) {
                throw new EmailAlreadyExistsException(
                        "An account with this email already exists"
                );
            }

            throw exception;
        }


        // Return updated profile information
        return new UserResponse(
                updatedUser.getId(),
                updatedUser.getFirstName(),
                updatedUser.getLastName(),
                updatedUser.getEmail(),
                updatedUser.getCreatedAt()
        );
    }


    @Transactional
    public void changePassword(
            Long userId,
            ChangePasswordRequest request
    ) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "User profile not found"
                ));

        if (!passwordEncoder.matches(
                request.currentPassword(),
                user.getPasswordHash()
        )) {
            throw new InvalidCurrentPasswordException();
        }

        if (passwordEncoder.matches(
                request.newPassword(),
                user.getPasswordHash()
        )) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "New password must be different from current password"
            );
        }

        String encodedPassword = passwordEncoder.encode(
                request.newPassword()
        );

        user.setPasswordHash(encodedPassword);

        user.setTokenVersion(user.getTokenVersion() + 1);

        userRepository.saveAndFlush(user);
    }



    private boolean isEmailUniqueConstraintViolation(
            DataIntegrityViolationException exception
    ) {
        Throwable cause = exception;

        while (cause != null) {

            if (cause instanceof ConstraintViolationException constraintException) {

                String constraintName =
                        constraintException.getConstraintName();

                SQLException sqlException =
                        constraintException.getSQLException();

                if (sqlException != null
                        && "23505".equals(sqlException.getSQLState())
                        && constraintName != null
                        && constraintName.equalsIgnoreCase("users_email_key")) {
                    return true;
                }
            }

            cause = cause.getCause();
        }

        return false;
    }


}
