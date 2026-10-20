
package com.marcel.ledgerflow.integration;

import com.marcel.ledgerflow.entity.Account;
import com.marcel.ledgerflow.entity.RecurringTransaction;
import com.marcel.ledgerflow.entity.User;
import com.marcel.ledgerflow.enums.AccountType;
import com.marcel.ledgerflow.enums.RecurringFrequency;
import com.marcel.ledgerflow.enums.TransactionType;
import com.marcel.ledgerflow.repository.AccountRepository;
import com.marcel.ledgerflow.repository.RecurringTransactionRepository;
import com.marcel.ledgerflow.repository.TransactionRepository;
import com.marcel.ledgerflow.repository.UserRepository;
import com.marcel.ledgerflow.entity.Transaction;
import java.time.YearMonth;
import java.util.List;

import com.marcel.ledgerflow.service.RecurringTransactionProcessorService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import jakarta.persistence.EntityManager;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;


import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = "ledgerflow.recurring.cron=-")
@Testcontainers
@Transactional
class RecurringTransactionProcessorIntegrationTest {

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
    private AccountRepository accountRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Test
    void applicationContext_loadsProcessorAndRepositories() {

        assertNotNull(processorService);
        assertNotNull(recurringTransactionRepository);
        assertNotNull(transactionRepository);
        assertNotNull(accountRepository);
        assertNotNull(userRepository);
    }

    @Test
    void shouldGenerateTransactionAndAdvanceRecurringSchedule() {

        // Arrange
        LocalDate occurrenceDate = LocalDate.now().minusDays(1);

        RecurringTransaction recurringTransaction =
                createRecurringTransaction(occurrenceDate);

        Long recurringTransactionId = recurringTransaction.getId();

        // Act
        processorService.processRecurringTransaction(recurringTransactionId);

        // Assert - verify generated transaction
        List<Transaction> transactions =
                transactionRepository.findAll().stream()
                        .filter(transaction ->
                                transaction.getRecurringTransaction() != null
                                        && transaction.getRecurringTransaction()
                                        .getId()
                                        .equals(recurringTransactionId)
                        )
                        .toList();

        assertEquals(1, transactions.size());

        Transaction generatedTransaction = transactions.get(0);

        assertEquals(
                "Gym membership",
                generatedTransaction.getDescription()
        );

        assertEquals(
                0,
                new BigDecimal("60.00")
                        .compareTo(generatedTransaction.getAmount())
        );

        assertEquals(
                TransactionType.EXPENSE,
                generatedTransaction.getType()
        );

        assertEquals(
                occurrenceDate,
                generatedTransaction.getTransactionDate()
        );

        // Assert - verify recurring schedule advanced
        RecurringTransaction updatedRecurringTransaction =
                recurringTransactionRepository.findById(
                        recurringTransactionId
                ).orElseThrow();

        LocalDate expectedNextOccurrence =
                YearMonth.from(occurrenceDate)
                        .plusMonths(1)
                        .atDay(
                                Math.min(
                                        occurrenceDate.getDayOfMonth(),
                                        YearMonth.from(occurrenceDate)
                                                .plusMonths(1)
                                                .lengthOfMonth()
                                )
                        );

        assertEquals(
                expectedNextOccurrence,
                updatedRecurringTransaction.getNextOccurrenceDate()
        );

        assertTrue(updatedRecurringTransaction.isActive());
    }

