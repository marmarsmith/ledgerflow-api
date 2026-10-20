
-- Link generated financial transactions to their recurring schedules.
-- Manually created transactions will have NULL in this column.

ALTER TABLE transactions
    ADD COLUMN recurring_transaction_id BIGINT;

-- Preserve transaction history when a recurring schedule is deleted.

ALTER TABLE transactions
    ADD CONSTRAINT fk_transactions_recurring_transaction
        FOREIGN KEY (recurring_transaction_id)
            REFERENCES recurring_transactions(id)
            ON DELETE SET NULL;

-- Prevent duplicate financial transactions for the same
-- recurring schedule and scheduled occurrence date.

CREATE UNIQUE INDEX uq_transactions_recurring_occurrence
    ON transactions (recurring_transaction_id, transaction_date)
    WHERE recurring_transaction_id IS NOT NULL;

-- Improve lookup performance for generated transactions.

CREATE INDEX idx_transactions_recurring_transaction_id
    ON transactions (recurring_transaction_id);
