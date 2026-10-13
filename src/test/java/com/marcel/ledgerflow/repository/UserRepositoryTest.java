
package com.marcel.ledgerflow.repository;

import com.marcel.ledgerflow.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    void shouldSaveAndFindUserByEmail() {

        // Arrange
        User user = new User();
        user.setEmail("test-" + UUID.randomUUID() + "@example.com");
        user.setPasswordHash("test-hash");
        user.setFirstName("Test");
        user.setLastName("User");

        // Act
        userRepository.saveAndFlush(user);

        Optional<User> result =
                userRepository.findByEmail(user.getEmail());

        // Assert
        assertTrue(result.isPresent());
        assertEquals("Test", result.get().getFirstName());
        assertEquals(user.getEmail(), result.get().getEmail());
    }

    @Test
    void shouldCheckWhetherEmailExists() {

        // Arrange
        User user = new User();
        user.setEmail("exists-" + UUID.randomUUID() + "@example.com");
        user.setPasswordHash("test-hash");
        user.setFirstName("Test");
        user.setLastName("User");

        userRepository.saveAndFlush(user);

        // Act
        boolean exists =
                userRepository.existsByEmail(user.getEmail());

        // Assert
        assertTrue(exists);
    }
}
