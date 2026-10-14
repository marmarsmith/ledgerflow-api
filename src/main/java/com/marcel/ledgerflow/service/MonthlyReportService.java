package com.marcel.ledgerflow.service;

import com.marcel.ledgerflow.dto.MonthlyReportResponse;
import com.marcel.ledgerflow.entity.TransactionType;
import com.marcel.ledgerflow.repository.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.marcel.ledgerflow.dto.CategoryExpenseResponse;
import com.marcel.ledgerflow.repository.CategoryExpenseProjection;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
public class MonthlyReportService {

    private final TransactionRepository transactionRepository;

    public MonthlyReportService(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    @Transactional(readOnly = true)
    public MonthlyReportResponse getMonthlyReport(
            Long userId,
            int year,
            int month
    ) {
        LocalDate startDate = LocalDate.of(year, month, 1);
        LocalDate endDate = startDate.plusMonths(1);

        BigDecimal totalIncome =
                transactionRepository.calculateTotalByUserAndTypeAndDateRange(
                        userId,
                        TransactionType.INCOME,
                        startDate,
                        endDate
                );

        BigDecimal totalExpenses =
                transactionRepository.calculateTotalByUserAndTypeAndDateRange(
                        userId,
                        TransactionType.EXPENSE,
                        startDate,
                        endDate
                );

        BigDecimal netSavings = totalIncome.subtract(totalExpenses);

        List<CategoryExpenseProjection> categoryExpenses =
                transactionRepository.calculateExpensesByCategory(
                        userId,
                        TransactionType.EXPENSE,
                        startDate,
                        endDate
                );

        List<CategoryExpenseResponse> expensesByCategory =
                categoryExpenses.stream()
                        .map(category -> new CategoryExpenseResponse(
                                category.getCategoryId(),
                                category.getCategoryName(),
                                category.getAmount()
                        ))
                        .toList();

        return new MonthlyReportResponse(
                year,
                month,
                totalIncome,
                totalExpenses,
                netSavings,
                expensesByCategory
        );
    }
}