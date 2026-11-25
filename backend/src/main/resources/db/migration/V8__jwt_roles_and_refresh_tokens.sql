CREATE TABLE roles (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(40) NOT NULL,
    CONSTRAINT uq_roles_name UNIQUE (name)
);
INSERT INTO roles (name) VALUES ('COMMITTEE_MEMBER'), ('VOLUNTEER'), ('FINANCE_TEAM_LEAD'), ('LOGISTICS_TEAM_LEAD');
ALTER TABLE chapter_users
    ADD COLUMN role_id BIGINT NULL,
    ADD COLUMN account_admin BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    ADD COLUMN updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6);
UPDATE chapter_users u JOIN roles r ON r.name = CASE WHEN u.role = 'MEMBER' THEN 'VOLUNTEER' WHEN u.role = 'FINANCE' THEN 'FINANCE_TEAM_LEAD' ELSE 'COMMITTEE_MEMBER' END
SET u.role_id = r.id, u.account_admin = (u.role = 'CHAIR');
ALTER TABLE chapter_users DROP CHECK chk_chapter_users_role, DROP COLUMN role,
    MODIFY role_id BIGINT NOT NULL,
    ADD CONSTRAINT fk_chapter_users_role FOREIGN KEY (role_id) REFERENCES roles(id);
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

CREATE TABLE event_memberships (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    event_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    CONSTRAINT uq_event_member UNIQUE (event_id,user_id),
    CONSTRAINT fk_membership_event FOREIGN KEY(event_id) REFERENCES events(id),
    CONSTRAINT fk_membership_user FOREIGN KEY(user_id) REFERENCES chapter_users(id),
    INDEX idx_membership_user(user_id)
);
ALTER TABLE expenses ADD COLUMN created_by_id BIGINT NULL, ADD CONSTRAINT fk_expense_creator FOREIGN KEY(created_by_id) REFERENCES chapter_users(id);
ALTER TABLE incomes ADD COLUMN created_by_id BIGINT NULL, ADD CONSTRAINT fk_income_creator FOREIGN KEY(created_by_id) REFERENCES chapter_users(id);
ALTER TABLE purchase_groups ADD COLUMN owner_id BIGINT NULL, ADD CONSTRAINT fk_purchase_owner FOREIGN KEY(owner_id) REFERENCES chapter_users(id);
ALTER TABLE quotations ADD COLUMN created_by_id BIGINT NULL, ADD CONSTRAINT fk_quotation_creator FOREIGN KEY(created_by_id) REFERENCES chapter_users(id);

CREATE TABLE income_sources (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 event_id BIGINT NOT NULL,
 name VARCHAR(150) NOT NULL,
 name_key VARCHAR(150) NOT NULL,
 CONSTRAINT uq_income_source UNIQUE(event_id,name_key),
 CONSTRAINT fk_income_source_event FOREIGN KEY(event_id) REFERENCES events(id)
);
INSERT INTO income_sources(event_id,name,name_key) SELECT event_id,MIN(TRIM(source)),LOWER(TRIM(source)) FROM incomes GROUP BY event_id,LOWER(TRIM(source));
