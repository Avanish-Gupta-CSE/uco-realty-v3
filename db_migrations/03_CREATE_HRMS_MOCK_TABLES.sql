-- ============================================================================
-- MOCK HRMS TABLES: for a DEVELOPER LAPTOP only. UAT and production already have the bank's real HRMS tables
-- (employee_contact_details, employee_tenure, LG_ENTRY_REVAMP, owned by the database user HRMS_DBA); NEVER run this there.
--
-- Run it AS the database user HRMS_DBA (on H2, scripts/h2_schema.py puts it into a schema of that name; on Docker Oracle,
-- scripts/dev-start.sh --oracle creates the user). The application reads the tables as HRMS_DBA.<table> (util/DatabaseTables.java).
-- Table and column names are exactly what the application queries; column types and sizes follow the bank's own
-- "desc employee_contact_details" (EMPNO varchar2(6), POSTING_SOL_ID varchar2(4), SCALE_CODE varchar2(2),
-- CADRE_CODE varchar2(4), POSTING_REGION_NAME varchar2(50)). Only the columns the application reads are created.
-- Fill with 05_LOCAL_TEST_EMPLOYEES.sql.
-- WARNING: this also creates a fake LG_ENTRY_REVAMP (with fake REALTY leads in 05). Never run 03/05 on a database that has the bank's real tables.
-- ============================================================================

CREATE TABLE employee_contact_details (
  EMPNO               VARCHAR2(6)   NOT NULL PRIMARY KEY,
  NAME                VARCHAR2(100) NOT NULL,
  POSTING_SOL_ID      VARCHAR2(4)   NOT NULL,
  POSTING_SOL_DESC    VARCHAR2(100),
  SCALE_CODE          VARCHAR2(2),
  CADRE_CODE          VARCHAR2(4),
  POSTING_REGION_NAME VARCHAR2(50),
  DESIG               VARCHAR2(100),
  MOBILE_NO           VARCHAR2(15),
  E_MAIL              VARCHAR2(50),
  RETIRE_FLAG         VARCHAR2(2)
);

-- Optional enrichment tables (used when present; they override SOL / scale / designation of the row above)
CREATE TABLE employee_tenure (
  EMPNO         VARCHAR2(6) NOT NULL,
  POSTING_SOL   VARCHAR2(4),
  SCALE_CODE    VARCHAR2(2),
  TENURE_AS     VARCHAR2(100),
  EFF_FROM_DATE DATE,
  EFF_TO_DATE   DATE,
  STATUS_FLAG   CHAR(1) DEFAULT 'V'
);

-- The bank's lead table (our Home Loan button sends customers to the bank's page, which stores each lead here with CHANNEL = 'REALTY').
-- Only the columns the Leads page reads. The real table has about 60 columns; types follow the bank's "desc LG_ENTRY_REVAMP".
CREATE TABLE LG_ENTRY_REVAMP (
  LEAD_ID       VARCHAR2(20) NOT NULL PRIMARY KEY,
  CRE_DT        DATE,
  REF_NAME      VARCHAR2(250),
  STATE         VARCHAR2(50),
  DISTRICT      VARCHAR2(100),
  MOBILE_NO     VARCHAR2(12),
  EMAIL         VARCHAR2(50),
  FACILITY_INT  VARCHAR2(50),
  LEAD_STATUS   VARCHAR2(2),            -- empty in the bank's data
  STATUS        VARCHAR2(20),           -- the real status (the bank's values such as OTP_VERIFIED, SB, CC)
  TENT_LOAN_AMT NUMBER(20,2),
  SUBMIT_DATE   DATE,
  CHANNEL       VARCHAR2(50),
  LOANAMOUNT    VARCHAR2(200)
);
