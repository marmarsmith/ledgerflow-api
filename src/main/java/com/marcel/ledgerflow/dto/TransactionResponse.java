package com.marcel.ledgerflow.dto;

import com.marcel.ledgerflow.enums.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

public record TransactionResponse(
        Long id,
        Long accountId,
        TransactionType type,
        BigDecimal amount,
        String description,
        LocalDate transactionDate,
        OffsetDateTime createdAt,
        Long categoryId
) {
}