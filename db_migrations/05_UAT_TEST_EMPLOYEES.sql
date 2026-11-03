-- ============================================================================
-- UAT TEST EMPLOYEES (UAT / local only - NEVER run in production)
-- Adds fictitious employees to the HRMS employee table so every portal role can be tested through
-- the normal bank SSO + HRMS lookup. The application derives the role at login; nothing is stored here.
--
-- Target table: employee_contact_details (same table/columns the application queries):
--   EMPNO, NAME, POSTING_SOL_ID, POSTING_SOL_DESC, SCALE_CODE, CADRE_CODE, POSTING_REGION_NAME, DESIG, MOBILE_NO, E_MAIL
-- If the UAT HRMS table lives in another schema, prefix it (e.g. hrms_dba.employee_contact_details).
-- If the UAT DB has no HRMS tables at all, run 03_CREATE_HRMS_MOCK_TABLES.sql first.
-- Idempotent (MERGE). To test through /entry, the UAT SSO portal (or a test token generator) must
-- issue a token for one of these EMPNO values.
--
--   EMPNO   Expected role      Posting
--   100001  SUPERADMIN_DIT  SOL 7114 (DIT Head Office Kolkata)
--   200001  ZO_MAKER        SOL 7015 (Zonal Office Ajmer)
--   200002  ZO_CHECKER      SOL 7015 (Zonal Office Ajmer)
--   300001  ZO_MAKER        SOL 7022 (Zonal Office Bhopal)
--   300002  ZO_CHECKER      SOL 7022 (Zonal Office Bhopal)
--   500001  ZO_MAKER        SOL 7030 (Zonal Office Jaipur)
--   500002  ZO_CHECKER      SOL 7030 (Zonal Office Jaipur)
--   400001  HO_RETAIL       SOL 7105 (HO Retail Loan Hub)
--   400002  HO_MARKETING    SOL 7102 (HO Marketing & PR)
--   400003  HO_ADMIN        SOL 7101 (HO General Administration)
--   61801   HO_ADMIN        SOL 7115 (SWDC Head Office)
--   600001  (no access)     SOL 1234 (a branch: must be refused with 'not authorised')
--   600002  (no access)     SOL 7015 (zonal office but scale 0 / clerical: must be refused)
-- ============================================================================

SET DEFINE OFF

MERGE INTO employee_contact_details t
USING (SELECT '100001' AS EMPNO FROM dual) s ON (t.EMPNO = s.EMPNO)
WHEN MATCHED THEN UPDATE SET NAME='Avanish Gupta', POSTING_SOL_ID='7114', POSTING_SOL_DESC='DIT Head Office Kolkata', SCALE_CODE='Scale 4', CADRE_CODE='EXEC',
  POSTING_REGION_NAME='Head Office', DESIG='IT Executive', MOBILE_NO='9876543210', E_MAIL='avanish.gupta@uco.bank.in'
WHEN NOT MATCHED THEN INSERT (EMPNO, NAME, POSTING_SOL_ID, POSTING_SOL_DESC, SCALE_CODE, CADRE_CODE, POSTING_REGION_NAME, DESIG, MOBILE_NO, E_MAIL)
  VALUES ('100001','Avanish Gupta','7114','DIT Head Office Kolkata','Scale 4','EXEC','Head Office','IT Executive','9876543210','avanish.gupta@uco.bank.in');
MERGE INTO employee_contact_details t
USING (SELECT '200001' AS EMPNO FROM dual) s ON (t.EMPNO = s.EMPNO)
WHEN MATCHED THEN UPDATE SET NAME='Rajesh Sharma', POSTING_SOL_ID='7015', POSTING_SOL_DESC='Zonal Office Ajmer', SCALE_CODE='Scale 2', CADRE_CODE='OFFICER',
  POSTING_REGION_NAME='Ajmer', DESIG='Credit Officer', MOBILE_NO='9812345678', E_MAIL='rajesh.sharma@uco.bank.in'
