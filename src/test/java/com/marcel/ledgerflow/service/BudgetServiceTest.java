package com.marcel.ledgerflow.service;

import com.marcel.ledgerflow.dto.UpdateBudgetRequest;
import com.marcel.ledgerflow.entity.TransactionType;
import com.marcel.ledgerflow.exception.BudgetNotFoundException;
import com.marcel.ledgerflow.repository.BudgetRepository;
import com.marcel.ledgerflow.repository.CategoryRepository;
import com.marcel.ledgerflow.repository.TransactionRepository;
import com.marcel.ledgerflow.repository.UserRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.marcel.ledgerflow.exception.CategoryNotFoundException;

import com.marcel.ledgerflow.dto.BudgetRequest;
import com.marcel.ledgerflow.dto.BudgetResponse;
import com.marcel.ledgerflow.entity.Budget;
import com.marcel.ledgerflow.entity.Category;

import com.marcel.ledgerflow.entity.User;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;

import com.marcel.ledgerflow.exception.DuplicateBudgetException;

import static org.mockito.Mockito.*;

import java.util.List;

@ExtendWith(MockitoExtension.class)
class BudgetServiceTest {

    @Mock
    private BudgetRepository budgetRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private BudgetService budgetService;

    @Test
    void shouldInitializeBudgetService() {

        // Arrange
        // Mockito initializes the mocked repositories.

        // Act & Assert
        assertNotNull(budgetService);
    }

    @Test
    void shouldCreateBudgetForAuthenticatedUser() {

        // Arrange
        Long userId = 5L;
        Long categoryId = 3L;

        BudgetRequest request = new BudgetRequest(
                categoryId,
                2026,
                10,
                new BigDecimal("500.00")
        );

        User user = new User();
        user.setId(userId);

        Category category = new Category();
        category.setId(categoryId);
        category.setUser(user);
        category.setName("Entertainment");
        category.setType(TransactionType.EXPENSE);

        when(userRepository.findById(userId))
                .thenReturn(Optional.of(user));

        when(categoryRepository.findByIdAndUserId(categoryId, userId))
                .thenReturn(Optional.of(category));

        when(budgetRepository.existsByUserIdAndCategoryIdAndYearAndMonth(
                userId,
                categoryId,
                2026,
                10
        )).thenReturn(false);

        when(budgetRepository.saveAndFlush(any(Budget.class)))
                .thenAnswer(invocation -> {
                    Budget budget = invocation.getArgument(0);

                    // Simulate the database assigning an ID
                    ReflectionTestUtils.setField(budget, "id", 10L);

                    return budget;
                });

        when(transactionRepository.calculateCategorySpending(
                userId,
                categoryId,
                TransactionType.EXPENSE,
                LocalDate.of(2026, 10, 1),
                LocalDate.of(2026, 11, 1)
        )).thenReturn(new BigDecimal("125.00"));

        // Act
        BudgetResponse response =
                budgetService.createBudget(userId, request);

        // Assert
        assertEquals(10L, response.id());
        assertEquals(categoryId, response.categoryId());
        assertEquals("Entertainment", response.categoryName());

        assertEquals(2026, response.year());
        assertEquals(10, response.month());

        assertEquals(0, new BigDecimal("500.00")
                .compareTo(response.amount()));

        assertEquals(0, new BigDecimal("125.00")
                .compareTo(response.spentAmount()));

        assertEquals(0, new BigDecimal("375.00")
                .compareTo(response.remainingAmount()));

        assertEquals(0, new BigDecimal("25.00")
                .compareTo(response.percentageUsed()));

        assertFalse(response.overBudget());

        verify(userRepository).findById(userId);

        verify(categoryRepository)
                .findByIdAndUserId(categoryId, userId);

        verify(budgetRepository)
                .existsByUserIdAndCategoryIdAndYearAndMonth(
                        userId,
                        categoryId,
                        2026,
                        10
                );

        verify(budgetRepository)
                .saveAndFlush(org.mockito.ArgumentMatchers.argThat(budget ->
                        budget.getUser().getId().equals(userId)
                                && budget.getCategory().getId().equals(categoryId)
                                && budget.getYear().equals(2026)
                                && budget.getMonth().equals(10)
                                && budget.getAmount().compareTo(
                                new BigDecimal("500.00")
                        ) == 0
                ));

        verify(transactionRepository)
                .calculateCategorySpending(
                        userId,
                        categoryId,
                        TransactionType.EXPENSE,
                        LocalDate.of(2026, 10, 1),
                        LocalDate.of(2026, 11, 1)
                );
    }

