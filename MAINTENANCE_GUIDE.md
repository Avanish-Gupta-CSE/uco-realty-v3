# Maintenance guide: "I want to change X, what do I do?"

Written for someone new to Java. Each recipe says which file to open, what to change, and how to check it worked.
Two applications share one database: `ucorealty-customer` (public site) and `ucorealty-staff` (staff site). Paths below are inside
`ucorealty-staff/src/main/` or `ucorealty-customer/src/main/` unless they start with a folder at the top (`scripts/`, `db_migrations/`).

## The map (what lives where)
| Folder (`java/com/ucorealty/...`) | What is inside |
|---|---|
| `controller` | One class per group of web pages. A method here answers one URL, puts data in the `model`, and names the page (JSP) to show. |
| `service` | The business rules (who may do what, the approval steps, sign-in checks). |
| `repository` | The ONLY place with SQL. Every query is `@Query(nativeQuery = true, value = "plain SQL")`. |
| `model` | One class per database table (or per read-only bank table), plus `AppUser` (the signed-in employee). |
| `dto` | Small classes that only carry data to a page (`ProjectRow`). |
| `config` | Security rules, filters, where static files are found. |
| `util` | Helpers: bank table names (`DatabaseTables`), role lists (`Roles`, `RoleCheck`), token decoding. |
| `exception` | What the user sees when something goes wrong. |
| `webapp/WEB-INF/jsp` | The pages (JSP). `webapp/assets` holds css, js, images. |

How one page works: browser asks for a URL -> a `controller` method runs -> it calls a `service` -> which calls a `repository` (SQL)
-> the result goes into the `model` -> the controller returns the name of a JSP -> the JSP prints the values with `${...}`.

Never put JavaScript or CSS inside a JSP (bank rule): put it in `webapp/assets/js` and `css`, and give elements an `id`.

## After ANY change: check nothing broke
1. `scripts/dev-start.sh` (builds both applications and starts them on your laptop with test data in memory).
2. `python3 scripts/smoke_test.py`, `python3 scripts/workflow_test.py`, `python3 scripts/context_path_check.py` (all must say passed / OK).
3. Look in `.run/internal.log` and `.run/public.log`: there must be no line with `[UCO ERROR]`.
4. Optional but strong proof that no page changed: `scripts/snapshot_fresh.sh .run/before` BEFORE your change, again as `.run/after` AFTER it,
   then `python3 scripts/snapshot_pages.py compare .run/before .run/after`. It prints every page that differs.

## Recipes

