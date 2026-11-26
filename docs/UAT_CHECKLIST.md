# UAT checklist: what to do, in order (no secrets in this file)

Fill in the real values only on your own PC. Do not commit the password or the SSO key.

## A. Before building the WARs (edit the two files, then rebuild; there are no server settings on the bank servers, so the file is the only place)
| What | File | Line | Change |
| :--- | :--- | :--- | :--- |
| DB password | `UcoRealtyStaff/src/main/resources/application.properties` | 24 | in `${DB_PASS:...}` replace what is after the colon with the real `HRMS_DBA` UAT password |
| DB password | `UcoRealty/src/main/resources/application.properties` | 24 | the same. **Or** leave `CHANGE_ME_DB_PASSWORD` in the build and set it on the server. On the server, after Tomcat has unpacked the WAR, you can also change the password in `<Tomcat>/webapps/UcoRealtyStaff/WEB-INF/classes/application.properties` and `<Tomcat>/webapps/UcoRealty/WEB-INF/classes/application.properties` (line 24) and restart Tomcat. Copying a new WAR over the old one replaces the unpacked folder, so the password must be set again each time. |
| (DB address and user are already right, lines 21 and 22) | | | `10.21.128.29:1530/CLDBUAT`, `HRMS_DBA` |
| Use Oracle instead of H2 | both `application.properties` | staff 67, customer 51 | nothing to do: the line `# spring.profiles.active=h2` is already commented out. Remove the `#` only for the laptop database (section D) |
| SSO key, when you get it | staff `application.properties` | 49 | replace what is after the colon in `${SSO_ENCRYPTION_KEY:...}` (16, 24 or 32 characters). Not needed for the first UAT run |

Then: `scripts/package-release.sh uat` gives both WARs in `release/ucorealty-uat-<date>/`.

## B. Queries to run in SQL Developer, connected as HRMS_DBA on CLDBUAT
1. Check you are the right user: `SELECT USER FROM dual;` (must say HRMS_DBA)
2. Check our table names are free (must return NO rows; if it returns rows, stop and ask the senior):
   ```sql
   SELECT table_name FROM user_tables
   WHERE table_name IN ('REALTY_PROJECTS','REALTY_PROJECT_STAGING','REALTY_PROJECT_IMAGES','REALTY_PROJECT_ATTACHMENTS','REALTY_AUDIT_LOG','REALTY_LOGIN_AUDIT');
   ```
3. Check the HRMS table has the retired flag (must return 1 row):
   ```sql
   SELECT column_name FROM user_tab_columns WHERE table_name='EMPLOYEE_CONTACT_DETAILS' AND column_name='RETIRE_FLAG';
   ```
4. Create our 6 tables: run the whole file `db_migrations/01_CREATE_TABLES.sql` (Run Script, F5).
5. Check they exist (must say 6):
   ```sql
   SELECT COUNT(*) FROM user_tables
   WHERE table_name IN ('REALTY_PROJECTS','REALTY_PROJECT_STAGING','REALTY_PROJECT_IMAGES','REALTY_PROJECT_ATTACHMENTS','REALTY_AUDIT_LOG','REALTY_LOGIN_AUDIT');
   ```
6. Find real testers (or ask the senior for numbers), one query per role:
   ```sql
   -- DIT superadmin
   SELECT empno, name, posting_sol_id, scale_code, cadre_code, retire_flag FROM employee_contact_details WHERE posting_sol_id='7115' AND NVL(retire_flag,'N')<>'RT';
   -- HO Marketing
   SELECT empno, name, posting_sol_id, scale_code, cadre_code, retire_flag FROM employee_contact_details WHERE posting_sol_id='8028' AND NVL(retire_flag,'N')<>'RT';
   -- zonal maker (scale 1 to 3) and checker (scale 4 or more / EXEC) of ONE zone, e.g. Bhopal 7013
   SELECT empno, name, posting_sol_id, scale_code, cadre_code FROM employee_contact_details WHERE posting_sol_id='7013' AND NVL(retire_flag,'N')<>'RT' ORDER BY scale_code;
   ```
