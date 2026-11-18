# Maintenance guide: "I want to change X, what do I do?"

Written for someone new to Java. Each recipe says which file to open, what to change, and how to check it worked.
Two applications share one database: `UcoRealty` (public site) and `UcoRealtyStaff` (staff site). Paths below are inside
`UcoRealtyStaff/src/main/` or `UcoRealty/src/main/` unless they start with a folder at the top (`scripts/`, `db_migrations/`).

## The map (what lives where)
| Folder (`java/com/ucorealty/...`) | What is inside |
|---|---|
| `controller` | One class per group of web pages. A method here answers one URL, puts data in the `model`, and names the page (JSP) to show. |
| `service` | The business rules (who may do what, the approval steps, sign-in checks). |
| `repository` | The ONLY place with SQL. Every query is `@Query(nativeQuery = true, value = "plain SQL")`. |
| `model` | One class per database table (or per read-only bank table), plus `StaffUser` (the signed-in employee). |
| `dto` | Small classes that only carry data to a page (`ProjectRow`). |
| `config` | Security rules, filters, where static files are found. |
| `util` | Helpers: bank table names (`DatabaseTables`), role lists (`StaffRoles`), token decoding. |
| `exception` | What the user sees when something goes wrong. |
| `webapp/WEB-INF/jsp` | The pages (JSP). `webapp/assets` holds css, js, images. |

### Which controller answers which pages (staff site `UcoRealtyStaff`)
| Class (`controller/`) | Pages it answers |
|---|---|
| `HomeController` | sign-in: `/entry` (bank SSO), `/login`, `/otp`, `/logout`, `/error-page` |
| `ProjectWorkflowController` | the project dashboard, the project form, send for approval, the checker's review, delete and unit-reduction requests |
| `ProjectCatalogController` | the project catalog `/projects`, one project `/project/{id}`, and every uploaded file `/files/{id}` |
| `LeadsController` | the Leads page, its Excel export, and the same list as JSON |
| `AuditLogController` | the Logs page (DIT only) |
| `PlacesApiController` | the state / district lists used by the form |
Customer site `UcoRealty`: `CatalogPageController` (catalog, one project, files, Home Loan link) and `CatalogApiController` (the JSON of the same).

How one page works: browser asks for a URL -> a `controller` method runs -> it calls a `service` -> which calls a `repository` (SQL)
-> the result goes into the `model` -> the controller returns the name of a JSP -> the JSP prints the values with `${...}`.

Never put JavaScript or CSS inside a JSP (bank rule): put it in `webapp/assets/js` and `css`, and give elements an `id`.

## After ANY change: check nothing broke
1. `scripts/dev-start.sh` (builds both applications and starts them on your laptop with test data in memory).
2. `python3 scripts/smoke_test.py`, `python3 scripts/workflow_test.py`, `python3 scripts/context_path_check.py` (all must say passed / OK).
3. Look in `.run/internal.log` and `.run/public.log`: there must be no line with `[UCO ERROR]`.
4. `python3 scripts/sync_check.py`: the two applications have not drifted apart (identical files, the same columns and page element ids, every JSON address described in `docs/openapi-customer.yaml` / `docs/openapi-staff.yaml`).
5. Optional but strong proof that no page changed: `scripts/snapshot_fresh.sh .run/before` BEFORE your change, again as `.run/after` AFTER it,
   then `python3 scripts/snapshot_pages.py compare .run/before .run/after`. It prints every page that differs.

## Recipes

