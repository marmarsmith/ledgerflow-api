CREATE TABLE savings_contributions (
                                       id BIGSERIAL PRIMARY KEY,

                                       savings_goal_id BIGINT NOT NULL,

                                       amount NUMERIC(15, 2) NOT NULL,

                                       contributed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                       CONSTRAINT fk_savings_contributions_goal
                                           FOREIGN KEY (savings_goal_id)
                                               REFERENCES savings_goals(id)
                                               ON DELETE CASCADE,

                                       CONSTRAINT chk_savings_contributions_amount
                                           CHECK (amount > 0)
);

CREATE INDEX idx_savings_contributions_goal_id
    ON savings_contributions(savings_goal_id);