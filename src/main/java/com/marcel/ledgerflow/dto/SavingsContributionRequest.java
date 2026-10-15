package com.marcel.ledgerflow.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record SavingsContributionRequest(

        @NotNull(message = "Contribution amount is required")
        @DecimalMin(
                value = "0.00",
                inclusive = false,
                message = "Contribution amount must be greater than zero"
        )
        @Digits(
                integer = 13,
                fraction = 2,
                message = "Contribution amount must have at most 13 integer digits and 2 decimal places"
        )
        BigDecimal amount

) {
}