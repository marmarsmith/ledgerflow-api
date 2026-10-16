
package com.marcel.ledgerflow.dto;

import java.math.BigDecimal;

public record SpendingInsightResponse(

        Long categoryId,
        String categoryName,

        BigDecimal budgetAmount,
        BigDecimal spentAmount,
        BigDecimal remainingAmount,
        BigDecimal percentageUsed,

        boolean overBudget

) {
}
