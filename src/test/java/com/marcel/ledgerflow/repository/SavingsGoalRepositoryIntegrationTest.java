package com.marcel.ledgerflow.repository;

import com.marcel.ledgerflow.entity.SavingsContribution;
import com.marcel.ledgerflow.entity.SavingsGoal;
import com.marcel.ledgerflow.entity.User;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import jakarta.persistence.EntityManager;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Testcontainers
@Transactional
class SavingsGoalRepositoryIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("ledgerflow_test")
                    .withUsername("test")
                    .withPassword("test");

    @Autowired
    private SavingsGoalRepository savingsGoalRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SavingsContributionRepository savingsContributionRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void findByIdAndUserId_ShouldReturnGoalBelongingToUser() {

        // Arrange
        User user = createUser("savings-owner@example.com");

        SavingsGoal goal = new SavingsGoal();
        goal.setUser(user);
        goal.setName("Emergency Fund");
        goal.setTargetAmount(new BigDecimal("5000.00"));
        goal.setSavedAmount(new BigDecimal("1000.00"));
        goal.setTargetDate(LocalDate.of(2027, 12, 31));

        goal = savingsGoalRepository.saveAndFlush(goal);

        // Act
        Optional<SavingsGoal> result =
                savingsGoalRepository.findByIdAndUserId(
                        goal.getId(),
                        user.getId()
                );

        // Assert
        assertTrue(result.isPresent());

        assertEquals("Emergency Fund", result.get().getName());

        assertEquals(
                0,
                new BigDecimal("5000.00")
                        .compareTo(result.get().getTargetAmount())
        );

        assertEquals(
                0,
                new BigDecimal("1000.00")
                        .compareTo(result.get().getSavedAmount())
        );

        assertEquals(user.getId(), result.get().getUser().getId());
    }

    @Test
    void findByIdAndUserId_ShouldNotReturnAnotherUsersGoal() {

        // Arrange
        User owner = createUser("goal-owner@example.com");
        User otherUser = createUser("other-savings-user@example.com");

        SavingsGoal goal = new SavingsGoal();
        goal.setUser(owner);
        goal.setName("Emergency Fund");
        goal.setTargetAmount(new BigDecimal("5000.00"));
        goal.setSavedAmount(new BigDecimal("1000.00"));

        goal = savingsGoalRepository.saveAndFlush(goal);

        // Act: Another user attempts to retrieve the owner's goal
        Optional<SavingsGoal> result =
                savingsGoalRepository.findByIdAndUserId(
                        goal.getId(),
                        otherUser.getId()
                );

        // Assert
        assertTrue(result.isEmpty());

        // Verify that the actual owner can still retrieve the goal
        Optional<SavingsGoal> ownerResult =
                savingsGoalRepository.findByIdAndUserId(
                        goal.getId(),
                        owner.getId()
                );

        assertTrue(ownerResult.isPresent());
        assertEquals("Emergency Fund", ownerResult.get().getName());
    }

    @Test
    void findByIdAndUserIdForUpdate_ShouldReturnGoalBelongingToUser() {

        // Arrange
        User owner = createUser("locked-goal-owner@example.com");
        User otherUser = createUser("locked-goal-other@example.com");

        SavingsGoal goal = new SavingsGoal();
        goal.setUser(owner);
        goal.setName("New Car Fund");
        goal.setTargetAmount(new BigDecimal("15000.00"));
        goal.setSavedAmount(new BigDecimal("3000.00"));

        goal = savingsGoalRepository.saveAndFlush(goal);

        // Act: Owner retrieves the goal using the locking query
        Optional<SavingsGoal> ownerResult =
                savingsGoalRepository.findByIdAndUserIdForUpdate(
                        goal.getId(),
                        owner.getId()
                );

        // Act: Another user attempts the same lookup
        Optional<SavingsGoal> otherUserResult =
                savingsGoalRepository.findByIdAndUserIdForUpdate(
                        goal.getId(),
                        otherUser.getId()
                );

        // Assert
        assertTrue(ownerResult.isPresent());

        assertEquals("New Car Fund", ownerResult.get().getName());

        assertEquals(
                0,
                new BigDecimal("3000.00")
                        .compareTo(ownerResult.get().getSavedAmount())
        );

        assertTrue(otherUserResult.isEmpty());
    }

    @Test
    void saveContribution_ShouldPersistContributionWithTimestamp() {

        // Arrange
        User user = createUser("contribution-persistence@example.com");

        SavingsGoal goal = new SavingsGoal();
        goal.setUser(user);
        goal.setName("Vacation Fund");
        goal.setTargetAmount(new BigDecimal("3000.00"));
        goal.setSavedAmount(BigDecimal.ZERO);

        goal = savingsGoalRepository.saveAndFlush(goal);

        SavingsContribution contribution = new SavingsContribution();
        contribution.setSavingsGoal(goal);
        contribution.setAmount(new BigDecimal("250.00"));

        // Act
        SavingsContribution savedContribution =
                savingsContributionRepository.saveAndFlush(contribution);

        List<SavingsContribution> contributions =
                savingsContributionRepository
                        .findBySavingsGoalIdOrderByContributedAtDesc(goal.getId());

        // Assert
        assertNotNull(savedContribution.getId());
        assertNotNull(savedContribution.getContributedAt());

        assertEquals(1, contributions.size());

        SavingsContribution retrievedContribution = contributions.get(0);

        assertEquals(savedContribution.getId(), retrievedContribution.getId());

        assertEquals(
                goal.getId(),
                retrievedContribution.getSavingsGoal().getId()
        );

        assertEquals(
                0,
                new BigDecimal("250.00")
                        .compareTo(retrievedContribution.getAmount())
        );

        assertNotNull(retrievedContribution.getContributedAt());
    }

    @Test
    void findBySavingsGoalId_ShouldReturnContributionsNewestFirst() {

        // Arrange
        User user = createUser("contribution-order@example.com");

        SavingsGoal goal = new SavingsGoal();
        goal.setUser(user);
        goal.setName("Emergency Fund");
        goal.setTargetAmount(new BigDecimal("5000.00"));
        goal.setSavedAmount(BigDecimal.ZERO);

        goal = savingsGoalRepository.saveAndFlush(goal);

        SavingsContribution firstContribution = new SavingsContribution();
        firstContribution.setSavingsGoal(goal);
        firstContribution.setAmount(new BigDecimal("100.00"));

        firstContribution =
                savingsContributionRepository.saveAndFlush(firstContribution);

        SavingsContribution secondContribution = new SavingsContribution();
        secondContribution.setSavingsGoal(goal);
        secondContribution.setAmount(new BigDecimal("250.00"));

        secondContribution =
                savingsContributionRepository.saveAndFlush(secondContribution);

        // Give each contribution a distinct database timestamp
        // so ordering is deterministic.
        entityManager.createNativeQuery("""
            UPDATE savings_contributions
            SET contributed_at = :timestamp
            WHERE id = :id
            """)
                .setParameter(
                        "timestamp",
                        java.sql.Timestamp.valueOf(
                                LocalDate.of(2026, 10, 10).atTime(10, 0)
                        )
                )
                .setParameter("id", firstContribution.getId())
                .executeUpdate();

        entityManager.createNativeQuery("""
            UPDATE savings_contributions
            SET contributed_at = :timestamp
            WHERE id = :id
            """)
                .setParameter(
                        "timestamp",
                        java.sql.Timestamp.valueOf(
                                LocalDate.of(2026, 10, 15).atTime(9, 30)
                        )
                )
                .setParameter("id", secondContribution.getId())
                .executeUpdate();

        entityManager.clear();

        // Act
        List<SavingsContribution> results =
                savingsContributionRepository
                        .findBySavingsGoalIdOrderByContributedAtDesc(goal.getId());

        // Assert
        assertEquals(2, results.size());

        assertEquals(
                secondContribution.getId(),
                results.get(0).getId()
        );

        assertEquals(
                firstContribution.getId(),
                results.get(1).getId()
        );

        assertEquals(
                0,
                new BigDecimal("250.00").compareTo(results.get(0).getAmount())
        );

        assertEquals(
                0,
                new BigDecimal("100.00").compareTo(results.get(1).getAmount())
        );
    }

    @Test
    void deleteGoal_ShouldCascadeDeleteContributions() {

        // Arrange
        User user = createUser("cascade-delete@example.com");

        SavingsGoal goal = new SavingsGoal();
        goal.setUser(user);
        goal.setName("Vacation Fund");
        goal.setTargetAmount(new BigDecimal("3000.00"));
        goal.setSavedAmount(new BigDecimal("750.00"));

        goal = savingsGoalRepository.saveAndFlush(goal);

        Long goalId = goal.getId();

        SavingsContribution firstContribution = new SavingsContribution();
        firstContribution.setSavingsGoal(goal);
        firstContribution.setAmount(new BigDecimal("500.00"));

        firstContribution =
                savingsContributionRepository.saveAndFlush(firstContribution);

        SavingsContribution secondContribution = new SavingsContribution();
        secondContribution.setSavingsGoal(goal);
        secondContribution.setAmount(new BigDecimal("250.00"));

        secondContribution =
                savingsContributionRepository.saveAndFlush(secondContribution);

        Long firstContributionId = firstContribution.getId();
        Long secondContributionId = secondContribution.getId();

        // Verify both contributions exist before deletion
        assertEquals(
                2,
                savingsContributionRepository
                        .findBySavingsGoalIdOrderByContributedAtDesc(goalId)
                        .size()
        );

        // Act

// Flush any pending changes and detach the managed entities
        entityManager.flush();
        entityManager.clear();

// Reload the savings goal from PostgreSQL
        SavingsGoal goalToDelete = savingsGoalRepository.findById(goalId)
                .orElseThrow();

// Delete the goal and let PostgreSQL cascade the deletion
        savingsGoalRepository.delete(goalToDelete);
        savingsGoalRepository.flush();

// Clear the persistence context before verifying the results
        entityManager.clear();

        // Assert
        assertFalse(savingsGoalRepository.existsById(goalId));

        assertFalse(
                savingsContributionRepository.existsById(firstContributionId)
        );

        assertFalse(
                savingsContributionRepository.existsById(secondContributionId)
        );

        assertTrue(
                savingsContributionRepository
                        .findBySavingsGoalIdOrderByContributedAtDesc(goalId)
                        .isEmpty()
        );
    }


    private User createUser(String email) {

        User user = new User();
        user.setFirstName("Test");
        user.setLastName("User");
        user.setEmail(email);
        user.setPasswordHash("test-hash");

        return userRepository.saveAndFlush(user);
    }
}