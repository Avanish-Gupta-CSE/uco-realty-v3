# UCO Realty

UCO Bank **Approved Housing Projects portal**: customers browse bank-approved housing projects and are sent to the UCO Bank Home Loan page; bank staff enter, verify and approve projects (Maker–Checker) across the 49 Zonal Offices.

**Stack:** Java 25, Spring Boot 4.1.2, Tomcat 11, JSP/JSTL + Bootstrap 5, Oracle. Two executable WARs, no Node.js, no tests (bank policy).

Project `ucorealty` (package `com.ucorealty`); the WAR file name is the URL path on Tomcat. `customer` = the internet-facing site for customers, `staff` = the intranet site for bank staff.

| Module / WAR | Port | Audience | What it does |
| :--- | :--- | :--- | :--- |
| `ucorealty-customer` | 8080 | Internet customers | Read-only catalog of **approved** projects, photos, Home Loan lead capture, JSON API `/api/public/**`. No login, no staff routes. |
| `ucorealty-staff` | 8081 | Bank staff (intranet) | Everything above (staff see all statuses) plus bank SSO login, Maker–Checker `/admin/projects`, leads + Excel export, DIT-only **Logs** page. |

Both use the same Oracle schema. There are **no local user accounts**: staff sign in through the bank SSO (`/entry?Value=<encrypted token>`), the employee is looked up in the bank HRMS tables, and the role is derived at login (SOL ID + cadre/scale). Details: `DEPLOYMENT.md`.

## Run locally
Needs Java 25 and Maven. Nothing else: the profile in `application.properties` is `h2`, so all tables and test employees are created in memory at start.
```bash
scripts/dev-start.sh            # build and start both apps (h2)
scripts/dev-stop.sh
scripts/dev-start.sh --oracle   # the same on Oracle in Docker (uat profile); add --reset to wipe it
```
- Customer portal: http://localhost:8080/ucorealty-customer/projects
- Staff portal: http://localhost:8081/ucorealty-staff/ (test employee `100001` to `600002`, OTP `123456`)

More: `TESTING_GUIDE.md`. Profiles: `DEPLOYMENT.md` section 2.

### Raw commands (without the scripts)
```bash
# each app is self-sufficient: it has its own pom.xml and is built on its own
(cd ucorealty-customer && mvn clean package -DskipTests)
(cd ucorealty-staff && mvn clean package -DskipTests)
java -jar ucorealty-staff/target/ucorealty-staff.war
java -jar ucorealty-customer/target/ucorealty-customer.war   # second shell
```

### Eclipse / STS
Import `ucorealty-customer` and `ucorealty-staff` as two separate *Existing Maven Projects* (each folder has its own `pom.xml`). Use a Java 25 JRE. Run `com.ucorealty.PublicPortalApplication` and/or `com.ucorealty.InternalPortalApplication` as a Java Application with VM argument `-Dspring.profiles.active=local` (add `-Dserver.port=8081` etc. only if you change ports).

## Layout
```
ucorealty-customer/         customer portal
ucorealty-staff/       staff portal
db_migrations/             Oracle scripts + DBA guide
scripts/                   dev-start/stop, smoke_test.py, workflow_test.py (maker-checker end to end), load_demo_data.py, package-release.sh
.brain/                    project knowledge base (never ship it)
```

## Hand-over bundle
```bash
scripts/package-release.sh uat      # or prod: WARs + DBA scripts + DEPLOYMENT.md + SHA256SUMS
```