    @Test
    void shouldNotCreateDuplicateBudget() {

        // Arrange
        Long userId = 5L;
        Long categoryId = 3L;

        BudgetRequest request = new BudgetRequest(
                categoryId,
                2026,
                10,
                new BigDecimal("500.00")
        );

        User user = new User();
        user.setId(userId);

        Category category = new Category();
        category.setId(categoryId);
        category.setUser(user);
        category.setName("Entertainment");
        category.setType(TransactionType.EXPENSE);

        when(userRepository.findById(userId))
                .thenReturn(Optional.of(user));

        when(categoryRepository.findByIdAndUserId(categoryId, userId))
                .thenReturn(Optional.of(category));

        // Simulate an existing budget for this category and month
        when(budgetRepository.existsByUserIdAndCategoryIdAndYearAndMonth(
                userId,
                categoryId,
                2026,
                10
        )).thenReturn(true);

        // Act & Assert
        DuplicateBudgetException exception = assertThrows(
                DuplicateBudgetException.class,
                () -> budgetService.createBudget(userId, request)
        );

        assertEquals(
                "A budget already exists for this category and month",
                exception.getMessage()
        );

        // Verify no new budget was saved
        verify(budgetRepository, never())
                .saveAndFlush(any(Budget.class));

        // Verify no spending calculation was performed
        verify(transactionRepository, never())
                .calculateCategorySpending(
                        any(),
                        any(),
                        any(),
                        any(),
                        any()
                );
    }

    @Test
    void shouldNotCreateBudgetForIncomeCategory() {

        // Arrange
        Long userId = 5L;
        Long categoryId = 4L;

        BudgetRequest request = new BudgetRequest(
                categoryId,
                2026,
                10,
                new BigDecimal("500.00")
        );

        User user = new User();
        user.setId(userId);

        Category category = new Category();
        category.setId(categoryId);
        category.setUser(user);
        category.setName("Salary");
        category.setType(TransactionType.INCOME);

        when(userRepository.findById(userId))
                .thenReturn(Optional.of(user));

        when(categoryRepository.findByIdAndUserId(categoryId, userId))
                .thenReturn(Optional.of(category));

        // Act & Assert
        assertThrows(
                IllegalArgumentException.class,
                () -> budgetService.createBudget(userId, request)
        );

        // Verify that no budget was saved
        verify(budgetRepository, never())
                .saveAndFlush(any(Budget.class));

        // Verify duplicate checking was never reached
        verify(budgetRepository, never())
                .existsByUserIdAndCategoryIdAndYearAndMonth(
                        any(),
                        any(),
                        any(),
                        any()
                );
    }

