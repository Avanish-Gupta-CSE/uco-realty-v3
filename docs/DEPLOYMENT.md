# UCO Realty — Deployment Guide

Two executable WARs (Java 25, Spring Boot 4.1, Tomcat 11 embedded, JSP inside) and one Oracle schema:

| WAR | Default port | Deploy on |
| :--- | :--- | :--- |
| `UcoRealty.war` | 8080 | internet-facing zone |
| `UcoRealtyStaff.war` | 8081 | **intranet only** |

Both WARs run two ways (same file): `java -jar <war>` (verified), or dropped into an **external Tomcat 11** `webapps/` folder (the normal bank way; the WARs are built for it: embedded Tomcat/Jasper/Servlet jars are "provided", so none are bundled; **not yet tested on a real Tomcat 11**).

**Server requirements:** JDK 25 (the classes are compiled for Java 25) and Tomcat 11 (Jakarta Servlet 6.1). An older Tomcat 9/10 or JDK 8/11/17/21 cannot run these WARs; for a Tomcat 9 / Java 8 server use the `uco-realty-v2` code line instead.

**URL path = WAR file name.** `UcoRealtyStaff.war` is served at `https://<host>/UcoRealtyStaff/`, `UcoRealty.war` at `/UcoRealty/` (use `ROOT.war` for the site root). All links in the application are relative to that path, so any name works. The bundle made by `scripts/package-release.sh` already uses these version-free names.

**Names and URLs.** Project `ucorealty` (Java package `com.ucorealty`), two WARs: `UcoRealty.war` (customers, internet zone) and `UcoRealtyStaff.war` (staff, intranet). Whatever the WAR is called, no URL in the code is hard-coded: JSPs use `${pageContext.request.contextPath}`, JavaScript reads `data-context-path`, Java redirects are context-relative and the public JSON API builds photo URLs from `request.getContextPath()`. So `/api/public/projects` becomes `/UcoRealty/api/public/projects` on Tomcat with no code change. `java -jar` runs under the same context paths (`application.properties`) and `python3 scripts/context_path_check.py` proves it before every hand-over.

**External Tomcat setup (per server): there are NO server settings.** On the bank servers we cannot set environment variables or Java options (`-D...`, `setenv`, `CATALINA_OPTS`). Every value that differs between UAT and production (database password, SSO key, test login, profile line) is written into `application.properties` **before the WAR is built** (`scripts/package-release.sh uat|prod`); then copy the WAR to `webapps/` and start Tomcat. Changing a value later means editing the file and rebuilding. The exact lines are listed in `.brain/03_SYSTEM/Login_Environments.md` and in section 2 below. `server.port` and `server.servlet.context-path` in the application files are ignored on an external Tomcat; the connector port and the WAR name decide.

