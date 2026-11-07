-- ============================================================================
-- OPTIONAL: four test employees for UAT, in the bank's UAT HRMS table (only if your senior / DBA agrees).
-- Use it ONLY when you cannot get real UAT employee numbers. Never in production.
-- They sign in with the test login: employee number + OTP 123456.
--
-- STEP 1 (before inserting): see which columns of the real table cannot be empty. Every NOT NULL column that is not in the
--        INSERT below (for example DOB, DOJ) must be added to the INSERT with a value, or the insert fails.
--
SELECT column_name, data_type, data_length, nullable
FROM all_tab_columns
WHERE owner = 'HRMS_DBA' AND table_name = 'EMPLOYEE_CONTACT_DETAILS'
ORDER BY column_id;

-- STEP 2: make sure the numbers are free (must return no rows)
SELECT empno FROM employee_contact_details WHERE empno IN ('990001','990002','990003','990004');

-- STEP 3: insert (SOL 7013 = Bhopal zonal office, as seen in the bank's SOL list)
INSERT INTO employee_contact_details (EMPNO, NAME, POSTING_SOL_ID, POSTING_SOL_DESC, SCALE_CODE, CADRE_CODE, POSTING_REGION_NAME, DESIG, RETIRE_FLAG)
VALUES ('990001', 'UAT Test DIT Superadmin', '7115', 'DIT-SWDC HEAD OFFICE', '3', 'OFFC', 'HEAD OFFICE', 'UAT Test', 'N');
INSERT INTO employee_contact_details (EMPNO, NAME, POSTING_SOL_ID, POSTING_SOL_DESC, SCALE_CODE, CADRE_CODE, POSTING_REGION_NAME, DESIG, RETIRE_FLAG)
VALUES ('990002', 'UAT Test HO Marketing', '8028', 'HO MARKETING AND WEALTH MANAGEMENT DEPARTMENT', '4', 'EXEC', 'HEAD OFFICE', 'UAT Test', 'N');
INSERT INTO employee_contact_details (EMPNO, NAME, POSTING_SOL_ID, POSTING_SOL_DESC, SCALE_CODE, CADRE_CODE, POSTING_REGION_NAME, DESIG, RETIRE_FLAG)
VALUES ('990003', 'UAT Test Zonal Maker', '7013', 'BHOPAL ZONAL OFFICE', '2', 'OFFC', 'BHOPAL', 'UAT Test', 'N');
INSERT INTO employee_contact_details (EMPNO, NAME, POSTING_SOL_ID, POSTING_SOL_DESC, SCALE_CODE, CADRE_CODE, POSTING_REGION_NAME, DESIG, RETIRE_FLAG)
VALUES ('990004', 'UAT Test Zonal Checker', '7013', 'BHOPAL ZONAL OFFICE', '4', 'EXEC', 'BHOPAL', 'UAT Test', 'N');
COMMIT;

-- STEP 4: check
SELECT empno, name, posting_sol_id, posting_sol_desc, scale_code, cadre_code FROM employee_contact_details WHERE empno LIKE '9900%';

-- WHEN TESTING IS OVER: remove them again
-- DELETE FROM employee_contact_details WHERE empno IN ('990001','990002','990003','990004');
-- COMMIT;