    @Test
    void shouldNotCreateBudgetUsingAnotherUsersCategory() {

        // Arrange
        Long userId = 5L;
        Long categoryId = 10L;

        BudgetRequest request = new BudgetRequest(
                categoryId,
                2026,
                10,
                new BigDecimal("500.00")
        );

        User user = new User();
        user.setId(userId);

        when(userRepository.findById(userId))
                .thenReturn(Optional.of(user));

        // The category does not belong to the authenticated user
        when(categoryRepository.findByIdAndUserId(categoryId, userId))
                .thenReturn(Optional.empty());

        // Act & Assert
        CategoryNotFoundException exception = assertThrows(
                CategoryNotFoundException.class,
                () -> budgetService.createBudget(userId, request)
        );

        assertEquals(
                "Category not found",
                exception.getMessage()
        );

        // Verify ownership was checked
        verify(categoryRepository)
                .findByIdAndUserId(categoryId, userId);

        // Verify no budget was saved
        verify(budgetRepository, never())
                .saveAndFlush(any(Budget.class));

        // Verify duplicate checking was never reached
        verify(budgetRepository, never())
                .existsByUserIdAndCategoryIdAndYearAndMonth(
                        any(),
                        any(),
                        any(),
                        any()
                );
    }

    @Test
    void shouldReturnBudgetsForSpecificMonth() {

        // Arrange
        Long userId = 5L;
        Integer year = 2026;
        Integer month = 10;

        User user = new User();
        user.setId(userId);

        Category entertainment = new Category();
        entertainment.setId(3L);
        entertainment.setUser(user);
        entertainment.setName("Entertainment");
        entertainment.setType(TransactionType.EXPENSE);

        Category groceries = new Category();
        groceries.setId(1L);
        groceries.setUser(user);
        groceries.setName("Groceries");
        groceries.setType(TransactionType.EXPENSE);

        Budget entertainmentBudget = new Budget();
        ReflectionTestUtils.setField(entertainmentBudget, "id", 10L);
        entertainmentBudget.setUser(user);
        entertainmentBudget.setCategory(entertainment);
        entertainmentBudget.setYear(year);
        entertainmentBudget.setMonth(month);
        entertainmentBudget.setAmount(new BigDecimal("500.00"));

        Budget groceriesBudget = new Budget();
        ReflectionTestUtils.setField(groceriesBudget, "id", 11L);
        groceriesBudget.setUser(user);
        groceriesBudget.setCategory(groceries);
        groceriesBudget.setYear(year);
        groceriesBudget.setMonth(month);
        groceriesBudget.setAmount(new BigDecimal("800.00"));

        when(budgetRepository.findByUserIdAndYearAndMonth(
                userId, year, month
        )).thenReturn(List.of(entertainmentBudget, groceriesBudget));

        when(transactionRepository.calculateCategorySpending(
                userId,
                3L,
                TransactionType.EXPENSE,
                LocalDate.of(2026, 10, 1),
                LocalDate.of(2026, 11, 1)
        )).thenReturn(new BigDecimal("125.00"));

        when(transactionRepository.calculateCategorySpending(
                userId,
                1L,
                TransactionType.EXPENSE,
                LocalDate.of(2026, 10, 1),
                LocalDate.of(2026, 11, 1)
        )).thenReturn(new BigDecimal("200.00"));

        // Act
        List<BudgetResponse> responses =
                budgetService.getBudgetsByMonth(userId, year, month);

        // Assert
        assertEquals(2, responses.size());

        BudgetResponse entertainmentResponse = responses.get(0);

        assertEquals(10L, entertainmentResponse.id());
        assertEquals("Entertainment", entertainmentResponse.categoryName());
        assertEquals(0, new BigDecimal("500.00")
                .compareTo(entertainmentResponse.amount()));
        assertEquals(0, new BigDecimal("125.00")
                .compareTo(entertainmentResponse.spentAmount()));
        assertEquals(0, new BigDecimal("375.00")
                .compareTo(entertainmentResponse.remainingAmount()));
        assertEquals(0, new BigDecimal("25.00")
                .compareTo(entertainmentResponse.percentageUsed()));
        assertFalse(entertainmentResponse.overBudget());

        BudgetResponse groceriesResponse = responses.get(1);

        assertEquals(11L, groceriesResponse.id());
        assertEquals("Groceries", groceriesResponse.categoryName());
        assertEquals(0, new BigDecimal("800.00")
                .compareTo(groceriesResponse.amount()));
        assertEquals(0, new BigDecimal("200.00")
                .compareTo(groceriesResponse.spentAmount()));
        assertEquals(0, new BigDecimal("600.00")
                .compareTo(groceriesResponse.remainingAmount()));
        assertEquals(0, new BigDecimal("25.00")
                .compareTo(groceriesResponse.percentageUsed()));
        assertFalse(groceriesResponse.overBudget());

        verify(budgetRepository)
                .findByUserIdAndYearAndMonth(userId, year, month);

        verify(transactionRepository)
                .calculateCategorySpending(
                        userId,
                        3L,
                        TransactionType.EXPENSE,
                        LocalDate.of(2026, 10, 1),
                        LocalDate.of(2026, 11, 1)
                );

        verify(transactionRepository)
                .calculateCategorySpending(
                        userId,
                        1L,
                        TransactionType.EXPENSE,
                        LocalDate.of(2026, 10, 1),
                        LocalDate.of(2026, 11, 1)
                );
    }

