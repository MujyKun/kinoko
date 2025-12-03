-- 6.sql
-- Includes migration updates such as changing player.stats.exp
-- from INT to BIGINT to support higher experience values.


BEGIN;

ALTER TABLE player.stats
ALTER COLUMN exp TYPE BIGINT;


COMMIT;