WHEN NOT MATCHED THEN INSERT (EMPNO, NAME, POSTING_SOL_ID, POSTING_SOL_DESC, SCALE_CODE, CADRE_CODE, POSTING_REGION_NAME, DESIG, MOBILE_NO, E_MAIL)
  VALUES ('200001','Rajesh Sharma','7015','Zonal Office Ajmer','Scale 2','OFFICER','Ajmer','Credit Officer','9812345678','rajesh.sharma@uco.bank.in');
MERGE INTO employee_contact_details t
USING (SELECT '200002' AS EMPNO FROM dual) s ON (t.EMPNO = s.EMPNO)
WHEN MATCHED THEN UPDATE SET NAME='Vikram Singh Rathore', POSTING_SOL_ID='7015', POSTING_SOL_DESC='Zonal Office Ajmer', SCALE_CODE='Scale 4', CADRE_CODE='EXEC',
  POSTING_REGION_NAME='Ajmer', DESIG='Chief Manager / Zonal Approver', MOBILE_NO='9812345679', E_MAIL='vikram.singh@uco.bank.in'
WHEN NOT MATCHED THEN INSERT (EMPNO, NAME, POSTING_SOL_ID, POSTING_SOL_DESC, SCALE_CODE, CADRE_CODE, POSTING_REGION_NAME, DESIG, MOBILE_NO, E_MAIL)
  VALUES ('200002','Vikram Singh Rathore','7015','Zonal Office Ajmer','Scale 4','EXEC','Ajmer','Chief Manager / Zonal Approver','9812345679','vikram.singh@uco.bank.in');
MERGE INTO employee_contact_details t
USING (SELECT '300001' AS EMPNO FROM dual) s ON (t.EMPNO = s.EMPNO)
WHEN MATCHED THEN UPDATE SET NAME='Pooja Verma', POSTING_SOL_ID='7022', POSTING_SOL_DESC='Zonal Office Bhopal', SCALE_CODE='Scale 1', CADRE_CODE='OFFICER',
  POSTING_REGION_NAME='Bhopal', DESIG='Assistant Manager', MOBILE_NO='9823456780', E_MAIL='pooja.verma@uco.bank.in'
WHEN NOT MATCHED THEN INSERT (EMPNO, NAME, POSTING_SOL_ID, POSTING_SOL_DESC, SCALE_CODE, CADRE_CODE, POSTING_REGION_NAME, DESIG, MOBILE_NO, E_MAIL)
  VALUES ('300001','Pooja Verma','7022','Zonal Office Bhopal','Scale 1','OFFICER','Bhopal','Assistant Manager','9823456780','pooja.verma@uco.bank.in');
MERGE INTO employee_contact_details t
USING (SELECT '300002' AS EMPNO FROM dual) s ON (t.EMPNO = s.EMPNO)
WHEN MATCHED THEN UPDATE SET NAME='Anand Mishra', POSTING_SOL_ID='7022', POSTING_SOL_DESC='Zonal Office Bhopal', SCALE_CODE='Scale 5', CADRE_CODE='EXEC',
  POSTING_REGION_NAME='Bhopal', DESIG='Deputy General Manager', MOBILE_NO='9823456781', E_MAIL='anand.mishra@uco.bank.in'
WHEN NOT MATCHED THEN INSERT (EMPNO, NAME, POSTING_SOL_ID, POSTING_SOL_DESC, SCALE_CODE, CADRE_CODE, POSTING_REGION_NAME, DESIG, MOBILE_NO, E_MAIL)
  VALUES ('300002','Anand Mishra','7022','Zonal Office Bhopal','Scale 5','EXEC','Bhopal','Deputy General Manager','9823456781','anand.mishra@uco.bank.in');
MERGE INTO employee_contact_details t
USING (SELECT '500001' AS EMPNO FROM dual) s ON (t.EMPNO = s.EMPNO)
WHEN MATCHED THEN UPDATE SET NAME='Neha Patel', POSTING_SOL_ID='7030', POSTING_SOL_DESC='Zonal Office Jaipur', SCALE_CODE='Scale 2', CADRE_CODE='OFFICER',
  POSTING_REGION_NAME='Jaipur', DESIG='Manager Credit', MOBILE_NO='9845678901', E_MAIL='neha.patel@uco.bank.in'
