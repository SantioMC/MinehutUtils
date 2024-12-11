-- Create your migration here

CREATE TABLE IF NOT EXISTS settings
(
    id                   INTEGER PRIMARY KEY,
    marketplace_channel  TEXT,
    marketplace_cooldown INTEGER NOT NULL
);

-- Seeding
INSERT INTO settings (id, marketplace_channel, marketplace_cooldown)
VALUES (1,
-- Marketplace configuration
        NULL,
        86400);
