-- Create the lockdown channels table

CREATE TABLE IF NOT EXISTS lockdown_channels(
    guild_id TEXT NOT NULL,
    channel_id TEXT NOT NULL,
    PRIMARY KEY (guild_id, channel_id)
);

-- Recreate the settings table to bind to guilds, will reset all setting data

DROP TABLE settings;

CREATE TABLE settings(
    guild_id TEXT NOT NULL PRIMARY KEY,
    marketplace_channel TEXT,
    marketplace_cooldown INTEGER NOT NULL,
    lockdown_role TEXT
);
