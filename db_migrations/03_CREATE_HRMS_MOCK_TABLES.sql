-- ============================================================================
-- MOCK HRMS TABLES (local / UAT databases that do NOT already have the bank's HRMS tables)
-- NEVER run in production: the bank owns the real tables and grants the application SELECT only.
--
-- Table and column names are exactly what HrmsServiceImpl queries (taken from the bank screenshots in
-- .brain/db-login-reference). Verify them against the real HRMS schema before the first UAT run.
-- Created in the current schema; the application also tries the hrms_dba. prefix.
-- Fill with 05_LOCAL_TEST_EMPLOYEES.sql (local developer machine only).
-- ============================================================================

CREATE TABLE employee_contact_details (
  EMPNO               VARCHAR2(20)  NOT NULL PRIMARY KEY,
  NAME                VARCHAR2(100) NOT NULL,
  POSTING_SOL_ID      VARCHAR2(10)  NOT NULL,
  POSTING_SOL_DESC    VARCHAR2(150),
  SCALE_CODE          VARCHAR2(20),
  CADRE_CODE          VARCHAR2(30),
  POSTING_REGION_NAME VARCHAR2(100),
  DESIG               VARCHAR2(100),
  MOBILE_NO           VARCHAR2(20),
  E_MAIL              VARCHAR2(100)
);

-- Optional enrichment tables (used when present; they override SOL / scale / designation of the row above)
CREATE TABLE employee_tenure (
  EMPNO         VARCHAR2(20) NOT NULL,
  POSTING_SOL   VARCHAR2(10),
  SCALE_CODE    VARCHAR2(20),
  TENURE_AS     VARCHAR2(100),
  EFF_FROM_DATE DATE,
  EFF_TO_DATE   DATE,
  STATUS_FLAG   CHAR(1) DEFAULT 'V'
);

CREATE TABLE employee_job_profile (
  EMPNO         VARCHAR2(20) NOT NULL,
  JOB_PROFILE   VARCHAR2(100),
  EFF_FROM_DATE DATE,
  EFF_TO_DATE   DATE,
  STATUS_FLAG   CHAR(1) DEFAULT 'V'
);
