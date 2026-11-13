# Coding guide: how code is written here, so it stays easy to read and change

Read this once (10 minutes). The recipes for "I want to change X" are in `MAINTENANCE_GUIDE.md`; this page explains the ideas behind them.

## 1. Six words you need
| Word | Meaning | Example |
|---|---|---|
| master | the approved, live project; customers see exactly this (table `realty_projects`, class `model/Project`) | "Green Heights", status APPROVED |
| staging | a maker's working copy of a change that is not approved yet (table `realty_project_staging`, class `model/ProjectStaging`) | a draft of a new project, or a request to reduce units |
| maker | an employee who creates or changes projects (zonal maker, or HO Marketing / DIT for every zone) | scale 1-3 at a zonal office |
| checker | an employee who approves, rejects or returns a request | scale 4 and above, or cadre EXEC, at a zonal office |
| SOL | the bank's posting code of a branch or office; it decides the role | 7115 = DIT, 8028 = HO Marketing, 7010 = Durgapur zone |
| zone | the zonal office area a project and a zonal employee belong to; a zonal maker or checker works only in their own zone | Durgapur |

Nothing a maker does touches the master table. Only a checker's approval copies the staging row into it (`ProjectWorkflowService.decide`).

## 2. The life of a request (follow these files with your editor)
**A customer opens the catalog** (`UcoRealty`): browser asks `/UcoRealty/projects` -> `config/CustomerSecurityConfig` (is this address open?) -> `controller/CatalogPageController.listProjects` -> `service/CatalogService` (which projects, which filter) -> `repository/ProjectRepository` (the SQL) -> `model/Project` -> `webapp/WEB-INF/jsp/public-projects.jsp` prints it. Each picture is another request to `/files/{id}` (`CatalogPageController.getFile`).

**A staff member signs in** (`UcoRealtyStaff`): the bank portal sends `/entry?Value=...` -> `controller/HomeController.entry` -> `util/SsoToken` (decrypt) -> `service/HrmsService.fetchUserProfile` -> `repository/HrmsRepository` (HRMS tables, names in `util/DatabaseTables`) -> `service/RoleResolver` (posting to role) -> a `model/StaffUser` is stored in the session -> `/admin/projects`. From then on `interceptor/SessionInterceptor` and `config/StaffSecurityConfig` guard every page, and each controller method starts with `StaffRoles.require(...)`.

**A maker sends a project, a checker approves it:** `controller/ProjectWorkflowController` (the screens) -> `service/ProjectWorkflowService` (`createDraft`, `saveDraft`, `submit`, then `decide`) -> `repository/ProjectStagingRepository` -> on approval `applyCreate` / `applyUpdate` / `applyDelete` write the master row -> `service/AuditService` records who did it -> customers see it in the first flow.

## 3. House rules (the code already follows them; keep it so)
1. **Names say what a thing does.** `ProjectAccessService` (who may do what), `CatalogService` (what customers see), `LeadsController` (the Leads page). No abbreviations: `findOpenRequestOfProject`, not `fFPSI`.
2. **Every class starts with a short comment** that ends with "Change this file when: ...". If you cannot finish that sentence, the class does too many things.
3. **One form of SQL, only in `repository/`:** `@Query(nativeQuery = true, value = """ ... """)` with named `:parameters`. Never put SQL in a controller or service. Never pass `null` or an empty text as a value (Oracle treats `''` as NULL): use a 0/1 flag like `LgEntryRepository`.
4. **No JavaScript or CSS inside a JSP** (bank rule); every element gets an `id`. Scripts go in `webapp/assets/js`, styles in `css`.
5. **No test or mock data in code.** Test employees live in `db_migrations/05_*.sql` and `scripts/`, never in Java.
6. **Plain constructs:** short methods (aim for 30 lines), `if` and `for` before clever streams, a comment above anything that is not obvious saying WHY.
7. **Two applications, two copies:** there is no shared module on purpose. When you change something that exists in both (`SecurityHeadersFilter`, `H2ServerStarter`, `public-detail.jsp`, `public-projects.jsp`, a project column), change both and run `python3 scripts/sync_check.py`.

## 4. How to check your work (3 minutes)
`scripts/dev-start.sh`, then `python3 scripts/smoke_test.py`, `python3 scripts/workflow_test.py`, `python3 scripts/context_path_check.py`, `python3 scripts/sync_check.py`, and look for `[UCO ERROR]` in `.run/*.log`. For a change that must not alter any page: `scripts/snapshot_fresh.sh` before and after, then `python3 scripts/snapshot_pages.py compare` (see `MAINTENANCE_GUIDE.md`).

## 5. Where the JSON addresses are described
`docs/openapi-customer.yaml` and `docs/openapi-staff.yaml` (OpenAPI 3, written by hand, open them at https://editor.swagger.io). `sync_check.py` fails when an address exists in the code and not in the file, or the other way round.
