CREATE TABLE categories (
                            id BIGSERIAL PRIMARY KEY,

                            user_id BIGINT NOT NULL,

                            name VARCHAR(100) NOT NULL,

                            type VARCHAR(20) NOT NULL,

                            created_at TIMESTAMP WITH TIME ZONE NOT NULL,

                            CONSTRAINT fk_categories_user
                                FOREIGN KEY (user_id)
                                    REFERENCES users(id),

                            CONSTRAINT chk_categories_type
                                CHECK (type IN ('INCOME', 'EXPENSE')),

                            CONSTRAINT uq_categories_user_name_type
                                UNIQUE (user_id, name, type)
);