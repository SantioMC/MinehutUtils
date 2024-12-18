-- Add a last_used column to the tags table

ALTER TABLE tags ADD COLUMN last_used BIGINT NOT NULL DEFAULT 0;
