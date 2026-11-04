-- ============================================================================
-- Migration for EXISTING databases: drops two tables the application no longer uses.
--   USERS             staff identity now comes from HRMS at login and lives in the session (no table to keep in sync)
--   LEAD_TRANSFER_LOG a queue for a future push to the bank LPS system that nothing ever read
-- New databases created from 01_CREATE_TABLES.sql do not have them (skip this script).
-- Safe to re-run: it does nothing when a table is already gone. Take a backup first if you want to keep the history.
-- ============================================================================
DECLARE
  PROCEDURE drop_if_exists(p_table VARCHAR2) IS
    v_count NUMBER;
  BEGIN
    SELECT COUNT(*) INTO v_count FROM user_tables WHERE table_name = UPPER(p_table);
    IF v_count > 0 THEN
      EXECUTE IMMEDIATE 'DROP TABLE ' || p_table || ' CASCADE CONSTRAINTS PURGE';
    END IF;
  END;
BEGIN
  drop_if_exists('LEAD_TRANSFER_LOG');
  drop_if_exists('USERS');
END;
/
