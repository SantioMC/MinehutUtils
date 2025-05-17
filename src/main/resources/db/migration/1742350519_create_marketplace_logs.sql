-- Create marketplace_logs table

CREATE TABLE IF NOT EXISTS marketplace_logs (
    id INTEGER PRIMARY KEY,
    posted_by TEXT,
    type Text,
    title TEXT,
    content TEXT,
    posted_at BIGINT
);
