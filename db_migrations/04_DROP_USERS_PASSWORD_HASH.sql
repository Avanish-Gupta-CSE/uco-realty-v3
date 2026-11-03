-- ============================================================================
-- Migration for EXISTING databases created before the password login was removed.
-- Staff now sign in only through bank SSO / OTP, so users.password_hash is unused.
-- New databases created from 01_CREATE_TABLES.sql do not have the column (skip this script).
-- Safe to re-run: it does nothing when the column is already gone.
-- ============================================================================
DECLARE
  v_count NUMBER;
BEGIN
  SELECT COUNT(*) INTO v_count FROM user_tab_columns
   WHERE table_name = 'USERS' AND column_name = 'PASSWORD_HASH';
  IF v_count > 0 THEN
    EXECUTE IMMEDIATE 'ALTER TABLE users DROP COLUMN password_hash';
  END IF;
END;
/
