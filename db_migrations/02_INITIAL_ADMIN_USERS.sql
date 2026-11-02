-- ============================================================================
-- UCO Bank Realty Portal - Initial Administrative Users
-- Target Environments: UAT, PRE-PROD, PRODUCTION
-- Notes:
--   - NO password is stored in this file. DIT supplies one BCrypt hash per account
--     (see README_DBA_DEPLOYMENT.md, section "Creating the admin password hashes").
--   - Run once after 01_CREATE_TABLES.sql, in SQL*Plus or SQL Developer (it asks for the hashes).
--   - Idempotent: MERGE prevents duplicate rows. Re-running does NOT change an existing password.
--   - Each admin must change the password at first login (menu: Change password).
-- ============================================================================

SET VERIFY OFF
ACCEPT dit_hash CHAR PROMPT 'BCrypt hash for dit.superadmin: '
ACCEPT ho_hash  CHAR PROMPT 'BCrypt hash for ho.admin: '

MERGE INTO users u
USING (
  SELECT 'dit.superadmin' AS username, '&dit_hash' AS password_hash, 'SUPERADMIN_DIT' AS role, NULL AS zone, NULL AS branch FROM dual
  UNION ALL
  SELECT 'ho.admin', '&ho_hash', 'HO_ADMIN', NULL, NULL FROM dual
) src
ON (u.username = src.username)
WHEN NOT MATCHED THEN
  INSERT (username, password_hash, role, zone, branch, is_active, created_at)
  VALUES (src.username, src.password_hash, src.role, src.zone, src.branch, 1, SYSTIMESTAMP);

COMMIT;

PROMPT Initial admin users (dit.superadmin, ho.admin) created.