    @Test
    void shouldReturnEmptyListWhenUserHasNoBudgetsForMonth() {

        // Arrange
        Long userId = 5L;
        Integer year = 2026;
        Integer month = 12;

        when(budgetRepository.findByUserIdAndYearAndMonth(
                userId,
                year,
                month
        )).thenReturn(List.of());

        // Act
        List<BudgetResponse> responses =
                budgetService.getBudgetsByMonth(userId, year, month);

        // Assert
        assertNotNull(responses);
        assertTrue(responses.isEmpty());

        verify(budgetRepository)
                .findByUserIdAndYearAndMonth(userId, year, month);

        // No budgets means no spending calculations
        verifyNoInteractions(transactionRepository);
    }

    @Test
    void shouldRejectInvalidMonthWhenRetrievingBudgets() {

        // Arrange
        Long userId = 5L;
        Integer year = 2026;
        Integer invalidMonth = 13;

        // Act & Assert
        assertThrows(
                IllegalArgumentException.class,
                () -> budgetService.getBudgetsByMonth(
                        userId,
                        year,
                        invalidMonth
                )
        );

        // Invalid input should be rejected before accessing the database
        verifyNoInteractions(budgetRepository);
        verifyNoInteractions(transactionRepository);
    }

    @Test
    void shouldRejectInvalidYearWhenRetrievingBudgets() {

        // Arrange
        Long userId = 5L;
        Integer invalidYear = 1999;
        Integer month = 10;

        // Act & Assert
        assertThrows(
                IllegalArgumentException.class,
                () -> budgetService.getBudgetsByMonth(
                        userId,
                        invalidYear,
                        month
                )
        );

        // Invalid input should be rejected before accessing the database
        verifyNoInteractions(budgetRepository);
        verifyNoInteractions(transactionRepository);
    }

