package com.marcel.ledgerflow.service;

import com.marcel.ledgerflow.dto.CreateTransactionRequest;
import com.marcel.ledgerflow.dto.TransactionResponse;
import com.marcel.ledgerflow.entity.Account;
import com.marcel.ledgerflow.entity.Transaction;
import com.marcel.ledgerflow.enums.TransactionType;
import com.marcel.ledgerflow.exception.AccountNotFoundException;
import com.marcel.ledgerflow.repository.AccountRepository;
import com.marcel.ledgerflow.repository.TransactionRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import java.util.List;
import com.marcel.ledgerflow.exception.TransactionNotFoundException;
import com.marcel.ledgerflow.dto.UpdateTransactionRequest;
import com.marcel.ledgerflow.repository.CategoryRepository;

import com.marcel.ledgerflow.entity.Category;
import org.mockito.ArgumentCaptor;
import com.marcel.ledgerflow.exception.CategoryNotFoundException;
import com.marcel.ledgerflow.exception.CategoryTypeMismatchException;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private TransactionService transactionService;

    private Account account;
    private CreateTransactionRequest request;

    @BeforeEach
    void setUp() {

        account = new Account();
        account.setId(10L);

        request = new CreateTransactionRequest(
                TransactionType.EXPENSE,
                new BigDecimal("85.50"),
                "Weekly groceries",
                LocalDate.of(2026, 10, 14),
                null
        );
    }

    @Test
    void shouldCreateTransactionForOwnedAccount() {

        Long userId = 5L;
        Long accountId = 10L;

        when(accountRepository.findByIdAndUserId(accountId, userId))
                .thenReturn(Optional.of(account));

        when(transactionRepository.saveAndFlush(any(Transaction.class)))
                .thenAnswer(invocation -> {
                    Transaction transaction = invocation.getArgument(0);
                    transaction.setId(100L);
                    transaction.setCreatedAt(
                            OffsetDateTime.parse("2026-10-14T09:30:00-05:00")
                    );
                    return transaction;
                });

        TransactionResponse response =
                transactionService.createTransaction(
                        userId,
                        accountId,
                        request
                );

        assertEquals(100L, response.id());
        assertEquals(10L, response.accountId());
        assertEquals(TransactionType.EXPENSE, response.type());
        assertEquals(new BigDecimal("85.50"), response.amount());
        assertEquals("Weekly groceries", response.description());
        assertEquals(LocalDate.of(2026, 10, 14), response.transactionDate());
        assertNotNull(response.createdAt());

        verify(accountRepository)
                .findByIdAndUserId(accountId, userId);

        verify(transactionRepository)
                .saveAndFlush(any(Transaction.class));
    }

    @Test
    void shouldRejectTransactionCreationForUnownedAccount() {

        Long userId = 5L;
        Long accountId = 99L;

        when(accountRepository.findByIdAndUserId(accountId, userId))
                .thenReturn(Optional.empty());

        assertThrows(
                AccountNotFoundException.class,
                () -> transactionService.createTransaction(
                        userId,
                        accountId,
                        request
                )
        );

        verify(transactionRepository, never())
                .saveAndFlush(any(Transaction.class));
    }

    @Test
    void shouldReturnTransactionsForOwnedAccount() {

        Long userId = 5L;
        Long accountId = 10L;

        Transaction paycheck = new Transaction();
        paycheck.setId(101L);
        paycheck.setAccount(account);
        paycheck.setType(TransactionType.INCOME);
        paycheck.setAmount(new BigDecimal("2500.00"));
        paycheck.setDescription("Paycheck");
        paycheck.setTransactionDate(LocalDate.of(2026, 10, 14));

        Transaction groceries = new Transaction();
        groceries.setId(102L);
        groceries.setAccount(account);
        groceries.setType(TransactionType.EXPENSE);
        groceries.setAmount(new BigDecimal("85.50"));
        groceries.setDescription("Weekly groceries");
        groceries.setTransactionDate(LocalDate.of(2026, 10, 12));

        when(accountRepository.findByIdAndUserId(accountId, userId))
                .thenReturn(Optional.of(account));

        when(transactionRepository
                .findAllByAccountIdOrderByTransactionDateDesc(accountId))
                .thenReturn(List.of(paycheck, groceries));

        List<TransactionResponse> responses =
                transactionService.getTransactionsByAccountId(userId, accountId);

        assertEquals(2, responses.size());

        assertEquals(101L, responses.get(0).id());
        assertEquals("Paycheck", responses.get(0).description());
        assertEquals(TransactionType.INCOME, responses.get(0).type());

        assertEquals(102L, responses.get(1).id());
        assertEquals("Weekly groceries", responses.get(1).description());
        assertEquals(TransactionType.EXPENSE, responses.get(1).type());

        verify(accountRepository).findByIdAndUserId(accountId, userId);

        verify(transactionRepository)
                .findAllByAccountIdOrderByTransactionDateDesc(accountId);
    }

    @Test
    void shouldRejectTransactionRetrievalForUnownedAccount() {

        Long userId = 5L;
        Long accountId = 99L;

        when(accountRepository.findByIdAndUserId(accountId, userId))
                .thenReturn(Optional.empty());

        assertThrows(
                AccountNotFoundException.class,
                () -> transactionService.getTransactionsByAccountId(
                        userId,
                        accountId
                )
        );

        verify(transactionRepository, never())
                .findAllByAccountIdOrderByTransactionDateDesc(any());
    }

    @Test
    void shouldReturnTransactionByIdForOwnedAccount() {

        Long userId = 5L;
        Long accountId = 10L;
        Long transactionId = 101L;

        Transaction transaction = new Transaction();
        transaction.setId(transactionId);
        transaction.setAccount(account);
        transaction.setType(TransactionType.EXPENSE);
        transaction.setAmount(new BigDecimal("85.50"));
        transaction.setDescription("Weekly groceries");
        transaction.setTransactionDate(LocalDate.of(2026, 10, 14));

        when(accountRepository.findByIdAndUserId(accountId, userId))
                .thenReturn(Optional.of(account));

        when(transactionRepository.findByIdAndAccountId(transactionId, accountId))
                .thenReturn(Optional.of(transaction));

        TransactionResponse response = transactionService.getTransactionById(
                userId,
                accountId,
                transactionId
        );

        assertEquals(101L, response.id());
        assertEquals(10L, response.accountId());
        assertEquals(TransactionType.EXPENSE, response.type());
        assertEquals(new BigDecimal("85.50"), response.amount());
        assertEquals("Weekly groceries", response.description());
        assertEquals(LocalDate.of(2026, 10, 14), response.transactionDate());

        verify(accountRepository).findByIdAndUserId(accountId, userId);
        verify(transactionRepository).findByIdAndAccountId(transactionId, accountId);
    }

    @Test
    void shouldRejectTransactionByIdForUnownedAccount() {

        Long userId = 5L;
        Long accountId = 99L;
        Long transactionId = 101L;

        when(accountRepository.findByIdAndUserId(accountId, userId))
                .thenReturn(Optional.empty());

        assertThrows(
                AccountNotFoundException.class,
                () -> transactionService.getTransactionById(
                        userId,
                        accountId,
                        transactionId
                )
        );

        verify(transactionRepository, never())
                .findByIdAndAccountId(any(), any());
    }

    @Test
    void shouldRejectTransactionBelongingToDifferentAccount() {

        Long userId = 5L;
        Long accountId = 10L;
        Long transactionId = 999L;

        when(accountRepository.findByIdAndUserId(accountId, userId))
                .thenReturn(Optional.of(account));

        when(transactionRepository.findByIdAndAccountId(transactionId, accountId))
                .thenReturn(Optional.empty());

        assertThrows(
                TransactionNotFoundException.class,
                () -> transactionService.getTransactionById(
                        userId,
                        accountId,
                        transactionId
                )
        );

        verify(transactionRepository)
                .findByIdAndAccountId(transactionId, accountId);
    }

    @Test
    void shouldUpdateTransactionForOwnedAccount() {

        Long userId = 5L;
        Long accountId = 10L;
        Long transactionId = 101L;

        Transaction transaction = new Transaction();
        transaction.setId(transactionId);
        transaction.setAccount(account);
        transaction.setType(TransactionType.EXPENSE);
        transaction.setAmount(new BigDecimal("85.50"));
        transaction.setDescription("Groceries");
        transaction.setTransactionDate(LocalDate.of(2026, 10, 12));

        UpdateTransactionRequest updateRequest = new UpdateTransactionRequest(
                TransactionType.EXPENSE,
                new BigDecimal("95.75"),
                "Groceries and household supplies",
                LocalDate.of(2026, 10, 14),
                null
        );

        when(accountRepository.findByIdAndUserId(accountId, userId))
                .thenReturn(Optional.of(account));

        when(transactionRepository.findByIdAndAccountId(transactionId, accountId))
                .thenReturn(Optional.of(transaction));

        when(transactionRepository.saveAndFlush(any(Transaction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        TransactionResponse response = transactionService.updateTransaction(
                userId,
                accountId,
                transactionId,
                updateRequest
        );

        assertEquals(101L, response.id());
        assertEquals(10L, response.accountId());
        assertEquals(TransactionType.EXPENSE, response.type());
        assertEquals(new BigDecimal("95.75"), response.amount());
        assertEquals("Groceries and household supplies", response.description());
        assertEquals(LocalDate.of(2026, 10, 14), response.transactionDate());

        verify(accountRepository).findByIdAndUserId(accountId, userId);
        verify(transactionRepository).findByIdAndAccountId(transactionId, accountId);
        verify(transactionRepository).saveAndFlush(transaction);
    }

    @Test
    void shouldRejectTransactionUpdateForUnownedAccount() {

        Long userId = 5L;
        Long accountId = 99L;
        Long transactionId = 101L;

        UpdateTransactionRequest updateRequest = new UpdateTransactionRequest(
                TransactionType.EXPENSE,
                new BigDecimal("95.75"),
                "Updated groceries",
                LocalDate.of(2026, 10, 14),
                null
        );

        when(accountRepository.findByIdAndUserId(accountId, userId))
                .thenReturn(Optional.empty());

        assertThrows(
                AccountNotFoundException.class,
                () -> transactionService.updateTransaction(
                        userId,
                        accountId,
                        transactionId,
                        updateRequest
                )
        );

        verify(transactionRepository, never())
                .findByIdAndAccountId(any(), any());

        verify(transactionRepository, never())
                .saveAndFlush(any(Transaction.class));
    }

    @Test
    void shouldRejectTransactionUpdateForDifferentAccount() {

        Long userId = 5L;
        Long accountId = 10L;
        Long transactionId = 999L;

        UpdateTransactionRequest updateRequest = new UpdateTransactionRequest(
                TransactionType.EXPENSE,
                new BigDecimal("95.75"),
                "Updated groceries",
                LocalDate.of(2026, 10, 14),
                null
        );

        when(accountRepository.findByIdAndUserId(accountId, userId))
                .thenReturn(Optional.of(account));

        when(transactionRepository.findByIdAndAccountId(transactionId, accountId))
                .thenReturn(Optional.empty());

        assertThrows(
                TransactionNotFoundException.class,
                () -> transactionService.updateTransaction(
                        userId,
                        accountId,
                        transactionId,
                        updateRequest
                )
        );

        verify(transactionRepository, never())
                .saveAndFlush(any(Transaction.class));
    }

    @Test
    void shouldDeleteTransactionForOwnedAccount() {

        Long userId = 5L;
        Long accountId = 10L;
        Long transactionId = 101L;

        Transaction transaction = new Transaction();
        transaction.setId(transactionId);
        transaction.setAccount(account);

        when(accountRepository.findByIdAndUserId(accountId, userId))
                .thenReturn(Optional.of(account));

        when(transactionRepository.findByIdAndAccountId(transactionId, accountId))
                .thenReturn(Optional.of(transaction));

        transactionService.deleteTransaction(
                userId,
                accountId,
                transactionId
        );

        verify(accountRepository)
                .findByIdAndUserId(accountId, userId);

        verify(transactionRepository)
                .findByIdAndAccountId(transactionId, accountId);

        verify(transactionRepository)
                .delete(transaction);
    }

    @Test
    void shouldRejectTransactionDeletionForUnownedAccount() {

        Long userId = 5L;
        Long accountId = 99L;
        Long transactionId = 101L;

        when(accountRepository.findByIdAndUserId(accountId, userId))
                .thenReturn(Optional.empty());

        assertThrows(
                AccountNotFoundException.class,
                () -> transactionService.deleteTransaction(
                        userId,
                        accountId,
                        transactionId
                )
        );

        verify(transactionRepository, never())
                .findByIdAndAccountId(any(), any());

        verify(transactionRepository, never())
                .delete(any(Transaction.class));
    }

    @Test
    void shouldRejectTransactionDeletionForDifferentAccount() {

        Long userId = 5L;
        Long accountId = 10L;
        Long transactionId = 999L;

        when(accountRepository.findByIdAndUserId(accountId, userId))
                .thenReturn(Optional.of(account));

        when(transactionRepository.findByIdAndAccountId(transactionId, accountId))
                .thenReturn(Optional.empty());

        assertThrows(
                TransactionNotFoundException.class,
                () -> transactionService.deleteTransaction(
                        userId,
                        accountId,
                        transactionId
                )
        );

        verify(transactionRepository, never())
                .delete(any(Transaction.class));
    }

    @Test
    void shouldCreateTransactionWithCategory() {

        // Arrange
        Long userId = 5L;
        Long accountId = 10L;
        Long categoryId = 3L;

        CreateTransactionRequest categorizedRequest =
                new CreateTransactionRequest(
                        TransactionType.EXPENSE,
                        new BigDecimal("85.50"),
                        "Weekly groceries",
                        LocalDate.of(2026, 10, 14),
                        categoryId
                );

        Category category = new Category();
        category.setId(categoryId);
        category.setName("Groceries");
        category.setType(TransactionType.EXPENSE);

        when(accountRepository.findByIdAndUserId(accountId, userId))
                .thenReturn(Optional.of(account));

        when(categoryRepository.findByIdAndUserId(categoryId, userId))
                .thenReturn(Optional.of(category));

        when(transactionRepository.saveAndFlush(any(Transaction.class)))
                .thenAnswer(invocation -> {
                    Transaction transaction = invocation.getArgument(0);
                    transaction.setId(100L);
                    transaction.setCreatedAt(
                            OffsetDateTime.parse("2026-10-14T09:30:00-05:00")
                    );
                    return transaction;
                });

        // Act
        TransactionResponse response =
                transactionService.createTransaction(
                        userId,
                        accountId,
                        categorizedRequest
                );

        // Assert
        assertEquals(100L, response.id());
        assertEquals(categoryId, response.categoryId());

        ArgumentCaptor<Transaction> transactionCaptor =
                ArgumentCaptor.forClass(Transaction.class);

        verify(transactionRepository)
                .saveAndFlush(transactionCaptor.capture());

        Transaction savedTransaction = transactionCaptor.getValue();

        assertNotNull(savedTransaction.getCategory());
        assertEquals(categoryId, savedTransaction.getCategory().getId());
        assertEquals("Groceries", savedTransaction.getCategory().getName());

        verify(categoryRepository)
                .findByIdAndUserId(categoryId, userId);
    }

    @Test
    void shouldRejectTransactionCreationWithAnotherUsersCategory() {

        // Arrange
        Long userId = 5L;
        Long accountId = 10L;
        Long categoryId = 3L;

        CreateTransactionRequest categorizedRequest =
                new CreateTransactionRequest(
                        TransactionType.EXPENSE,
                        new BigDecimal("85.50"),
                        "Weekly groceries",
                        LocalDate.of(2026, 10, 14),
                        categoryId
                );

        when(accountRepository.findByIdAndUserId(accountId, userId))
                .thenReturn(Optional.of(account));

        when(categoryRepository.findByIdAndUserId(categoryId, userId))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(
                CategoryNotFoundException.class,
                () -> transactionService.createTransaction(
                        userId,
                        accountId,
                        categorizedRequest
                )
        );

        verify(accountRepository)
                .findByIdAndUserId(accountId, userId);

        verify(categoryRepository)
                .findByIdAndUserId(categoryId, userId);

        verify(transactionRepository, never())
                .saveAndFlush(any(Transaction.class));
    }

    @Test
    void shouldRejectTransactionCreationWithMismatchedCategoryType() {

        // Arrange
        Long userId = 5L;
        Long accountId = 10L;
        Long categoryId = 7L;

        CreateTransactionRequest categorizedRequest =
                new CreateTransactionRequest(
                        TransactionType.EXPENSE,
                        new BigDecimal("85.50"),
                        "Weekly groceries",
                        LocalDate.of(2026, 10, 14),
                        categoryId
                );

        Category category = new Category();
        category.setId(categoryId);
        category.setName("Salary");
        category.setType(TransactionType.INCOME);

        when(accountRepository.findByIdAndUserId(accountId, userId))
                .thenReturn(Optional.of(account));

        when(categoryRepository.findByIdAndUserId(categoryId, userId))
                .thenReturn(Optional.of(category));

        // Act & Assert
        assertThrows(
                CategoryTypeMismatchException.class,
                () -> transactionService.createTransaction(
                        userId,
                        accountId,
                        categorizedRequest
                )
        );

        verify(accountRepository)
                .findByIdAndUserId(accountId, userId);

        verify(categoryRepository)
                .findByIdAndUserId(categoryId, userId);

        verify(transactionRepository, never())
                .saveAndFlush(any(Transaction.class));
    }

    @Test
    void shouldAssignCategoryWhenUpdatingTransaction() {

        // Arrange
        Long userId = 5L;
        Long accountId = 10L;
        Long transactionId = 100L;
        Long categoryId = 7L;

        Transaction transaction = new Transaction();
        transaction.setId(transactionId);
        transaction.setAccount(account);
        transaction.setType(TransactionType.EXPENSE);
        transaction.setAmount(new BigDecimal("85.50"));
        transaction.setDescription("Weekly groceries");
        transaction.setTransactionDate(LocalDate.of(2026, 10, 14));
        transaction.setCreatedAt(
                OffsetDateTime.parse("2026-10-14T09:30:00-05:00")
        );

        Category category = new Category();
        category.setId(categoryId);
        category.setName("Groceries");
        category.setType(TransactionType.EXPENSE);

        UpdateTransactionRequest request = new UpdateTransactionRequest(
                TransactionType.EXPENSE,
                new BigDecimal("95.00"),
                "Updated grocery shopping",
                LocalDate.of(2026, 10, 15),
                categoryId
        );

        when(accountRepository.findByIdAndUserId(accountId, userId))
                .thenReturn(Optional.of(account));

        when(transactionRepository.findByIdAndAccountId(transactionId, accountId))
                .thenReturn(Optional.of(transaction));

        when(categoryRepository.findByIdAndUserId(categoryId, userId))
                .thenReturn(Optional.of(category));

        when(transactionRepository.saveAndFlush(any(Transaction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        TransactionResponse response = transactionService.updateTransaction(
                userId,
                accountId,
                transactionId,
                request
        );

        // Assert
        assertEquals(categoryId, response.categoryId());
        assertEquals(new BigDecimal("95.00"), response.amount());
        assertEquals("Updated grocery shopping", response.description());

        ArgumentCaptor<Transaction> captor =
                ArgumentCaptor.forClass(Transaction.class);

        verify(transactionRepository).saveAndFlush(captor.capture());

        Transaction savedTransaction = captor.getValue();

        assertNotNull(savedTransaction.getCategory());
        assertEquals(categoryId, savedTransaction.getCategory().getId());

        verify(categoryRepository)
                .findByIdAndUserId(categoryId, userId);
    }

    @Test
    void shouldReplaceCategoryWhenUpdatingTransaction() {

        // Arrange
        Long userId = 5L;
        Long accountId = 10L;
        Long transactionId = 100L;

        Long oldCategoryId = 7L;
        Long newCategoryId = 9L;

        Category oldCategory = new Category();
        oldCategory.setId(oldCategoryId);
        oldCategory.setName("Groceries");
        oldCategory.setType(TransactionType.EXPENSE);

        Category newCategory = new Category();
        newCategory.setId(newCategoryId);
        newCategory.setName("Dining Out");
        newCategory.setType(TransactionType.EXPENSE);

        Transaction transaction = new Transaction();
        transaction.setId(transactionId);
        transaction.setAccount(account);
        transaction.setType(TransactionType.EXPENSE);
        transaction.setAmount(new BigDecimal("85.50"));
        transaction.setDescription("Dinner");
        transaction.setTransactionDate(LocalDate.of(2026, 10, 14));
        transaction.setCreatedAt(
                OffsetDateTime.parse("2026-10-14T09:30:00-05:00")
        );
        transaction.setCategory(oldCategory);

        UpdateTransactionRequest request = new UpdateTransactionRequest(
                TransactionType.EXPENSE,
                new BigDecimal("95.00"),
                "Dinner with friends",
                LocalDate.of(2026, 10, 15),
                newCategoryId
        );

        when(accountRepository.findByIdAndUserId(accountId, userId))
                .thenReturn(Optional.of(account));

        when(transactionRepository.findByIdAndAccountId(transactionId, accountId))
                .thenReturn(Optional.of(transaction));

        when(categoryRepository.findByIdAndUserId(newCategoryId, userId))
                .thenReturn(Optional.of(newCategory));

        when(transactionRepository.saveAndFlush(any(Transaction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        TransactionResponse response = transactionService.updateTransaction(
                userId,
                accountId,
                transactionId,
                request
        );

        // Assert
        assertEquals(newCategoryId, response.categoryId());
        assertEquals(new BigDecimal("95.00"), response.amount());
        assertEquals("Dinner with friends", response.description());

        ArgumentCaptor<Transaction> captor =
                ArgumentCaptor.forClass(Transaction.class);

        verify(transactionRepository).saveAndFlush(captor.capture());

        Transaction savedTransaction = captor.getValue();

        assertNotNull(savedTransaction.getCategory());
        assertEquals(newCategoryId, savedTransaction.getCategory().getId());
        assertEquals("Dining Out", savedTransaction.getCategory().getName());

        verify(categoryRepository)
                .findByIdAndUserId(newCategoryId, userId);
    }

    @Test
    void shouldRemoveCategoryWhenUpdatingTransaction() {

        // Arrange
        Long userId = 5L;
        Long accountId = 10L;
        Long transactionId = 100L;

        Category existingCategory = new Category();
        existingCategory.setId(7L);
        existingCategory.setName("Groceries");
        existingCategory.setType(TransactionType.EXPENSE);

        Transaction transaction = new Transaction();
        transaction.setId(transactionId);
        transaction.setAccount(account);
        transaction.setType(TransactionType.EXPENSE);
        transaction.setAmount(new BigDecimal("85.50"));
        transaction.setDescription("Weekly groceries");
        transaction.setTransactionDate(LocalDate.of(2026, 10, 14));
        transaction.setCreatedAt(
                OffsetDateTime.parse("2026-10-14T09:30:00-05:00")
        );
        transaction.setCategory(existingCategory);

        UpdateTransactionRequest request = new UpdateTransactionRequest(
                TransactionType.EXPENSE,
                new BigDecimal("85.50"),
                "Uncategorized expense",
                LocalDate.of(2026, 10, 14),
                null
        );

        when(accountRepository.findByIdAndUserId(accountId, userId))
                .thenReturn(Optional.of(account));

        when(transactionRepository.findByIdAndAccountId(transactionId, accountId))
                .thenReturn(Optional.of(transaction));

        when(transactionRepository.saveAndFlush(any(Transaction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        TransactionResponse response = transactionService.updateTransaction(
                userId,
                accountId,
                transactionId,
                request
        );

        // Assert
        assertNull(response.categoryId());

        ArgumentCaptor<Transaction> captor =
                ArgumentCaptor.forClass(Transaction.class);

        verify(transactionRepository).saveAndFlush(captor.capture());

        Transaction savedTransaction = captor.getValue();

        assertNull(savedTransaction.getCategory());

        verifyNoInteractions(categoryRepository);
    }

    @Test
    void shouldRejectTransactionUpdateWithAnotherUsersCategory() {

        // Arrange
        Long userId = 5L;
        Long accountId = 10L;
        Long transactionId = 100L;
        Long categoryId = 7L;

        Transaction transaction = new Transaction();
        transaction.setId(transactionId);
        transaction.setAccount(account);
        transaction.setType(TransactionType.EXPENSE);
        transaction.setAmount(new BigDecimal("85.50"));
        transaction.setDescription("Weekly groceries");
        transaction.setTransactionDate(LocalDate.of(2026, 10, 14));

        UpdateTransactionRequest request = new UpdateTransactionRequest(
                TransactionType.EXPENSE,
                new BigDecimal("95.00"),
                "Updated groceries",
                LocalDate.of(2026, 10, 15),
                categoryId
        );

        when(accountRepository.findByIdAndUserId(accountId, userId))
                .thenReturn(Optional.of(account));

        when(transactionRepository.findByIdAndAccountId(transactionId, accountId))
                .thenReturn(Optional.of(transaction));

        // Category 7 is not accessible to User 5
        when(categoryRepository.findByIdAndUserId(categoryId, userId))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(
                CategoryNotFoundException.class,
                () -> transactionService.updateTransaction(
                        userId,
                        accountId,
                        transactionId,
                        request
                )
        );

        verify(categoryRepository)
                .findByIdAndUserId(categoryId, userId);

        verify(transactionRepository, never())
                .saveAndFlush(any(Transaction.class));
    }

    @Test
    void shouldRejectTransactionUpdateWithMismatchedCategoryType() {

        // Arrange
        Long userId = 5L;
        Long accountId = 10L;
        Long transactionId = 100L;
        Long categoryId = 7L;

        Transaction transaction = new Transaction();
        transaction.setId(transactionId);
        transaction.setAccount(account);
        transaction.setType(TransactionType.EXPENSE);
        transaction.setAmount(new BigDecimal("85.50"));
        transaction.setDescription("Weekly groceries");
        transaction.setTransactionDate(LocalDate.of(2026, 10, 14));

        Category category = new Category();
        category.setId(categoryId);
        category.setName("Salary");
        category.setType(TransactionType.INCOME);

        UpdateTransactionRequest request = new UpdateTransactionRequest(
                TransactionType.EXPENSE,
                new BigDecimal("95.00"),
                "Updated groceries",
                LocalDate.of(2026, 10, 15),
                categoryId
        );

        when(accountRepository.findByIdAndUserId(accountId, userId))
                .thenReturn(Optional.of(account));

        when(transactionRepository.findByIdAndAccountId(transactionId, accountId))
                .thenReturn(Optional.of(transaction));

        when(categoryRepository.findByIdAndUserId(categoryId, userId))
                .thenReturn(Optional.of(category));

        // Act & Assert
        assertThrows(
                CategoryTypeMismatchException.class,
                () -> transactionService.updateTransaction(
                        userId,
                        accountId,
                        transactionId,
                        request
                )
        );

        verify(categoryRepository)
                .findByIdAndUserId(categoryId, userId);

        verify(transactionRepository, never())
                .saveAndFlush(any(Transaction.class));
    }
}