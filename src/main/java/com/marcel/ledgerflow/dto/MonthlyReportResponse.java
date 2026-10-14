package com.marcel.ledgerflow.dto;

import java.math.BigDecimal;
import java.util.List;

public record MonthlyReportResponse(
        int year,
        int month,
        BigDecimal totalIncome,
        BigDecimal totalExpenses,
        BigDecimal netSavings,
        List<CategoryExpenseResponse> expensesByCategory
) {
}
