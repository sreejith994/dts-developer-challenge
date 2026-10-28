CREATE TABLE task
(
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    title       VARCHAR(255)             NOT NULL,
    description VARCHAR(2000),
    status      VARCHAR(20)              NOT NULL,
    due_date    TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT task_status_check CHECK (status IN ('CREATED', 'INPROGRESS', 'BLOCKED', 'COMPLETED', 'CANCELLED'))
);

CREATE INDEX idx_task_due_date ON task (due_date);
