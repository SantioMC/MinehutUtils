-- Add a non-null paid column to marketplace_logs

ALTER TABLE marketplace_logs
ADD COLUMN paid BOOLEAN NOT NULL DEFAULT FALSE;