### Give a role to another posting (SOL), or take it away
File: `UcoRealtyStaff/src/main/resources/application.properties`, the lines `uco.rbac.superadmin-sols`, `marketing-sols`, `retail-sols`
(comma separated numbers) and `uco.rbac.zonal-sol-regex`. Rebuild. The rules that turn a posting into a role are in `service/RoleResolver.java`
(zonal office: scale 4 and above or cadre EXEC = checker, scale 1-3 = maker, scale 0 = no access).
Before production: `superadmin-sols` must be only `7115` (7103 is there only because UAT's DIT staff sit at 7103).

### The bank moved the HRMS tables to another database user
File: `util/DatabaseTables.java`. Edit the one line of the table (for example `"HRMS_DBA.employee_contact_details"`). If the tables belong
to our own user, write the plain name without `HRMS_DBA.`. Rebuild and deploy. Nothing else mentions the owner.

### Add or remove a column of the Leads page (example: remove Email)
The Leads page reads the bank table `LG_ENTRY_REVAMP`. Four places mention a column:
1. `repository/LgEntryRepository.java`: the column name in the `SELECT` of `findLeads` (remove `EMAIL`).
2. `model/Lead.java`: the field with `@Column(name = "EMAIL")` and its getter `getEmail()` (a mapped column that is not in the SELECT makes the query fail, so remove both).
3. `webapp/WEB-INF/jsp/leads.jsp`: the heading `<th>Email</th>` and the cell `<td data-label="Email">`.
4. `controller/LeadsController.java`: the Excel heading `"Email"` (list of headings) and `l.email` in the row values (keep the two lists in the same order).
To ADD a column do the same in reverse (`SELECT`, a field in `Lead`, the page, the Excel).

### Change who sees which projects or which statuses exist
Rules about who may do what: `service/ProjectAccessService.java` (zone access, maker/checker rules) and `service/ProjectWorkflowService.java` (the approval steps).
Which roles may open a page: the `StaffRoles.require(auth, StaffRoles.XYZ)` line at the top of the controller method; the role lists are in `util/StaffRoles.java`.

### Add a field to the project form (example: "Parking slots")
1. Database: add the column to BOTH tables `realty_projects` and `realty_project_staging` in `db_migrations/01_CREATE_TABLES.sql`
   (a database that already exists needs an `ALTER TABLE ... ADD ...` run by the DBA on both tables), then run `python3 scripts/h2_schema.py`.
2. `UcoRealtyStaff/.../model/ProjectFields.java`: a field with `@Column(name = "parking_slots")` and its getter and setter. Staging and master tables share this class, and the
   "what changed" table, the staging-to-master copy and the text-field length check pick the new field up automatically.
3. `UcoRealtyStaff/.../webapp/WEB-INF/jsp/project-form.jsp`: an input named exactly like the Java field (`name="parkingSlots"`).
4. Customer site: the same field in `UcoRealty/.../model/Project.java` and a line in `webapp/WEB-INF/jsp/public-detail.jsp`
   (and the staff preview copy `UcoRealtyStaff/.../public-detail.jsp`, because the two sites have separate copies of that page).
5. If it must be validated (required, length, number): `service/ProjectValidator.java`.

### Change a size limit
* Photo 1 MB and each PDF attachment 3 MB: `controller/ProjectWorkflowController.java` (`MAX_PHOTO_BYTES`, `MAX_ATTACHMENT_BYTES`) AND the browser check in `webapp/assets/js/project-form.js` (`MAX_ATTACHMENT_BYTES`).
* Attachments (PDF files) have no limit on how many a project holds. One save is limited to 30 attachments and 33 MB of files in total: `MAX_ATTACHMENTS_PER_SAVE` and `MAX_UPLOAD_BYTES_PER_SAVE` in `project-form.js`, and `spring.servlet.multipart.max-request-size` (35 MB) in the staff `application.properties`. Photos are table `realty_project_images`, attachments `realty_project_attachments` (`model/ProjectAttachment.java`, `repository/ProjectAttachmentRepository.java`).
  (search for `1 * 1024 * 1024` and `3 * 1024 * 1024`) AND `spring.servlet.multipart.max-file-size` in `application.properties` (must be at least the largest file).
* Number of photos: `MAX_PHOTOS` in `service/ProjectWorkflowService.java`.
* How an attachment's file name is cleaned (any language kept, folder part and markup characters removed, 100 characters at most): `util/UploadChecks.java` (`attachmentName`); the column is `realty_project_attachments.file_name VARCHAR2(255 CHAR)`.
* Idle session time: `server.servlet.session.timeout` in `application.properties`.

### Change the Home Loan link
Customer app: `UcoRealty/src/main/resources/application.properties`, line `uco.homeloan.base-url` (the host). The path and `?utm=REALTY`
are fixed in `controller/CatalogPageController.java` (`HOME_LOAN_PATH`). Rebuild.

### Turn the test login off (production) / see the test users
`UcoRealtyStaff/.../application.properties`: `uco.sso.dev-login-enabled=false` and delete the `uco.sso.test-otp` line; keep the line `spring.profiles.active=h2` commented out (it already is)
(in both apps) so Oracle is used. Test employees and what they may do: `TESTING_GUIDE.md`.

### Add a new page
1. A method in a controller (`controller/...`) with `@GetMapping("/admin/xyz")` that returns the name of a JSP; first line `StaffRoles.require(auth, StaffRoles....)`.
2. The JSP in `webapp/WEB-INF/jsp/xyz.jsp` (copy a small one such as `leads.jsp`).
3. Staff site: `/admin/**` already needs a sign-in. A page outside `/admin` must be listed in `config/StaffSecurityConfig.java`.
   Customer site: add the URL to the list in `config/CustomerSecurityConfig.java`, otherwise it is refused.

### Add a new database query
In the matching file in `repository/`, copy an existing method and keep the form:
`@Query(nativeQuery = true, value = """ SELECT ... WHERE x = :x """)` with `@Param("x")`. Return the model class (whole rows) or a small interface (a few columns,
with the column names in quotes as `AS "name"` matching the getter). Never pass `null` or `''` as a value (Oracle treats an empty text as NULL): use a 0/1 flag like `LgEntryRepository` does.

### Where are the logs, and how do I read them?
`E:/Realty/UcoRealtyStaff/logs/` and `E:/Realty/UcoRealty/logs/` on the bank server, one file `<app>-<dateTime>.log` per start (set in `src/main/resources/logback-spring.xml`, see `docs/DEPLOYMENT.md` section Logs); on your laptop `.run/internal.log` and `.run/public.log`.
Lines start with a tag: `[UCO SECURITY]`, `[UCO ACCESS]` (someone was refused), `[UCO ERROR]` (a bug: always look at these), `[UCO LEAD]`, `[UCO HRMS]`.

### Build and hand over
`scripts/package-release.sh uat` (or `prod`) builds both WARs and a folder with the DBA scripts. Before `prod` read the list in `DEPLOYMENT.md`
(real SSO key, test login off, no H2 profile, real database password, production Home Loan host).

### I broke something and the tests fail
`git status` shows what you changed; `git diff` shows how; `git restore <file>` throws a change away. Run the three scripts again after each small step
rather than after many changes.
