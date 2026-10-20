
package com.marcel.ledgerflow.dto;

import com.marcel.ledgerflow.enums.RecurringFrequency;
import com.marcel.ledgerflow.enums.TransactionType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record RecurringTransactionRequest(

        @NotNull(message = "Account ID is required")
        Long accountId,

        Long categoryId,

        @NotNull(message = "Amount is required")
        @DecimalMin(
                value = "0.01",
                message = "Amount must be greater than zero"
        )
        @Digits(
                integer = 17,
                fraction = 2,
                message = "Amount must have at most 17 integer digits and 2 decimal places"
        )
        BigDecimal amount,

        @NotBlank(message = "Description is required")
        @Size(
                max = 255,
                message = "Description cannot exceed 255 characters"
        )
        String description,

        @NotNull(message = "Transaction type is required")
        TransactionType type,

        @NotNull(message = "Recurring frequency is required")
        RecurringFrequency frequency,

        @NotNull(message = "Start date is required")
        LocalDate startDate,

        LocalDate endDate

) {
}
