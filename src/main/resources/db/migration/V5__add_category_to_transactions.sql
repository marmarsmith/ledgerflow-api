ALTER TABLE transactions
    ADD COLUMN category_id BIGINT;

ALTER TABLE transactions
    ADD CONSTRAINT fk_transactions_category
        FOREIGN KEY (category_id)
            REFERENCES categories(id)
            ON DELETE SET NULL;

CREATE INDEX idx_transactions_category_id
    ON transactions(category_id);