package com.marcel.ledgerflow.dto;

import com.marcel.ledgerflow.enums.AccountType;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record AccountResponse(
        Long id,
        String name,
        AccountType type,
        BigDecimal initialBalance,
        BigDecimal currentBalance,
        OffsetDateTime createdAt
) {}