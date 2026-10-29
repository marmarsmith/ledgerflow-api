
package com.marcel.ledgerflow.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class AccountHasTransactionsException extends RuntimeException {

    public AccountHasTransactionsException() {
        super("Cannot delete an account that contains transactions.");
    }
}
