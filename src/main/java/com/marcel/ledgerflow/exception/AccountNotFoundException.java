
package com.marcel.ledgerflow.exception;

public class AccountNotFoundException extends RuntimeException {

    public AccountNotFoundException() {
        super("Account not found");
    }
}
