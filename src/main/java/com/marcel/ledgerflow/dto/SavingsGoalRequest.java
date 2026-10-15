package com.marcel.ledgerflow.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record SavingsGoalRequest(

        @NotBlank(message = "Goal name is required")
        @Size(max = 100, message = "Goal name cannot exceed 100 characters")
        String name,

        @NotNull(message = "Target amount is required")
        @DecimalMin(
                value = "0.00",
                inclusive = false,
                message = "Target amount must be greater than zero"
        )
        @Digits(
                integer = 13,
                fraction = 2,
                message = "Target amount must have at most 13 integer digits and 2 decimal places"
        )
        BigDecimal targetAmount,

        LocalDate targetDate

) {
}