### Give a role to another posting (SOL), or take it away
File: `ucorealty-staff/src/main/resources/application.properties`, the lines `uco.rbac.superadmin-sols`, `marketing-sols`, `retail-sols`
(comma separated numbers) and `uco.rbac.zonal-sol-regex`. Rebuild. The rules that turn a posting into a role are in `service/RoleResolver.java`
(zonal office: scale 4 and above or cadre EXEC = checker, scale 1-3 = maker, scale 0 = no access).
Before production: `superadmin-sols` must be only `7115` (7103 is there only because UAT's DIT staff sit at 7103).

### The bank moved the HRMS tables to another database user
File: `util/DatabaseTables.java`. Edit the one line of the table (for example `"HRMS_DBA.employee_contact_details"`). If the tables belong
to our own user, write the plain name without `HRMS_DBA.`. Rebuild and deploy. Nothing else mentions the owner.

### Add or remove a column of the Leads page (example: remove Email)
The Leads page reads the bank table `LG_ENTRY_REVAMP`. Four places mention a column:
1. `repository/LgEntryRepository.java`: the column name in the `SELECT` of `findLeads` (remove `EMAIL`).
2. `model/LeadRow.java`: the field with `@Column(name = "EMAIL")` and its getter `getEmail()` (a mapped column that is not in the SELECT makes the query fail, so remove both).
3. `webapp/WEB-INF/jsp/leads.jsp`: the heading `<th>Email</th>` and the cell `<td data-label="Email">`.
4. `controller/LeadExportController.java`: the Excel heading `"Email"` (list of headings) and `l.email` in the row values (keep the two lists in the same order).
To ADD a column do the same in reverse (`SELECT`, a field in `LeadRow`, the page, the Excel).

### Change who sees which projects or which statuses exist
Rules about who may do what: `service/ProjectService.java` (zone access, maker/checker rules) and `service/ProjectWorkflowService.java` (the approval steps).
Which roles may open a page: the `RoleCheck.require(auth, Roles.XYZ)` line at the top of the controller method; the role lists are in `util/Roles.java`.

### Add a field to the project form (example: "Parking slots")
1. Database: add the column to BOTH tables `realty_projects` and `realty_project_staging` in `db_migrations/01_CREATE_TABLES.sql`
   (a database that already exists needs an `ALTER TABLE ... ADD ...` run by the DBA on both tables), then run `python3 scripts/h2_schema.py`.
2. `ucorealty-staff/.../model/ProjectData.java`: a field with `@Column(name = "parking_slots")` and its getter and setter. Staging and master tables share this class, and the
   "what changed" table, the staging-to-master copy and the text-field length check pick the new field up automatically.
3. `ucorealty-staff/.../webapp/WEB-INF/jsp/project-form.jsp`: an input named exactly like the Java field (`name="parkingSlots"`).
4. Customer site: the same field in `ucorealty-customer/.../model/Project.java` and a line in `webapp/WEB-INF/jsp/public-detail.jsp`
   (and the staff preview copy `ucorealty-staff/.../public-detail.jsp`, because the two sites have separate copies of that page).
5. If it must be validated (required, length, number): `service/ProjectValidator.java`.

### Change a size limit
* Photo 1 MB and PDF 3 MB: `controller/ProjectWorkflowController.java` (`MAX_PHOTO_BYTES`, `MAX_DOC_BYTES`) AND the browser check in `webapp/assets/js/project-form.js`
  (search for `1 * 1024 * 1024` and `3 * 1024 * 1024`) AND `spring.servlet.multipart.max-file-size` in `application.properties` (must be at least the largest file).
* Number of photos: `MAX_PHOTOS` in `service/ProjectWorkflowService.java`.
* Idle session time: `server.servlet.session.timeout` in `application.properties`.

### Change the Home Loan link
Customer app: `ucorealty-customer/src/main/resources/application.properties`, line `uco.homeloan.base-url` (the host). The path and `?utm=REALTY`
are fixed in `controller/PublicWebController.java` (`HOME_LOAN_PATH`). Rebuild.

### Turn the test login off (production) / see the test users
`ucorealty-staff/.../application.properties`: `uco.sso.dev-login-enabled=false` and delete the `uco.sso.test-otp` line; also remove the line `spring.profiles.active=h2`
(in both apps) so Oracle is used. Test employees and what they may do: `TESTING_GUIDE.md`.

### Add a new page
1. A method in a controller (`controller/...`) with `@GetMapping("/admin/xyz")` that returns the name of a JSP; first line `RoleCheck.require(auth, Roles....)`.
2. The JSP in `webapp/WEB-INF/jsp/xyz.jsp` (copy a small one such as `leads.jsp`).
3. Staff site: `/admin/**` already needs a sign-in. A page outside `/admin` must be listed in `config/InternalSecurityConfig.java`.
   Customer site: add the URL to the list in `config/PublicSecurityConfig.java`, otherwise it is refused.

### Add a new database query
In the matching file in `repository/`, copy an existing method and keep the form:
`@Query(nativeQuery = true, value = """ SELECT ... WHERE x = :x """)` with `@Param("x")`. Return the model class (whole rows) or a small interface (a few columns,
with the column names in quotes as `AS "name"` matching the getter). Never pass `null` or `''` as a value (Oracle treats an empty text as NULL): use a 0/1 flag like `LgEntryRepository` does.

### Where are the logs, and how do I read them?
`E:/ucorealty-staff/logs/` and `E:/ucorealty-customer/logs/` on the bank server (set in `src/main/resources/logback-spring.xml`); on your laptop `.run/internal.log` and `.run/public.log`.
Lines start with a tag: `[UCO SECURITY]`, `[UCO ACCESS]` (someone was refused), `[UCO ERROR]` (a bug: always look at these), `[UCO LEAD]`, `[UCO HRMS]`.

### Build and hand over
`scripts/package-release.sh uat` (or `prod`) builds both WARs and a folder with the DBA scripts. Before `prod` read the list in `DEPLOYMENT.md`
(real SSO key, test login off, no H2 profile, real database password, production Home Loan host).

### I broke something and the tests fail
`git status` shows what you changed; `git diff` shows how; `git restore <file>` throws a change away. Run the three scripts again after each small step
rather than after many changes.
