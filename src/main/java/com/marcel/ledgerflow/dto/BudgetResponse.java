package com.marcel.ledgerflow.dto;

import java.math.BigDecimal;

public record BudgetResponse(

        Long id,

        Long categoryId,

        String categoryName,

        Integer year,

        Integer month,

        BigDecimal amount,

        BigDecimal spentAmount,

        BigDecimal remainingAmount,

        BigDecimal percentageUsed,

        boolean overBudget

) {
}