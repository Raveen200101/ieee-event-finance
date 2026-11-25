CREATE TABLE budget_categories (
    id BIGINT NOT NULL AUTO_INCREMENT,
    event_id BIGINT NOT NULL,
    category VARCHAR(50) NOT NULL,
    category_key VARCHAR(50) COLLATE utf8mb4_bin NOT NULL,
    owner VARCHAR(150) NOT NULL,
    planned_amount DECIMAL(15,2) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_budget_categories_event FOREIGN KEY (event_id) REFERENCES events(id),
    CONSTRAINT uq_budget_categories_event_key UNIQUE (event_id, category_key),
    CONSTRAINT chk_budget_categories_amount CHECK (planned_amount >= 0)
);
