# What the scripts do (nothing here is needed on the bank servers)

**Deploying to UAT/production uses none of these scripts.** They only make a laptop easier. The real deployment is five plain steps (also in `DEPLOYMENT.md`):

| # | Step | Who | Command / action |
| :-: | :--- | :--- | :--- |
| 1 | Create the tables | DBA | run `db_migrations/01_CREATE_TABLES.sql` as the application schema user (existing databases also `06_STAGING_MODEL.sql`, `07_DROP_UNUSED_TABLES.sql`) |
| 2 | Build the two WARs | you | `cd ucorealty-customer && mvn clean package -DskipTests`, then the same in `ucorealty-staff` (JDK 25). Result: `target/ucorealty-customer.war`, `target/ucorealty-staff.war` |
| 3 | Set the settings on each server | server admin | `SPRING_PROFILES_ACTIVE=uat` (or `prod`), `DB_URL`, `DB_USER`, `DB_PASS`; staff server also `SSO_ENCRYPTION_KEY`, `SSO_ENCRYPTION_IV` (e.g. in Tomcat's `bin/setenv.sh`) |
| 4 | Copy the WAR into Tomcat's `webapps/` | server admin | customer WAR on the internet-zone server, staff WAR on the intranet server; Tomcat 11 + JDK 25 |
| 5 | Open the URL | everyone | `https://<host>/ucorealty-customer/projects`, `https://<host>/ucorealty-staff/` |

`scripts/package-release.sh uat` does steps 2 plus "put the WARs, the SQL files and DEPLOYMENT.md in one zip with checksums" for the hand-over.

## Running it by hand, without `dev-start.sh` (verified)
Open a **new** terminal (Java 25 must be the default: `java -version` shows 25), start in the repository folder.

### Route A: Oracle in Docker
```bash
docker start uco-oracle                      # wait ~30 s until:  docker inspect -f '{{.State.Health.Status}}' uco-oracle   says healthy
# first time only, on an empty database: create the tables
docker cp db_migrations uco-oracle:/tmp/
echo exit | docker exec -i uco-oracle sqlplus -s uco_realty/uco_realty_pwd@//localhost/XEPDB1 @/tmp/db_migrations/01_CREATE_TABLES.sql

(cd ucorealty-customer && mvn clean package -DskipTests)      # -> target/ucorealty-customer.war
(cd ucorealty-staff && mvn clean package -DskipTests)         # -> target/ucorealty-staff.war

# terminal 1 (customer site):
SPRING_PROFILES_ACTIVE=local DB_URL=jdbc:oracle:thin:@localhost:1521/XEPDB1 DB_USER=uco_realty DB_PASS=uco_realty_pwd \
  java -Dserver.port=8080 -jar ucorealty-customer/target/ucorealty-customer.war
# terminal 2 (staff site):
SPRING_PROFILES_ACTIVE=local DB_URL=jdbc:oracle:thin:@localhost:1521/XEPDB1 DB_USER=uco_realty DB_PASS=uco_realty_pwd \
  java -Dserver.port=8081 -jar ucorealty-staff/target/ucorealty-staff.war
```
### Route B: in-memory H2 (no Docker)
```bash
H2=$HOME/.m2/repository/com/h2database/h2/2.4.240/h2-2.4.240.jar
mvn dependency:get -Dartifact=com.h2database:h2:2.4.240       # once; downloads the H2 jar

# terminal 1: the H2 database server (leave it running; web console http://localhost:8082)
java -cp $H2 org.h2.tools.Server -tcp -tcpPort 9092 -web -webPort 8082 -ifNotExists

# terminal 2: create the tables, build WITH the H2 driver (-Ph2), run both apps
python3 scripts/h2_schema.py > /tmp/h2-schema.sql
java -cp $H2 org.h2.tools.RunScript -url "jdbc:h2:tcp://localhost:9092/mem:ucorealty;MODE=Oracle;DB_CLOSE_DELAY=-1" -user sa -script /tmp/h2-schema.sql
(cd ucorealty-customer && mvn -Ph2 clean package -DskipTests)
(cd ucorealty-staff && mvn -Ph2 clean package -DskipTests)
SPRING_PROFILES_ACTIVE=local java -Dserver.port=8080 -jar ucorealty-customer/target/ucorealty-customer.war     # terminal 3
SPRING_PROFILES_ACTIVE=local java -Dserver.port=8081 -jar ucorealty-staff/target/ucorealty-staff.war            # terminal 4
```
### Open and stop
Customer: http://localhost:8080/ucorealty-customer/projects. Staff: http://localhost:8081/ucorealty-staff/ (goes to the login page; test employee `200001`, OTP `123456`).
Stop an app or the H2 server with **Ctrl+C** in its terminal; `docker stop uco-oracle` stops Oracle. H2 data disappears when the H2 server stops.

## `dev-start.sh`: what runs behind it (the script now prints each command in grey as it runs it)
It does exactly steps 1 to 4 above, on your laptop, in four stages:

| Stage | Oracle (default) | `--h2` (no Docker) |
| :--- | :--- | :--- |
| 1. Database | `docker start uco-oracle` (waits until its health check says healthy) | starts the H2 database server: `java -cp h2-2.4.240.jar org.h2.tools.Server -tcp -tcpPort 9092 -web -webPort 8082 -ifNotExists` (log `.run/h2-server.log`, web console http://localhost:8082) |
| 2. Build | `cd ucorealty-customer && mvn -q clean package -DskipTests`, same in `ucorealty-staff` | the same with `-Ph2`, which adds the H2 driver to the WAR |
| 3. Tables | `docker cp db_migrations uco-oracle:/tmp/`, then `sqlplus uco_realty` runs `01_CREATE_TABLES.sql` only if the schema has no tables (`--reset` runs `00_DROP_ALL_TABLES.sql` first) | `python3 scripts/h2_schema.py > .run/h2-schema.sql` (the same `01_CREATE_TABLES.sql`, made H2-friendly), then `org.h2.tools.RunScript` runs it (drops and recreates) |
| 4. Start the apps | `SPRING_PROFILES_ACTIVE=local DB_URL=... DB_USER=... DB_PASS=... java -Dserver.port=8080 -jar ucorealty-customer.war`, then the same for `ucorealty-staff.war` on 8081. Each runs in the background; logs `.run/public.log`, `.run/internal.log`; process ids `.run/*.pid` | the same without `DB_*`: the `local` profile defaults to the H2 server |

Why the `local` profile: `application-local.properties` switches on the test OTP (123456; employees come from the stand-in HRMS tables that dev-start loads from `db_migrations/03` and `05`), plain-http cookies and the `/ucorealty-...` context path. On a bank server you never use it.
The script then waits until each log says "Started ...Application" and prints the URLs.

## The other scripts
| Script | What it does | Needed for deployment? |
| :--- | :--- | :--- |
| `dev-stop.sh` | kills the two apps (and, without `--apps-only`, the H2 server) | no |
| `package-release.sh uat|prod` | builds both WARs and zips them with the SQL files, the deployment guide and SHA-256 checksums (never mock HRMS tables or demo data). A `uat` build adds the H2 driver (`-Ph2`) so the same WAR runs with Oracle or, with no database, on an H2 file; a `prod` build never contains H2 | optional: a convenient hand-over bundle |
| `h2_schema.py` | converts `db_migrations/01_CREATE_TABLES.sql` for H2 (drops the Oracle-only storage clause and the expression index) | no (H2 only) |
| `uat_demo_check.py` | 26 checks of the UAT demo setup (`uco.auth.directory=demo`): demo login page, wrong password, closed SSO doors, the four roles, one project from maker to checker to the customer site. Reads the passwords from `DemoUserDirectory.java` | no (run against the apps started with `SPRING_PROFILES_ACTIVE=uat,h2` or `uat`) |
| `smoke_test.py` | 35 quick checks: lockdown of the customer site, headers, CSRF, bad tokens, role access, logout | no (run before a release, local profile only) |
| `workflow_test.py` | 83 checks of the whole maker-checker flow through the real screens; leaves two test projects behind | no (local profile only) |
| `context_path_check.py` | crawls both apps as several roles and fails if any URL lacks the context path | no (run before a release) |
| `load_demo_data.py` / `remove_demo_data.sh` | create / delete nine demo projects, leads and photos (all names end in "(Demo)") | no |
| `uco_http.py` | tiny helper the Python scripts share (cookies, CSRF token, login) | no |
| `build_geo_districts.py` | one-off tool that built `geo/districts.json` from a government CSV | no |

## Reading the logs
`.run/internal.log` (staff) and `.run/public.log` (customer). On a Tomcat server the same output goes to `catalina.out`. A failed start shows "APPLICATION FAILED TO START" with the reason (wrong `DB_*`, missing `SSO_ENCRYPTION_KEY`, or in `uat` tables that do not match the code).