7. The app also reads the CURRENT posting from the tenure table, which wins over the contact table. For each tester:
   ```sql
   SELECT empno, posting_sol, scale_code, cadre_code, tenure_as FROM employee_tenure
   WHERE empno = '<EMPNO>' AND SYSDATE BETWEEN eff_from_date AND eff_to_date AND status_flag = 'V';
   ```
8. Zone names (compare with `UcoRealtyStaff/src/main/resources/geo/zones.json`; the text must read `<ZONE NAME> ZONAL OFFICE`):
   ```sql
   SELECT DISTINCT posting_sol_id, posting_sol_desc FROM employee_contact_details
   WHERE posting_sol_id BETWEEN '7001' AND '7099' ORDER BY 1;
   ```
9. **Senior said NO to adding test employees to the UAT HRMS** (9 Nov). So do not run `db_migrations/UAT_TEST_USERS.sql`. Use real UAT employees found with the queries in step 6: the test login accepts ANY employee that exists in HRMS, and the role comes from their posting.
   Do NOT run `03_CREATE_HRMS_MOCK_TABLES.sql` or `05_LOCAL_TEST_EMPLOYEES.sql` here: the real HRMS tables already exist.

## C. Deploy
1. Copy `UcoRealtyStaff.war` and `UcoRealty.war` to Tomcat's `webapps` folder (the senior helps). Tomcat 11, JDK 25.
2. Open `http://<server>:<port>/UcoRealtyStaff/` and `http://<server>:<port>/UcoRealty/projects`.
3. Sign in with an employee number from B6 and OTP `123456` (the test login).
4. Logs: `E:/Realty/UcoRealtyStaff/logs/UcoRealtyStaff-<dateTime>.log` and `E:/Realty/UcoRealty/logs/UcoRealty-<dateTime>.log` (the E: drive must exist on the server), and Tomcat's `catalina.out`.

## D. If you cannot reach the Oracle database
Run on H2 instead (in memory, test employees 55012 ... 600003, data lost when Tomcat stops). There is no server setting on the bank servers, so this is a file edit and a rebuild:
remove the `#` in front of `# spring.profiles.active=h2` in both `application.properties` files (staff line 69, customer line 57), build with `scripts/package-release.sh uat`, deploy.
To go back to Oracle: put the `#` back in both files, rebuild, deploy.

## E. Before production (nothing to delete in Java)
1. Staff `application.properties`: set `uco.sso.dev-login-enabled=false` and delete the `uco.sso.test-otp` line (search for TODO).
2. Put the real SSO key in `sso.encryption.key` (search for TODO).
   The line `spring.profiles.active=h2` is already commented out in both files and must stay that way (the production build refuses a build where it is active). All of this is a file edit before the build: there are no server settings.
   The session cookie `Secure` flag is not set from our files on a Tomcat server (`server.*` lines are ignored there): ask the server team to set it on the https connector.
3. Run `UAT_TEST_USERS.sql`'s DELETE if you used it.
4. Rebuild with `scripts/package-release.sh prod`. After that only the portal link `/entry?Value=...` signs people in.

## F. Ask the senior (only these)
1. The `HRMS_DBA` password for UAT.
2. Tell the senior which 4 real UAT employees you will test with (found by the step B6 queries: DIT 7115, HO Marketing 8028, a zonal maker and a zonal checker of one zone) and that testers sign in with them using the test OTP.
3. The intranet portal link (the SSO): who adds "UCO Realty" to the bank's intranet portal and gives us the key? When a person clicks our link there, the portal sends them to `http://<server>:<port>/UcoRealtyStaff/entry?Value=<encrypted>`. The portal team must be told that address, and they tell us the secret key their portal encrypts with. Only needed when you want the real portal link to work, not for the first UAT run.
