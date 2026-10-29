
package com.marcel.ledgerflow.exception;

public class AccountHasRecurringTransactionsException
        extends RuntimeException {

    public AccountHasRecurringTransactionsException() {
        super(
                "Cannot delete an account that has recurring transactions."
        );
    }
}
