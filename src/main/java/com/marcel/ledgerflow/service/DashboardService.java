package com.marcel.ledgerflow.service;

import com.marcel.ledgerflow.dto.DashboardResponse;
import com.marcel.ledgerflow.dto.MonthlyTrendResponse;
import com.marcel.ledgerflow.enums.TransactionType;
import com.marcel.ledgerflow.repository.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.ArrayList;

@Service
public class DashboardService {

    private final TransactionRepository transactionRepository;

    public DashboardService(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    @Transactional(readOnly = true)
    public DashboardResponse getDashboard(Long userId, int year) {

        LocalDate startOfYear = LocalDate.of(year, 1, 1);
        LocalDate startOfNextYear = startOfYear.plusYears(1);

        BigDecimal totalIncome =
                transactionRepository.calculateTotalByUserAndTypeAndDateRange(
                        userId,
                        TransactionType.INCOME,
                        startOfYear,
                        startOfNextYear
                );

        BigDecimal totalExpenses =
                transactionRepository.calculateTotalByUserAndTypeAndDateRange(
                        userId,
                        TransactionType.EXPENSE,
                        startOfYear,
                        startOfNextYear
                );

        BigDecimal netSavings = totalIncome.subtract(totalExpenses);

        List<MonthlyTrendResponse> monthlyTrends = new ArrayList<>();

        for (int month = 1; month <= 12; month++) {

            LocalDate startOfMonth = LocalDate.of(year, month, 1);
            LocalDate startOfNextMonth = startOfMonth.plusMonths(1);

            BigDecimal monthlyIncome =
                    transactionRepository.calculateTotalByUserAndTypeAndDateRange(
                            userId,
                            TransactionType.INCOME,
                            startOfMonth,
                            startOfNextMonth
                    );

            BigDecimal monthlyExpenses =
                    transactionRepository.calculateTotalByUserAndTypeAndDateRange(
                            userId,
                            TransactionType.EXPENSE,
                            startOfMonth,
                            startOfNextMonth
                    );

            BigDecimal monthlyNetSavings =
                    monthlyIncome.subtract(monthlyExpenses);

            monthlyTrends.add(
                    new MonthlyTrendResponse(
                            month,
                            monthlyIncome,
                            monthlyExpenses,
                            monthlyNetSavings
                    )
            );
        }

        return new DashboardResponse(
                year,
                totalIncome,
                totalExpenses,
                netSavings,
                monthlyTrends
        );
    }
}