    @Test
    void shouldReturnBudgetWhenItBelongsToAuthenticatedUser() {

        // Arrange
        Long userId = 5L;
        Long budgetId = 10L;
        Long categoryId = 3L;

        User user = new User();
        user.setId(userId);

        Category category = new Category();
        category.setId(categoryId);
        category.setUser(user);
        category.setName("Entertainment");
        category.setType(TransactionType.EXPENSE);

        Budget budget = new Budget();
        ReflectionTestUtils.setField(budget, "id", budgetId);
        budget.setUser(user);
        budget.setCategory(category);
        budget.setYear(2026);
        budget.setMonth(10);
        budget.setAmount(new BigDecimal("500.00"));

        when(budgetRepository.findByIdAndUserId(budgetId, userId))
                .thenReturn(Optional.of(budget));

        when(transactionRepository.calculateCategorySpending(
                userId,
                categoryId,
                TransactionType.EXPENSE,
                LocalDate.of(2026, 10, 1),
                LocalDate.of(2026, 11, 1)
        )).thenReturn(new BigDecimal("125.00"));

        // Act
        BudgetResponse response =
                budgetService.getBudgetById(budgetId, userId);

        // Assert
        assertNotNull(response);

        assertEquals(budgetId, response.id());
        assertEquals(categoryId, response.categoryId());
        assertEquals("Entertainment", response.categoryName());

        assertEquals(2026, response.year());
        assertEquals(10, response.month());

        assertEquals(0, new BigDecimal("500.00")
                .compareTo(response.amount()));

        assertEquals(0, new BigDecimal("125.00")
                .compareTo(response.spentAmount()));

        assertEquals(0, new BigDecimal("375.00")
                .compareTo(response.remainingAmount()));

        assertEquals(0, new BigDecimal("25.00")
                .compareTo(response.percentageUsed()));

        assertFalse(response.overBudget());

        // Verify ownership was checked
        verify(budgetRepository)
                .findByIdAndUserId(budgetId, userId);

        // Verify spending was calculated for the correct month
        verify(transactionRepository)
                .calculateCategorySpending(
                        userId,
                        categoryId,
                        TransactionType.EXPENSE,
                        LocalDate.of(2026, 10, 1),
                        LocalDate.of(2026, 11, 1)
                );
    }

    @Test
    void shouldNotReturnBudgetWhenItBelongsToAnotherUser() {

        // Arrange
        Long budgetId = 10L;
        Long userId = 8L;

        // No budget matches both the budget ID and authenticated user ID
        when(budgetRepository.findByIdAndUserId(budgetId, userId))
                .thenReturn(Optional.empty());

        // Act & Assert
        BudgetNotFoundException exception = assertThrows(
                BudgetNotFoundException.class,
                () -> budgetService.getBudgetById(budgetId, userId)
        );

        assertEquals(
                "Budget not found",
                exception.getMessage()
        );

        // Verify ownership was checked
        verify(budgetRepository)
                .findByIdAndUserId(budgetId, userId);

        // No financial calculations should occur
        verifyNoInteractions(transactionRepository);
    }

