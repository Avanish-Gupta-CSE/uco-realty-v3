# Where to change the constant values (DB password, SSO key, ...)

Every value below sits in a `application.properties` file with a `# TODO:` comment on the line above it. Change it **before building** (unzipped WAR: `WEB-INF/classes/application.properties`), then deploy. There is no server setting on the bank servers. Never commit real values.

## UcoRealtyStaff (`UcoRealtyStaff/src/main/resources/application.properties`)
| Line | Key | UAT | Production |
| :-: | :--- | :--- | :--- |
| 21 | `spring.datasource.url` | CLDBUAT address (already set) | production DB host/port/service |
| 22 | `spring.datasource.username` | `HRMS_DBA` (the one application user) | production user |
| 24 | `spring.datasource.password` (`CHANGE_ME_DB_PASSWORD` after the colon) | **real DB password** | production password |
| 32 | `spring.jpa.properties.hibernate.dialect` | `org.hibernate.dialect.OracleDialect` (Oracle 19c or newer) | same; keep. Hibernate 7 does not support Oracle 12c |
| 51 | `sso.encryption.key` | dev key until the bank gives one | **real bank key** (16, 24 or 32 characters) |
| 55 | `uco.sso.dev-login-enabled` | `false` (since the 26 Nov UAT bundle: SSO only; the laptop script turns it on itself) | `false` |
| 56 | `uco.sso.test-otp` | `123456` | **delete the line** |
| 61 | `uco.rbac.superadmin-sols` | `7115,7103` | `7115` only |
| 64-66 | marketing/retail SOLs, zonal regex | keep | keep |

## UcoRealty (`UcoRealty/src/main/resources/application.properties`)
| Line | Key | UAT | Production |
| :-: | :--- | :--- | :--- |
| 21, 22 | `spring.datasource.url`, `username` | as above | production |
| 24 | `spring.datasource.password` | **real DB password** | production password |
| 32 | `spring.jpa.properties.hibernate.dialect` | `org.hibernate.dialect.OracleDialect` (Oracle 19c or newer) | same; keep. Hibernate 7 does not support Oracle 12c |
| 44 | `uco.homeloan.base-url` | UAT Home Loan host | production Home Loan host (no trailing slash) |
| 48 | `uco.homeloan.online-apply-url` | `https://stp.uco.bank.in/home-loan/#/home-loan-login` | keep; change only if the bank gives another address for the online Home Loan application (the "Apply Online" button) |

## Logs
`UcoRealtyStaff/src/main/resources/logback-spring.xml` and `UcoRealty/.../logback-spring.xml` write to `E:/Realty/<App>/logs/` (lines 15 and 17). Files are never deleted. Change the drive there only if the server team says so.

## Steps
1. Edit the lines above in both files (search for `TODO`).
2. `scripts/package-release.sh uat` (or `prod`; prod refuses a build that still has the dev key, test OTP or dev login).
3. Copy `UcoRealtyStaff.war` and `UcoRealty.war` from `release/<bundle>/` to Tomcat `webapps`. Run `db/01_CREATE_TABLES.sql` first (see `docs/UAT_CHECKLIST.md`).
4. The session cookie `Secure` flag cannot be set from these files: the server team sets it on the https connector. Also ask them to remove Tomcat's default root/manager apps.
