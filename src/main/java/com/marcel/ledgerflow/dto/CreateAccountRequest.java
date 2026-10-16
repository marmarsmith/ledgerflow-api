
package com.marcel.ledgerflow.dto;

import com.marcel.ledgerflow.enums.AccountType;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CreateAccountRequest(

        @NotBlank(message = "Account name is required")
        @Size(max = 100, message = "Account name cannot exceed 100 characters")
        String name,

        @NotNull(message = "Account type is required")
        AccountType type,

        @NotNull(message = "Initial balance is required")
        @Digits(
                integer = 17,
                fraction = 2,
                message = "Initial balance must have at most 17 integer digits and 2 decimal places"
        )
        BigDecimal initialBalance

) {}
