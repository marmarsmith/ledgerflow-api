
CREATE TABLE recurring_transactions (
                                        id BIGSERIAL PRIMARY KEY,

                                        user_id BIGINT NOT NULL,
                                        account_id BIGINT NOT NULL,
                                        category_id BIGINT,

                                        amount NUMERIC(19, 2) NOT NULL,
                                        description VARCHAR(255) NOT NULL,

                                        type VARCHAR(20) NOT NULL,
                                        frequency VARCHAR(20) NOT NULL,

                                        start_date DATE NOT NULL,
                                        next_occurrence_date DATE NOT NULL,
                                        end_date DATE,

                                        active BOOLEAN NOT NULL DEFAULT TRUE,

                                        CONSTRAINT fk_recurring_transactions_user
                                            FOREIGN KEY (user_id)
                                                REFERENCES users(id),

                                        CONSTRAINT fk_recurring_transactions_account
                                            FOREIGN KEY (account_id)
                                                REFERENCES accounts(id),

                                        CONSTRAINT fk_recurring_transactions_category
                                            FOREIGN KEY (category_id)
                                                REFERENCES categories(id),

                                        CONSTRAINT chk_recurring_transactions_amount
                                            CHECK (amount > 0),

                                        CONSTRAINT chk_recurring_transactions_frequency
                                            CHECK (frequency IN ('DAILY', 'WEEKLY', 'MONTHLY')),

                                        CONSTRAINT chk_recurring_transactions_dates
                                            CHECK (end_date IS NULL OR end_date >= start_date)
);

CREATE INDEX idx_recurring_transactions_user_id
    ON recurring_transactions(user_id);

CREATE INDEX idx_recurring_transactions_due
    ON recurring_transactions(next_occurrence_date)
    WHERE active = TRUE;
