# What the scripts do (nothing here is needed on the bank servers)

**Deploying uses none of these scripts.** They only make a laptop easier. Deployment is in `DEPLOYMENT.md` section 2; the plain steps for a database (`uat`, `prod`):

| # | Step | Who | Command / action |
| :-: | :--- | :--- | :--- |
| 1 | Create the tables | DBA | run `db_migrations` 01, and for `uat` also 03 and 05, as the application schema user |
| 2 | Build the two WARs | you | `cd ucorealty-customer && mvn clean package -DskipTests`, then the same in `ucorealty-staff` (JDK 25). Result: `target/ucorealty-customer.war`, `target/ucorealty-staff.war` |
| 3 | Set the settings on each server | server admin | `SPRING_PROFILES_ACTIVE=uat` (or set it in `application.properties`), `DB_URL`, `DB_USER`, `DB_PASS`; staff server also `SSO_ENCRYPTION_KEY`, `SSO_ENCRYPTION_IV` (e.g. in Tomcat's `bin/setenv.sh`) |
| 4 | Copy the WAR into Tomcat's `webapps/` | server admin | customer WAR on the internet-zone server, staff WAR on the intranet server; Tomcat 11 + JDK 25 |
| 5 | Open the URL | everyone | `https://<host>/ucorealty-customer/projects`, `https://<host>/ucorealty-staff/` |

`scripts/package-release.sh uat` does steps 2 plus "put the WARs, the SQL files and DEPLOYMENT.md in one zip with checksums" for the hand-over.

## Running it by hand, without `dev-start.sh`
```bash
(cd ucorealty-customer && mvn clean package -DskipTests)
(cd ucorealty-staff && mvn clean package -DskipTests)
java -jar ucorealty-staff/target/ucorealty-staff.war          # terminal 1
java -jar ucorealty-customer/target/ucorealty-customer.war    # terminal 2
```
That is all with the `h2` profile set in `application.properties`: H2 starts inside the app and the staff app runs all the SQL. For Oracle put `SPRING_PROFILES_ACTIVE=uat DB_URL=... DB_USER=... DB_PASS=...` in front of each `java`. Stop with Ctrl+C.

Customer: http://localhost:8080/ucorealty-customer/projects. Staff: http://localhost:8081/ucorealty-staff/ (employee `200001`, OTP `123456`). H2 console: http://localhost:8082 (`jdbc:h2:tcp://localhost:9092/mem:ucorealty`, user `sa`, no password).

## `dev-start.sh`
It stops old runs, builds both apps, starts staff then customer, and waits until each log says "Started". `--oracle` also starts the Docker Oracle, creates the tables once (01, 03, 05; `--reset` drops them first) and runs with the `uat` profile. It prints each command in grey.

## The other scripts
| Script | What it does | Needed for deployment? |
| :--- | :--- | :--- |
| `dev-stop.sh` | kills the two apps (with `h2`, that also ends the in-memory database) | no |
| `package-release.sh uat|prod` | builds both WARs and zips them with the SQL files, the deployment guide and SHA-256 checksums. `uat` includes the test employees (03, 05); `prod` never does and refuses to build unless `spring.profiles.active=prod` | optional: a convenient hand-over bundle |
| `h2_schema.py` | regenerates `ucorealty-staff/src/main/resources/h2-init.sql` from `db_migrations` 01, 03, 05. **Run it after changing any of those** | no |
| `smoke_test.py` | 35 quick checks: lockdown of the customer site, headers, CSRF, bad tokens, role access, logout | no (run before a release, `h2` or `uat` profile) |
| `workflow_test.py` | 83 checks of the whole maker-checker flow through the real screens; leaves two test projects behind | no (`h2` or `uat` profile) |
| `context_path_check.py` | crawls both apps as several roles and fails if any URL lacks the context path | no (run before a release) |
| `load_demo_data.py` / `remove_demo_data.sh` | create / delete nine demo projects, leads and photos (all names end in "(Demo)") | no |
| `uco_http.py` | tiny helper the Python scripts share (cookies, CSRF token, login) | no |
| `build_geo_districts.py` | one-off tool that built `geo/districts.json` from a government CSV | no |

## Reading the logs
`.run/internal.log` (staff) and `.run/public.log` (customer). On a Tomcat server the same output goes to `catalina.out`. A failed start shows "APPLICATION FAILED TO START" with the reason (wrong `DB_*`, missing `SSO_ENCRYPTION_KEY`, or in `uat` tables that do not match the code).
