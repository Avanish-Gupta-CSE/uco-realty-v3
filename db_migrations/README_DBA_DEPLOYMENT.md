# UCO Realty — DBA deployment guide

Scripts for UCO Bank's Oracle database (UAT and Production). The application needs **no seed data and no application accounts**: staff identities and roles come from bank SSO and the HRMS tables at login time (nothing is stored per user; the login history is in `login_audit`).

## 1. Prerequisites
* Oracle Database 19c / 21c / 23c.
* Application schema user (for example `uco_realty`) with:
  ```sql
  GRANT CREATE SESSION, CREATE TABLE, CREATE SEQUENCE TO uco_realty;
  ```
  and a tablespace quota of at least 10 GB (project photos are stored in the database as SecureFiles BLOBs; the browser shrinks them to about 0.3 MB each).
* `SELECT` on the real HRMS tables (`employee_contact_details`, `hrms_dba.employee_tenure`, `hrms_dba.employee_job_profile`) for the same schema user. Both portals use this single database connection.

## 2. Run order (as the application schema user, in SQL*Plus or SQL Developer)
| Step | Script | What it does |
| :--- | :--- | :--- |
| 1 | `01_CREATE_TABLES.sql` | Creates all 6 tables (`projects`, `project_staging`, `project_images`, `leads`, `audit_log`, `login_audit`), constraints, indexes and the BLOB storage |
| 2 | `06_STAGING_MODEL.sql` | **Existing databases only.** Adds `project_staging` (+ unique one-open-request index), `row_version` columns, `project_images.staging_id` and moves not-yet-approved projects into staging (new databases get all of it from `01`). Safe to re-run |
| 3 | `07_DROP_UNUSED_TABLES.sql` | **Existing databases only.** Drops the unused `users` and `lead_transfer_log` tables |

**Not for UAT/production:** `03_CREATE_HRMS_MOCK_TABLES.sql` and `05_LOCAL_TEST_EMPLOYEES.sql` are for a developer's laptop only (UAT and production already have the real HRMS data in `hrms_dba`, which the application only reads). They are not included in release bundles.

`00_DROP_ALL_TABLES.sql` removes everything. **UAT resets only. Never run it in production.**

## 3. Verification
```sql
SELECT table_name, status FROM user_tables
WHERE table_name IN ('PROJECTS','PROJECT_STAGING','PROJECT_IMAGES','LEADS','AUDIT_LOG','LOGIN_AUDIT')
ORDER BY table_name;          -- expect 6 rows, all VALID

SELECT COUNT(*) FROM login_audit;   -- 0 on a fresh database; one row per sign-in attempt
```

## Notes
* In SQL*Plus, type `EXIT` afterwards (the scripts do not exit by themselves, so they can also be run from SQL Developer).
* Non-interactive example: `echo exit | sqlplus -s user/pass@service @01_CREATE_TABLES.sql`.
