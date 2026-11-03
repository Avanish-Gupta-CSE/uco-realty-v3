# UCO Realty — Deployment Guide

Two executable WARs (Java 25, Spring Boot 4.1, Tomcat 11 embedded, JSP inside) and one Oracle schema:

| WAR | Default port | Deploy on |
| :--- | :--- | :--- |
| `uco-realty-public-0.1.0.war` | 8080 | internet-facing zone |
| `uco-realty-internal-0.1.0.war` | 8081 | **intranet only** |

Both WARs run two ways (same file): `java -jar <war>` (verified), or dropped into an **external Tomcat 11** `webapps/` folder (the normal bank way; the WARs are built for it: embedded Tomcat/Jasper/Servlet jars are "provided", so none are bundled; **not yet tested on a real Tomcat 11**).

**Server requirements:** JDK 25 (the classes are compiled for Java 25) and Tomcat 11 (Jakarta Servlet 6.1). An older Tomcat 9/10 or JDK 8/11/17/21 cannot run these WARs; for a Tomcat 9 / Java 8 server use the `uco-realty-v2` code line instead.

**URL path = WAR file name.** `uco-realty-internal.war` is served at `https://<host>/uco-realty-internal/`, `uco-realty-public.war` at `/uco-realty-public/` (use `ROOT.war` for the site root). All links in the application are relative to that path, so any name works. The bundle made by `scripts/package-release.sh` already uses these version-free names.

**External Tomcat setup (per server):** put the settings in `bin/setenv.sh` (Linux) / `bin/setenv.bat` (Windows), then copy the WAR to `webapps/` and start Tomcat:
```bash
export CATALINA_OPTS="-Dspring.profiles.active=uat \
  -DUAT_ORACLE_URL=jdbc:oracle:thin:@//db-host:1521/SERVICE -DUAT_ORACLE_USER=... -DUAT_ORACLE_PASS=... \
  -DSSO_ENCRYPTION_KEY=... -DSSO_ENCRYPTION_IV=... -DSESSION_COOKIE_SECURE=true"
```
(`-D...` JVM properties and OS environment variables both work.) `server.port` and `server.servlet.context-path` in the application files are ignored on an external Tomcat; the connector port and the WAR name decide.

## 1. Database (DBA)
Follow `db/README_DBA_DEPLOYMENT.md`. Order: `01_CREATE_TABLES.sql`; for an existing older database also `04_DROP_USERS_PASSWORD_HASH.sql`. **UAT only:** `03_CREATE_HRMS_MOCK_TABLES.sql` (if the UAT DB has no HRMS tables) and `05_UAT_TEST_EMPLOYEES.sql` (fictitious test employees). There is no seed data and no application accounts.
The application schema user needs `SELECT` on the bank HRMS tables (`employee_contact_details`, and optionally `employee_tenure`, `employee_job_profile`). Columns read from `employee_contact_details`: `EMPNO, NAME, POSTING_SOL_ID, POSTING_SOL_DESC, SCALE_CODE, CADRE_CODE, POSTING_REGION_NAME, DESIG, MOBILE_NO, E_MAIL` (tables are tried with and without the `hrms_dba.` prefix). Please verify these names against the real HRMS schema.

## 2. Configuration (environment variables)
| Variable | Module | Required | Meaning |
| :--- | :--- | :--- | :--- |
| `SPRING_PROFILES_ACTIVE` | both | yes (`uat` or `prod`) | default is `prod`; never use `dev` outside a laptop |
| `PROD_ORACLE_URL` / `_USER` / `_PASS` | both | prod | `jdbc:oracle:thin:@//host:1521/SERVICE` |
| `UAT_ORACLE_URL` / `_USER` / `_PASS` | both | uat | same, UAT database (`uat` validates the schema at start) |
| `SSO_ENCRYPTION_KEY` | internal | yes | bank SSO key (Base64). **The app refuses to start without it** |
| `SSO_ENCRYPTION_IV` | internal | if the bank uses AES-CBC | Base64 IV |
| `APP_REDIRECT_URL` | internal | no | landing page after login, default `/admin/projects` |
| `SESSION_COOKIE_SECURE` | both | no | default `true`; set `false` only for a plain-HTTP UAT |
| `SERVER_PORT` | both | no | 8080 public / 8081 internal |

```bash
export SPRING_PROFILES_ACTIVE=uat
export UAT_ORACLE_URL='jdbc:oracle:thin:@//db-host:1521/SERVICE' UAT_ORACLE_USER='...' UAT_ORACLE_PASS='...'
export SSO_ENCRYPTION_KEY='...' SSO_ENCRYPTION_IV='...'
java -jar uco-realty-internal.war        # standalone alternative to Tomcat; same for uco-realty-public.war (without the SSO variables)
```
The WARs contain no sample data, test users or secrets. In `uat`/`prod` the dev conveniences are off: no plain `/sso?emp_id=` login, no application-issued OTP, no built-in employees (the bank SSO portal does the OTP).

## 3. Bank SSO integration
The bank portal redirects the employee to `https://<intranet-host>/uco-realty-internal/entry?value=<token>` (also accepted: `/GetValue.jsp`, `/sso?value=`). The token is the encrypted string `emp_id=<empno>&sol_id=<sol>`; if it decrypts, the employee is genuine. The application then:
1. looks the employee up in HRMS (the SOL in the token is **not** trusted; the HRMS posting decides),
2. derives the role: SOL 7114 → DIT superadmin, 7105 → HO Retail, 7102 → HO Marketing, other 71xx → HO Admin (view only), 70xx + a known zone → ZO Maker (Scale 1–3 / OFFICER) or ZO Checker (Scale 4+ / EXEC); **any other SOL is refused**,
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
2. UAT: open the UAT SSO portal link for a test employee (see `05_UAT_TEST_EMPLOYEES.sql`: maker `200001`, checker `200002`, DIT `100001`, branch employee `600001` must be refused).
3. Maker creates a project with photos and submits; the checker of the same zone approves; it appears on the customer site; a customer lead is saved; leads export to Excel; DIT sees the Logs page and a maker does not.

## 6. Hand-over bundle
`scripts/package-release.sh uat|prod` on the build machine → zip with both WARs, DBA scripts (production bundles omit drop/mock/test scripts), this guide and SHA-256 checksums.
