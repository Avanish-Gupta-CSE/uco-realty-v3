# UCO Realty — Deployment Guide

Two executable WARs (Java 25, Spring Boot 4.1, Tomcat 11 embedded, JSP inside) and one Oracle schema:

| WAR | Default port | Deploy on |
| :--- | :--- | :--- |
| `ucorealty-customer.war` | 8080 | internet-facing zone |
| `ucorealty-staff.war` | 8081 | **intranet only** |

Both WARs run two ways (same file): `java -jar <war>` (verified), or dropped into an **external Tomcat 11** `webapps/` folder (the normal bank way; the WARs are built for it: embedded Tomcat/Jasper/Servlet jars are "provided", so none are bundled; **not yet tested on a real Tomcat 11**).

**Server requirements:** JDK 25 (the classes are compiled for Java 25) and Tomcat 11 (Jakarta Servlet 6.1). An older Tomcat 9/10 or JDK 8/11/17/21 cannot run these WARs; for a Tomcat 9 / Java 8 server use the `uco-realty-v2` code line instead.

**URL path = WAR file name.** `ucorealty-staff.war` is served at `https://<host>/ucorealty-staff/`, `ucorealty-customer.war` at `/ucorealty-customer/` (use `ROOT.war` for the site root). All links in the application are relative to that path, so any name works. The bundle made by `scripts/package-release.sh` already uses these version-free names.

**Names and URLs.** Project `ucorealty` (Java package `com.ucorealty`), two WARs: `ucorealty-customer.war` (customers, internet zone) and `ucorealty-staff.war` (staff, intranet). Whatever the WAR is called, no URL in the code is hard-coded: JSPs use `${pageContext.request.contextPath}`, JavaScript reads `data-context-path`, Java redirects are context-relative and the public JSON API builds photo URLs from `request.getContextPath()`. So `/api/public/projects` becomes `/ucorealty-customer/api/public/projects` on Tomcat with no code change. Local development runs under the same context paths (`application-local.properties`) and `python3 scripts/context_path_check.py` proves it before every hand-over.

**External Tomcat setup (per server):** put the settings in `bin/setenv.sh` (Linux) / `bin/setenv.bat` (Windows), then copy the WAR to `webapps/` and start Tomcat:
```bash
export CATALINA_OPTS="-Dspring.profiles.active=uat \
  -DDB_URL=jdbc:oracle:thin:@//db-host:1521/SERVICE -DDB_USER=... -DDB_PASS=... \
  -DSSO_ENCRYPTION_KEY=... -DSSO_ENCRYPTION_IV=... -DSESSION_COOKIE_SECURE=true"
```
(`-D...` JVM properties and OS environment variables both work.) `server.port` and `server.servlet.context-path` in the application files are ignored on an external Tomcat; the connector port and the WAR name decide.

