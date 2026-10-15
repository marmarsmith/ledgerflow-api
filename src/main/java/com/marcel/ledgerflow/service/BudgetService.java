package com.marcel.ledgerflow.service;

import com.marcel.ledgerflow.dto.BudgetRequest;
import com.marcel.ledgerflow.dto.BudgetResponse;
import com.marcel.ledgerflow.dto.UpdateBudgetRequest;
import com.marcel.ledgerflow.entity.Budget;
import com.marcel.ledgerflow.entity.Category;
import com.marcel.ledgerflow.entity.TransactionType;
import com.marcel.ledgerflow.entity.User;
import com.marcel.ledgerflow.exception.BudgetNotFoundException;
import com.marcel.ledgerflow.exception.CategoryNotFoundException;
import com.marcel.ledgerflow.exception.DuplicateBudgetException;
import com.marcel.ledgerflow.repository.BudgetRepository;
import com.marcel.ledgerflow.repository.CategoryRepository;
import com.marcel.ledgerflow.repository.UserRepository;
import com.marcel.ledgerflow.repository.TransactionRepository;
import java.time.LocalDate;
import java.math.RoundingMode;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class BudgetService {

    private final BudgetRepository budgetRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;

    public BudgetService(
            BudgetRepository budgetRepository,
            CategoryRepository categoryRepository,
            UserRepository userRepository,
            TransactionRepository transactionRepository
    ) {
        this.budgetRepository = budgetRepository;
        this.categoryRepository = categoryRepository;
        this.userRepository = userRepository;
        this.transactionRepository = transactionRepository;
    }

    @Transactional
    public BudgetResponse createBudget(
            Long userId,
            BudgetRequest request
    ) {

        // Find the authenticated user
        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Authenticated user not found"
                        )
                );

        // Find the category and verify ownership
        Category category = categoryRepository
                .findByIdAndUserId(request.categoryId(), userId)
                .orElseThrow(CategoryNotFoundException::new);

        // Only expense categories can have budgets
        if (category.getType() != TransactionType.EXPENSE) {
            throw new IllegalArgumentException(
                    "Budgets can only be created for expense categories"
            );
        }

        // Prevent duplicate monthly budgets
        boolean budgetExists =
                budgetRepository.existsByUserIdAndCategoryIdAndYearAndMonth(
                        userId,
                        request.categoryId(),
                        request.year(),
                        request.month()
                );

        if (budgetExists) {
            throw new DuplicateBudgetException();
        }

        // Create the budget
        Budget budget = new Budget();
        budget.setUser(user);
        budget.setCategory(category);
        budget.setYear(request.year());
        budget.setMonth(request.month());
        budget.setAmount(request.amount());

        Budget savedBudget = budgetRepository.saveAndFlush(budget);

        return buildBudgetResponse(savedBudget);
    }

    @Transactional(readOnly = true)
    public List<BudgetResponse> getBudgetsByMonth(
            Long userId,
            Integer year,
            Integer month
    ) {

        if (year == null || year < 2000 || year > 2100) {
            throw new IllegalArgumentException(
                    "Year must be between 2000 and 2100"
            );
        }

        if (month == null || month < 1 || month > 12) {
            throw new IllegalArgumentException(
                    "Month must be between 1 and 12"
            );
        }

        return budgetRepository
                .findByUserIdAndYearAndMonth(userId, year, month)
                .stream()
                .map(this::buildBudgetResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public BudgetResponse getBudgetById(
            Long budgetId,
            Long userId
    ) {

        Budget budget = budgetRepository
                .findByIdAndUserId(budgetId, userId)
                .orElseThrow(BudgetNotFoundException::new);

        return buildBudgetResponse(budget);
    }

    @Transactional
    public BudgetResponse updateBudget(
            Long budgetId,
            Long userId,
            UpdateBudgetRequest request
    ) {

        // Find the budget and verify ownership
        Budget budget = budgetRepository
                .findByIdAndUserId(budgetId, userId)
                .orElseThrow(BudgetNotFoundException::new);

        // Update the spending limit
        budget.setAmount(request.amount());

        Budget savedBudget = budgetRepository.saveAndFlush(budget);

        // Recalculate spending against the updated limit
        return buildBudgetResponse(savedBudget);
    }

    @Transactional
    public void deleteBudget(
            Long budgetId,
            Long userId
    ) {

        // Find the budget and verify ownership
        Budget budget = budgetRepository
                .findByIdAndUserId(budgetId, userId)
                .orElseThrow(BudgetNotFoundException::new);

        budgetRepository.delete(budget);
    }

    private BudgetResponse buildBudgetResponse(Budget budget) {

        LocalDate startDate = LocalDate.of(
                budget.getYear(),
                budget.getMonth(),
                1
        );

        LocalDate endDate = startDate.plusMonths(1);

        BigDecimal spentAmount = transactionRepository.calculateCategorySpending(
                budget.getUser().getId(),
                budget.getCategory().getId(),
                TransactionType.EXPENSE,
                startDate,
                endDate
        );

        BigDecimal remainingAmount = budget.getAmount()
                .subtract(spentAmount)
                .max(BigDecimal.ZERO);

        BigDecimal percentageUsed = spentAmount
                .multiply(BigDecimal.valueOf(100))
                .divide(
                        budget.getAmount(),
                        2,
                        RoundingMode.HALF_UP
                );

        boolean overBudget =
                spentAmount.compareTo(budget.getAmount()) > 0;

        return new BudgetResponse(
                budget.getId(),
                budget.getCategory().getId(),
                budget.getCategory().getName(),
                budget.getYear(),
                budget.getMonth(),
                budget.getAmount(),
                spentAmount,
                remainingAmount,
                percentageUsed,
                overBudget
        );
    }
}