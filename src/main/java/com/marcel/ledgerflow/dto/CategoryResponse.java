package com.marcel.ledgerflow.dto;

import com.marcel.ledgerflow.enums.TransactionType;
import java.time.OffsetDateTime;

public record CategoryResponse(
        Long id,
        String name,
        TransactionType type,
        OffsetDateTime createdAt
) {}