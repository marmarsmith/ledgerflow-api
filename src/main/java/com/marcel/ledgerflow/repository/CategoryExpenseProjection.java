package com.marcel.ledgerflow.repository;

import java.math.BigDecimal;

public interface CategoryExpenseProjection {

    Long getCategoryId();

    String getCategoryName();

    BigDecimal getAmount();
}