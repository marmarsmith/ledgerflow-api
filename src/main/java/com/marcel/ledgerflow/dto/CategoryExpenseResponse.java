package com.marcel.ledgerflow.dto;

import java.math.BigDecimal;

public record CategoryExpenseResponse(
        Long categoryId,
        String categoryName,
        BigDecimal amount
) {
}