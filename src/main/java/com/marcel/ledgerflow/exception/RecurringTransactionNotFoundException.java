package com.marcel.ledgerflow.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class RecurringTransactionNotFoundException extends RuntimeException {

    public RecurringTransactionNotFoundException() {
        super("Recurring transaction not found");
    }
}
