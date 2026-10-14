package com.marcel.ledgerflow.repository;

import com.marcel.ledgerflow.entity.Account;
import com.marcel.ledgerflow.entity.AccountType;
import com.marcel.ledgerflow.entity.Transaction;
import com.marcel.ledgerflow.entity.TransactionType;
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

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Testcontainers
@Transactional
class TransactionRepositoryTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("ledgerflow_test")
                    .withUsername("test")
                    .withPassword("test");

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    void shouldReturnTransactionsForAccountOrderedByNewestDate() {

        User user = createUser("transactions@example.com");

        Account checking = createAccount(user, "Checking");
        Account savings = createAccount(user, "Savings");

        createTransaction(
                checking,
                TransactionType.EXPENSE,
                "Groceries",
                "85.00",
                LocalDate.of(2026, 10, 10)
        );

        createTransaction(
                checking,
                TransactionType.INCOME,
                "Paycheck",
                "2500.00",
                LocalDate.of(2026, 10, 14)
        );

        createTransaction(
                savings,
                TransactionType.INCOME,
                "Interest",
                "5.00",
                LocalDate.of(2026, 10, 12)
        );

        List<Transaction> results =
                transactionRepository
                        .findAllByAccountIdOrderByTransactionDateDesc(
                                checking.getId()
                        );

        assertEquals(2, results.size());
        assertEquals("Paycheck", results.get(0).getDescription());
        assertEquals("Groceries", results.get(1).getDescription());
    }

    @Test
    void shouldNotReturnTransactionForDifferentAccount() {

        User user = createUser("account-isolation@example.com");

        Account checking = createAccount(user, "Checking");
        Account savings = createAccount(user, "Savings");

        Transaction transaction = createTransaction(
                checking,
                TransactionType.EXPENSE,
                "Gas",
                "40.00",
                LocalDate.of(2026, 10, 13)
        );

        assertTrue(
                transactionRepository
                        .findByIdAndAccountId(
                                transaction.getId(),
                                savings.getId()
                        )
                        .isEmpty()
        );

        assertTrue(
                transactionRepository
                        .findByIdAndAccountId(
                                transaction.getId(),
                                checking.getId()
                        )
                        .isPresent()
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

    private Account createAccount(User user, String name) {

        Account account = new Account();
        account.setUser(user);
        account.setName(name);
        account.setType(AccountType.CHECKING);
        account.setInitialBalance(BigDecimal.ZERO);

        return accountRepository.saveAndFlush(account);
    }

    private Transaction createTransaction(
            Account account,
            TransactionType type,
            String description,
            String amount,
            LocalDate date
    ) {

        Transaction transaction = new Transaction();
        transaction.setAccount(account);
        transaction.setType(type);
        transaction.setDescription(description);
        transaction.setAmount(new BigDecimal(amount));
        transaction.setTransactionDate(date);

        return transactionRepository.saveAndFlush(transaction);
    }
}