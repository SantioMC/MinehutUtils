-- Add a nullable guild_id column to tags

ALTER TABLE tags
ADD COLUMN guild_id TEXT;