    @Test
    void shouldUpdateBudgetForAuthenticatedUser() {

        // Arrange
        Long userId = 5L;
        Long budgetId = 10L;
        Long categoryId = 3L;

        User user = new User();
        user.setId(userId);

        Category category = new Category();
        category.setId(categoryId);
        category.setUser(user);
        category.setName("Entertainment");
        category.setType(TransactionType.EXPENSE);

        Budget budget = new Budget();
        ReflectionTestUtils.setField(budget, "id", budgetId);
        budget.setUser(user);
        budget.setCategory(category);
        budget.setYear(2026);
        budget.setMonth(10);
        budget.setAmount(new BigDecimal("500.00"));

        UpdateBudgetRequest request = new UpdateBudgetRequest(
                new BigDecimal("750.00")
        );

        when(budgetRepository.findByIdAndUserId(budgetId, userId))
                .thenReturn(Optional.of(budget));

        when(budgetRepository.saveAndFlush(any(Budget.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        when(transactionRepository.calculateCategorySpending(
                userId,
                categoryId,
                TransactionType.EXPENSE,
                LocalDate.of(2026, 10, 1),
                LocalDate.of(2026, 11, 1)
        )).thenReturn(new BigDecimal("125.00"));

        // Act
        BudgetResponse response =
                budgetService.updateBudget(budgetId, userId, request);

        // Assert
        assertNotNull(response);

        assertEquals(budgetId, response.id());
        assertEquals(categoryId, response.categoryId());
        assertEquals("Entertainment", response.categoryName());

        assertEquals(0, new BigDecimal("750.00")
                .compareTo(response.amount()));

        assertEquals(0, new BigDecimal("125.00")
                .compareTo(response.spentAmount()));

        assertEquals(0, new BigDecimal("625.00")
                .compareTo(response.remainingAmount()));

        assertEquals(0, new BigDecimal("16.67")
                .compareTo(response.percentageUsed()));

        assertFalse(response.overBudget());

        // Verify the correct budget was retrieved
        verify(budgetRepository)
                .findByIdAndUserId(budgetId, userId);

        // Verify the updated amount was saved
        verify(budgetRepository).saveAndFlush(
                org.mockito.ArgumentMatchers.argThat(savedBudget ->
                        savedBudget.getId().equals(budgetId)
                                && savedBudget.getAmount().compareTo(
                                new BigDecimal("750.00")
                        ) == 0
                )
        );

        // Verify spending was recalculated
        verify(transactionRepository)
                .calculateCategorySpending(
                        userId,
                        categoryId,
                        TransactionType.EXPENSE,
                        LocalDate.of(2026, 10, 1),
                        LocalDate.of(2026, 11, 1)
                );
    }

    @Test
    void shouldNotUpdateBudgetWhenItBelongsToAnotherUser() {

        // Arrange
        Long budgetId = 10L;
        Long userId = 8L;

        UpdateBudgetRequest request = new UpdateBudgetRequest(
                new BigDecimal("9999.00")
        );

        // No budget matches both the budget ID and authenticated user ID
        when(budgetRepository.findByIdAndUserId(budgetId, userId))
                .thenReturn(Optional.empty());

        // Act & Assert
        BudgetNotFoundException exception = assertThrows(
                BudgetNotFoundException.class,
                () -> budgetService.updateBudget(
                        budgetId,
                        userId,
                        request
                )
        );

        assertEquals(
                "Budget not found",
                exception.getMessage()
        );

        // Verify ownership was checked
        verify(budgetRepository)
                .findByIdAndUserId(budgetId, userId);

        // Verify no budget was saved
        verify(budgetRepository, never())
                .saveAndFlush(any(Budget.class));

        // Verify no spending calculations occurred
        verifyNoInteractions(transactionRepository);
    }

    @Test
    void shouldDeleteBudgetForAuthenticatedUser() {

        // Arrange
        Long userId = 5L;
        Long budgetId = 10L;

        User user = new User();
        user.setId(userId);

        Category category = new Category();
        category.setId(3L);
        category.setUser(user);
        category.setName("Entertainment");
        category.setType(TransactionType.EXPENSE);

        Budget budget = new Budget();
        ReflectionTestUtils.setField(budget, "id", budgetId);
        budget.setUser(user);
        budget.setCategory(category);
        budget.setYear(2026);
        budget.setMonth(10);
        budget.setAmount(new BigDecimal("500.00"));

        when(budgetRepository.findByIdAndUserId(budgetId, userId))
                .thenReturn(Optional.of(budget));

        // Act
        budgetService.deleteBudget(budgetId, userId);

        // Assert
        verify(budgetRepository)
                .findByIdAndUserId(budgetId, userId);

        verify(budgetRepository)
                .delete(budget);

        // Deleting a budget should not require spending calculations
        verifyNoInteractions(transactionRepository);
    }

    @Test
    void shouldNotDeleteBudgetWhenItBelongsToAnotherUser() {

        // Arrange
        Long budgetId = 10L;
        Long userId = 8L;

        // No budget belongs to this authenticated user
        when(budgetRepository.findByIdAndUserId(budgetId, userId))
                .thenReturn(Optional.empty());

        // Act & Assert
        BudgetNotFoundException exception = assertThrows(
                BudgetNotFoundException.class,
                () -> budgetService.deleteBudget(budgetId, userId)
        );

        assertEquals(
                "Budget not found",
                exception.getMessage()
        );

        // Verify ownership was checked
        verify(budgetRepository)
                .findByIdAndUserId(budgetId, userId);

        // Verify no budget was deleted
        verify(budgetRepository, never())
                .delete(any(Budget.class));

        // Verify no financial calculations occurred
        verifyNoInteractions(transactionRepository);
    }

    @Test
    void shouldCalculateOverBudgetSpendingCorrectly() {

        // Arrange
        Long userId = 5L;
        Long budgetId = 10L;
        Long categoryId = 3L;

        User user = new User();
        user.setId(userId);

        Category category = new Category();
        category.setId(categoryId);
        category.setUser(user);
        category.setName("Entertainment");
        category.setType(TransactionType.EXPENSE);

        Budget budget = new Budget();
        ReflectionTestUtils.setField(budget, "id", budgetId);
        budget.setUser(user);
        budget.setCategory(category);
        budget.setYear(2026);
        budget.setMonth(10);
        budget.setAmount(new BigDecimal("500.00"));

        when(budgetRepository.findByIdAndUserId(budgetId, userId))
                .thenReturn(Optional.of(budget));

        // Spending exceeds the $500 budget
        when(transactionRepository.calculateCategorySpending(
                userId,
                categoryId,
                TransactionType.EXPENSE,
                LocalDate.of(2026, 10, 1),
                LocalDate.of(2026, 11, 1)
        )).thenReturn(new BigDecimal("575.00"));

        // Act
        BudgetResponse response =
                budgetService.getBudgetById(budgetId, userId);

        // Assert
        assertEquals(0, new BigDecimal("500.00")
                .compareTo(response.amount()));

        assertEquals(0, new BigDecimal("575.00")
                .compareTo(response.spentAmount()));

        assertEquals(0, BigDecimal.ZERO
                .compareTo(response.remainingAmount()));

        assertEquals(0, new BigDecimal("115.00")
                .compareTo(response.percentageUsed()));

        assertTrue(response.overBudget());

        // Verify spending was calculated for the correct month
        verify(transactionRepository)
                .calculateCategorySpending(
                        userId,
                        categoryId,
                        TransactionType.EXPENSE,
                        LocalDate.of(2026, 10, 1),
                        LocalDate.of(2026, 11, 1)
                );
    }

    @Test
    void shouldCalculateBudgetWithZeroSpendingCorrectly() {

        // Arrange
        Long userId = 5L;
        Long budgetId = 10L;
        Long categoryId = 3L;

        User user = new User();
        user.setId(userId);

        Category category = new Category();
        category.setId(categoryId);
        category.setUser(user);
        category.setName("Entertainment");
        category.setType(TransactionType.EXPENSE);

        Budget budget = new Budget();
        ReflectionTestUtils.setField(budget, "id", budgetId);
        budget.setUser(user);
        budget.setCategory(category);
        budget.setYear(2026);
        budget.setMonth(10);
        budget.setAmount(new BigDecimal("500.00"));

        when(budgetRepository.findByIdAndUserId(budgetId, userId))
                .thenReturn(Optional.of(budget));

        // No expenses have been recorded for this category
        when(transactionRepository.calculateCategorySpending(
                userId,
                categoryId,
                TransactionType.EXPENSE,
                LocalDate.of(2026, 10, 1),
                LocalDate.of(2026, 11, 1)
        )).thenReturn(BigDecimal.ZERO);

        // Act
        BudgetResponse response =
                budgetService.getBudgetById(budgetId, userId);

        // Assert
        assertNotNull(response);

        assertEquals(0, new BigDecimal("500.00")
                .compareTo(response.amount()));

        assertEquals(0, BigDecimal.ZERO
                .compareTo(response.spentAmount()));

        assertEquals(0, new BigDecimal("500.00")
                .compareTo(response.remainingAmount()));

        assertEquals(0, BigDecimal.ZERO
                .compareTo(response.percentageUsed()));

        assertFalse(response.overBudget());

        // Verify spending was calculated for the correct month
        verify(transactionRepository)
                .calculateCategorySpending(
                        userId,
                        categoryId,
                        TransactionType.EXPENSE,
                        LocalDate.of(2026, 10, 1),
                        LocalDate.of(2026, 11, 1)
                );
    }
}