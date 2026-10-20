
package com.marcel.ledgerflow.dto;

import com.marcel.ledgerflow.enums.RecurringFrequency;
import com.marcel.ledgerflow.enums.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDate;

public record RecurringTransactionResponse(

        Long id,

        Long accountId,

        Long categoryId,

        BigDecimal amount,

        String description,

        TransactionType type,

        RecurringFrequency frequency,

        LocalDate startDate,

        LocalDate nextOccurrenceDate,

        LocalDate endDate,

        boolean active

) {
}
