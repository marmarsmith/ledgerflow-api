package com.marcel.ledgerflow.exception;

public class BudgetNotFoundException extends RuntimeException {

    public BudgetNotFoundException() {
        super("Budget not found");
    }
}