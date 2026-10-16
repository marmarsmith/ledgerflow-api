package com.marcel.ledgerflow.service;

import com.marcel.ledgerflow.dto.*;
import com.marcel.ledgerflow.entity.Budget;
import com.marcel.ledgerflow.entity.Category;
import com.marcel.ledgerflow.enums.BudgetAlertStatus;
import com.marcel.ledgerflow.enums.TransactionType;
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

    public BudgetSummaryResponse getMonthlyBudgetSummary(
            Long userId,
            Integer year,
            Integer month
    ) {

        List<BudgetResponse> budgets =
                getBudgetsByMonth(userId, year, month);

        BigDecimal totalBudgeted = BigDecimal.ZERO;
        BigDecimal totalSpent = BigDecimal.ZERO;

        int overBudgetCount = 0;
        int nearLimitCount = 0;

        for (BudgetResponse budget : budgets) {

            totalBudgeted = totalBudgeted.add(budget.amount());

            totalSpent = totalSpent.add(budget.spentAmount());

            if (budget.overBudget()) {

                overBudgetCount++;

            } else if (budget.spentAmount()
                    .multiply(new BigDecimal("100"))
                    .compareTo(
                            budget.amount().multiply(new BigDecimal("80"))
                    ) >= 0) {

                nearLimitCount++;
            }
        }

        BigDecimal totalRemaining =
                totalBudgeted.subtract(totalSpent);

        BigDecimal overallPercentageUsed = BigDecimal.ZERO;

        if (totalBudgeted.compareTo(BigDecimal.ZERO) > 0) {

            overallPercentageUsed = totalSpent
                    .multiply(new BigDecimal("100"))
                    .divide(totalBudgeted, 2, RoundingMode.HALF_UP);
        }

        return new BudgetSummaryResponse(
                year,
                month,
                totalBudgeted,
                totalSpent,
                totalRemaining,
                overallPercentageUsed,
                overBudgetCount,
                nearLimitCount
        );
    }

    public List<BudgetAlertResponse> getMonthlyBudgetAlerts(
            Long userId,
            Integer year,
            Integer month
    ) {

        List<BudgetResponse> budgets =
                getBudgetsByMonth(userId, year, month);

        return budgets.stream()
                .map(budget -> new BudgetAlertResponse(

                        budget.id(),
                        budget.categoryId(),
                        budget.categoryName(),

                        budget.year(),
                        budget.month(),

                        budget.amount(),
                        budget.spentAmount(),
                        budget.remainingAmount(),
                        budget.percentageUsed(),

                        determineBudgetAlertStatus(
                                budget.spentAmount(),
                                budget.amount()
                        )

                ))
                .toList();
    }


    @Transactional(readOnly = true)
    public List<SpendingInsightResponse> getMonthlySpendingInsights(
            Long userId,
            Integer year,
            Integer month
    ) {

        List<BudgetResponse> budgets =
                getBudgetsByMonth(userId, year, month);

        return budgets.stream()

                // Rank categories by actual spending, highest first
                .sorted((first, second) ->
                        second.spentAmount()
                                .compareTo(first.spentAmount())
                )

                // Convert each budget into a spending insight
                .map(budget -> new SpendingInsightResponse(
                        budget.categoryId(),
                        budget.categoryName(),
                        budget.amount(),
                        budget.spentAmount(),
                        budget.remainingAmount(),
                        budget.percentageUsed(),
                        budget.overBudget()
                ))

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

    private BudgetAlertStatus determineBudgetAlertStatus(
            BigDecimal spentAmount,
            BigDecimal budgetAmount
    ) {

        // Spending exceeds 100% of the budget
        if (spentAmount.compareTo(budgetAmount) > 0) {
            return BudgetAlertStatus.OVER_BUDGET;
        }

        // Spending reaches at least 90% of the budget
        if (spentAmount.multiply(new BigDecimal("100"))
                .compareTo(budgetAmount.multiply(new BigDecimal("90"))) >= 0) {

            return BudgetAlertStatus.CRITICAL;
        }

        // Spending reaches at least 80% of the budget
        if (spentAmount.multiply(new BigDecimal("100"))
                .compareTo(budgetAmount.multiply(new BigDecimal("80"))) >= 0) {

            return BudgetAlertStatus.WARNING;
        }

        return BudgetAlertStatus.ON_TRACK;
    }
}