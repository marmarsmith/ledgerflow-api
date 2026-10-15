package com.marcel.ledgerflow.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record UpdateBudgetRequest(

        @NotNull(message = "Budget amount is required")
        @DecimalMin(
                value = "0.00",
                inclusive = false,
                message = "Budget amount must be greater than zero"
        )
        @Digits(
                integer = 13,
                fraction = 2,
                message = "Budget amount must have at most 13 integer digits and 2 decimal places"
        )
        BigDecimal amount

) {
}