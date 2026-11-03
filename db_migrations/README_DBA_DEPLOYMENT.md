# UCO Realty — DBA deployment guide

Scripts for UCO Bank's Oracle database (UAT and Production). The application needs **no seed data and no application accounts**: staff identities and roles come from bank SSO and the HRMS tables at login time (`users` is filled automatically).

## 1. Prerequisites
* Oracle Database 19c / 21c / 23c.
* Application schema user (for example `uco_realty`) with:
  ```sql
  GRANT CREATE SESSION, CREATE TABLE, CREATE SEQUENCE TO uco_realty;
  ```
  and a tablespace quota of at least 10 GB (project photos are stored in the database as SecureFiles BLOBs; the browser shrinks them to about 0.3 MB each).
* `SELECT` on the HRMS tables (`employee_contact_details` and the tenure / job-profile tables) for the same schema user. Both portals use this single database connection.

## 2. Run order (as the application schema user, in SQL*Plus or SQL Developer)
| Step | Script | What it does |
| :--- | :--- | :--- |
| 1 | `01_CREATE_TABLES.sql` | Creates all tables (including `login_audit`), constraints, indexes and the BLOB storage |
| 2 | `03_CREATE_HRMS_MOCK_TABLES.sql` | **Local/UAT only, and only if the DB has no HRMS tables.** Mock tables named as the application queries them. Never in production |
| 3 | `05_UAT_TEST_EMPLOYEES.sql` | **UAT/local only.** Adds 12 fictitious employees (one per role, plus a no-access branch employee). Never in production |
| 4 | `04_DROP_USERS_PASSWORD_HASH.sql` | **Existing databases only.** Drops the obsolete `users.password_hash` column |

`00_DROP_ALL_TABLES.sql` removes everything. **UAT resets only. Never run it in production.**

## 3. Verification
```sql
SELECT table_name, status FROM user_tables
WHERE table_name IN ('USERS','PROJECTS','PROJECT_IMAGES','LEADS','AUDIT_LOG','LOGIN_AUDIT','LEAD_TRANSFER_LOG')
ORDER BY table_name;          -- expect 7 rows, all VALID

SELECT COUNT(*) FROM users;   -- 0 on a fresh database; rows appear as employees sign in
```

## Notes
* In SQL*Plus, type `EXIT` afterwards (the scripts do not exit by themselves, so they can also be run from SQL Developer).
* Non-interactive example: `echo exit | sqlplus -s user/pass@service @01_CREATE_TABLES.sql`.
