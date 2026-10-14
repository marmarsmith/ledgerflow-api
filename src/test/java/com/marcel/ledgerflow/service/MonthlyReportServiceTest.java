package com.marcel.ledgerflow.service;

import com.marcel.ledgerflow.dto.MonthlyReportResponse;
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
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import com.marcel.ledgerflow.repository.CategoryExpenseProjection;

@ExtendWith(MockitoExtension.class)
class MonthlyReportServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private MonthlyReportService monthlyReportService;

    private Long userId;

    @BeforeEach
    void setUp() {
        userId = 1L;
    }

    @Test
    void getMonthlyReport_ShouldCalculateIncomeExpensesAndNetSavings() {

        LocalDate startDate = LocalDate.of(2026, 10, 1);
        LocalDate endDate = LocalDate.of(2026, 11, 1);

        when(transactionRepository.calculateTotalByUserAndTypeAndDateRange(
                userId,
                TransactionType.INCOME,
                startDate,
                endDate
        )).thenReturn(new BigDecimal("5000.00"));

        when(transactionRepository.calculateTotalByUserAndTypeAndDateRange(
                userId,
                TransactionType.EXPENSE,
                startDate,
                endDate
        )).thenReturn(new BigDecimal("1850.00"));

        when(transactionRepository.calculateExpensesByCategory(
                userId,
                TransactionType.EXPENSE,
                startDate,
                endDate
        )).thenReturn(List.of());

        MonthlyReportResponse response =
                monthlyReportService.getMonthlyReport(
                        userId,
                        2026,
                        10
                );

        assertEquals(2026, response.year());
        assertEquals(10, response.month());

        assertEquals(
                0,
                new BigDecimal("5000.00").compareTo(response.totalIncome())
        );

        assertEquals(
                0,
                new BigDecimal("1850.00").compareTo(response.totalExpenses())
        );

        assertEquals(
                0,
                new BigDecimal("3150.00").compareTo(response.netSavings())
        );

        assertTrue(response.expensesByCategory().isEmpty());

        verify(transactionRepository).calculateTotalByUserAndTypeAndDateRange(
                userId,
                TransactionType.INCOME,
                startDate,
                endDate
        );

        verify(transactionRepository).calculateTotalByUserAndTypeAndDateRange(
                userId,
                TransactionType.EXPENSE,
                startDate,
                endDate
        );
    }

    @Test
    void getMonthlyReport_ShouldReturnExpensesGroupedByCategory() {

        LocalDate startDate = LocalDate.of(2026, 10, 1);
        LocalDate endDate = LocalDate.of(2026, 11, 1);

        when(transactionRepository.calculateTotalByUserAndTypeAndDateRange(
                userId,
                TransactionType.INCOME,
                startDate,
                endDate
        )).thenReturn(new BigDecimal("5000.00"));

        when(transactionRepository.calculateTotalByUserAndTypeAndDateRange(
                userId,
                TransactionType.EXPENSE,
                startDate,
                endDate
        )).thenReturn(new BigDecimal("1850.00"));

        CategoryExpenseProjection groceries =
                mock(CategoryExpenseProjection.class);

        when(groceries.getCategoryId()).thenReturn(1L);
        when(groceries.getCategoryName()).thenReturn("Groceries");
        when(groceries.getAmount()).thenReturn(new BigDecimal("450.00"));

        CategoryExpenseProjection diningOut =
                mock(CategoryExpenseProjection.class);

        when(diningOut.getCategoryId()).thenReturn(2L);
        when(diningOut.getCategoryName()).thenReturn("Dining Out");
        when(diningOut.getAmount()).thenReturn(new BigDecimal("200.00"));

        CategoryExpenseProjection uncategorized =
                mock(CategoryExpenseProjection.class);

        when(uncategorized.getCategoryId()).thenReturn(null);
        when(uncategorized.getCategoryName()).thenReturn("Uncategorized");
        when(uncategorized.getAmount()).thenReturn(new BigDecimal("1200.00"));

        when(transactionRepository.calculateExpensesByCategory(
                userId,
                TransactionType.EXPENSE,
                startDate,
                endDate
        )).thenReturn(List.of(
                groceries,
                diningOut,
                uncategorized
        ));

        MonthlyReportResponse response =
                monthlyReportService.getMonthlyReport(userId, 2026, 10);

        assertEquals(3, response.expensesByCategory().size());

        assertEquals(1L, response.expensesByCategory().get(0).categoryId());
        assertEquals("Groceries", response.expensesByCategory().get(0).categoryName());

        assertEquals(
                0,
                new BigDecimal("450.00").compareTo(
                        response.expensesByCategory().get(0).amount()
                )
        );

        assertEquals(2L, response.expensesByCategory().get(1).categoryId());
        assertEquals("Dining Out", response.expensesByCategory().get(1).categoryName());

        assertEquals(
                0,
                new BigDecimal("200.00").compareTo(
                        response.expensesByCategory().get(1).amount()
                )
        );

        assertNull(response.expensesByCategory().get(2).categoryId());
        assertEquals(
                "Uncategorized",
                response.expensesByCategory().get(2).categoryName()
        );

        assertEquals(
                0,
                new BigDecimal("1200.00").compareTo(
                        response.expensesByCategory().get(2).amount()
                )
        );

        BigDecimal categoryTotal = response.expensesByCategory()
                .stream()
                .map(category -> category.amount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        assertEquals(
                0,
                response.totalExpenses().compareTo(categoryTotal)
        );
    }

    @Test
    void getMonthlyReport_ShouldReturnZerosWhenNoTransactionsExist() {

        LocalDate startDate = LocalDate.of(2026, 2, 1);
        LocalDate endDate = LocalDate.of(2026, 3, 1);

        when(transactionRepository.calculateTotalByUserAndTypeAndDateRange(
                userId,
                TransactionType.INCOME,
                startDate,
                endDate
        )).thenReturn(BigDecimal.ZERO);

        when(transactionRepository.calculateTotalByUserAndTypeAndDateRange(
                userId,
                TransactionType.EXPENSE,
                startDate,
                endDate
        )).thenReturn(BigDecimal.ZERO);

        when(transactionRepository.calculateExpensesByCategory(
                userId,
                TransactionType.EXPENSE,
                startDate,
                endDate
        )).thenReturn(List.of());

        MonthlyReportResponse response =
                monthlyReportService.getMonthlyReport(userId, 2026, 2);

        assertEquals(2026, response.year());
        assertEquals(2, response.month());

        assertEquals(0, BigDecimal.ZERO.compareTo(response.totalIncome()));
        assertEquals(0, BigDecimal.ZERO.compareTo(response.totalExpenses()));
        assertEquals(0, BigDecimal.ZERO.compareTo(response.netSavings()));

        assertTrue(response.expensesByCategory().isEmpty());

        verify(transactionRepository).calculateTotalByUserAndTypeAndDateRange(
                userId,
                TransactionType.INCOME,
                startDate,
                endDate
        );

        verify(transactionRepository).calculateTotalByUserAndTypeAndDateRange(
                userId,
                TransactionType.EXPENSE,
                startDate,
                endDate
        );

        verify(transactionRepository).calculateExpensesByCategory(
                userId,
                TransactionType.EXPENSE,
                startDate,
                endDate
        );
    }
}