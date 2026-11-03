# UCO Realty

UCO Bank **Approved Housing Projects portal**: customers browse bank-approved housing projects and are sent to the UCO Bank Home Loan page; bank staff enter, verify and approve projects (Maker–Checker) across the 49 Zonal Offices.

**Stack:** Java 25, Spring Boot 4.1.2, Tomcat 11, JSP/JSTL + Bootstrap 5, Oracle. Two executable WARs, no Node.js, no tests (bank policy).

| Module | Port | Audience | What it does |
| :--- | :--- | :--- | :--- |
| `uco-realty-public` | 8080 | Internet customers | Read-only catalog of **approved** projects, photos, Home Loan lead capture, JSON API `/api/public/**`. No login, no staff routes. |
| `uco-realty-internal` | 8081 | Bank staff (intranet) | Everything above (staff see all statuses) plus bank SSO login, Maker–Checker `/admin/projects`, leads + Excel export, DIT-only **Logs** page. |

Both use the same Oracle schema. There are **no local user accounts**: staff sign in through the bank SSO (`/entry?value=<encrypted token>`), the employee is looked up in the bank HRMS tables, and the role is derived at login (SOL ID + cadre/scale). Details: `DEPLOYMENT.md`.

## Run locally
Needs Docker (Oracle XE container), Java 25 and Maven.

```bash
scripts/dev-start.sh            # Oracle + tables + both portals (dev profile)
scripts/dev-stop.sh
scripts/dev-start.sh --reset    # wipe the database and start again
```
- Customer portal: http://localhost:8080/projects
- Staff portal: http://localhost:8081/login (dev profile only: sign in with a test employee id and OTP `123456`, e.g. `100001` DIT superadmin, `200001` Ajmer maker, `200002` Ajmer checker)

More: `TESTING_GUIDE.md`.

### Raw commands (without the scripts)
```bash
mvn clean package -DskipTests
SPRING_PROFILES_ACTIVE=dev java -jar uco-realty-public/target/uco-realty-public-0.1.0.war
SPRING_PROFILES_ACTIVE=dev java -jar uco-realty-internal/target/uco-realty-internal-0.1.0.war   # second shell
```
The default profile is **prod**; local runs must say `dev`.

### Eclipse / STS
Import as *Existing Maven Projects* from the repository root (two modules). Use a Java 25 JRE. Run `com.ucorealty.PublicPortalApplication` and/or `com.ucorealty.InternalPortalApplication` as a Java Application with VM argument `-Dspring.profiles.active=dev` (add `-Dserver.port=8081` etc. only if you change ports).

## Layout
```
pom.xml                    parent (modules: uco-realty-public, uco-realty-internal)
uco-realty-public/         customer portal
uco-realty-internal/       staff portal
db_migrations/             Oracle scripts + DBA guide
scripts/                   dev-start/stop, smoke_test.py, load_demo_data.py, package-release.sh
.brain/                    project knowledge base (never ship it)
```

## Hand-over bundle
```bash
scripts/package-release.sh uat      # or prod: WARs + DBA scripts + DEPLOYMENT.md + SHA256SUMS
```
