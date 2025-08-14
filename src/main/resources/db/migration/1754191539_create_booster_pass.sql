-- Create booster_pass table

CREATE TABLE IF NOT EXISTS booster_pass (
    id INTEGER PRIMARY KEY,
    guild_id TEXT NOT NULL,
    giver TEXT NOT NULL,
    receiver TEXT NOT NULL,
    given_at INTEGER NOT NULL
);

-- add booster configuration to settings table
ALTER TABLE settings ADD COLUMN booster_pass_role TEXT;
ALTER TABLE settings ADD COLUMN max_booster_passes INTEGER;
