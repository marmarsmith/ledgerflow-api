
package com.marcel.ledgerflow.service;

import com.marcel.ledgerflow.dto.RecurringTransactionRequest;
import com.marcel.ledgerflow.dto.RecurringTransactionResponse;
import com.marcel.ledgerflow.entity.Account;
import com.marcel.ledgerflow.entity.RecurringTransaction;
import com.marcel.ledgerflow.entity.User;
import com.marcel.ledgerflow.entity.Category;
import com.marcel.ledgerflow.enums.RecurringFrequency;
import com.marcel.ledgerflow.enums.TransactionType;
import com.marcel.ledgerflow.exception.AccountNotFoundException;
import com.marcel.ledgerflow.exception.CategoryNotFoundException;
import com.marcel.ledgerflow.exception.CategoryTypeMismatchException;
import com.marcel.ledgerflow.repository.AccountRepository;
import com.marcel.ledgerflow.repository.CategoryRepository;
import com.marcel.ledgerflow.repository.RecurringTransactionRepository;
import com.marcel.ledgerflow.repository.UserRepository;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import com.marcel.ledgerflow.exception.RecurringTransactionNotFoundException;

import com.marcel.ledgerflow.util.RecurringTransactionDateCalculator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RecurringTransactionServiceTest {

    @Mock
    private RecurringTransactionRepository recurringTransactionRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private Clock clock;

    @InjectMocks
    private RecurringTransactionService recurringTransactionService;

    @Test
    void createRecurringTransaction_shouldCreateMonthlyRuleSuccessfully() {

        Long userId = 1L;
        Long accountId = 2L;

        User user = new User();
        ReflectionTestUtils.setField(user, "id", userId);

        Account account = new Account();
        ReflectionTestUtils.setField(account, "id", accountId);

        RecurringTransactionRequest request =
                new RecurringTransactionRequest(
                        accountId,
                        null,
                        new BigDecimal("1500.00"),
                        "Monthly Rent",
                        TransactionType.EXPENSE,
                        RecurringFrequency.MONTHLY,
                        LocalDate.of(2026, 11, 1),
                        null
                );

        when(userRepository.findById(userId))
                .thenReturn(Optional.of(user));

        when(accountRepository.findByIdAndUserId(accountId, userId))
                .thenReturn(Optional.of(account));

        when(recurringTransactionRepository.saveAndFlush(
                any(RecurringTransaction.class)
        )).thenAnswer(invocation -> {

            RecurringTransaction recurringTransaction =
                    invocation.getArgument(0);

            ReflectionTestUtils.setField(
                    recurringTransaction,
                    "id",
                    10L
            );

            return recurringTransaction;
        });

        RecurringTransactionResponse response =
                recurringTransactionService.createRecurringTransaction(
                        userId,
                        request
                );

        assertNotNull(response);
        assertEquals(10L, response.id());
        assertEquals(accountId, response.accountId());
        assertNull(response.categoryId());
        assertEquals(
                0,
                new BigDecimal("1500.00")
                        .compareTo(response.amount())
        );
        assertEquals("Monthly Rent", response.description());
        assertEquals(TransactionType.EXPENSE, response.type());
        assertEquals(RecurringFrequency.MONTHLY, response.frequency());
        assertEquals(
                LocalDate.of(2026, 11, 1),
                response.startDate()
        );
        assertEquals(
                LocalDate.of(2026, 11, 1),
                response.nextOccurrenceDate()
        );
        assertNull(response.endDate());
        assertTrue(response.active());

        verify(recurringTransactionRepository)
                .saveAndFlush(any(RecurringTransaction.class));
    }


    @Test
    void createRecurringTransaction_shouldRejectAccountNotOwnedByUser() {

        Long userId = 1L;
        Long accountId = 99L;

        User user = new User();
        ReflectionTestUtils.setField(user, "id", userId);

        RecurringTransactionRequest request =
                new RecurringTransactionRequest(
                        accountId,
                        null,
                        new BigDecimal("1500.00"),
                        "Monthly Rent",
                        TransactionType.EXPENSE,
                        RecurringFrequency.MONTHLY,
                        LocalDate.of(2026, 11, 1),
                        null
                );

        when(userRepository.findById(userId))
                .thenReturn(Optional.of(user));

        when(accountRepository.findByIdAndUserId(accountId, userId))
                .thenReturn(Optional.empty());

        assertThrows(
                AccountNotFoundException.class,
                () -> recurringTransactionService.createRecurringTransaction(
                        userId,
                        request
                )
        );

        verify(recurringTransactionRepository, never())
                .saveAndFlush(any(RecurringTransaction.class));
    }


    @Test
    void createRecurringTransaction_shouldRejectCategoryNotOwnedByUser() {

        Long userId = 1L;
        Long accountId = 2L;
        Long categoryId = 99L;

        User user = new User();
        ReflectionTestUtils.setField(user, "id", userId);

        Account account = new Account();
        ReflectionTestUtils.setField(account, "id", accountId);

        RecurringTransactionRequest request =
                new RecurringTransactionRequest(
                        accountId,
                        categoryId,
                        new BigDecimal("1500.00"),
                        "Monthly Rent",
                        TransactionType.EXPENSE,
                        RecurringFrequency.MONTHLY,
                        LocalDate.of(2026, 11, 1),
                        null
                );

        when(userRepository.findById(userId))
                .thenReturn(Optional.of(user));

        when(accountRepository.findByIdAndUserId(accountId, userId))
                .thenReturn(Optional.of(account));

        when(categoryRepository.findByIdAndUserId(categoryId, userId))
                .thenReturn(Optional.empty());

        assertThrows(
                CategoryNotFoundException.class,
                () -> recurringTransactionService.createRecurringTransaction(
                        userId,
                        request
                )
        );

        verify(recurringTransactionRepository, never())
                .saveAndFlush(any(RecurringTransaction.class));
    }


    @Test
    void createRecurringTransaction_shouldRejectCategoryTypeMismatch() {

        Long userId = 1L;
        Long accountId = 2L;
        Long categoryId = 3L;

        User user = new User();
        ReflectionTestUtils.setField(user, "id", userId);

        Account account = new Account();
        ReflectionTestUtils.setField(account, "id", accountId);

        Category category = new Category();
        ReflectionTestUtils.setField(category, "id", categoryId);
        category.setType(TransactionType.INCOME);

        RecurringTransactionRequest request =
                new RecurringTransactionRequest(
                        accountId,
                        categoryId,
                        new BigDecimal("1500.00"),
                        "Monthly Rent",
                        TransactionType.EXPENSE,
                        RecurringFrequency.MONTHLY,
                        LocalDate.of(2026, 11, 1),
                        null
                );

        when(userRepository.findById(userId))
                .thenReturn(Optional.of(user));

        when(accountRepository.findByIdAndUserId(accountId, userId))
                .thenReturn(Optional.of(account));

        when(categoryRepository.findByIdAndUserId(categoryId, userId))
                .thenReturn(Optional.of(category));

        assertThrows(
                CategoryTypeMismatchException.class,
                () -> recurringTransactionService.createRecurringTransaction(
                        userId,
                        request
                )
        );

        verify(recurringTransactionRepository, never())
                .saveAndFlush(any(RecurringTransaction.class));
    }


    @Test
    void createRecurringTransaction_shouldRejectEndDateBeforeStartDate() {

        Long userId = 1L;
        Long accountId = 2L;

        User user = new User();
        ReflectionTestUtils.setField(user, "id", userId);

        Account account = new Account();
        ReflectionTestUtils.setField(account, "id", accountId);

        RecurringTransactionRequest request =
                new RecurringTransactionRequest(
                        accountId,
                        null,
                        new BigDecimal("1500.00"),
                        "Monthly Rent",
                        TransactionType.EXPENSE,
                        RecurringFrequency.MONTHLY,
                        LocalDate.of(2026, 11, 1),
                        LocalDate.of(2026, 10, 1)
                );

        when(userRepository.findById(userId))
                .thenReturn(Optional.of(user));

        when(accountRepository.findByIdAndUserId(accountId, userId))
                .thenReturn(Optional.of(account));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> recurringTransactionService.createRecurringTransaction(
                        userId,
                        request
                )
        );

        assertEquals(
                "End date cannot be before start date",
                exception.getMessage()
        );

        verify(recurringTransactionRepository, never())
                .saveAndFlush(any(RecurringTransaction.class));
    }


    @Test
    void createRecurringTransaction_shouldCreateRuleWithValidCategory() {

        Long userId = 1L;
        Long accountId = 2L;
        Long categoryId = 3L;

        User user = new User();
        ReflectionTestUtils.setField(user, "id", userId);

        Account account = new Account();
        ReflectionTestUtils.setField(account, "id", accountId);

        Category category = new Category();
        ReflectionTestUtils.setField(category, "id", categoryId);
        category.setType(TransactionType.EXPENSE);

        RecurringTransactionRequest request =
                new RecurringTransactionRequest(
                        accountId,
                        categoryId,
                        new BigDecimal("1500.00"),
                        "Monthly Rent",
                        TransactionType.EXPENSE,
                        RecurringFrequency.MONTHLY,
                        LocalDate.of(2026, 11, 1),
                        null
                );

        when(userRepository.findById(userId))
                .thenReturn(Optional.of(user));

        when(accountRepository.findByIdAndUserId(accountId, userId))
                .thenReturn(Optional.of(account));

        when(categoryRepository.findByIdAndUserId(categoryId, userId))
                .thenReturn(Optional.of(category));

        when(recurringTransactionRepository.saveAndFlush(
                any(RecurringTransaction.class)
        )).thenAnswer(invocation -> {

            RecurringTransaction recurringTransaction =
                    invocation.getArgument(0);

            ReflectionTestUtils.setField(
                    recurringTransaction,
                    "id",
                    10L
            );

            return recurringTransaction;
        });

        RecurringTransactionResponse response =
                recurringTransactionService.createRecurringTransaction(
                        userId,
                        request
                );

        assertNotNull(response);
        assertEquals(10L, response.id());
        assertEquals(accountId, response.accountId());
        assertEquals(categoryId, response.categoryId());

        assertEquals(
                0,
                new BigDecimal("1500.00").compareTo(response.amount())
        );

        assertEquals(TransactionType.EXPENSE, response.type());
        assertEquals(RecurringFrequency.MONTHLY, response.frequency());

        assertEquals(
                LocalDate.of(2026, 11, 1),
                response.nextOccurrenceDate()
        );

        assertTrue(response.active());

        verify(categoryRepository)
                .findByIdAndUserId(categoryId, userId);

        verify(recurringTransactionRepository)
                .saveAndFlush(argThat(recurringTransaction ->
                        recurringTransaction.getCategory() == category
                                && recurringTransaction.getAccount() == account
                                && recurringTransaction.getUser() == user
                ));
    }


    @Test
    void getRecurringTransactions_shouldReturnAllUserRules() {

        Long userId = 1L;
        Long accountId = 2L;

        Account account = new Account();
        ReflectionTestUtils.setField(account, "id", accountId);

        RecurringTransaction rent = new RecurringTransaction();
        ReflectionTestUtils.setField(rent, "id", 10L);

        rent.setAccount(account);
        rent.setAmount(new BigDecimal("1500.00"));
        rent.setDescription("Monthly Rent");
        rent.setType(TransactionType.EXPENSE);
        rent.setFrequency(RecurringFrequency.MONTHLY);
        rent.setStartDate(LocalDate.of(2026, 11, 1));
        rent.setNextOccurrenceDate(LocalDate.of(2026, 11, 1));
        rent.setActive(true);

        RecurringTransaction salary = new RecurringTransaction();
        ReflectionTestUtils.setField(salary, "id", 11L);

        salary.setAccount(account);
        salary.setAmount(new BigDecimal("3000.00"));
        salary.setDescription("Weekly Salary");
        salary.setType(TransactionType.INCOME);
        salary.setFrequency(RecurringFrequency.WEEKLY);
        salary.setStartDate(LocalDate.of(2026, 11, 6));
        salary.setNextOccurrenceDate(LocalDate.of(2026, 11, 6));
        salary.setActive(true);

        when(recurringTransactionRepository.findByUserId(userId))
                .thenReturn(List.of(rent, salary));

        List<RecurringTransactionResponse> responses =
                recurringTransactionService.getRecurringTransactions(userId);

        assertEquals(2, responses.size());

        RecurringTransactionResponse rentResponse = responses.get(0);

        assertEquals(10L, rentResponse.id());
        assertEquals(accountId, rentResponse.accountId());
        assertNull(rentResponse.categoryId());
        assertEquals("Monthly Rent", rentResponse.description());
        assertEquals(RecurringFrequency.MONTHLY, rentResponse.frequency());
        assertEquals(
                0,
                new BigDecimal("1500.00").compareTo(rentResponse.amount())
        );
        assertTrue(rentResponse.active());

        RecurringTransactionResponse salaryResponse = responses.get(1);

        assertEquals(11L, salaryResponse.id());
        assertEquals("Weekly Salary", salaryResponse.description());
        assertEquals(TransactionType.INCOME, salaryResponse.type());
        assertEquals(RecurringFrequency.WEEKLY, salaryResponse.frequency());
        assertEquals(
                LocalDate.of(2026, 11, 6),
                salaryResponse.nextOccurrenceDate()
        );

        verify(recurringTransactionRepository)
                .findByUserId(userId);
    }


    @Test
    void getRecurringTransactions_shouldReturnEmptyListWhenNoRulesExist() {

        Long userId = 1L;

        when(recurringTransactionRepository.findByUserId(userId))
                .thenReturn(List.of());

        List<RecurringTransactionResponse> responses =
                recurringTransactionService.getRecurringTransactions(userId);

        assertNotNull(responses);
        assertTrue(responses.isEmpty());

        verify(recurringTransactionRepository)
                .findByUserId(userId);

        verifyNoInteractions(
                accountRepository,
                categoryRepository,
                userRepository
        );
    }


    @Test
    void getRecurringTransactionById_shouldReturnRuleSuccessfully() {

        Long userId = 1L;
        Long accountId = 2L;
        Long recurringTransactionId = 10L;

        Account account = new Account();
        ReflectionTestUtils.setField(account, "id", accountId);

        RecurringTransaction recurringTransaction =
                new RecurringTransaction();

        ReflectionTestUtils.setField(
                recurringTransaction,
                "id",
                recurringTransactionId
        );

        recurringTransaction.setAccount(account);
        recurringTransaction.setAmount(new BigDecimal("1500.00"));
        recurringTransaction.setDescription("Monthly Rent");
        recurringTransaction.setType(TransactionType.EXPENSE);
        recurringTransaction.setFrequency(RecurringFrequency.MONTHLY);
        recurringTransaction.setStartDate(LocalDate.of(2026, 11, 1));
        recurringTransaction.setNextOccurrenceDate(
                LocalDate.of(2026, 12, 1)
        );
        recurringTransaction.setActive(true);

        when(recurringTransactionRepository.findByIdAndUserId(
                recurringTransactionId,
                userId
        )).thenReturn(Optional.of(recurringTransaction));

        RecurringTransactionResponse response =
                recurringTransactionService.getRecurringTransactionById(
                        userId,
                        recurringTransactionId
                );

        assertNotNull(response);
        assertEquals(recurringTransactionId, response.id());
        assertEquals(accountId, response.accountId());
        assertNull(response.categoryId());

        assertEquals(
                0,
                new BigDecimal("1500.00").compareTo(response.amount())
        );

        assertEquals("Monthly Rent", response.description());
        assertEquals(TransactionType.EXPENSE, response.type());
        assertEquals(RecurringFrequency.MONTHLY, response.frequency());

        assertEquals(
                LocalDate.of(2026, 11, 1),
                response.startDate()
        );

        assertEquals(
                LocalDate.of(2026, 12, 1),
                response.nextOccurrenceDate()
        );

        assertTrue(response.active());

        verify(recurringTransactionRepository)
                .findByIdAndUserId(recurringTransactionId, userId);
    }


    @Test
    void getRecurringTransactionById_shouldRejectMissingOrUnauthorizedRule() {

        Long userId = 1L;
        Long recurringTransactionId = 99L;

        when(recurringTransactionRepository.findByIdAndUserId(
                recurringTransactionId,
                userId
        )).thenReturn(Optional.empty());

        RecurringTransactionNotFoundException exception =
                assertThrows(
                        RecurringTransactionNotFoundException.class,
                        () -> recurringTransactionService
                                .getRecurringTransactionById(
                                        userId,
                                        recurringTransactionId
                                )
                );

        assertEquals(
                "Recurring transaction not found",
                exception.getMessage()
        );

        verify(recurringTransactionRepository)
                .findByIdAndUserId(recurringTransactionId, userId);

        verify(recurringTransactionRepository, never())
                .saveAndFlush(any(RecurringTransaction.class));

        verifyNoInteractions(
                accountRepository,
                categoryRepository,
                userRepository
        );
    }


    @Test
    void updateRecurringTransaction_shouldUpdateRuleSuccessfully() {

        Long userId = 1L;
        Long recurringTransactionId = 10L;
        Long accountId = 2L;

        Account account = new Account();
        ReflectionTestUtils.setField(account, "id", accountId);

        RecurringTransaction recurringTransaction =
                new RecurringTransaction();

        ReflectionTestUtils.setField(
                recurringTransaction,
                "id",
                recurringTransactionId
        );

        recurringTransaction.setAccount(account);
        recurringTransaction.setAmount(new BigDecimal("1000.00"));
        recurringTransaction.setDescription("Old Rent");
        recurringTransaction.setType(TransactionType.EXPENSE);
        recurringTransaction.setFrequency(RecurringFrequency.MONTHLY);
        recurringTransaction.setStartDate(LocalDate.of(2026, 11, 1));
        recurringTransaction.setNextOccurrenceDate(
                LocalDate.of(2026, 12, 1)
        );
        recurringTransaction.setActive(true);

        RecurringTransactionRequest request =
                new RecurringTransactionRequest(
                        accountId,
                        null,
                        new BigDecimal("1500.00"),
                        "Updated Monthly Rent",
                        TransactionType.EXPENSE,
                        RecurringFrequency.MONTHLY,
                        LocalDate.of(2026, 11, 1),
                        null
                );

        when(recurringTransactionRepository.findByIdAndUserId(
                recurringTransactionId,
                userId
        )).thenReturn(Optional.of(recurringTransaction));

        when(accountRepository.findByIdAndUserId(accountId, userId))
                .thenReturn(Optional.of(account));

        when(recurringTransactionRepository.saveAndFlush(
                any(RecurringTransaction.class)
        )).thenAnswer(invocation -> invocation.getArgument(0));

        RecurringTransactionResponse response =
                recurringTransactionService.updateRecurringTransaction(
                        userId,
                        recurringTransactionId,
                        request
                );

        assertNotNull(response);
        assertEquals(recurringTransactionId, response.id());
        assertEquals(accountId, response.accountId());

        assertEquals(
                0,
                new BigDecimal("1500.00").compareTo(response.amount())
        );

        assertEquals("Updated Monthly Rent", response.description());
        assertEquals(TransactionType.EXPENSE, response.type());
        assertEquals(RecurringFrequency.MONTHLY, response.frequency());

        assertEquals(
                LocalDate.of(2026, 11, 1),
                response.startDate()
        );

        assertEquals(
                LocalDate.of(2026, 12, 1),
                response.nextOccurrenceDate()
        );

        assertTrue(response.active());

        verify(recurringTransactionRepository)
                .findByIdAndUserId(recurringTransactionId, userId);

        verify(accountRepository)
                .findByIdAndUserId(accountId, userId);

        verify(recurringTransactionRepository)
                .saveAndFlush(recurringTransaction);
    }


    @Test
    void updateRecurringTransaction_shouldRejectMissingOrUnauthorizedRule() {

        Long userId = 1L;
        Long recurringTransactionId = 99L;

        RecurringTransactionRequest request =
                new RecurringTransactionRequest(
                        2L,
                        null,
                        new BigDecimal("1500.00"),
                        "Monthly Rent",
                        TransactionType.EXPENSE,
                        RecurringFrequency.MONTHLY,
                        LocalDate.of(2026, 11, 1),
                        null
                );

        when(recurringTransactionRepository.findByIdAndUserId(
                recurringTransactionId,
                userId
        )).thenReturn(Optional.empty());

        RecurringTransactionNotFoundException exception =
                assertThrows(
                        RecurringTransactionNotFoundException.class,
                        () -> recurringTransactionService
                                .updateRecurringTransaction(
                                        userId,
                                        recurringTransactionId,
                                        request
                                )
                );

        assertEquals(
                "Recurring transaction not found",
                exception.getMessage()
        );

        verify(recurringTransactionRepository)
                .findByIdAndUserId(recurringTransactionId, userId);

        verify(recurringTransactionRepository, never())
                .saveAndFlush(any(RecurringTransaction.class));

        verifyNoInteractions(
                accountRepository,
                categoryRepository,
                userRepository
        );
    }


    @Test
    void updateRecurringTransaction_shouldRejectAccountNotOwnedByUser() {

        Long userId = 1L;
        Long recurringTransactionId = 10L;
        Long unauthorizedAccountId = 99L;

        Account originalAccount = new Account();
        ReflectionTestUtils.setField(originalAccount, "id", 2L);

        RecurringTransaction recurringTransaction =
                new RecurringTransaction();

        ReflectionTestUtils.setField(
                recurringTransaction,
                "id",
                recurringTransactionId
        );

        recurringTransaction.setAccount(originalAccount);
        recurringTransaction.setAmount(new BigDecimal("1000.00"));
        recurringTransaction.setDescription("Monthly Rent");
        recurringTransaction.setType(TransactionType.EXPENSE);
        recurringTransaction.setFrequency(RecurringFrequency.MONTHLY);
        recurringTransaction.setStartDate(LocalDate.of(2026, 11, 1));
        recurringTransaction.setNextOccurrenceDate(
                LocalDate.of(2026, 12, 1)
        );
        recurringTransaction.setActive(true);

        RecurringTransactionRequest request =
                new RecurringTransactionRequest(
                        unauthorizedAccountId,
                        null,
                        new BigDecimal("1500.00"),
                        "Updated Monthly Rent",
                        TransactionType.EXPENSE,
                        RecurringFrequency.MONTHLY,
                        LocalDate.of(2026, 11, 1),
                        null
                );

        when(recurringTransactionRepository.findByIdAndUserId(
                recurringTransactionId,
                userId
        )).thenReturn(Optional.of(recurringTransaction));

        when(accountRepository.findByIdAndUserId(
                unauthorizedAccountId,
                userId
        )).thenReturn(Optional.empty());

        assertThrows(
                AccountNotFoundException.class,
                () -> recurringTransactionService.updateRecurringTransaction(
                        userId,
                        recurringTransactionId,
                        request
                )
        );

        verify(recurringTransactionRepository)
                .findByIdAndUserId(recurringTransactionId, userId);

        verify(accountRepository)
                .findByIdAndUserId(unauthorizedAccountId, userId);

        verify(recurringTransactionRepository, never())
                .saveAndFlush(any(RecurringTransaction.class));

        assertEquals(2L, recurringTransaction.getAccount().getId());

        assertEquals(
                0,
                new BigDecimal("1000.00").compareTo(
                        recurringTransaction.getAmount()
                )
        );

        assertEquals(
                "Monthly Rent",
                recurringTransaction.getDescription()
        );

        verifyNoInteractions(categoryRepository, userRepository);
    }


    @Test
    void updateRecurringTransaction_shouldRejectCategoryNotOwnedByUser() {

        Long userId = 1L;
        Long recurringTransactionId = 10L;
        Long accountId = 2L;
        Long unauthorizedCategoryId = 99L;

        Account account = new Account();
        ReflectionTestUtils.setField(account, "id", accountId);

        RecurringTransaction recurringTransaction =
                new RecurringTransaction();

        ReflectionTestUtils.setField(
                recurringTransaction,
                "id",
                recurringTransactionId
        );

        recurringTransaction.setAccount(account);
        recurringTransaction.setAmount(new BigDecimal("1000.00"));
        recurringTransaction.setDescription("Monthly Rent");
        recurringTransaction.setType(TransactionType.EXPENSE);
        recurringTransaction.setFrequency(RecurringFrequency.MONTHLY);
        recurringTransaction.setStartDate(LocalDate.of(2026, 11, 1));
        recurringTransaction.setNextOccurrenceDate(
                LocalDate.of(2026, 12, 1)
        );
        recurringTransaction.setActive(true);

        RecurringTransactionRequest request =
                new RecurringTransactionRequest(
                        accountId,
                        unauthorizedCategoryId,
                        new BigDecimal("1500.00"),
                        "Updated Monthly Rent",
                        TransactionType.EXPENSE,
                        RecurringFrequency.MONTHLY,
                        LocalDate.of(2026, 11, 1),
                        null
                );

        when(recurringTransactionRepository.findByIdAndUserId(
                recurringTransactionId,
                userId
        )).thenReturn(Optional.of(recurringTransaction));

        when(accountRepository.findByIdAndUserId(accountId, userId))
                .thenReturn(Optional.of(account));

        when(categoryRepository.findByIdAndUserId(
                unauthorizedCategoryId,
                userId
        )).thenReturn(Optional.empty());

        assertThrows(
                CategoryNotFoundException.class,
                () -> recurringTransactionService.updateRecurringTransaction(
                        userId,
                        recurringTransactionId,
                        request
                )
        );

        verify(recurringTransactionRepository)
                .findByIdAndUserId(recurringTransactionId, userId);

        verify(accountRepository)
                .findByIdAndUserId(accountId, userId);

        verify(categoryRepository)
                .findByIdAndUserId(unauthorizedCategoryId, userId);

        verify(recurringTransactionRepository, never())
                .saveAndFlush(any(RecurringTransaction.class));

        assertEquals(accountId, recurringTransaction.getAccount().getId());

        assertEquals(
                0,
                new BigDecimal("1000.00").compareTo(
                        recurringTransaction.getAmount()
                )
        );

        assertEquals(
                "Monthly Rent",
                recurringTransaction.getDescription()
        );

        assertNull(recurringTransaction.getCategory());

        verifyNoInteractions(userRepository);
    }


    @Test
    void updateRecurringTransaction_shouldRejectCategoryTypeMismatch() {

        Long userId = 1L;
        Long recurringTransactionId = 10L;
        Long accountId = 2L;
        Long categoryId = 3L;

        Account account = new Account();
        ReflectionTestUtils.setField(account, "id", accountId);

        Category category = new Category();
        ReflectionTestUtils.setField(category, "id", categoryId);
        category.setType(TransactionType.INCOME);

        RecurringTransaction recurringTransaction =
                new RecurringTransaction();

        ReflectionTestUtils.setField(
                recurringTransaction,
                "id",
                recurringTransactionId
        );

        recurringTransaction.setAccount(account);
        recurringTransaction.setAmount(new BigDecimal("1000.00"));
        recurringTransaction.setDescription("Monthly Rent");
        recurringTransaction.setType(TransactionType.EXPENSE);
        recurringTransaction.setFrequency(RecurringFrequency.MONTHLY);
        recurringTransaction.setStartDate(LocalDate.of(2026, 11, 1));
        recurringTransaction.setNextOccurrenceDate(
                LocalDate.of(2026, 12, 1)
        );
        recurringTransaction.setActive(true);

        RecurringTransactionRequest request =
                new RecurringTransactionRequest(
                        accountId,
                        categoryId,
                        new BigDecimal("1500.00"),
                        "Updated Monthly Rent",
                        TransactionType.EXPENSE,
                        RecurringFrequency.MONTHLY,
                        LocalDate.of(2026, 11, 1),
                        null
                );

        when(recurringTransactionRepository.findByIdAndUserId(
                recurringTransactionId,
                userId
        )).thenReturn(Optional.of(recurringTransaction));

        when(accountRepository.findByIdAndUserId(accountId, userId))
                .thenReturn(Optional.of(account));

        when(categoryRepository.findByIdAndUserId(categoryId, userId))
                .thenReturn(Optional.of(category));

        assertThrows(
                CategoryTypeMismatchException.class,
                () -> recurringTransactionService.updateRecurringTransaction(
                        userId,
                        recurringTransactionId,
                        request
                )
        );

        verify(recurringTransactionRepository)
                .findByIdAndUserId(recurringTransactionId, userId);

        verify(accountRepository)
                .findByIdAndUserId(accountId, userId);

        verify(categoryRepository)
                .findByIdAndUserId(categoryId, userId);

        verify(recurringTransactionRepository, never())
                .saveAndFlush(any(RecurringTransaction.class));

        assertEquals(
                0,
                new BigDecimal("1000.00").compareTo(
                        recurringTransaction.getAmount()
                )
        );

        assertEquals("Monthly Rent", recurringTransaction.getDescription());
        assertEquals(TransactionType.EXPENSE, recurringTransaction.getType());
        assertNull(recurringTransaction.getCategory());

        verifyNoInteractions(userRepository);
    }


    @Test
    void updateRecurringTransaction_shouldRejectEndDateBeforeStartDate() {

        Long userId = 1L;
        Long recurringTransactionId = 10L;
        Long accountId = 2L;

        Account account = new Account();
        ReflectionTestUtils.setField(account, "id", accountId);

        RecurringTransaction recurringTransaction =
                new RecurringTransaction();

        ReflectionTestUtils.setField(
                recurringTransaction,
                "id",
                recurringTransactionId
        );

        recurringTransaction.setAccount(account);
        recurringTransaction.setAmount(new BigDecimal("1000.00"));
        recurringTransaction.setDescription("Monthly Rent");
        recurringTransaction.setType(TransactionType.EXPENSE);
        recurringTransaction.setFrequency(RecurringFrequency.MONTHLY);
        recurringTransaction.setStartDate(LocalDate.of(2026, 11, 1));
        recurringTransaction.setNextOccurrenceDate(
                LocalDate.of(2026, 12, 1)
        );
        recurringTransaction.setActive(true);

        RecurringTransactionRequest request =
                new RecurringTransactionRequest(
                        accountId,
                        null,
                        new BigDecimal("1500.00"),
                        "Updated Monthly Rent",
                        TransactionType.EXPENSE,
                        RecurringFrequency.MONTHLY,
                        LocalDate.of(2026, 11, 1),
                        LocalDate.of(2026, 10, 1)
                );

        when(recurringTransactionRepository.findByIdAndUserId(
                recurringTransactionId,
                userId
        )).thenReturn(Optional.of(recurringTransaction));

        when(accountRepository.findByIdAndUserId(accountId, userId))
                .thenReturn(Optional.of(account));

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> recurringTransactionService
                                .updateRecurringTransaction(
                                        userId,
                                        recurringTransactionId,
                                        request
                                )
                );

        assertEquals(
                "End date cannot be before start date",
                exception.getMessage()
        );

        verify(recurringTransactionRepository)
                .findByIdAndUserId(recurringTransactionId, userId);

        verify(accountRepository)
                .findByIdAndUserId(accountId, userId);

        verify(recurringTransactionRepository, never())
                .saveAndFlush(any(RecurringTransaction.class));

        assertEquals(
                0,
                new BigDecimal("1000.00").compareTo(
                        recurringTransaction.getAmount()
                )
        );

        assertEquals("Monthly Rent", recurringTransaction.getDescription());
        assertNull(recurringTransaction.getEndDate());

        verifyNoInteractions(categoryRepository, userRepository);
    }



    @Test
    void updateRecurringTransaction_shouldValidateEndDateAgainstPersistedStartDate() {

        Long userId = 1L;
        Long recurringTransactionId = 10L;
        Long accountId = 2L;

        Account account = new Account();
        ReflectionTestUtils.setField(account, "id", accountId);

        RecurringTransaction recurringTransaction =
                new RecurringTransaction();

        ReflectionTestUtils.setField(
                recurringTransaction,
                "id",
                recurringTransactionId
        );

        recurringTransaction.setAccount(account);
        recurringTransaction.setAmount(new BigDecimal("1000.00"));
        recurringTransaction.setDescription("Monthly Rent");
        recurringTransaction.setType(TransactionType.EXPENSE);
        recurringTransaction.setFrequency(RecurringFrequency.MONTHLY);

        // This is the actual start date stored in the database.
        recurringTransaction.setStartDate(LocalDate.of(2026, 11, 1));
        recurringTransaction.setNextOccurrenceDate(
                LocalDate.of(2026, 12, 1)
        );
        recurringTransaction.setActive(true);

        RecurringTransactionRequest request =
                new RecurringTransactionRequest(
                        accountId,
                        null,
                        new BigDecimal("1500.00"),
                        "Updated Monthly Rent",
                        TransactionType.EXPENSE,
                        RecurringFrequency.MONTHLY,

                        // The client supplies an earlier start date.
                        LocalDate.of(2026, 9, 1),

                        // This is after the requested start date,
                        // but before the actual persisted start date.
                        LocalDate.of(2026, 10, 1)
                );

        when(recurringTransactionRepository.findByIdAndUserId(
                recurringTransactionId,
                userId
        )).thenReturn(Optional.of(recurringTransaction));

        when(accountRepository.findByIdAndUserId(accountId, userId))
                .thenReturn(Optional.of(account));

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> recurringTransactionService
                                .updateRecurringTransaction(
                                        userId,
                                        recurringTransactionId,
                                        request
                                )
                );

        assertEquals(
                "End date cannot be before start date",
                exception.getMessage()
        );

        // The original dates must remain unchanged.
        assertEquals(
                LocalDate.of(2026, 11, 1),
                recurringTransaction.getStartDate()
        );

        assertEquals(
                LocalDate.of(2026, 12, 1),
                recurringTransaction.getNextOccurrenceDate()
        );

        assertNull(recurringTransaction.getEndDate());

        // No updates should be saved.
        verify(recurringTransactionRepository, never())
                .saveAndFlush(any(RecurringTransaction.class));

        verify(recurringTransactionRepository)
                .findByIdAndUserId(recurringTransactionId, userId);

        verify(accountRepository)
                .findByIdAndUserId(accountId, userId);

        verifyNoInteractions(categoryRepository, userRepository);
    }



    @Test
    void updateRecurringTransaction_shouldRecalculateNextOccurrenceWhenFrequencyChanges() {

        // Simulate November 10, 2026.
        Clock fixedClock = Clock.fixed(
                Instant.parse("2026-11-10T12:00:00Z"),
                ZoneOffset.UTC
        );

        ReflectionTestUtils.setField(
                recurringTransactionService,
                "clock",
                fixedClock
        );

        LocalDate startDate = LocalDate.of(2026, 11, 1);

        User user = new User();
        ReflectionTestUtils.setField(user, "id", 1L);

        Account account = new Account();
        ReflectionTestUtils.setField(account, "id", 1L);

        RecurringTransaction recurringTransaction = new RecurringTransaction();
        ReflectionTestUtils.setField(recurringTransaction, "id", 1L);

        recurringTransaction.setUser(user);
        recurringTransaction.setAccount(account);
        recurringTransaction.setAmount(new BigDecimal("100.00"));
        recurringTransaction.setDescription("Monthly subscription");
        recurringTransaction.setType(TransactionType.EXPENSE);
        recurringTransaction.setFrequency(RecurringFrequency.MONTHLY);
        recurringTransaction.setStartDate(startDate);
        recurringTransaction.setNextOccurrenceDate(
                LocalDate.of(2026, 12, 1)
        );
        recurringTransaction.setActive(true);

        when(recurringTransactionRepository.findByIdAndUserId(1L, 1L))
                .thenReturn(Optional.of(recurringTransaction));

        when(accountRepository.findByIdAndUserId(1L, 1L))
                .thenReturn(Optional.of(account));

        when(recurringTransactionRepository.saveAndFlush(any(RecurringTransaction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        RecurringTransactionRequest request = new RecurringTransactionRequest(
                1L,
                null,
                new BigDecimal("100.00"),
                "Weekly subscription",
                TransactionType.EXPENSE,
                RecurringFrequency.WEEKLY,
                startDate,
                null
        );

        RecurringTransactionResponse response =
                recurringTransactionService.updateRecurringTransaction(
                        1L,
                        1L,
                        request
                );

        assertEquals(
                RecurringFrequency.WEEKLY,
                response.frequency()
        );

        assertEquals(
                LocalDate.of(2026, 11, 1),
                response.startDate()
        );

        assertEquals(
                LocalDate.of(2026, 11, 15),
                response.nextOccurrenceDate()
        );
    }



    @Test
    void updateRecurringTransaction_shouldPreserveNextOccurrenceWhenFrequencyDoesNotChange() {

        LocalDate startDate = LocalDate.of(2026, 10, 25);
        LocalDate nextOccurrenceDate = LocalDate.of(2026, 11, 25);

        User user = new User();
        ReflectionTestUtils.setField(user, "id", 1L);

        Account account = new Account();
        ReflectionTestUtils.setField(account, "id", 1L);

        RecurringTransaction recurringTransaction = new RecurringTransaction();
        ReflectionTestUtils.setField(recurringTransaction, "id", 1L);

        recurringTransaction.setUser(user);
        recurringTransaction.setAccount(account);
        recurringTransaction.setAmount(new BigDecimal("15.00"));
        recurringTransaction.setDescription("Streaming subscription");
        recurringTransaction.setType(TransactionType.EXPENSE);
        recurringTransaction.setFrequency(RecurringFrequency.MONTHLY);
        recurringTransaction.setStartDate(startDate);
        recurringTransaction.setNextOccurrenceDate(nextOccurrenceDate);
        recurringTransaction.setActive(true);

        when(recurringTransactionRepository.findByIdAndUserId(1L, 1L))
                .thenReturn(Optional.of(recurringTransaction));

        when(accountRepository.findByIdAndUserId(1L, 1L))
                .thenReturn(Optional.of(account));

        when(recurringTransactionRepository.saveAndFlush(any(RecurringTransaction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        RecurringTransactionRequest request = new RecurringTransactionRequest(
                1L,
                null,
                new BigDecimal("20.00"),
                "Updated streaming subscription",
                TransactionType.EXPENSE,
                RecurringFrequency.MONTHLY,
                startDate,
                null
        );

        RecurringTransactionResponse response =
                recurringTransactionService.updateRecurringTransaction(
                        1L,
                        1L,
                        request
                );

        assertEquals(new BigDecimal("20.00"), response.amount());
        assertEquals("Updated streaming subscription", response.description());
        assertEquals(RecurringFrequency.MONTHLY, response.frequency());
        assertEquals(startDate, response.startDate());
        assertEquals(nextOccurrenceDate, response.nextOccurrenceDate());
    }


    @Test
    void pauseRecurringTransaction_shouldDeactivateRecurringTransaction() {

        LocalDate startDate = LocalDate.of(2026, 11, 1);
        LocalDate nextOccurrenceDate = LocalDate.of(2026, 12, 1);

        User user = new User();
        ReflectionTestUtils.setField(user, "id", 1L);

        Account account = new Account();
        ReflectionTestUtils.setField(account, "id", 1L);

        RecurringTransaction recurringTransaction = new RecurringTransaction();
        ReflectionTestUtils.setField(recurringTransaction, "id", 1L);

        recurringTransaction.setUser(user);
        recurringTransaction.setAccount(account);
        recurringTransaction.setAmount(new BigDecimal("50.00"));
        recurringTransaction.setDescription("Gym membership");
        recurringTransaction.setType(TransactionType.EXPENSE);
        recurringTransaction.setFrequency(RecurringFrequency.MONTHLY);
        recurringTransaction.setStartDate(startDate);
        recurringTransaction.setNextOccurrenceDate(nextOccurrenceDate);
        recurringTransaction.setActive(true);

        when(recurringTransactionRepository.findByIdAndUserId(1L, 1L))
                .thenReturn(Optional.of(recurringTransaction));

        when(recurringTransactionRepository.save(any(RecurringTransaction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        RecurringTransactionResponse response =
                recurringTransactionService.pauseRecurringTransaction(
                        1L,
                        1L
                );

        assertFalse(response.active());
        assertEquals(startDate, response.startDate());
        assertEquals(nextOccurrenceDate, response.nextOccurrenceDate());
        assertEquals(RecurringFrequency.MONTHLY, response.frequency());

        verify(recurringTransactionRepository)
                .save(recurringTransaction);
    }


    @Test
    void pauseRecurringTransaction_shouldThrowExceptionWhenNotOwnedByUser() {

        Long userId = 1L;
        Long recurringTransactionId = 99L;

        when(recurringTransactionRepository.findByIdAndUserId(
                recurringTransactionId,
                userId
        )).thenReturn(Optional.empty());

        assertThrows(
                RecurringTransactionNotFoundException.class,
                () -> recurringTransactionService.pauseRecurringTransaction(
                        userId,
                        recurringTransactionId
                )
        );

        verify(recurringTransactionRepository, never())
                .save(any(RecurringTransaction.class));
    }



    @Test
    void resumeRecurringTransaction_shouldReactivateAndRecalculateNextOccurrence() {

        // Simulate January 15, 2027.
        Clock fixedClock = Clock.fixed(
                Instant.parse("2027-01-15T12:00:00Z"),
                ZoneOffset.UTC
        );

        ReflectionTestUtils.setField(
                recurringTransactionService,
                "clock",
                fixedClock
        );

        LocalDate startDate = LocalDate.of(2026, 11, 1);
        LocalDate oldNextOccurrenceDate = LocalDate.of(2026, 12, 1);

        User user = new User();
        ReflectionTestUtils.setField(user, "id", 1L);

        Account account = new Account();
        ReflectionTestUtils.setField(account, "id", 1L);

        RecurringTransaction recurringTransaction = new RecurringTransaction();
        ReflectionTestUtils.setField(recurringTransaction, "id", 1L);

        recurringTransaction.setUser(user);
        recurringTransaction.setAccount(account);
        recurringTransaction.setAmount(new BigDecimal("50.00"));
        recurringTransaction.setDescription("Gym membership");
        recurringTransaction.setType(TransactionType.EXPENSE);
        recurringTransaction.setFrequency(RecurringFrequency.MONTHLY);
        recurringTransaction.setStartDate(startDate);
        recurringTransaction.setNextOccurrenceDate(oldNextOccurrenceDate);
        recurringTransaction.setActive(false);

        when(recurringTransactionRepository.findByIdAndUserId(1L, 1L))
                .thenReturn(Optional.of(recurringTransaction));

        when(recurringTransactionRepository.save(any(RecurringTransaction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        RecurringTransactionResponse response =
                recurringTransactionService.resumeRecurringTransaction(
                        1L,
                        1L
                );

        assertTrue(response.active());

        assertEquals(
                LocalDate.of(2027, 2, 1),
                response.nextOccurrenceDate()
        );

        assertEquals(startDate, response.startDate());
        assertEquals(RecurringFrequency.MONTHLY, response.frequency());

        verify(recurringTransactionRepository)
                .save(recurringTransaction);
    }



    @Test
    void resumeRecurringTransaction_shouldThrowExceptionWhenNotOwnedByUser() {

        Long userId = 1L;
        Long recurringTransactionId = 99L;

        when(recurringTransactionRepository.findByIdAndUserId(
                recurringTransactionId,
                userId
        )).thenReturn(Optional.empty());

        assertThrows(
                RecurringTransactionNotFoundException.class,
                () -> recurringTransactionService.resumeRecurringTransaction(
                        userId,
                        recurringTransactionId
                )
        );

        verify(recurringTransactionRepository, never())
                .save(any(RecurringTransaction.class));
    }



    @Test
    void resumeRecurringTransaction_shouldRejectExpiredRecurringTransaction() {

        // Simulate January 15, 2027.
        Clock fixedClock = Clock.fixed(
                Instant.parse("2027-01-15T12:00:00Z"),
                ZoneOffset.UTC
        );

        ReflectionTestUtils.setField(
                recurringTransactionService,
                "clock",
                fixedClock
        );

        User user = new User();
        ReflectionTestUtils.setField(user, "id", 1L);

        Account account = new Account();
        ReflectionTestUtils.setField(account, "id", 1L);

        RecurringTransaction recurringTransaction = new RecurringTransaction();
        ReflectionTestUtils.setField(recurringTransaction, "id", 1L);

        recurringTransaction.setUser(user);
        recurringTransaction.setAccount(account);
        recurringTransaction.setAmount(new BigDecimal("50.00"));
        recurringTransaction.setDescription("Gym membership");
        recurringTransaction.setType(TransactionType.EXPENSE);
        recurringTransaction.setFrequency(RecurringFrequency.MONTHLY);
        recurringTransaction.setStartDate(LocalDate.of(2026, 11, 1));
        recurringTransaction.setNextOccurrenceDate(LocalDate.of(2026, 12, 1));
        recurringTransaction.setEndDate(LocalDate.of(2026, 12, 31));
        recurringTransaction.setActive(false);

        when(recurringTransactionRepository.findByIdAndUserId(1L, 1L))
                .thenReturn(Optional.of(recurringTransaction));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> recurringTransactionService.resumeRecurringTransaction(
                        1L,
                        1L
                )
        );

        assertEquals(
                "Cannot resume recurring transaction because its end date has passed",
                exception.getMessage()
        );

        assertFalse(recurringTransaction.isActive());

        assertEquals(
                LocalDate.of(2026, 12, 1),
                recurringTransaction.getNextOccurrenceDate()
        );

        verify(recurringTransactionRepository, never())
                .save(any(RecurringTransaction.class));
    }



    @Test
    void resumeRecurringTransaction_shouldAllowOccurrenceOnEndDate() {

        // Simulate January 15, 2027.
        Clock fixedClock = Clock.fixed(
                Instant.parse("2027-01-15T12:00:00Z"),
                ZoneOffset.UTC
        );

        ReflectionTestUtils.setField(
                recurringTransactionService,
                "clock",
                fixedClock
        );

        User user = new User();
        ReflectionTestUtils.setField(user, "id", 1L);

        Account account = new Account();
        ReflectionTestUtils.setField(account, "id", 1L);

        RecurringTransaction recurringTransaction = new RecurringTransaction();
        ReflectionTestUtils.setField(recurringTransaction, "id", 1L);

        recurringTransaction.setUser(user);
        recurringTransaction.setAccount(account);
        recurringTransaction.setAmount(new BigDecimal("50.00"));
        recurringTransaction.setDescription("Gym membership");
        recurringTransaction.setType(TransactionType.EXPENSE);
        recurringTransaction.setFrequency(RecurringFrequency.MONTHLY);
        recurringTransaction.setStartDate(LocalDate.of(2026, 11, 1));
        recurringTransaction.setNextOccurrenceDate(LocalDate.of(2026, 12, 1));
        recurringTransaction.setEndDate(LocalDate.of(2027, 2, 1));
        recurringTransaction.setActive(false);

        when(recurringTransactionRepository.findByIdAndUserId(1L, 1L))
                .thenReturn(Optional.of(recurringTransaction));

        when(recurringTransactionRepository.save(any(RecurringTransaction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        RecurringTransactionResponse response =
                recurringTransactionService.resumeRecurringTransaction(
                        1L,
                        1L
                );

        assertTrue(response.active());

        assertEquals(
                LocalDate.of(2027, 2, 1),
                response.nextOccurrenceDate()
        );

        assertEquals(
                LocalDate.of(2027, 2, 1),
                response.endDate()
        );

        verify(recurringTransactionRepository)
                .save(recurringTransaction);
    }



    @Test
    void deleteRecurringTransaction_shouldDeleteOwnedRecurringTransaction() {

        Long userId = 1L;
        Long recurringTransactionId = 1L;

        RecurringTransaction recurringTransaction = new RecurringTransaction();
        ReflectionTestUtils.setField(
                recurringTransaction,
                "id",
                recurringTransactionId
        );

        when(recurringTransactionRepository.findByIdAndUserId(
                recurringTransactionId,
                userId
        )).thenReturn(Optional.of(recurringTransaction));

        recurringTransactionService.deleteRecurringTransaction(
                userId,
                recurringTransactionId
        );

        verify(recurringTransactionRepository)
                .findByIdAndUserId(recurringTransactionId, userId);

        verify(recurringTransactionRepository)
                .delete(recurringTransaction);
    }



    @Test
    void deleteRecurringTransaction_shouldThrowExceptionWhenNotOwnedByUser() {

        Long userId = 1L;
        Long recurringTransactionId = 99L;

        when(recurringTransactionRepository.findByIdAndUserId(
                recurringTransactionId,
                userId
        )).thenReturn(Optional.empty());

        assertThrows(
                RecurringTransactionNotFoundException.class,
                () -> recurringTransactionService.deleteRecurringTransaction(
                        userId,
                        recurringTransactionId
                )
        );

        verify(recurringTransactionRepository)
                .findByIdAndUserId(recurringTransactionId, userId);

        verify(recurringTransactionRepository, never())
                .delete(any(RecurringTransaction.class));
    }




}
