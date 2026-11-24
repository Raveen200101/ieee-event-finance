CREATE TABLE incomes (
    id BIGINT NOT NULL AUTO_INCREMENT,
    event_id BIGINT NOT NULL,
    description VARCHAR(500) NOT NULL,
    source VARCHAR(150) NOT NULL,
    amount DECIMAL(15,2) NOT NULL,
    income_date DATE NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_incomes_event FOREIGN KEY (event_id) REFERENCES events(id),
    CONSTRAINT chk_incomes_amount CHECK (amount > 0),
    INDEX idx_incomes_event_date (event_id, income_date, id)
);
