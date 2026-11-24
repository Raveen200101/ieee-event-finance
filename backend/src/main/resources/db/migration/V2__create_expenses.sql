CREATE TABLE expenses (
    id BIGINT NOT NULL AUTO_INCREMENT,
    event_id BIGINT NOT NULL,
    description VARCHAR(500) NOT NULL,
    category VARCHAR(50) NOT NULL,
    amount DECIMAL(15,2) NOT NULL,
    expense_date DATE NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_expenses_event FOREIGN KEY (event_id) REFERENCES events(id),
    CONSTRAINT chk_expenses_amount CHECK (amount > 0),
    INDEX idx_expenses_event_date (event_id, expense_date, id)
);
