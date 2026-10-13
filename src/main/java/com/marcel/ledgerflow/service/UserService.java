
package com.marcel.ledgerflow.service;

import com.marcel.ledgerflow.dto.RegisterRequest;
import com.marcel.ledgerflow.dto.UserResponse;
import com.marcel.ledgerflow.entity.User;
import com.marcel.ledgerflow.exception.EmailAlreadyExistsException;
import com.marcel.ledgerflow.repository.UserRepository;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
}
