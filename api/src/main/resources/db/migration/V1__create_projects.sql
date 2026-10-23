CREATE TABLE projects (
    id          CHAR(36)     NOT NULL,
    name        VARCHAR(200) NOT NULL,
    imported_at DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    INDEX idx_projects_imported_at (imported_at)
);
