package com.marcel.ledgerflow.service;

import com.marcel.ledgerflow.dto.DashboardResponse;
import com.marcel.ledgerflow.dto.MonthlyTrendResponse;
import com.marcel.ledgerflow.entity.TransactionType;
import com.marcel.ledgerflow.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private DashboardService dashboardService;

    private Long userId;
    private int year;

    @BeforeEach
    void setUp() {
        userId = 1L;
        year = 2026;
    }

    @Test
    void shouldCalculateAnnualFinancialTotals() {

        LocalDate startOfYear = LocalDate.of(2026, 1, 1);
        LocalDate startOfNextYear = LocalDate.of(2027, 1, 1);

        // Return zero for monthly queries by default.
        when(transactionRepository.calculateTotalByUserAndTypeAndDateRange(
                eq(userId),
                any(TransactionType.class),
                any(LocalDate.class),
                any(LocalDate.class)
        )).thenReturn(BigDecimal.ZERO);

// Return the actual annual income.
        when(transactionRepository.calculateTotalByUserAndTypeAndDateRange(
                eq(userId),
                eq(TransactionType.INCOME),
                eq(startOfYear),
                eq(startOfNextYear)
        )).thenReturn(new BigDecimal("60000.00"));

// Return the actual annual expenses.
        when(transactionRepository.calculateTotalByUserAndTypeAndDateRange(
                eq(userId),
                eq(TransactionType.EXPENSE),
                eq(startOfYear),
                eq(startOfNextYear)
        )).thenReturn(new BigDecimal("38500.00"));

        DashboardResponse response =
                dashboardService.getDashboard(userId, year);

        assertEquals(2026, response.year());

        assertEquals(
                0,
                new BigDecimal("60000.00")
                        .compareTo(response.totalIncome())
        );

        assertEquals(
                0,
                new BigDecimal("38500.00")
                        .compareTo(response.totalExpenses())
        );

        assertEquals(
                0,
                new BigDecimal("21500.00")
                        .compareTo(response.netSavings())
        );

        assertEquals(12, response.monthlyTrends().size());

        for (int month = 1; month <= 12; month++) {
            MonthlyTrendResponse trend =
                    response.monthlyTrends().get(month - 1);

            assertEquals(month, trend.month());
        }
    }

    @Test
    void shouldCalculateMonthlyTrendsAndIncludeEmptyMonths() {

        // Default all income and expense queries to zero.
        when(transactionRepository.calculateTotalByUserAndTypeAndDateRange(
                eq(userId),
                any(TransactionType.class),
                any(LocalDate.class),
                any(LocalDate.class)
        )).thenReturn(BigDecimal.ZERO);

        LocalDate januaryStart = LocalDate.of(2026, 1, 1);
        LocalDate februaryStart = LocalDate.of(2026, 2, 1);

        // January income
        when(transactionRepository.calculateTotalByUserAndTypeAndDateRange(
                userId,
                TransactionType.INCOME,
                januaryStart,
                februaryStart
        )).thenReturn(new BigDecimal("5000.00"));

        // January expenses
        when(transactionRepository.calculateTotalByUserAndTypeAndDateRange(
                userId,
                TransactionType.EXPENSE,
                januaryStart,
                februaryStart
        )).thenReturn(new BigDecimal("3200.00"));

        DashboardResponse response =
                dashboardService.getDashboard(userId, year);

        assertEquals(12, response.monthlyTrends().size());

        // Verify January's financial calculations.
        MonthlyTrendResponse january = response.monthlyTrends().get(0);

        assertEquals(1, january.month());

        assertEquals(
                0,
                new BigDecimal("5000.00").compareTo(january.income())
        );

        assertEquals(
                0,
                new BigDecimal("3200.00").compareTo(january.expenses())
        );

        assertEquals(
                0,
                new BigDecimal("1800.00").compareTo(january.netSavings())
        );

        // February has no transactions.
        MonthlyTrendResponse february = response.monthlyTrends().get(1);

        assertEquals(2, february.month());
        assertEquals(0, BigDecimal.ZERO.compareTo(february.income()));
        assertEquals(0, BigDecimal.ZERO.compareTo(february.expenses()));
        assertEquals(0, BigDecimal.ZERO.compareTo(february.netSavings()));

        // December must still appear even without transactions.
        MonthlyTrendResponse december = response.monthlyTrends().get(11);

        assertEquals(12, december.month());
        assertEquals(0, BigDecimal.ZERO.compareTo(december.income()));
        assertEquals(0, BigDecimal.ZERO.compareTo(december.expenses()));
        assertEquals(0, BigDecimal.ZERO.compareTo(december.netSavings()));
    }

    @Test
    void shouldHandleMaximumSupportedYear() {

        int maximumYear = 9999;

        when(transactionRepository.calculateTotalByUserAndTypeAndDateRange(
                eq(userId),
                any(TransactionType.class),
                any(LocalDate.class),
                any(LocalDate.class)
        )).thenReturn(BigDecimal.ZERO);

        DashboardResponse response =
                dashboardService.getDashboard(userId, maximumYear);

        assertEquals(9999, response.year());
        assertEquals(12, response.monthlyTrends().size());

        assertEquals(
                0,
                BigDecimal.ZERO.compareTo(response.totalIncome())
        );

        assertEquals(
                0,
                BigDecimal.ZERO.compareTo(response.totalExpenses())
        );

        assertEquals(
                0,
                BigDecimal.ZERO.compareTo(response.netSavings())
        );

        verify(
                transactionRepository,
                times(26)
        ).calculateTotalByUserAndTypeAndDateRange(
                eq(userId),
                any(TransactionType.class),
                any(LocalDate.class),
                any(LocalDate.class)
        );
    }
}