-- 4.sql
-- Creates an expedition sequence to handle IDs.
-- An expedition table is not needed at the moment.
-- Also adds expedition_id column to player.characters


BEGIN;

CREATE SEQUENCE expedition_id_seq START 1;

ALTER TABLE player.characters
ADD COLUMN expedition_id INTEGER;

COMMIT;
