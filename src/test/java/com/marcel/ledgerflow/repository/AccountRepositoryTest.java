
package com.marcel.ledgerflow.repository;

import com.marcel.ledgerflow.entity.Account;
import com.marcel.ledgerflow.enums.AccountType;
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
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Testcontainers
@Transactional
class AccountRepositoryTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("ledgerflow_test")
                    .withUsername("test")
                    .withPassword("test");

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    void shouldReturnOnlyAccountsBelongingToUser() {
        // Arrange
        User firstUser = createUser("first@example.com");
        User secondUser = createUser("second@example.com");

        createAccount(firstUser, "Checking", AccountType.CHECKING);
        createAccount(firstUser, "Savings", AccountType.SAVINGS);
        createAccount(secondUser, "Other Checking", AccountType.CHECKING);

        // Act
        List<Account> accounts =
                accountRepository.findAllByUserId(firstUser.getId());

        // Assert
        assertEquals(2, accounts.size());

        assertTrue(accounts.stream()
                .allMatch(account ->
                        account.getUser().getId().equals(firstUser.getId())
                ));
    }

    private User createUser(String email) {
        User user = new User();
        user.setEmail(email);
        user.setPasswordHash("test-hash");
        user.setFirstName("Test");
        user.setLastName("User");

        return userRepository.saveAndFlush(user);
    }

    private Account createAccount(
            User user,
            String name,
            AccountType type
    ) {
        Account account = new Account();
        account.setUser(user);
        account.setName(name);
        account.setType(type);
        account.setInitialBalance(new BigDecimal("100.00"));

        return accountRepository.saveAndFlush(account);
    }


    @Test
    void shouldNotReturnAccountBelongingToAnotherUser() {
        // Arrange
        User firstUser = createUser("owner@example.com");
        User secondUser = createUser("other@example.com");

        Account account = createAccount(
                firstUser,
                "Personal Checking",
                AccountType.CHECKING
        );

        // Act
        Optional<Account> result = accountRepository.findByIdAndUserId(
                account.getId(),
                secondUser.getId()
        );

        // Assert
        assertTrue(result.isEmpty());

        // Verify the actual owner can access the account
        Optional<Account> ownerResult = accountRepository.findByIdAndUserId(
                account.getId(),
                firstUser.getId()
        );

        assertTrue(ownerResult.isPresent());
        assertEquals(account.getId(), ownerResult.get().getId());
    }

}
