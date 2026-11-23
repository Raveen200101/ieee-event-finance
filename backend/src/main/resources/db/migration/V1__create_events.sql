CREATE TABLE events (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(150) NOT NULL,
    description VARCHAR(2000) NOT NULL,
    event_date DATE NOT NULL,
    venue VARCHAR(255) NOT NULL,
    planned_budget DECIMAL(15, 2) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT chk_events_budget CHECK (planned_budget >= 0)
);
