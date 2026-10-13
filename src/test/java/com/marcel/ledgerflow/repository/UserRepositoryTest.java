
package com.marcel.ledgerflow.repository;

import com.marcel.ledgerflow.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
@Testcontainers
class UserRepositoryTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("ledgerflow_test")
                    .withUsername("test")
                    .withPassword("test");

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
