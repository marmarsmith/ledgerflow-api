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

import com.marcel.ledgerflow.entity.Category;
import com.marcel.ledgerflow.repository.CategoryRepository;

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

    @Autowired
    private CategoryRepository categoryRepository;

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

    @Test
    void shouldCalculateNetTransactionAmount() {

        // Arrange
        User user = createUser("balance-calculation@example.com");

        Account checking = createAccount(user, "Checking");

        createTransaction(
                checking,
                TransactionType.INCOME,
                "Paycheck",
                "2500.00",
                LocalDate.of(2026, 10, 14)
        );

        createTransaction(
                checking,
                TransactionType.EXPENSE,
                "Groceries",
                "150.00",
                LocalDate.of(2026, 10, 14)
        );

        createTransaction(
                checking,
                TransactionType.EXPENSE,
                "Utilities",
                "200.00",
                LocalDate.of(2026, 10, 14)
        );

        // Act
        BigDecimal netAmount =
                transactionRepository.calculateNetTransactionAmount(
                        checking.getId(),
                        TransactionType.INCOME
                );

        // Assert
        assertEquals(
                0,
                new BigDecimal("2150.00").compareTo(netAmount)
        );
    }

    @Test
    void shouldReturnZeroWhenAccountHasNoTransactions() {

        // Arrange
        User user = createUser("empty-balance@example.com");

        Account savings = createAccount(user, "Savings");

        // Act
        BigDecimal netAmount =
                transactionRepository.calculateNetTransactionAmount(
                        savings.getId(),
                        TransactionType.INCOME
                );

        // Assert
        assertNotNull(netAmount);

        assertEquals(
                0,
                BigDecimal.ZERO.compareTo(netAmount)
        );
    }

    @Test
    void shouldOnlyCalculateTransactionsForSpecifiedAccount() {

        // Arrange
        User user = createUser("balance-isolation@example.com");

        Account checking = createAccount(user, "Checking");
        Account savings = createAccount(user, "Savings");

        createTransaction(
                checking,
                TransactionType.INCOME,
                "Paycheck",
                "2500.00",
                LocalDate.of(2026, 10, 14)
        );

        createTransaction(
                checking,
                TransactionType.EXPENSE,
                "Groceries",
                "150.00",
                LocalDate.of(2026, 10, 14)
        );

        createTransaction(
                savings,
                TransactionType.INCOME,
                "Savings Deposit",
                "5000.00",
                LocalDate.of(2026, 10, 14)
        );

        // Act
        BigDecimal checkingNet =
                transactionRepository.calculateNetTransactionAmount(
                        checking.getId(),
                        TransactionType.INCOME
                );

        BigDecimal savingsNet =
                transactionRepository.calculateNetTransactionAmount(
                        savings.getId(),
                        TransactionType.INCOME
                );

        // Assert
        assertEquals(
                0,
                new BigDecimal("2350.00").compareTo(checkingNet)
        );

        assertEquals(
                0,
                new BigDecimal("5000.00").compareTo(savingsNet)
        );
    }

    @Test
    void shouldCalculateMonthlyTotalsForSpecifiedUser() {

        // Arrange
        User user = createUser("monthly-report@example.com");
        User otherUser = createUser("other-monthly-user@example.com");

        Account checking = createAccount(user, "Checking");
        Account savings = createAccount(user, "Savings");
        Account otherAccount = createAccount(otherUser, "Other Checking");

        // Income across multiple accounts
        createTransaction(
                checking,
                TransactionType.INCOME,
                "Paycheck",
                "3000.00",
                LocalDate.of(2026, 10, 5)
        );

        createTransaction(
                savings,
                TransactionType.INCOME,
                "Interest",
                "50.00",
                LocalDate.of(2026, 10, 10)
        );

        // Expenses across multiple accounts
        createTransaction(
                checking,
                TransactionType.EXPENSE,
                "Groceries",
                "150.00",
                LocalDate.of(2026, 10, 12)
        );

        createTransaction(
                savings,
                TransactionType.EXPENSE,
                "Utilities",
                "200.00",
                LocalDate.of(2026, 10, 15)
        );

        // Outside the reporting month — should be excluded
        createTransaction(
                checking,
                TransactionType.EXPENSE,
                "November Purchase",
                "500.00",
                LocalDate.of(2026, 11, 1)
        );

        // Another user's transaction — should be excluded
        createTransaction(
                otherAccount,
                TransactionType.EXPENSE,
                "Other User Purchase",
                "1000.00",
                LocalDate.of(2026, 10, 15)
        );

        LocalDate startDate = LocalDate.of(2026, 10, 1);
        LocalDate endDate = LocalDate.of(2026, 11, 1);

        // Act
        BigDecimal totalIncome =
                transactionRepository.calculateTotalByUserAndTypeAndDateRange(
                        user.getId(),
                        TransactionType.INCOME,
                        startDate,
                        endDate
                );

        BigDecimal totalExpenses =
                transactionRepository.calculateTotalByUserAndTypeAndDateRange(
                        user.getId(),
                        TransactionType.EXPENSE,
                        startDate,
                        endDate
                );

        // Assert
        assertEquals(
                0,
                new BigDecimal("3050.00").compareTo(totalIncome)
        );

        assertEquals(
                0,
                new BigDecimal("350.00").compareTo(totalExpenses)
        );
    }

    @Test
    void shouldCalculateMonthlyExpensesGroupedByCategory() {

        // Arrange
        User user = createUser("category-report@example.com");
        User otherUser = createUser("other-category-report@example.com");

        Account checking = createAccount(user, "Checking");
        Account savings = createAccount(user, "Savings");
        Account otherAccount = createAccount(otherUser, "Other Checking");

        Category groceries = new Category();
        groceries.setUser(user);
        groceries.setName("Groceries");
        groceries.setType(TransactionType.EXPENSE);
        groceries = categoryRepository.saveAndFlush(groceries);

        Category diningOut = new Category();
        diningOut.setUser(user);
        diningOut.setName("Dining Out");
        diningOut.setType(TransactionType.EXPENSE);
        diningOut = categoryRepository.saveAndFlush(diningOut);

        // Two grocery expenses across different accounts
        Transaction walmart = createTransaction(
                checking,
                TransactionType.EXPENSE,
                "Walmart",
                "125.00",
                LocalDate.of(2026, 10, 5)
        );
        walmart.setCategory(groceries);
        transactionRepository.saveAndFlush(walmart);

        Transaction kroger = createTransaction(
                savings,
                TransactionType.EXPENSE,
                "Kroger",
                "75.00",
                LocalDate.of(2026, 10, 10)
        );
        kroger.setCategory(groceries);
        transactionRepository.saveAndFlush(kroger);

        // Dining Out expense
        Transaction restaurant = createTransaction(
                checking,
                TransactionType.EXPENSE,
                "Restaurant",
                "80.00",
                LocalDate.of(2026, 10, 12)
        );
        restaurant.setCategory(diningOut);
        transactionRepository.saveAndFlush(restaurant);

        // Uncategorized expense
        createTransaction(
                checking,
                TransactionType.EXPENSE,
                "Miscellaneous",
                "40.00",
                LocalDate.of(2026, 10, 15)
        );

        // Income should not appear in expense breakdown
        createTransaction(
                checking,
                TransactionType.INCOME,
                "Paycheck",
                "3000.00",
                LocalDate.of(2026, 10, 15)
        );

        // Expense outside October should be excluded
        Transaction novemberExpense = createTransaction(
                checking,
                TransactionType.EXPENSE,
                "November Groceries",
                "500.00",
                LocalDate.of(2026, 11, 1)
        );
        novemberExpense.setCategory(groceries);
        transactionRepository.saveAndFlush(novemberExpense);

        // Another user's expense should be excluded
        createTransaction(
                otherAccount,
                TransactionType.EXPENSE,
                "Other User Purchase",
                "1000.00",
                LocalDate.of(2026, 10, 15)
        );

        // Act
        List<CategoryExpenseProjection> results =
                transactionRepository.calculateExpensesByCategory(
                        user.getId(),
                        TransactionType.EXPENSE,
                        LocalDate.of(2026, 10, 1),
                        LocalDate.of(2026, 11, 1)
                );

        // Assert
        assertEquals(3, results.size());

        CategoryExpenseProjection groceryResult = results.stream()
                .filter(result -> "Groceries".equals(result.getCategoryName()))
                .findFirst()
                .orElseThrow();

        assertEquals(groceries.getId(), groceryResult.getCategoryId());
        assertEquals(
                0,
                new BigDecimal("200.00").compareTo(groceryResult.getAmount())
        );

        CategoryExpenseProjection diningResult = results.stream()
                .filter(result -> "Dining Out".equals(result.getCategoryName()))
                .findFirst()
                .orElseThrow();

        assertEquals(diningOut.getId(), diningResult.getCategoryId());
        assertEquals(
                0,
                new BigDecimal("80.00").compareTo(diningResult.getAmount())
        );

        CategoryExpenseProjection uncategorizedResult = results.stream()
                .filter(result -> "Uncategorized".equals(result.getCategoryName()))
                .findFirst()
                .orElseThrow();

        assertNull(uncategorizedResult.getCategoryId());
        assertEquals(
                0,
                new BigDecimal("40.00").compareTo(uncategorizedResult.getAmount())
        );

        BigDecimal total = results.stream()
                .map(CategoryExpenseProjection::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        assertEquals(
                0,
                new BigDecimal("320.00").compareTo(total)
        );
    }
}