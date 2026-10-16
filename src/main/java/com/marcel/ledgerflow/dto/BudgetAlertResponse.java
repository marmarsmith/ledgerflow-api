package com.marcel.ledgerflow.dto;

import com.marcel.ledgerflow.enums.BudgetAlertStatus;

import java.math.BigDecimal;

public record BudgetAlertResponse(

        Long budgetId,
        Long categoryId,
        String categoryName,

        Integer year,
        Integer month,

        BigDecimal budgetAmount,
        BigDecimal spentAmount,
        BigDecimal remainingAmount,
        BigDecimal percentageUsed,

        BudgetAlertStatus status

) {
}