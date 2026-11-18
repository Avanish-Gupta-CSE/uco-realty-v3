-- ============================================================================
-- UCO Realty: queries to run in SQL Developer on the server (UAT / production)
-- Travels inside the staff WAR: WEB-INF/classes/db_migrations/
-- Every statement below is a READ-ONLY SELECT. Nothing here creates, changes or deletes anything.
-- Run one statement at a time (select it, Ctrl+Enter), connected as the application schema user
-- (UAT: HRMS_DBA). The table script itself is 01_CREATE_TABLES.sql (next to this file in the WAR);
-- never run 03, 05, 06, 07 on UAT or production (see README_DBA_DEPLOYMENT.md).
-- ============================================================================


-- ============================================================================
-- A. BEFORE creating the tables
-- ============================================================================

-- A1. Am I connected as the right user? (UAT: must say HRMS_DBA)
SELECT USER FROM dual;

-- A2. Are our six table names free? Must return NO rows. If rows come back, STOP and ask the senior.
SELECT table_name FROM user_tables
WHERE table_name IN ('REALTY_PROJECTS','REALTY_PROJECT_STAGING','REALTY_PROJECT_IMAGES','REALTY_PROJECT_ATTACHMENTS','REALTY_AUDIT_LOG','REALTY_LOGIN_AUDIT');

-- A3. Can this user read the three bank HRMS tables? Each must return a number (not an error).
SELECT COUNT(*) AS employees FROM HRMS_DBA.employee_contact_details;
SELECT COUNT(*) AS postings  FROM HRMS_DBA.employee_tenure;
SELECT COUNT(*) AS realty_leads FROM HRMS_DBA.LG_ENTRY_REVAMP WHERE CHANNEL = 'REALTY';

-- A4. Does the HRMS contact table have the retired flag? Must return 1 row.
SELECT column_name FROM all_tab_columns
WHERE owner = 'HRMS_DBA' AND table_name = 'EMPLOYEE_CONTACT_DETAILS' AND column_name = 'RETIRE_FLAG';

-- A5. Columns the application reads from the HRMS contact table: must return 11 rows.
SELECT column_name FROM all_tab_columns
WHERE owner = 'HRMS_DBA' AND table_name = 'EMPLOYEE_CONTACT_DETAILS'
  AND column_name IN ('EMPNO','NAME','POSTING_SOL_ID','POSTING_SOL_DESC','SCALE_CODE','CADRE_CODE',
                      'POSTING_REGION_NAME','DESIG','MOBILE_NO','E_MAIL','RETIRE_FLAG');


-- ============================================================================
-- B. Now run the script 01_CREATE_TABLES.sql (Run Script, F5), then check:
-- ============================================================================

-- B1. Six tables, all VALID.
SELECT table_name, status FROM user_tables
WHERE table_name IN ('REALTY_PROJECTS','REALTY_PROJECT_STAGING','REALTY_PROJECT_IMAGES','REALTY_PROJECT_ATTACHMENTS','REALTY_AUDIT_LOG','REALTY_LOGIN_AUDIT')
ORDER BY table_name;

-- B2. Fifteen indexes of ours (the primary-key indexes are not counted here).
SELECT COUNT(*) AS indexes_expected_15 FROM user_indexes
WHERE index_name LIKE 'IDX_REALTY%' OR index_name LIKE 'UX_REALTY%';

-- B3. Four foreign keys (photo and attachment, each to a project and to a staging row) and four check constraints.
SELECT constraint_name, table_name, constraint_type FROM user_constraints
WHERE constraint_name IN ('FK_REALTY_IMG_PROJECT','FK_REALTY_IMG_STAGING','FK_REALTY_ATT_PROJECT','FK_REALTY_ATT_STAGING',
                          'CHK_REALTY_INVENTORY','CHK_REALTY_STAGING_INV','CHK_REALTY_IMAGE_OWNER','CHK_REALTY_ATT_OWNER')
ORDER BY constraint_name;

-- B4. The photo column and the PDF column must be SecureFile BLOBs (SECUREFILE = YES, 2 rows).
SELECT table_name, column_name, securefile FROM user_lobs WHERE table_name IN ('REALTY_PROJECT_IMAGES','REALTY_PROJECT_ATTACHMENTS');

