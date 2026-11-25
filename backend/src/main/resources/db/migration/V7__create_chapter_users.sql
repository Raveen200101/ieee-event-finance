CREATE TABLE chapter_users (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(254) NOT NULL,
    display_name VARCHAR(150) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    role VARCHAR(20) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    credential_version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_chapter_users_email UNIQUE (email),
    CONSTRAINT chk_chapter_users_role CHECK (role IN ('CHAIR', 'FINANCE', 'MEMBER'))
);
