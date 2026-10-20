
package com.marcel.ledgerflow.integration;

import com.marcel.ledgerflow.entity.Account;
import com.marcel.ledgerflow.entity.RecurringTransaction;
import com.marcel.ledgerflow.entity.User;
import com.marcel.ledgerflow.enums.AccountType;
import com.marcel.ledgerflow.enums.RecurringFrequency;
import com.marcel.ledgerflow.enums.TransactionType;
import com.marcel.ledgerflow.repository.AccountRepository;
import com.marcel.ledgerflow.repository.RecurringTransactionRepository;
import org.junit.jupiter.api.AfterEach;
import com.marcel.ledgerflow.repository.TransactionRepository;
import com.marcel.ledgerflow.repository.UserRepository;
import com.marcel.ledgerflow.service.RecurringTransactionProcessorService;
import com.marcel.ledgerflow.entity.Transaction;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;

import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = "ledgerflow.recurring.cron=-")
@Testcontainers
class RecurringTransactionConcurrencyIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("ledgerflow_test")
                    .withUsername("test")
                    .withPassword("test");

    @Autowired
    private RecurringTransactionProcessorService processorService;

    @Autowired
    private RecurringTransactionRepository recurringTransactionRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final List<Long> createdRecurringTransactionIds =
            new ArrayList<>();

    @Test
    void applicationContext_loadsConcurrencyTestDependencies() {

        assertNotNull(processorService);
        assertNotNull(recurringTransactionRepository);
        assertNotNull(transactionRepository);
        assertNotNull(transactionManager);
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
                    "concurrency-test-" + UUID.randomUUID() + "@example.com"
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
    void shouldCreateCommittedRecurringTransactionForConcurrencyTesting() {

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

        assertTrue(savedRecurringTransaction.isActive());
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


    @Test
    void shouldPreventDuplicateTransactionsDuringConcurrentProcessing()
            throws Exception {

        // Arrange - create a committed recurring schedule
        LocalDate occurrenceDate = LocalDate.now().minusDays(1);

        Long recurringTransactionId =
                createCommittedRecurringTransaction(occurrenceDate);

        ExecutorService executor = Executors.newFixedThreadPool(2);

        CountDownLatch readyLatch = new CountDownLatch(2);
        CountDownLatch startLatch = new CountDownLatch(1);

        try {

            // Both workers wait until they are allowed to start
            java.util.concurrent.Callable<Void> processingTask = () -> {

                readyLatch.countDown();

                if (!startLatch.await(10, TimeUnit.SECONDS)) {
                    throw new IllegalStateException(
                            "Timed out waiting to start processing"
                    );
                }

                processorService.processRecurringTransaction(
                        recurringTransactionId
                );

                return null;
            };

            // Act - submit two independent processing requests
            Future<Void> firstExecution =
                    executor.submit(processingTask);

            Future<Void> secondExecution =
                    executor.submit(processingTask);

            // Wait until both threads are ready
            assertTrue(
                    readyLatch.await(10, TimeUnit.SECONDS),
                    "Both processing threads should become ready"
            );

            // Release both threads together
            startLatch.countDown();

            // Wait for both processing operations to complete
            firstExecution.get(30, TimeUnit.SECONDS);
            secondExecution.get(30, TimeUnit.SECONDS);

            // Assert - only one transaction was generated
            List<Transaction> generatedTransactions =
                    transactionRepository.findAll().stream()
                            .filter(transaction ->
                                    transaction.getRecurringTransaction() != null
                                            && transaction.getRecurringTransaction()
                                            .getId()
                                            .equals(recurringTransactionId)
                            )
                            .toList();

            assertEquals(1, generatedTransactions.size());

            assertEquals(
                    occurrenceDate,
                    generatedTransactions.get(0).getTransactionDate()
            );

            // Assert - recurring schedule advanced only once
            RecurringTransaction updatedRecurringTransaction =
                    recurringTransactionRepository.findById(
                            recurringTransactionId
                    ).orElseThrow();

            LocalDate expectedNextOccurrence =
                    com.marcel.ledgerflow.util.RecurringTransactionDateCalculator
                            .calculateNextMonthlyOccurrence(
                                    occurrenceDate,
                                    occurrenceDate
                            );

            assertEquals(
                    expectedNextOccurrence,
                    updatedRecurringTransaction.getNextOccurrenceDate()
            );

            assertTrue(updatedRecurringTransaction.isActive());

        } finally {

            startLatch.countDown();

            executor.shutdownNow();

            assertTrue(
                    executor.awaitTermination(10, TimeUnit.SECONDS),
                    "Processing threads should terminate"
            );
        }
    }




}
