ALTER TABLE events
    ADD COLUMN term VARCHAR(30) NULL,
    ADD COLUMN finance_lead VARCHAR(150) NULL,
    ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'Planning',
    ADD COLUMN expected_participants INT NULL,
    ADD CONSTRAINT chk_event_participants CHECK (expected_participants >= 0);