    @Test
    void shouldNotGenerateDuplicateTransactionsWhenProcessedTwice() {

        // Arrange
        LocalDate occurrenceDate = LocalDate.now().minusDays(1);

        RecurringTransaction recurringTransaction =
                createRecurringTransaction(occurrenceDate);

        Long recurringTransactionId = recurringTransaction.getId();

        // Act - process the same recurring schedule twice
        processorService.processRecurringTransaction(recurringTransactionId);
        processorService.processRecurringTransaction(recurringTransactionId);

        // Assert - only one transaction should exist
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

        // Assert - schedule should advance only once
        RecurringTransaction updatedRecurringTransaction =
                recurringTransactionRepository.findById(
                        recurringTransactionId
                ).orElseThrow();

        LocalDate expectedNextOccurrence =
                YearMonth.from(occurrenceDate)
                        .plusMonths(1)
                        .atDay(
                                Math.min(
                                        occurrenceDate.getDayOfMonth(),
                                        YearMonth.from(occurrenceDate)
                                                .plusMonths(1)
                                                .lengthOfMonth()
                                )
                        );

        assertEquals(
                expectedNextOccurrence,
                updatedRecurringTransaction.getNextOccurrenceDate()
        );

        assertTrue(updatedRecurringTransaction.isActive());
    }
    @Test
    void shouldRejectDuplicateRecurringOccurrencesAtDatabaseLevel() {

        // Arrange
        LocalDate occurrenceDate = LocalDate.now().minusDays(1);

        RecurringTransaction recurringTransaction =
                createRecurringTransaction(occurrenceDate);

        Long recurringTransactionId = recurringTransaction.getId();
        Long accountId = recurringTransaction.getAccount().getId();

        // Insert the first transaction directly into PostgreSQL
        jdbcTemplate.update(
                """
                INSERT INTO transactions (
                    account_id,
                    recurring_transaction_id,
                    amount,
                    description,
                    type,
                    transaction_date
                )
                VALUES (?, ?, ?, ?, ?, ?)
                """,
                accountId,
                recurringTransactionId,
                new BigDecimal("60.00"),
                "Gym membership",
                TransactionType.EXPENSE.name(),
                java.sql.Date.valueOf(occurrenceDate)
        );

        // Act + Assert - PostgreSQL must reject the duplicate
        assertThrows(
                DataIntegrityViolationException.class,
                () -> jdbcTemplate.update(
                        """
                        INSERT INTO transactions (
                            account_id,
                            recurring_transaction_id,
                            amount,
                            description,
                            type,
                            transaction_date
                        )
                        VALUES (?, ?, ?, ?, ?, ?)
                        """,
                        accountId,
                        recurringTransactionId,
                        new BigDecimal("60.00"),
                        "Gym membership duplicate",
                        TransactionType.EXPENSE.name(),
                        java.sql.Date.valueOf(occurrenceDate)
                )
        );
    }

    private RecurringTransaction createRecurringTransaction(
            LocalDate occurrenceDate
    ) {


        User user = new User();

        user.setEmail(
                "recurring-test-" + UUID.randomUUID() + "@example.com"
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

        return recurringTransactionRepository.saveAndFlush(
                recurringTransaction
        );
    }


    @Test
    void shouldRollbackGeneratedTransactionAndScheduleUpdateWhenTransactionFails() {

        TransactionTemplate transactionTemplate =
                new TransactionTemplate(transactionManager);

        transactionTemplate.setPropagationBehavior(
                TransactionDefinition.PROPAGATION_REQUIRES_NEW
        );
        // Arrange
        LocalDate occurrenceDate = LocalDate.now().minusDays(1);

        RecurringTransaction recurringTransaction =
                createRecurringTransaction(occurrenceDate);

        Long recurringTransactionId = recurringTransaction.getId();

        // Act - process the schedule, then simulate a failure
        transactionTemplate.executeWithoutResult(status -> {

            processorService.processRecurringTransaction(
                    recurringTransactionId
            );

            // Simulate a failure requiring the transaction to roll back
            status.setRollbackOnly();
        });

        // Clear the persistence context before verifying database state
        entityManager.clear();

        // Assert - generated transaction should not exist
        boolean transactionExists =
                transactionRepository
                        .existsByRecurringTransactionIdAndTransactionDate(
                                recurringTransactionId,
                                occurrenceDate
                        );

        assertFalse(transactionExists);

        // Assert - recurring schedule should remain unchanged
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


}
