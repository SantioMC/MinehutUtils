-- Create guild_data table

CREATE TABLE IF NOT EXISTS guild_data(
    guild_id TEXT NOT NULL PRIMARY KEY,
    sticky_message TEXT
);