WHEN NOT MATCHED THEN INSERT (EMPNO, NAME, POSTING_SOL_ID, POSTING_SOL_DESC, SCALE_CODE, CADRE_CODE, POSTING_REGION_NAME, DESIG, MOBILE_NO, E_MAIL)
  VALUES ('500001','Neha Patel','7030','Zonal Office Jaipur','Scale 2','OFFICER','Jaipur','Manager Credit','9845678901','neha.patel@uco.bank.in');
MERGE INTO employee_contact_details t
USING (SELECT '500002' AS EMPNO FROM dual) s ON (t.EMPNO = s.EMPNO)
WHEN MATCHED THEN UPDATE SET NAME='Mahendra Choudhary', POSTING_SOL_ID='7030', POSTING_SOL_DESC='Zonal Office Jaipur', SCALE_CODE='Scale 4', CADRE_CODE='EXEC',
  POSTING_REGION_NAME='Jaipur', DESIG='Chief Manager Credit', MOBILE_NO='9845678902', E_MAIL='m.choudhary@uco.bank.in'
WHEN NOT MATCHED THEN INSERT (EMPNO, NAME, POSTING_SOL_ID, POSTING_SOL_DESC, SCALE_CODE, CADRE_CODE, POSTING_REGION_NAME, DESIG, MOBILE_NO, E_MAIL)
  VALUES ('500002','Mahendra Choudhary','7030','Zonal Office Jaipur','Scale 4','EXEC','Jaipur','Chief Manager Credit','9845678902','m.choudhary@uco.bank.in');
MERGE INTO employee_contact_details t
USING (SELECT '400001' AS EMPNO FROM dual) s ON (t.EMPNO = s.EMPNO)
WHEN MATCHED THEN UPDATE SET NAME='Sanjay Mukherjee', POSTING_SOL_ID='7105', POSTING_SOL_DESC='HO Retail Loan Hub', SCALE_CODE='Scale 3', CADRE_CODE='OFFICER',
  POSTING_REGION_NAME='Head Office', DESIG='Senior Manager Retail', MOBILE_NO='9834567890', E_MAIL='sanjay.mukherjee@uco.bank.in'
WHEN NOT MATCHED THEN INSERT (EMPNO, NAME, POSTING_SOL_ID, POSTING_SOL_DESC, SCALE_CODE, CADRE_CODE, POSTING_REGION_NAME, DESIG, MOBILE_NO, E_MAIL)
  VALUES ('400001','Sanjay Mukherjee','7105','HO Retail Loan Hub','Scale 3','OFFICER','Head Office','Senior Manager Retail','9834567890','sanjay.mukherjee@uco.bank.in');
MERGE INTO employee_contact_details t
USING (SELECT '400002' AS EMPNO FROM dual) s ON (t.EMPNO = s.EMPNO)
WHEN MATCHED THEN UPDATE SET NAME='Sunita Sen', POSTING_SOL_ID='7102', POSTING_SOL_DESC='HO Marketing & PR', SCALE_CODE='Scale 4', CADRE_CODE='EXEC',
  POSTING_REGION_NAME='Head Office', DESIG='Chief Manager PR', MOBILE_NO='9834567891', E_MAIL='sunita.sen@uco.bank.in'
WHEN NOT MATCHED THEN INSERT (EMPNO, NAME, POSTING_SOL_ID, POSTING_SOL_DESC, SCALE_CODE, CADRE_CODE, POSTING_REGION_NAME, DESIG, MOBILE_NO, E_MAIL)
  VALUES ('400002','Sunita Sen','7102','HO Marketing & PR','Scale 4','EXEC','Head Office','Chief Manager PR','9834567891','sunita.sen@uco.bank.in');
