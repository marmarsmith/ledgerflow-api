package com.marcel.ledgerflow.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record SavingsContributionResponse(
        Long id,
        Long savingsGoalId,
        BigDecimal amount,
        LocalDateTime contributedAt
) {
}