## 1. Database (DBA)
Follow `db/README_DBA_DEPLOYMENT.md`. Order: `01_CREATE_TABLES.sql`; for an existing older database also `06_STAGING_MODEL.sql` and `07_DROP_UNUSED_TABLES.sql` (maker's working-copy table `project_staging`). There is no seed data and no application accounts. UAT and production use the bank's **real HRMS data** (`hrms_dba`); `03_`/`05_` scripts are only for a developer laptop.
The application schema user needs `SELECT` on the bank HRMS tables (`employee_contact_details`, and optionally `employee_tenure`, `employee_job_profile`). Columns read from `employee_contact_details`: `EMPNO, NAME, POSTING_SOL_ID, POSTING_SOL_DESC, SCALE_CODE, CADRE_CODE, POSTING_REGION_NAME, DESIG, MOBILE_NO, E_MAIL` (tables are tried with and without the `hrms_dba.` prefix). Please verify these names against the real HRMS schema.

## 2. Configuration (environment variables)
Each WAR has four small settings files: `application.properties` (shared), `application-prod.properties` (the default profile), `application-uat.properties` and `application-local.properties` (laptop only). Nothing needs editing for UAT or production, only the variables below. `SPRING_PROFILES_ACTIVE` picks one of `prod`, `uat`, `local`.

| Variable | Module | Required | Meaning |
| :--- | :--- | :--- | :--- |
| `SPRING_PROFILES_ACTIVE` | both | yes (`uat` or `prod`) | default is `prod`; never use `local` outside a laptop |
| `DB_URL` / `DB_USER` / `DB_PASS` | both | yes | the same three names in UAT and production: `jdbc:oracle:thin:@//host:1521/SERVICE`, schema user, password. `uat` validates the tables against the code at start; `prod` does not |
| `DB_POOL_MAX` | both | no | connection pool size (default 10 in uat, 25 in prod) |
| `SSO_ENCRYPTION_KEY` | internal | yes | bank SSO key (Base64). **The app refuses to start without it** |
| `SSO_ENCRYPTION_IV` | internal | if the bank uses AES-CBC | Base64 IV |
| `APP_REDIRECT_URL` | internal | no | landing page after login, default `/admin/projects` |
| `UCO_RBAC_RETAIL_SOLS` | internal | no (default 8001) | SOL(s) of HO Retail Banking, comma separated. Other role SOLs have defaults (`UCO_RBAC_SUPERADMIN_SOLS`=7115, `UCO_RBAC_MARKETING_SOLS`=8028, `UCO_RBAC_ZONAL_SOL_REGEX`=`70(0[1-9]|[1-4]\d)`) |
| `SESSION_COOKIE_SECURE` | both | no | default `true`; set `false` only for a plain-HTTP UAT |
| `SERVER_PORT` | both | no | 8080 public / 8081 internal |

```bash
export SPRING_PROFILES_ACTIVE=uat
export DB_URL='jdbc:oracle:thin:@//db-host:1521/SERVICE' DB_USER='...' DB_PASS='...'
export SSO_ENCRYPTION_KEY='...' SSO_ENCRYPTION_IV='...'
java -jar ucorealty-staff.war        # standalone alternative to Tomcat; same for ucorealty-customer.war (without the SSO variables)
```
The WARs contain no sample data, test users or secrets. In `uat`/`prod` the dev conveniences are off: no plain `/sso?emp_id=` login, no application-issued OTP, no built-in employees (the bank SSO portal does the OTP).

## 3. Bank SSO integration
The bank portal redirects the employee to `https://<intranet-host>/ucorealty-staff/entry?value=<token>` (also accepted: `/GetValue.jsp`, `/sso?value=`). The token is the encrypted string `emp_id=<empno>&sol_id=<sol>`; if it decrypts, the employee is genuine. The application then:
1. looks the employee up in HRMS (the SOL in the token is **not** trusted; the HRMS posting decides),
2. derives the role from the posting SOL (configurable, see the environment table above): **7115** DIT superadmin (Logs page), **8028** HO Marketing and Wealth Management (create/approve in any zone), **8001** HO Retail Banking (customer leads and Excel only), **7001–7049** zonal offices (e.g. 7001 Patna): Scale 4+ or EXEC cadre = Checker, Scale 1–3 = Maker, in their own zone only. The zone comes from the SOL description (`PATNA ZONAL OFFICE` → Patna). **Any other posting, and scale 0/blank, is refused**,
3. starts a new session and writes the attempt (success or failure) to the Logs page.

Open points to confirm with the SSO team before go-live: key/IV and cipher (CBC vs GCM), whether the token can carry an expiry (it has none today, so a captured link can be replayed), and the real HRMS column names.

## 4. Network rules (reverse proxy / firewall)
| Path | Internet | Intranet |
| :--- | :--- | :--- |
| public WAR: `/`, `/projects`, `/project/*`, `/lead`, `/files/*`, `/assets/*`, `/api/public/*` | allow | allow |
| internal WAR (everything: `/entry`, `/login`, `/admin/**`, `/api/admin/**`) | **do not expose** | allow |

Terminate HTTPS at the proxy, pass `X-Forwarded-For` (overwrite any client-supplied value) and `X-Forwarded-Proto`. The app itself sends CSP, `X-Frame-Options: DENY`, nosniff, `Referrer-Policy`, and HSTS on HTTPS.

## 5. Smoke test after deployment
1. Customer site: catalog loads, only approved projects, `/login` and `/admin` return 403.
2. UAT: open the UAT SSO portal link with real UAT employees: one at SOL 7115 (DIT), one at 8028 (Marketing), one at 8001 (Retail), a Scale 1–3 officer and a Scale 4+ executive at the same zonal office (e.g. 7001 Patna), and an employee at an ordinary branch (must be refused).
3. Maker creates a project with photos and submits; the checker of the same zone approves; it appears on the customer site; a customer lead is saved; leads export to Excel; DIT sees the Logs page and a maker does not.

## 6. Hand-over bundle
`scripts/package-release.sh uat|prod` on the build machine → zip with both WARs, DBA scripts (production bundles omit drop/mock/test scripts), this guide and SHA-256 checksums.
