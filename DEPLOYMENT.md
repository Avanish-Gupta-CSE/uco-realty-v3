# UCO Realty — Deployment Guide

Two executable WARs (Java 25, Spring Boot 4.1, Tomcat 11 embedded, JSP inside) and one Oracle schema:

| WAR | Default port | Deploy on |
| :--- | :--- | :--- |
| `ucorealty-customer.war` | 8080 | internet-facing zone |
| `ucorealty-staff.war` | 8081 | **intranet only** |

Both WARs run two ways (same file): `java -jar <war>` (verified), or dropped into an **external Tomcat 11** `webapps/` folder (the normal bank way; the WARs are built for it: embedded Tomcat/Jasper/Servlet jars are "provided", so none are bundled; **not yet tested on a real Tomcat 11**).

**Server requirements:** JDK 25 (the classes are compiled for Java 25) and Tomcat 11 (Jakarta Servlet 6.1). An older Tomcat 9/10 or JDK 8/11/17/21 cannot run these WARs; for a Tomcat 9 / Java 8 server use the `uco-realty-v2` code line instead.

**URL path = WAR file name.** `ucorealty-staff.war` is served at `https://<host>/ucorealty-staff/`, `ucorealty-customer.war` at `/ucorealty-customer/` (use `ROOT.war` for the site root). All links in the application are relative to that path, so any name works. The bundle made by `scripts/package-release.sh` already uses these version-free names.

**Names and URLs.** Project `ucorealty` (Java package `com.ucorealty`), two WARs: `ucorealty-customer.war` (customers, internet zone) and `ucorealty-staff.war` (staff, intranet). Whatever the WAR is called, no URL in the code is hard-coded: JSPs use `${pageContext.request.contextPath}`, JavaScript reads `data-context-path`, Java redirects are context-relative and the public JSON API builds photo URLs from `request.getContextPath()`. So `/api/public/projects` becomes `/ucorealty-customer/api/public/projects` on Tomcat with no code change. `java -jar` runs under the same context paths (`application.properties`) and `python3 scripts/context_path_check.py` proves it before every hand-over.

**External Tomcat setup (per server):** put the settings in `bin/setenv.sh` (Linux) / `bin/setenv.bat` (Windows), then copy the WAR to `webapps/` and start Tomcat:
```bash
export CATALINA_OPTS="-Dspring.profiles.active=uat \
  -DDB_URL=jdbc:oracle:thin:@//db-host:1521/SERVICE -DDB_USER=... -DDB_PASS=... \
  -DSSO_ENCRYPTION_KEY=... -DSESSION_COOKIE_SECURE=true"
```
(`-D...` JVM properties and OS environment variables both work.) `server.port` and `server.servlet.context-path` in the application files are ignored on an external Tomcat; the connector port and the WAR name decide.

