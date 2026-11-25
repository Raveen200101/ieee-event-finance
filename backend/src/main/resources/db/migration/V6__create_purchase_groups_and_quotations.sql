CREATE TABLE purchase_groups (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    event_id BIGINT NOT NULL,
    budget_category_id BIGINT NOT NULL,
    name VARCHAR(150) NOT NULL,
    notes VARCHAR(1000) NOT NULL,
    CONSTRAINT fk_purchase_group_event FOREIGN KEY (event_id) REFERENCES events(id),
    CONSTRAINT fk_purchase_group_budget FOREIGN KEY (budget_category_id) REFERENCES budget_categories(id),
    INDEX idx_purchase_group_event (event_id)
);
CREATE TABLE quotations (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    purchase_group_id BIGINT NOT NULL,
    supplier VARCHAR(150) NOT NULL,
    quantity INT NOT NULL,
    unit_price DECIMAL(15,2) NOT NULL,
    notes VARCHAR(1000) NOT NULL,
    selected BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_quotation_group FOREIGN KEY (purchase_group_id) REFERENCES purchase_groups(id),
    CONSTRAINT chk_quotation_quantity CHECK (quantity BETWEEN 1 AND 1000000),
    CONSTRAINT chk_quotation_price CHECK (unit_price > 0),
    INDEX idx_quotation_group (purchase_group_id)
);
