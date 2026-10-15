CREATE TABLE savings_goals (
                               id BIGSERIAL PRIMARY KEY,
                               user_id BIGINT NOT NULL,
                               name VARCHAR(100) NOT NULL,
                               target_amount NUMERIC(15, 2) NOT NULL,
                               saved_amount NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
                               target_date DATE,
                               created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                               updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

                               CONSTRAINT fk_savings_goals_user
                                   FOREIGN KEY (user_id)
                                       REFERENCES users(id)
                                       ON DELETE CASCADE,

                               CONSTRAINT chk_savings_goals_target_amount
                                   CHECK (target_amount > 0),

                               CONSTRAINT chk_savings_goals_saved_amount
                                   CHECK (saved_amount >= 0)
);

CREATE INDEX idx_savings_goals_user_id
    ON savings_goals(user_id);