-- B5. All tables are empty on a fresh database (0, 0, 0, 0, 0, 0).
SELECT (SELECT COUNT(*) FROM realty_projects)         AS projects,
       (SELECT COUNT(*) FROM realty_project_staging)  AS staging,
       (SELECT COUNT(*) FROM realty_project_images)   AS images,
       (SELECT COUNT(*) FROM realty_project_attachments) AS attachments,
       (SELECT COUNT(*) FROM realty_audit_log)        AS audit_rows,
       (SELECT COUNT(*) FROM realty_login_audit)      AS logins
FROM dual;


-- ============================================================================
-- C. Finding real employees to test with (UAT)
--    The test login accepts ANY employee found in HRMS; the role comes from the posting.
-- ============================================================================

-- C1. DIT superadmin (SOL 7115; on UAT the DIT staff may sit at 7103)
SELECT empno, name, posting_sol_id, scale_code, cadre_code, retire_flag
FROM HRMS_DBA.employee_contact_details
WHERE posting_sol_id IN ('7115','7103') AND NVL(retire_flag,'N') <> 'RT';

-- C2. HO Marketing and Wealth Management (SOL 8028)
SELECT empno, name, posting_sol_id, scale_code, cadre_code, retire_flag
FROM HRMS_DBA.employee_contact_details
WHERE posting_sol_id = '8028' AND NVL(retire_flag,'N') <> 'RT';

-- C3. HO Retail Banking (SOL 8001)
SELECT empno, name, posting_sol_id, scale_code, cadre_code, retire_flag
FROM HRMS_DBA.employee_contact_details
WHERE posting_sol_id = '8001' AND NVL(retire_flag,'N') <> 'RT';

-- C4. A zonal maker (scale 1 to 3) and checker (scale 4 or more, or cadre EXEC) of ONE zone. Example: Bhopal, SOL 7013.
SELECT empno, name, posting_sol_id, scale_code, cadre_code
FROM HRMS_DBA.employee_contact_details
WHERE posting_sol_id = '7013' AND NVL(retire_flag,'N') <> 'RT'
ORDER BY scale_code;

-- C5. The application also reads the CURRENT posting from the tenure table, and that one wins over the contact row.
--     For each tester, replace <EMPNO>:
SELECT empno, posting_sol, scale_code, tenure_as
FROM HRMS_DBA.employee_tenure
WHERE empno = '<EMPNO>' AND SYSDATE BETWEEN eff_from_date AND eff_to_date AND status_flag = 'V';

-- C6. Zone names: the text must read "<ZONE NAME> ZONAL OFFICE" and match UcoRealtyStaff/.../geo/zones.json
SELECT DISTINCT posting_sol_id, posting_sol_desc
FROM HRMS_DBA.employee_contact_details
WHERE posting_sol_id BETWEEN '7001' AND '7099'
ORDER BY 1;


-- ============================================================================
-- D. After the application has run (any time)
-- ============================================================================

-- D1. Who tried to sign in, and what happened (newest first). One row per attempt, success or failure.
SELECT username, ip_address, success, remarks, login_time
FROM realty_login_audit
ORDER BY login_time DESC
FETCH FIRST 50 ROWS ONLY;

-- D2. What makers and checkers did (newest first).
SELECT entity_type, entity_id, action, by_user, created_at
FROM realty_audit_log
ORDER BY created_at DESC
FETCH FIRST 50 ROWS ONLY;

-- D3. Projects by state: draft work in staging, live projects in the master table.
SELECT status, request_type, COUNT(*) AS requests FROM realty_project_staging GROUP BY status, request_type ORDER BY 1, 2;
SELECT status, COUNT(*) AS projects FROM realty_projects GROUP BY status ORDER BY 1;

-- D4. How much space the photos and the PDF attachments use (megabytes).
SELECT 'PHOTOS' AS kind, COUNT(*) AS files, ROUND(NVL(SUM(file_size), 0)/1024/1024, 1) AS megabytes FROM realty_project_images
UNION ALL
SELECT 'ATTACHMENTS', COUNT(*), ROUND(NVL(SUM(file_size), 0)/1024/1024, 1) FROM realty_project_attachments;

-- D5. Projects with the most attachments (top 10).
SELECT project_id, COUNT(*) AS attachments FROM realty_project_attachments
WHERE project_id IS NOT NULL GROUP BY project_id ORDER BY 2 DESC FETCH FIRST 10 ROWS ONLY;
