package com.marcel.ledgerflow.exception;

public class CategoryTypeMismatchException extends RuntimeException {

    public CategoryTypeMismatchException() {
        super("Category type must match transaction type");
    }
}