-- ============================================================================
-- UCO Realty: 08  Document slots for PDF attachments (Brochure, Floor Plan, RERA Certificate, Others)
-- For a database that was created BEFORE this change (UAT): run ONCE as the application schema user, then deploy the new WARs.
-- A NEW database does not need it (01_CREATE_TABLES.sql already has the columns).
-- No data is changed or lost: every existing PDF becomes an "OTHER" document and keeps showing under its file name.
-- The previous WAR keeps working after this script (the new columns have a default).
-- ============================================================================

ALTER TABLE realty_project_attachments ADD (doc_type VARCHAR2(20) DEFAULT 'OTHER' NOT NULL);
ALTER TABLE realty_project_attachments ADD (doc_title VARCHAR2(100 CHAR));
ALTER TABLE realty_project_attachments ADD CONSTRAINT chk_realty_att_doctype CHECK (doc_type IN ('BROCHURE','FLOOR_PLAN','RERA','OTHER'));

-- Check (read only): 2 rows, then the existing PDFs all show OTHER.
SELECT column_name, data_type, nullable FROM user_tab_columns
WHERE table_name = 'REALTY_PROJECT_ATTACHMENTS' AND column_name IN ('DOC_TYPE','DOC_TITLE') ORDER BY column_name;
SELECT doc_type, COUNT(*) AS files FROM realty_project_attachments GROUP BY doc_type;
