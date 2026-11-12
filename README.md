# UCO Realty

UCO Bank **Approved Housing Projects portal**: customers browse bank-approved housing projects and are sent to the UCO Bank Home Loan page; bank staff enter, verify and approve projects (Maker–Checker) across the 49 Zonal Offices.

**Stack:** Java 25, Spring Boot 4.1.2, Tomcat 11, JSP/JSTL + Bootstrap 5, Oracle. Two executable WARs, no Node.js, no tests (bank policy).

Project `ucorealty` (package `com.ucorealty`); the WAR file name is the URL path on Tomcat. `customer` = the internet-facing site for customers, `staff` = the intranet site for bank staff.

| Module / WAR | Port | Audience | What it does |
| :--- | :--- | :--- | :--- |
| `UcoRealty` | 8080 | Internet customers | Read-only catalog of **approved** projects, photos, an Apply for Home Loan link to the bank's page, JSON API `/api/public/**`. No login, no staff routes. |
| `UcoRealtyStaff` | 8081 | Bank staff (intranet) | Everything above (staff see all statuses) plus bank SSO login, Maker–Checker `/admin/projects`, leads (read from the bank's LG_ENTRY_REVAMP) + Excel export, DIT-only **Logs** page. |

Both use the same Oracle schema. There are **no local user accounts**: staff sign in through the bank SSO (`/entry?Value=<encrypted token>`), the employee is looked up in the bank HRMS tables, and the role is derived at login (SOL ID + cadre/scale). Details: `DEPLOYMENT.md`.

## Run locally
Needs Java 25 and Maven. Nothing else: the profile in `application.properties` is `h2`, so all tables and test employees are created in memory at start.
```bash
scripts/dev-start.sh            # build and start both apps (h2)
scripts/dev-stop.sh
scripts/dev-start.sh --oracle   # the same on Oracle in Docker (uat profile); add --reset to wipe it
```
- Customer portal: http://localhost:8080/UcoRealty/projects
- Staff portal: http://localhost:8081/UcoRealtyStaff/ (real UAT tester numbers such as `55012`, `52260`, `68696`; list in TESTING_GUIDE.md, OTP `123456`)

More: `TESTING_GUIDE.md`. Profiles: `DEPLOYMENT.md` section 2. **Want to change something? `MAINTENANCE_GUIDE.md` says which file to open.** Code layout: `config, controller, dto, exception, interceptor, model, repository, service, util`; every SQL is a native `@Query` in `repository`.

### Raw commands (without the scripts)
```bash
# each app is self-sufficient: it has its own pom.xml and is built on its own
(cd UcoRealty && mvn clean package -DskipTests)
(cd UcoRealtyStaff && mvn clean package -DskipTests)
java -jar UcoRealtyStaff/target/UcoRealtyStaff.war
java -jar UcoRealty/target/UcoRealty.war   # second shell
```

### Eclipse / STS
Import `UcoRealty` and `UcoRealtyStaff` as two separate *Existing Maven Projects* (each folder has its own `pom.xml`). Use a Java 25 JRE. Run `com.ucorealty.PublicPortalApplication` and/or `com.ucorealty.InternalPortalApplication` as a Java Application with VM argument `-Dspring.profiles.active=local` (add `-Dserver.port=8081` etc. only if you change ports).

## Layout
```
UcoRealty/         customer portal
UcoRealtyStaff/       staff portal
db_migrations/             Oracle scripts + DBA guide
scripts/                   dev-start/stop, smoke_test.py, workflow_test.py (maker-checker end to end), load_demo_data.py, package-release.sh
.brain/                    project knowledge base (never ship it)
```

## Hand-over bundle
```bash
scripts/package-release.sh uat      # or prod: WARs + DBA scripts + DEPLOYMENT.md + SHA256SUMS
```
