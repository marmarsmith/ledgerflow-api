package com.marcel.ledgerflow.dto;

import java.math.BigDecimal;

public record MonthlyTrendResponse(
        int month,
        BigDecimal income,
        BigDecimal expenses,
        BigDecimal netSavings
) {
}