## 1. Database (DBA)
Follow `db/README_DBA_DEPLOYMENT.md`. Order: `01_CREATE_TABLES.sql`; for an existing older database also `06_STAGING_MODEL.sql` and `07_DROP_UNUSED_TABLES.sql` (maker's working-copy table `realty_project_staging`). There is no seed data and no application accounts. UAT and production use the bank's **real HRMS data** and never run `03_`/`05_` (laptop / H2 / Docker only; they are not in the release bundles).
The application schema user needs `SELECT` on the bank HRMS tables of the user `HRMS_DBA` (`employee_contact_details`, `employee_tenure`, `LG_ENTRY_REVAMP`). Columns read from `employee_contact_details`: `EMPNO, NAME, POSTING_SOL_ID, POSTING_SOL_DESC, SCALE_CODE, CADRE_CODE, POSTING_REGION_NAME, DESIG, MOBILE_NO, E_MAIL` (the tables are read as `HRMS_DBA.employee_contact_details`, `HRMS_DBA.employee_tenure` and `HRMS_DBA.LG_ENTRY_REVAMP`; the owner name is written once, in `util/DatabaseTables.java`). The owner confirmed on 18 Nov 2026 that these are the real HRMS column names (checked on another developer's bank PC).

## 2. Configuration
**Single `application.properties` per module (aligned with the bank IndiaPost UAT pattern, Decision 133):**
Each app has one `application.properties` file (`WEB-INF/classes/application.properties`) that holds the bank UAT values. The file `application-h2.properties` is only for laptops; it is used only when the line `spring.profiles.active=h2` is active. In the files that line is commented out (`# spring.profiles.active=h2`) so UAT and production use Oracle; `scripts/dev-start.sh` switches H2 on for a laptop by itself.
The bank UAT database connection is embedded directly as defaults:
- URL: `jdbc:oracle:thin:@//10.21.128.29:1530/CLDBUAT`
- User: `HRMS_DBA`
- Password: UAT password. It is written in `application.properties` before the build, or changed on the server in the unpacked WAR: `<Tomcat>/webapps/<WAR name>/WEB-INF/classes/application.properties` (line 24, both apps), then restart Tomcat. Copying a new WAR over the old one replaces the unpacked folder, so set it again.
- SSO key: UAT testing key

**Tomcat on Windows VM (the bank way):**
Copy both WAR files into Tomcat's `webapps/` folder and start Tomcat. The values built into `application.properties` are used unless you override them. Before a production build: `scripts/package-release.sh prod` refuses to build while the laptop profile, the test login, the test OTP, the UAT SSO key, SOL 7103 or the placeholder database password are still in the files.

**Settings that only the server team can change (Tomcat, not the WAR; not ours to set):** the `server.*` lines in `application.properties` (`server.servlet.session.*`, `server.error.*`, `server.port`, `server.servlet.context-path`) are used only when an application runs on its own (`java -jar`, as `scripts/dev-start.sh` does). On the bank's Tomcat they are ignored: the URL path comes from the WAR file name and the rest from Tomcat itself. Please check on UAT and write the answer here:
- gzip of pages (HTML, CSS, JS, JSON): `compression="on"` on the Tomcat `<Connector>` in `server.xml` (saves most of the page size).
- session time-out: `<session-timeout>` in Tomcat's `conf/web.xml` (the staff app expects 30 minutes).
- session cookie: `HttpOnly` (Tomcat default) and, once the site runs on https, `Secure` (`<Context useHttpOnly="true">` and the `secure` cookie setting in `context.xml` or the https connector).

**Overrides:** the `${NAME:default}` form in the file would accept a JVM `-D` flag or an environment variable, but that works only where we control the machine (a laptop, `scripts/dev-start.sh`, a `java -jar` run). **On the bank servers do not rely on it: edit the default after the colon and rebuild.**

**Before production (all in `application.properties`, then rebuild):**
In `UcoRealtyStaff`: set `uco.sso.dev-login-enabled=false` (line 55), delete the `uco.sso.test-otp` line (56), write the real key after the colon in `sso.encryption.key` (49). Keep `spring.profiles.active=h2` commented out in **both** apps (it already is). `scripts/package-release.sh prod` refuses to build while any of these is still there. It does not check the session cookie `Secure` flag: `server.servlet.session.cookie.secure` is ignored on an external Tomcat (see above), so ask the server team to set it on the https connector.

The names below are the setting names written in `application.properties` (`${NAME:default}`). On the bank servers you change the default in the file; the "override" column only applies on a laptop or `java -jar`.

| Variable | Module | Required | Meaning |
| :--- | :--- | :--- | :--- |
| `SPRING_PROFILES_ACTIVE` | both | no | the line `# spring.profiles.active=h2` in `application.properties`: commented out = Oracle (UAT, production); remove the `#` only for the laptop database |
| `DB_URL` / `DB_USER` / `DB_PASS` | both | `uat`, `prod` | Oracle connection |
| `DB_POOL_MAX` | both | no | connection pool size (default 10 in uat, 25 in prod) |
| `SSO_ENCRYPTION_KEY` | staff | `prod` | the bank SSO key as plain text (16, 24 or 32 characters). The app refuses to start without a valid key in `prod`; `h2` and `uat` use a development key until set |
| `UCO_RBAC_RETAIL_SOLS` | staff | no (default 8001) | SOL(s) of HO Retail Banking. Others: `UCO_RBAC_SUPERADMIN_SOLS`=7115, `UCO_RBAC_MARKETING_SOLS`=8028, `UCO_RBAC_ZONAL_SOL_REGEX` |
| `SESSION_COOKIE_SECURE` | both | no | `java -jar` only; ignored on an external Tomcat (the server team sets `Secure` there) |
| `SERVER_PORT` | both | no | 8080 customer / 8081 staff (`java -jar` only) |

Every WAR holds all the profile files and the H2 driver; only the active profile is used.

### Logs
Every line the application writes about its own work starts with a tag: `[UCO SSO]` sign-ins and sign-outs, `[UCO HRMS]` employee lookups, `[UCO WORKFLOW]` what a maker or checker did to which project, `[UCO LEAD]` lead saved or exported (never names or phone numbers), `[UCO ACCESS]` / `[UCO SECURITY]` / `[UCO HTTP 4xx]` refused actions (wrong role or zone, missing CSRF token, unknown URL), `[UCO ERROR]` unexpected failures with the full stack trace. On Tomcat they go to `catalina.out`; with `java -jar` to the terminal. Filter with `grep '\[UCO'`.
* More detail: change `logging.level.com.ucorealty=INFO` to `DEBUG` in `application.properties` and rebuild. The log file location is written in `logback-spring.xml` (staff: `E:/Realty/UcoRealtyStaff/logs/`).
* Log files: staff `E:/Realty/UcoRealtyStaff/logs/UcoRealtyStaff-<dateTime>.log`, customer `E:/Realty/UcoRealty/logs/UcoRealty-<dateTime>.log` (e.g. `UcoRealtyStaff-2026-11-18_14-35-52.log`; <dateTime> is when that run of the application started, so every start opens a new file). Every line starts with the date and time (`yyyy-MM-dd HH:mm:ss`, server time). When the day changes, or a file reaches 50 MB, it is closed and compressed to `UcoRealtyStaff-2026-11-17.0.log.gz` (`.1`, `.2` ... for more in one day); those archives are kept 30 days and the oldest are deleted when one app's archives exceed 3 GB. The current run's file and the files of earlier runs that never rolled are NOT deleted automatically (one small file per restart): the server team removes old ones now and then. All of this is in `logback-spring.xml`. The same lines also go to the console (`catalina.out` or the Tomcat service log), which Tomcat rotates, not us.
* The permanent record of who did what is the database (`realty_audit_log`, `realty_login_audit`; DIT Logs page); the log is for the server team.

## 3. Bank SSO integration
Register this URL with the SSO team: `https://<host>/UcoRealtyStaff/entry` . The portal opens it as `/entry?Value=<token>`.

The token is the bank's standard: by default Base64 of a 12-byte IV followed by AES/GCM data with a 128-bit tag, using `SSO_ENCRYPTION_KEY`. The cipher is the setting `sso.cipher` (`AES/GCM/NoPadding` or `AES/CBC/PKCS5Padding` with a 16-byte IV first; nothing else is accepted) as raw text. After decrypting it reads `emp_id=<empno>&sol_id=<sol>`.

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
2. Test login (`h2`, `uat`): sign in with employee `52260`, OTP `123456`. With the bank SSO: open the UAT SSO portal link with real UAT employees: one at SOL 7103 (DIT, 7115 in production), one at 8028 (Marketing), one at 8001 (Retail), a Scale 1–3 officer and a Scale 4+ executive at the same zonal office (e.g. 7010 Durgapur), and an employee at an ordinary branch (must be refused).
3. Maker creates a project with photos and submits; the checker of the same zone approves; it appears on the customer site; the leads page (read from the bank's LG_ENTRY_REVAMP) exports to Excel; DIT sees the Logs page and a maker does not.

## 6. Hand-over bundle
`scripts/package-release.sh uat|prod` on the build machine → zip with both WARs DBA scripts (production bundles omit drop/mock/test scripts), this guide and SHA-256 checksums.
