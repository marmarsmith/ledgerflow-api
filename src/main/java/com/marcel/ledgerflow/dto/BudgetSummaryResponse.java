package com.marcel.ledgerflow.dto;

import java.math.BigDecimal;

public record BudgetSummaryResponse(

        Integer year,
        Integer month,

        BigDecimal totalBudgeted,
        BigDecimal totalSpent,
        BigDecimal totalRemaining,
        BigDecimal overallPercentageUsed,

        Integer overBudgetCount,
        Integer nearLimitCount

) {
}