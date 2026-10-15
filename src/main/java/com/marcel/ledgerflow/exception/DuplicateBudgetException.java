package com.marcel.ledgerflow.exception;

public class DuplicateBudgetException extends RuntimeException {

    public DuplicateBudgetException() {
        super("A budget already exists for this category and month");
    }
}