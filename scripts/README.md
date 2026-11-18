# What the scripts do (nothing here is needed on the bank servers)

**Deploying uses none of these scripts.** They only make a laptop easier. Deployment is in `docs/DEPLOYMENT.md` section 2; the plain steps for a database (`uat`, `prod`):

| # | Step | Who | Command / action |
| :-: | :--- | :--- | :--- |
| 1 | Create the tables | DBA | run `db_migrations/01_CREATE_TABLES.sql` as the application schema user. `03` and `05` (stand-in HRMS tables and test employees) are for a laptop or Docker only: never on UAT or production, where the real HRMS tables exist |
| 2 | Write the settings into the files | you | there are NO server settings on the bank servers (no environment variables, no `-D`, no `setenv`): edit `application.properties` of both apps before the build: DB password (line 24), delete the line `spring.profiles.active=h2`, staff also `sso.encryption.key` (line 49) and, for production, `dev-login-enabled=false` and no `test-otp` line. List: `docs/UAT_CHECKLIST.md` sections A and E |
| 3 | Build the two WARs | you | `scripts/package-release.sh uat` (or `prod`), or by hand `cd UcoRealty && mvn clean package -DskipTests`, then the same in `UcoRealtyStaff` (JDK 25). Result: `target/UcoRealty.war`, `target/UcoRealtyStaff.war` |
| 4 | Copy the WAR into Tomcat's `webapps/` | server admin | customer WAR on the internet-zone server, staff WAR on the intranet server; Tomcat 11 + JDK 25 |
| 5 | Open the URL | everyone | `https://<host>/UcoRealty/projects`, `https://<host>/UcoRealtyStaff/` |

`scripts/package-release.sh uat` does step 3 plus "put the WARs, the SQL files and DEPLOYMENT.md in one zip with checksums" for the hand-over.

## Running it by hand, without `dev-start.sh`
```bash
(cd UcoRealty && mvn clean package -DskipTests)
(cd UcoRealtyStaff && mvn clean package -DskipTests)
java -jar UcoRealtyStaff/target/UcoRealtyStaff.war          # terminal 1
java -jar UcoRealty/target/UcoRealty.war    # terminal 2
```
That is all with the `h2` profile set in `application.properties`: H2 starts inside the app and the staff app runs all the SQL. For Oracle on your own laptop you may put `DB_URL=... DB_USER=... DB_PASS=...` in front of each `java` (this works only on a machine you control, not on the bank servers), or delete the `h2` line in the files and rebuild. Stop with Ctrl+C.

Customer: http://localhost:8080/UcoRealty/projects. Staff: http://localhost:8081/UcoRealtyStaff/ (employee `52260`, OTP `123456`). H2 console: http://localhost:8082 (`jdbc:h2:tcp://localhost:9092/mem:ucorealty`, user `sa`, no password).

## `dev-start.sh`
It stops old runs, builds both apps, starts staff then customer, and waits until each log says "Started". `--oracle` also starts the Docker Oracle, creates the tables once (01, 03, 05; `--reset` drops them first) and runs with the `uat` profile. It prints each command in grey.

## The other scripts
| Script | What it does | Needed for deployment? |
| :--- | :--- | :--- |
| `dev-stop.sh` | kills the two apps (with `h2`, that also ends the in-memory database) | no |
| `package-release.sh uat|prod` | builds both WARs and zips them with the SQL files, the deployment guide and SHA-256 checksums. `uat` includes the test employees (03, 05); `prod` never does and refuses to build unless `spring.profiles.active=prod` | optional: a convenient hand-over bundle |
| `h2_schema.py` | regenerates `UcoRealtyStaff/src/main/resources/h2-init.sql` from `db_migrations` 01, 03, 05. **Run it after changing any of those** | no |
| `smoke_test.py` | 34 quick checks: lockdown of the customer site, headers, CSRF, bad tokens, role access, logout | no (run before a release, `h2` or `uat` profile) |
| `workflow_test.py` | 83 checks of the whole maker-checker flow through the real screens; leaves two test projects behind | no (`h2` or `uat` profile) |
| `context_path_check.py` | crawls both apps as several roles and fails if any URL lacks the context path | no (run before a release) |
| `load_demo_data.py` / `remove_demo_data.sh` | create / delete nine demo projects and photos (all names end in "(Demo)") | no |
| `uco_http.py` | tiny helper the Python scripts share (cookies, CSRF token, login) | no |
| `build_geo_districts.py` | one-off tool that built `geo/districts.json` from a government CSV | no |

## Reading the logs
`.run/internal.log` (staff) and `.run/public.log` (customer). On a Tomcat server the same output goes to `catalina.out`. A failed start shows "APPLICATION FAILED TO START" with the reason (wrong database lines, an invalid `sso.encryption.key` (it must be 16, 24 or 32 characters), or tables that do not match the code).

## Page snapshot (proof that a clean-up changed nothing visible)
`scripts/snapshot_fresh.sh .run/snap-before` (stop, rebuild, start on H2, load demo data, save every page for every role), make the change,
`scripts/snapshot_fresh.sh .run/snap-after`, then `python3 scripts/snapshot_pages.py compare .run/snap-before .run/snap-after`. 410 pages; two clean runs on the same code are identical, so any difference is a real change.
