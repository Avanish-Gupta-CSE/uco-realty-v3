# UCO Realty — DBA deployment guide

Scripts for UCO Bank's Oracle database (UAT and Production). The application needs **no seed data and no application accounts**: staff identities and roles come from bank SSO and the HRMS tables at login time (nothing is stored per user; the login history is in `realty_login_audit`).

## 1. Prerequisites
* Oracle Database 19c / 21c / 23c.
* Application schema user (for example `uco_realty`) with:
  ```sql
  GRANT CREATE SESSION, CREATE TABLE, CREATE SEQUENCE TO uco_realty;
  ```
  and a tablespace quota of at least 10 GB (project photos are stored in the database as SecureFiles BLOBs; the browser shrinks them to about 0.3 MB each).
* `SELECT` on the real HRMS tables (`employee_contact_details`, `hrms_dba.employee_tenure`) for the same schema user. Both portals use this single database connection.

## 2. What the DBA runs
**A new database (UAT, production): one script.**
| Step | Script | What it does |
| :--- | :--- | :--- |
| 1 | `01_CREATE_TABLES.sql` | Creates the 5 tables (`realty_projects`, `realty_project_staging`, `realty_project_images`, `realty_audit_log`, `realty_login_audit`; every table, index and constraint name starts with `realty_` because the schema already holds many other tables), constraints, indexes and the BLOB storage. Run once as the application schema user |

That is all. The application creates and changes no tables itself; in UAT it checks at start that the tables match the code (`validate`) and refuses to start otherwise.

**`03` and `05` are for a laptop / H2 / Docker database only, NOT for the bank's UAT or production** (they create stand-in HRMS tables, including a fake `LG_ENTRY_REVAMP` with fake REALTY leads, which would hide the bank's real tables). `00`, `03`, `05` are in a UAT bundle only; `06`, `07` only for an old laptop database:
| Script | Purpose |
| :--- | :--- |
| `00_DROP_ALL_TABLES.sql` | drops the five `realty_` application tables (nothing else). Included in a UAT bundle for resets, **never run in production** |
| `03_CREATE_HRMS_MOCK_TABLES.sql` + `05_LOCAL_TEST_EMPLOYEES.sql` | stand-ins for the bank's HRMS tables and 12 test employees, in our own schema, for the `uat` profile. **Never in production** (`scripts/dev-start.sh --oracle` loads them for you). UAT and production already have the real HRMS data in `hrms_dba` |
| `06_STAGING_MODEL.sql`, `07_DROP_UNUSED_TABLES.sql` | one-time upgrade of a laptop database created by earlier builds, **before the `realty_` prefix existed; they use the old table names and are not for a new or UAT database** (old change-request design, `users` and `lead_transfer_log` tables). A new database never needs them |

## 3. Verification
```sql
SELECT table_name, status FROM user_tables
WHERE table_name IN ('REALTY_PROJECTS','REALTY_PROJECT_STAGING','REALTY_PROJECT_IMAGES','REALTY_AUDIT_LOG','REALTY_LOGIN_AUDIT')
ORDER BY table_name;          -- expect 5 rows, all VALID

SELECT COUNT(*) FROM realty_login_audit;   -- 0 on a fresh database; one row per sign-in attempt
```

## Notes
* In SQL*Plus, type `EXIT` afterwards (the scripts do not exit by themselves, so they can also be run from SQL Developer).
* Non-interactive example: `echo exit | sqlplus -s user/pass@service @01_CREATE_TABLES.sql`.
