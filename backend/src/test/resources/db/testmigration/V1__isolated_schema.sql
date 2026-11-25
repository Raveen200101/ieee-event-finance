-- Isolated H2 schema mirror. Production MySQL migrations are tested separately.
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

CREATE TABLE budget_categories (
    id BIGINT NOT NULL AUTO_INCREMENT,
    event_id BIGINT NOT NULL,
    category VARCHAR(50) NOT NULL,
    category_key VARCHAR(50) NOT NULL,
    owner VARCHAR(150) NOT NULL,
    planned_amount DECIMAL(15,2) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_budget_categories_event FOREIGN KEY (event_id) REFERENCES events(id),
    CONSTRAINT uq_budget_categories_event_key UNIQUE (event_id, category_key),
    CONSTRAINT chk_budget_categories_amount CHECK (planned_amount >= 0)
);

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

ALTER TABLE events ADD COLUMN term VARCHAR(30);
ALTER TABLE events ADD COLUMN finance_lead VARCHAR(150);
ALTER TABLE events ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'Planning';
ALTER TABLE events ADD COLUMN expected_participants INT;
CREATE TABLE roles(id BIGINT AUTO_INCREMENT PRIMARY KEY,name VARCHAR(40) NOT NULL UNIQUE);
INSERT INTO roles(name) VALUES ('COMMITTEE_MEMBER'),('VOLUNTEER'),('FINANCE_TEAM_LEAD'),('LOGISTICS_TEAM_LEAD');
CREATE TABLE chapter_users(id BIGINT AUTO_INCREMENT PRIMARY KEY,email VARCHAR(254) NOT NULL UNIQUE,display_name VARCHAR(150) NOT NULL,password_hash VARCHAR(100) NOT NULL,role_id BIGINT NOT NULL,enabled BOOLEAN NOT NULL DEFAULT TRUE,credential_version BIGINT NOT NULL DEFAULT 0,account_admin BOOLEAN NOT NULL DEFAULT FALSE,created_at TIMESTAMP(6) NOT NULL,updated_at TIMESTAMP(6) NOT NULL,FOREIGN KEY(role_id) REFERENCES roles(id));
CREATE TABLE refresh_tokens (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    token_hash VARCHAR(64) NOT NULL,
    family_id VARCHAR(36) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    expires_at TIMESTAMP(6) NOT NULL,
    revoked_at TIMESTAMP(6) NULL,
    CONSTRAINT uq_refresh_token_hash UNIQUE (token_hash),
    CONSTRAINT fk_refresh_tokens_user FOREIGN KEY (user_id) REFERENCES chapter_users(id),
    INDEX idx_refresh_family (family_id),
    INDEX idx_refresh_user (user_id),
    INDEX idx_refresh_expiry (expires_at)
);

CREATE TABLE event_memberships(id BIGINT AUTO_INCREMENT PRIMARY KEY,event_id BIGINT NOT NULL,user_id BIGINT NOT NULL,UNIQUE(event_id,user_id),FOREIGN KEY(event_id) REFERENCES events(id),FOREIGN KEY(user_id) REFERENCES chapter_users(id));
ALTER TABLE expenses ADD COLUMN created_by_id BIGINT;
ALTER TABLE incomes ADD COLUMN created_by_id BIGINT;
ALTER TABLE purchase_groups ADD COLUMN owner_id BIGINT;
ALTER TABLE quotations ADD COLUMN created_by_id BIGINT;

CREATE TABLE income_sources (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 event_id BIGINT NOT NULL,
 name VARCHAR(150) NOT NULL,
 name_key VARCHAR(150) NOT NULL,
 CONSTRAINT uq_income_source UNIQUE(event_id,name_key),
 CONSTRAINT fk_income_source_event FOREIGN KEY(event_id) REFERENCES events(id)
);
