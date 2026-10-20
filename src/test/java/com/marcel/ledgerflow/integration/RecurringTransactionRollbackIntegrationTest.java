
package com.marcel.ledgerflow.integration;

import com.marcel.ledgerflow.entity.RecurringTransaction;
import com.marcel.ledgerflow.repository.AccountRepository;
import com.marcel.ledgerflow.repository.RecurringTransactionRepository;
import com.marcel.ledgerflow.repository.TransactionRepository;

import com.marcel.ledgerflow.repository.UserRepository;
import com.marcel.ledgerflow.service.RecurringTransactionProcessorService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import com.marcel.ledgerflow.entity.Account;
import com.marcel.ledgerflow.entity.User;
import com.marcel.ledgerflow.enums.AccountType;
import com.marcel.ledgerflow.enums.RecurringFrequency;
import com.marcel.ledgerflow.enums.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = "ledgerflow.recurring.cron=-")
@Testcontainers
class RecurringTransactionRollbackIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("ledgerflow_test")
                    .withUsername("test")
                    .withPassword("test");

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private RecurringTransactionProcessorService processorService;

    @Autowired
    private RecurringTransactionRepository recurringTransactionRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final List<Long> createdRecurringTransactionIds =
            new ArrayList<>();

    @Test
    void applicationContext_supportsTransactionManagement() {

        assertNotNull(transactionManager);
        assertNotNull(processorService);
        assertNotNull(recurringTransactionRepository);
        assertNotNull(transactionRepository);

        TransactionTemplate transactionTemplate =
                new TransactionTemplate(transactionManager);

        assertNotNull(transactionTemplate);
    }


    private Long createCommittedRecurringTransaction(
            LocalDate occurrenceDate
    ) {

        TransactionTemplate transactionTemplate =
                new TransactionTemplate(transactionManager);

        Long recurringTransactionId =
                transactionTemplate.execute(status -> {

            User user = new User();
            user.setEmail(
                    "rollback-test-" + UUID.randomUUID() + "@example.com"
            );
            user.setPasswordHash("test-password-hash");
            user.setFirstName("Test");
            user.setLastName("User");

            user = userRepository.saveAndFlush(user);

            Account account = new Account();
            account.setUser(user);
            account.setName("Test Checking");
            account.setType(AccountType.CHECKING);
            account.setInitialBalance(new BigDecimal("1000.00"));

            account = accountRepository.saveAndFlush(account);

            RecurringTransaction recurringTransaction =
                    new RecurringTransaction();

            recurringTransaction.setUser(user);
            recurringTransaction.setAccount(account);
            recurringTransaction.setAmount(new BigDecimal("60.00"));
            recurringTransaction.setDescription("Gym membership");
            recurringTransaction.setType(TransactionType.EXPENSE);
            recurringTransaction.setFrequency(RecurringFrequency.MONTHLY);
            recurringTransaction.setStartDate(occurrenceDate);
            recurringTransaction.setNextOccurrenceDate(occurrenceDate);
            recurringTransaction.setActive(true);

            recurringTransaction =
                    recurringTransactionRepository.saveAndFlush(
                            recurringTransaction
                    );

            return recurringTransaction.getId();
        });

        createdRecurringTransactionIds.add(recurringTransactionId);

        return recurringTransactionId;
    }


    @Test
    void shouldCommitRecurringTransactionSetup() {

        // Arrange
        LocalDate occurrenceDate = LocalDate.now().minusDays(1);

        // Act
        Long recurringTransactionId =
                createCommittedRecurringTransaction(occurrenceDate);

        // Assert
        RecurringTransaction savedRecurringTransaction =
                recurringTransactionRepository.findById(
                        recurringTransactionId
                ).orElseThrow();

        assertEquals(
                occurrenceDate,
                savedRecurringTransaction.getNextOccurrenceDate()
        );

        assertEquals(
                "Gym membership",
                savedRecurringTransaction.getDescription()
        );
    }


    @Test
    void shouldRollbackGeneratedTransactionAndScheduleUpdate() {

        // Arrange - create and commit the recurring schedule
        LocalDate occurrenceDate = LocalDate.now().minusDays(1);

        Long recurringTransactionId =
                createCommittedRecurringTransaction(occurrenceDate);

        TransactionTemplate transactionTemplate =
                new TransactionTemplate(transactionManager);

        // Act - process the recurring transaction, then force rollback
        transactionTemplate.executeWithoutResult(status -> {

            processorService.processRecurringTransaction(
                    recurringTransactionId
            );

            // Verify the generated transaction exists before rollback
            boolean transactionExists =
                    transactionRepository
                            .existsByRecurringTransactionIdAndTransactionDate(
                                    recurringTransactionId,
                                    occurrenceDate
                            );

            assertTrue(transactionExists);

            // Simulate a failure requiring rollback
            status.setRollbackOnly();
        });

        // Assert - query PostgreSQL after rollback
        boolean transactionExistsAfterRollback =
                transactionRepository
                        .existsByRecurringTransactionIdAndTransactionDate(
                                recurringTransactionId,
                                occurrenceDate
                        );

        assertFalse(transactionExistsAfterRollback);

        RecurringTransaction unchangedRecurringTransaction =
                recurringTransactionRepository.findById(
                        recurringTransactionId
                ).orElseThrow();

        assertEquals(
                occurrenceDate,
                unchangedRecurringTransaction.getNextOccurrenceDate()
        );

        assertTrue(unchangedRecurringTransaction.isActive());
    }

    @Test
    void shouldAutomaticallyRollbackWhenProcessingTransactionThrowsException() {

        // Arrange - commit the recurring schedule
        LocalDate occurrenceDate = LocalDate.now().minusDays(1);

        Long recurringTransactionId =
                createCommittedRecurringTransaction(occurrenceDate);

        TransactionTemplate transactionTemplate =
                new TransactionTemplate(transactionManager);

        // Act - simulate a failure after processing
        assertThrows(
                IllegalStateException.class,
                () -> transactionTemplate.executeWithoutResult(status -> {

                    processorService.processRecurringTransaction(
                            recurringTransactionId
                    );

                    // Confirm the transaction was generated
                    boolean transactionExists =
                            transactionRepository
                                    .existsByRecurringTransactionIdAndTransactionDate(
                                            recurringTransactionId,
                                            occurrenceDate
                                    );

                    assertTrue(transactionExists);

                    // Simulate a failure before the transaction commits
                    throw new IllegalStateException(
                            "Simulated processing failure"
                    );
                })
        );

        // Assert - no generated transaction should remain
        boolean transactionExistsAfterRollback =
                transactionRepository
                        .existsByRecurringTransactionIdAndTransactionDate(
                                recurringTransactionId,
                                occurrenceDate
                        );

        assertFalse(transactionExistsAfterRollback);

        // Assert - schedule should remain unchanged
        RecurringTransaction unchangedRecurringTransaction =
                recurringTransactionRepository.findById(
                        recurringTransactionId
                ).orElseThrow();

        assertEquals(
                occurrenceDate,
                unchangedRecurringTransaction.getNextOccurrenceDate()
        );

        assertTrue(unchangedRecurringTransaction.isActive());
    }


    @AfterEach
    void cleanupCommittedTestData() {

        TransactionTemplate transactionTemplate =
                new TransactionTemplate(transactionManager);

        transactionTemplate.executeWithoutResult(status -> {

            for (Long recurringTransactionId :
                    createdRecurringTransactionIds) {

                RecurringTransaction recurringTransaction =
                        recurringTransactionRepository
                                .findById(recurringTransactionId)
                                .orElse(null);

                if (recurringTransaction == null) {
                    continue;
                }

                Long accountId =
                        recurringTransaction.getAccount().getId();

                Long userId =
                        recurringTransaction.getUser().getId();

                jdbcTemplate.update(
                        "DELETE FROM transactions WHERE recurring_transaction_id = ?",
                        recurringTransactionId
                );

                jdbcTemplate.update(
                        "DELETE FROM recurring_transactions WHERE id = ?",
                        recurringTransactionId
                );

                jdbcTemplate.update(
                        "DELETE FROM accounts WHERE id = ?",
                        accountId
                );

                jdbcTemplate.update(
                        "DELETE FROM users WHERE id = ?",
                        userId
                );
            }
        });

        createdRecurringTransactionIds.clear();
    }



}
