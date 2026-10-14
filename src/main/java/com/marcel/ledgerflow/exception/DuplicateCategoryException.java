package com.marcel.ledgerflow.exception;

public class DuplicateCategoryException extends RuntimeException {

    public DuplicateCategoryException() {
        super("Category already exists for this transaction type");
    }
}