## 1. Database (DBA)
Follow `db/README_DBA_DEPLOYMENT.md`. Order: `01_CREATE_TABLES.sql`; for an existing older database also `06_STAGING_MODEL.sql` and `07_DROP_UNUSED_TABLES.sql` (maker's working-copy table `realty_project_staging`). There is no seed data and no application accounts. UAT and production use the bank's **real HRMS data** and never run `03_`/`05_` (laptop / H2 / Docker only; they are not in the release bundles).
The application schema user needs `SELECT` on the bank HRMS tables (`employee_contact_details`, and optionally `employee_tenure`, `employee_job_profile`). Columns read from `employee_contact_details`: `EMPNO, NAME, POSTING_SOL_ID, POSTING_SOL_DESC, SCALE_CODE, CADRE_CODE, POSTING_REGION_NAME, DESIG, MOBILE_NO, E_MAIL` (the table prefixes tried, in order, are the setting `uco.hrms.table-prefixes` / env `HRMS_TABLE_PREFIXES`; default `,hrms_dba.` = first no prefix, then `hrms_dba.`). Please verify these names against the real HRMS schema.

## 2. Configuration
**Single `application.properties` per module (aligned with the bank IndiaPost UAT pattern, Decision 133):**
Each app maintains only one `application.properties` file (`WEB-INF/classes/application.properties`). No separate profile files exist.
The bank UAT database connection is embedded directly as defaults:
- URL: `jdbc:oracle:thin:@//10.21.128.29:1530/CLDBUAT`
- User: `HRMS_DBA`
- Password: UAT password
- SSO key: UAT testing key

**Tomcat on Windows VM (the bank way):**
Simply copy both WAR files into Tomcat's `webapps/` folder and start Tomcat. Zero server configuration or environment variables are required.

**Optional overrides (if connecting to a different DB or port):**
Any setting can still be overridden using JVM `-D` flags or environment variables:
```bash
-DDB_URL='jdbc:oracle:thin:@//different-host:1521/SERVICE' -DDB_USER=... -DDB_PASS=...
```

**Before production:**
Set `uco.sso.dev-login-enabled=false` in `ucorealty-staff/src/main/resources/application.properties`, provide the production `SSO_ENCRYPTION_KEY`, and set `server.servlet.session.cookie.secure=true` on HTTPS. `scripts/package-release.sh prod` enforces that `dev-login-enabled` is false.

| Variable | Module | Required | Meaning |
| :--- | :--- | :--- | :--- |
| `SPRING_PROFILES_ACTIVE` | both | no | overrides the line in `application.properties` |
| `DB_URL` / `DB_USER` / `DB_PASS` | both | `uat`, `prod` | Oracle connection |
| `DB_POOL_MAX` | both | no | connection pool size (default 10 in uat, 25 in prod) |
| `SSO_ENCRYPTION_KEY` | staff | `prod` | the bank SSO key as plain text (16, 24 or 32 characters). The app refuses to start without a valid key in `prod`; `h2` and `uat` use a development key until set |
| `HRMS_TABLE_PREFIXES` | staff | no | schema prefixes tried for the HRMS tables, default `,hrms_dba.` (first none, then `hrms_dba.`) |
| `UCO_RBAC_RETAIL_SOLS` | staff | no (default 8001) | SOL(s) of HO Retail Banking. Others: `UCO_RBAC_SUPERADMIN_SOLS`=7115, `UCO_RBAC_MARKETING_SOLS`=8028, `UCO_RBAC_ZONAL_SOL_REGEX` |
| `SESSION_COOKIE_SECURE` | both | no | `false` in `h2`/`uat`, `true` in `prod` |
| `SERVER_PORT` | both | no | 8080 customer / 8081 staff (`java -jar` only) |

Every WAR holds all the profile files and the H2 driver; only the active profile is used.

### Logs
Every line the application writes about its own work starts with a tag: `[UCO SSO]` sign-ins and sign-outs, `[UCO HRMS]` employee lookups, `[UCO WORKFLOW]` what a maker or checker did to which project, `[UCO LEAD]` lead saved or exported (never names or phone numbers), `[UCO ACCESS]` / `[UCO SECURITY]` / `[UCO HTTP 4xx]` refused actions (wrong role or zone, missing CSRF token, unknown URL), `[UCO ERROR]` unexpected failures with the full stack trace. On Tomcat they go to `catalina.out`; with `java -jar` to the terminal. Filter with `grep '\[UCO'`.
* More detail: set `LOGGING_LEVEL_COM_UCOREALTY=DEBUG` (environment). To also write a file: `-Dlogging.file.name=/var/log/ucorealty/ucorealty-staff.log`.
* The permanent record of who did what is the database (`realty_audit_log`, `realty_login_audit`; DIT Logs page); the log is for the server team.

## 3. Bank SSO integration
Register this URL with the SSO team: `https://<host>/ucorealty-staff/entry` . The portal opens it as `/entry?Value=<token>`.

The token is the bank's standard: Base64 of a 12-byte IV followed by AES/GCM data with a 128-bit tag, using `SSO_ENCRYPTION_KEY` as raw text. After decrypting it reads `emp_id=<empno>&sol_id=<sol>`.

The code is in `HomeController`:
1. `/entry` decrypts the token and looks the employee up in HRMS (the SOL in the token is **not** trusted; the HRMS posting decides).
2. The role comes from the posting: **7115** DIT superadmin, **8028** HO Marketing and Wealth Management, **8001** HO Retail Banking, **7001 to 7049** zonal offices (scale 4+ or EXEC = checker, scale 1 to 3 = maker). Any other posting is refused.
3. It starts a new session holding `emp_id` and `sol_id`, writes the attempt to the Logs page, and goes to `/home`.
4. `/home` opens the dashboard, or the login page when those session values are missing. `SessionInterceptor` applies the same check to every `/admin` page.

Open point for the SSO team: the token has no expiry, so a captured link can be replayed until the key changes.

## 4. Network rules (reverse proxy / firewall)
| Path | Internet | Intranet |
| :--- | :--- | :--- |
| public WAR: `/`, `/projects`, `/project/*`, `/files/*`, `/assets/*`, `/api/public/*` | allow | allow |
| internal WAR (everything: `/entry`, `/login`, `/admin/**`, `/api/admin/**`) | **do not expose** | allow |

Terminate HTTPS at the proxy, pass `X-Forwarded-For` (overwrite any client-supplied value) and `X-Forwarded-Proto`. The app itself sends CSP, `X-Frame-Options: DENY`, nosniff, `Referrer-Policy`, and HSTS on HTTPS.

## 5. Smoke test after deployment
1. Customer site: catalog loads, only approved projects, `/login` and `/admin` return 403.
2. Test login (`h2`, `uat`): sign in with employee `200001`, OTP `123456`. With the bank SSO: open the UAT SSO portal link with real UAT employees: one at SOL 7115 (DIT), one at 8028 (Marketing), one at 8001 (Retail), a Scale 1–3 officer and a Scale 4+ executive at the same zonal office (e.g. 7001 Patna), and an employee at an ordinary branch (must be refused).
3. Maker creates a project with photos and submits; the checker of the same zone approves; it appears on the customer site; the leads page (read from the bank's LG_ENTRY_REVAMP) exports to Excel; DIT sees the Logs page and a maker does not.

## 6. Hand-over bundle
`scripts/package-release.sh uat|prod` on the build machine → zip with both WARs DBA scripts (production bundles omit drop/mock/test scripts), this guide and SHA-256 checksums.