MERGE INTO employee_contact_details t
USING (SELECT '400003' AS EMPNO FROM dual) s ON (t.EMPNO = s.EMPNO)
WHEN MATCHED THEN UPDATE SET NAME='Debashis Roy', POSTING_SOL_ID='7101', POSTING_SOL_DESC='HO General Administration', SCALE_CODE='Scale 4', CADRE_CODE='EXEC',
  POSTING_REGION_NAME='Head Office', DESIG='Chief Manager Administration', MOBILE_NO='9834567892', E_MAIL='debashis.roy@uco.bank.in'
WHEN NOT MATCHED THEN INSERT (EMPNO, NAME, POSTING_SOL_ID, POSTING_SOL_DESC, SCALE_CODE, CADRE_CODE, POSTING_REGION_NAME, DESIG, MOBILE_NO, E_MAIL)
  VALUES ('400003','Debashis Roy','7101','HO General Administration','Scale 4','EXEC','Head Office','Chief Manager Administration','9834567892','debashis.roy@uco.bank.in');
MERGE INTO employee_contact_details t
USING (SELECT '61801' AS EMPNO FROM dual) s ON (t.EMPNO = s.EMPNO)
WHEN MATCHED THEN UPDATE SET NAME='SWDC Test Developer', POSTING_SOL_ID='7115', POSTING_SOL_DESC='SWDC Head Office', SCALE_CODE='Scale 3', CADRE_CODE='OFFICER',
  POSTING_REGION_NAME='Head Office', DESIG='Software Development Cell', MOBILE_NO='9811122233', E_MAIL='staff.61801@uco.bank.in'
WHEN NOT MATCHED THEN INSERT (EMPNO, NAME, POSTING_SOL_ID, POSTING_SOL_DESC, SCALE_CODE, CADRE_CODE, POSTING_REGION_NAME, DESIG, MOBILE_NO, E_MAIL)
  VALUES ('61801','SWDC Test Developer','7115','SWDC Head Office','Scale 3','OFFICER','Head Office','Software Development Cell','9811122233','staff.61801@uco.bank.in');

MERGE INTO employee_contact_details t
USING (SELECT '600001' AS EMPNO FROM dual) s ON (t.EMPNO = s.EMPNO)
WHEN MATCHED THEN UPDATE SET NAME='Branch Test Officer', POSTING_SOL_ID='1234', POSTING_SOL_DESC='Test Branch', SCALE_CODE='Scale 2', CADRE_CODE='OFFICER',
  POSTING_REGION_NAME='Kolkata', DESIG='Branch Officer', MOBILE_NO='9800000001', E_MAIL='branch.test@uco.bank.in'
WHEN NOT MATCHED THEN INSERT (EMPNO, NAME, POSTING_SOL_ID, POSTING_SOL_DESC, SCALE_CODE, CADRE_CODE, POSTING_REGION_NAME, DESIG, MOBILE_NO, E_MAIL)
  VALUES ('600001','Branch Test Officer','1234','Test Branch','Scale 2','OFFICER','Kolkata','Branch Officer','9800000001','branch.test@uco.bank.in');

MERGE INTO employee_contact_details t
USING (SELECT '600002' AS EMPNO FROM dual) s ON (t.EMPNO = s.EMPNO)
WHEN MATCHED THEN UPDATE SET NAME='Clerical Test Staff', POSTING_SOL_ID='7015', POSTING_SOL_DESC='Zonal Office Ajmer', SCALE_CODE='0', CADRE_CODE='CLRK',
  POSTING_REGION_NAME='Ajmer', DESIG='Clerk', MOBILE_NO='9800000002', E_MAIL='clerk.test@uco.bank.in'
WHEN NOT MATCHED THEN INSERT (EMPNO, NAME, POSTING_SOL_ID, POSTING_SOL_DESC, SCALE_CODE, CADRE_CODE, POSTING_REGION_NAME, DESIG, MOBILE_NO, E_MAIL)
  VALUES ('600002','Clerical Test Staff','7015','Zonal Office Ajmer','0','CLRK','Ajmer','Clerk','9800000002','clerk.test@uco.bank.in');

COMMIT;
