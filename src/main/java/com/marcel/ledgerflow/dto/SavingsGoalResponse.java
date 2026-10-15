package com.marcel.ledgerflow.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record SavingsGoalResponse(
        Long id,
        String name,
        BigDecimal targetAmount,
        BigDecimal savedAmount,
        BigDecimal remainingAmount,
        BigDecimal progressPercentage,
        LocalDate targetDate
) {
}