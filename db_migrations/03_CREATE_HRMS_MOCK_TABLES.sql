-- ============================================================================
-- MOCK HRMS TABLES: for a DEVELOPER LAPTOP only (Docker Oracle). UAT and production already have the bank's real HRMS tables
-- (employee_contact_details, hrms_dba.employee_tenure, hrms_dba.employee_job_profile); NEVER run this there.
--
-- Table and column names are exactly what HrmsRepository queries; column types and sizes follow the bank's own
-- "desc employee_contact_details" (EMPNO varchar2(6), POSTING_SOL_ID varchar2(4), SCALE_CODE varchar2(2),
-- CADRE_CODE varchar2(4), POSTING_REGION_NAME varchar2(50)). Only the columns the application reads are created.
-- Created in the current schema; the application also tries the hrms_dba. prefix.
-- Fill with 05_LOCAL_TEST_EMPLOYEES.sql.
-- ============================================================================

CREATE TABLE employee_contact_details (
  EMPNO               VARCHAR2(6)   NOT NULL PRIMARY KEY,
  NAME                VARCHAR2(100) NOT NULL,
  POSTING_SOL_ID      VARCHAR2(4)   NOT NULL,
  POSTING_SOL_DESC    VARCHAR2(150),
  SCALE_CODE          VARCHAR2(2),
  CADRE_CODE          VARCHAR2(4),
  POSTING_REGION_NAME VARCHAR2(50),
  DESIG               VARCHAR2(100),
  MOBILE_NO           VARCHAR2(20),
  E_MAIL              VARCHAR2(100)
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

CREATE TABLE employee_job_profile (
  EMPNO         VARCHAR2(6) NOT NULL,
  JOB_PROFILE   VARCHAR2(100),
  EFF_FROM_DATE DATE,
  EFF_TO_DATE   DATE,
  STATUS_FLAG   CHAR(1) DEFAULT 'V'
);
