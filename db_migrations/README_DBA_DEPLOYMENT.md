# UCO Realty — DBA deployment guide

Scripts for UCO Bank's Oracle database (UAT and Production). The only data the application needs on day one is **two administrator accounts**. There is no sample data and no test users.

## 1. Prerequisites
* Oracle Database 19c / 21c / 23c.
* Application schema user (for example `uco_realty`) with:
  ```sql
  GRANT CREATE SESSION, CREATE TABLE, CREATE SEQUENCE TO uco_realty;
  ```
  and a tablespace quota of at least 10 GB (project photos are stored in the database as SecureFiles BLOBs; the browser shrinks them to about 0.3 MB each).

## 2. Run order (as the application schema user, in SQL*Plus or SQL Developer)
| Step | Script | What it does |
| :--- | :--- | :--- |
| 1 | `01_CREATE_TABLES.sql` | Creates the 6 tables, constraints, indexes and the BLOB storage |
| 2 | `02_INITIAL_ADMIN_USERS.sql` | Creates `dit.superadmin` and `ho.admin`. Asks for one BCrypt hash for each |

`00_DROP_ALL_TABLES.sql` removes everything. **UAT resets only. Never run it in production.**

## 3. Creating the two password hashes
No password is stored in any script. DIT creates the hashes on a secure machine:

```bash
java -Dloader.main=com.ucorealty.tools.PasswordHashTool \
     -cp uco-realty-0.1.0.jar org.springframework.boot.loader.launch.PropertiesLauncher
```

Type the chosen password at the hidden prompt (8+ characters, at least one letter and one digit). The tool prints a hash starting with `$2a$10$`. Give the DBA the hashes, never the passwords. Use a different password for each account.

| Username | Role | Purpose |
| :--- | :--- | :--- |
| `dit.superadmin` | `SUPERADMIN_DIT` | System oversight, support, audit log |
| `ho.admin` | `HO_ADMIN` | Creates and manages the Zonal Office makers, checkers and Call Centre users in the application |

Each administrator uses **Change password** after the first sign-in.

## 4. Verification
```sql
SELECT table_name, status FROM user_tables
WHERE table_name IN ('USERS','PROJECTS','PROJECT_IMAGES','LEADS','AUDIT_LOG','LEAD_TRANSFER_LOG')
ORDER BY table_name;          -- expect 6 rows, all VALID

SELECT username, role FROM users;   -- expect exactly 2 rows: dit.superadmin, ho.admin
```
All other users (Zonal makers and checkers, Call Centre) are created by `ho.admin` inside the application.

## Notes for running the scripts
* Run them in order as the application schema user. In SQL*Plus, type `EXIT` afterwards (the scripts do not exit by themselves, so they can also be run from SQL Developer).
* Non-interactive example: `echo exit | sqlplus -s user/pass@service @01_CREATE_TABLES.sql`.
* `02_INITIAL_ADMIN_USERS.sql` asks for the two hashes (it does not echo them back).
* Tested on Oracle XE 21c (22 Oct 2026): 00 -> 01 -> 02, then the application started with `ddl-auto=validate`.
