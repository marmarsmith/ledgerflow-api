
package com.marcel.ledgerflow.dto;

import com.marcel.ledgerflow.enums.AccountType;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record UpdateAccountRequest(

        @NotBlank
        @Size(max = 100)
        String name,

        @NotNull
        AccountType type,

        @NotNull
        @Digits(integer = 17, fraction = 2)
        BigDecimal initialBalance

) {
}
