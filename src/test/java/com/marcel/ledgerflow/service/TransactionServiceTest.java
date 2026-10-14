package com.marcel.ledgerflow.service;

import com.marcel.ledgerflow.dto.CreateTransactionRequest;
import com.marcel.ledgerflow.dto.TransactionResponse;
import com.marcel.ledgerflow.entity.Account;
import com.marcel.ledgerflow.entity.Transaction;
import com.marcel.ledgerflow.entity.TransactionType;
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

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private AccountRepository accountRepository;

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
                LocalDate.of(2026, 10, 14)
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
                LocalDate.of(2026, 10, 14)
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
                LocalDate.of(2026, 10, 14)
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
                LocalDate.of(2026, 10, 14)
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
}