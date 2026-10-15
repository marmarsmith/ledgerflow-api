CREATE TABLE budgets (
                         id BIGSERIAL PRIMARY KEY,

                         user_id BIGINT NOT NULL,
                         category_id BIGINT NOT NULL,

                         year INTEGER NOT NULL,
                         month INTEGER NOT NULL,

                         amount NUMERIC(15, 2) NOT NULL,

                         created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                         updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

                         CONSTRAINT fk_budgets_user
                             FOREIGN KEY (user_id)
                                 REFERENCES users(id)
                                 ON DELETE CASCADE,

                         CONSTRAINT fk_budgets_category
                             FOREIGN KEY (category_id)
                                 REFERENCES categories(id),

                         CONSTRAINT chk_budgets_year
                             CHECK (year BETWEEN 2000 AND 2100),

    CONSTRAINT chk_budgets_month
        CHECK (month BETWEEN 1 AND 12),

    CONSTRAINT chk_budgets_amount
        CHECK (amount > 0),

    CONSTRAINT uq_budgets_user_category_month
        UNIQUE (user_id, category_id, year, month)
);

CREATE INDEX idx_budgets_user_year_month
    ON budgets(user_id, year, month);

CREATE INDEX idx_budgets_category_id
    ON budgets(category_id);