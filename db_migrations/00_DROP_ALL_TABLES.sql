-- ============================================================================
-- UCO Bank Realty Portal - Teardown / Cleanup Script
-- Execution Environment: Oracle SQL Developer / SQL*Plus / Toad
-- Target: DEV / UAT Reset Only (DO NOT RUN IN PRODUCTION)
-- ============================================================================

SET SERVEROUTPUT ON;

BEGIN
  FOR t IN (
    SELECT table_name 
    FROM user_tables 
    WHERE table_name IN (
      'LEAD_TRANSFER_LOG', 
      'AUDIT_LOG', 
      'LOGIN_AUDIT', 
      'LEADS', 
      'PROJECT_IMAGES', 
      'PROJECT_STAGING', 
      'PROJECTS', 
      'USERS'
    )
  ) LOOP
    BEGIN
      EXECUTE IMMEDIATE 'DROP TABLE ' || t.table_name || ' CASCADE CONSTRAINTS PURGE';
      DBMS_OUTPUT.PUT_LINE('Dropped table: ' || t.table_name);
    EXCEPTION
      WHEN OTHERS THEN
        DBMS_OUTPUT.PUT_LINE('Error dropping ' || t.table_name || ': ' || SQLERRM);
    END;
  END LOOP;
END;
/

PROMPT Teardown completed. Schema tables dropped.
