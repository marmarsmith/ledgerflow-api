package com.marcel.ledgerflow.dto;

import java.math.BigDecimal;
import java.util.List;

public record DashboardResponse(
        int year,
        BigDecimal totalIncome,
        BigDecimal totalExpenses,
        BigDecimal netSavings,
        List<MonthlyTrendResponse> monthlyTrends
) {
}