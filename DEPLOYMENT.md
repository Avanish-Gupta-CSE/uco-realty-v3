# UCO Realty — Deployment Guide

Single executable WAR (`uco-realty-0.1.0.war`) built on **Java 1.8** and **Spring Boot 2.7.18** with standard **JSP** pages embedded inside (`WEB-INF/jsp/`). Oracle is the only database. Zero Node.js or external runtime dependencies.

## 1. Database (DBA)
Follow `db/README_DBA_DEPLOYMENT.md`. Order: `01_CREATE_TABLES.sql`, then `02_INITIAL_ADMIN_USERS.sql` (prompts for two BCrypt hashes; there is no default password). Nothing else is seeded.

## 2. Start the Application
Run as a standalone executable WAR or deploy to an external Tomcat/WebLogic server:

```bash
export SPRING_PROFILES_ACTIVE=uat        # or prod
export UAT_ORACLE_URL='jdbc:oracle:thin:@//db-host:1521/SERVICE'   # prod: PROD_ORACLE_URL
export UAT_ORACLE_USER='...'             # prod: PROD_ORACLE_USER
export UAT_ORACLE_PASS='...'             # prod: PROD_ORACLE_PASS
export SERVER_PORT=8080                  # optional, default 8080

java -jar uco-realty-0.1.0.war
```

| Variable | Needed in | Note |
|---|---|---|
| `SPRING_PROFILES_ACTIVE` | uat, prod | `uat` validates schema on start; `prod` never touches it |
| `*_ORACLE_URL / _USER / _PASS` | uat, prod | UAT_ or PROD_ prefix |
| `SERVER_PORT` | optional | default 8080 |

The WAR contains no sample data, mock passwords, or private development folders.

## 3. Architecture & URLs
The application runs as a single unified service:
- **Public Portal:** `http://<host>:8080/projects` (Customer project catalog and details)
- **Staff Login:** `http://<host>:8080/login` (Standard session-based login)
- **Maker-Checker Admin:** `http://<host>:8080/admin/projects` (Zone maker draft creation & checker approval)
- **User Management:** `http://<host>:8080/admin/users` (DIT Superadmin / HO Admin)
- **Leads & Export:** `http://<host>:8080/admin/leads` (Call Centre lead viewing & Excel export)

## 4. Reverse Proxy Rules
| Path | Internet | Intranet |
|---|---|---|
| `/`, `/projects`, `/project/*`, `/lead`, `/files/*`, `/css/*`, `/images/*` | allow | allow |
| `/login`, `/logout`, `/admin/**` | **deny** | allow |

Staff login (`/login`) and administration (`/admin/**`) must not be accessible from the public internet. Ensure reverse proxy configures:
- HTTPS termination
- `X-Frame-Options: DENY`
- Forwarding real client IP address (`X-Forwarded-For`)

## 5. First Login Checklist
1. DIT signs in as `dit.superadmin` at `/login` and changes password immediately. Same for `ho.admin`.
2. `ho.admin` creates the ZO Maker / ZO Checker / Call Centre accounts at `/admin/users`.
3. Smoke test:
   - Maker creates project draft and uploads photos.
   - Maker clicks "Submit for Approval".
   - Checker reviews draft, verifies details, and approves.
   - Project appears immediately in public catalog (`/projects`).
   - Customer submits "Apply for Home Loan" lead.
   - Call Centre views and exports leads to Excel (`/admin/leads/export.xlsx`).

## 6. Handing Over a Release Bundle
Run `scripts/package-release.sh uat|prod` on the build server. It executes all unit tests, compiles the WAR, bundles the DBA scripts, and computes SHA-256 checksums into a release zip.
