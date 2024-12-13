-- Add the log_channel column to settings

ALTER TABLE settings
ADD COLUMN log_channel TEXT;
