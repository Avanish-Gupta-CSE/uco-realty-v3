# Testing UCO Realty by hand (`h2` or `uat` profile)

## 1. Start
```bash
scripts/dev-start.sh            # build and start both apps with the h2 profile (nothing else needed)
scripts/dev-stop.sh             # stop; with h2 the data is gone
scripts/dev-start.sh --oracle   # the same on Oracle in Docker (uat profile); add --reset to wipe it
```

Staff sign in with an **employee id** and the OTP `123456` (`h2` and `uat`; production uses the bank SSO). There are no passwords and no user-management screens. The roles come from the employee's posting (SOL), exactly as in the bank:

| Employee id | Posting (SOL) | Role | Can do |
|---|---|---|---|
| `55012` | 7103 DIT (7115 in production) | **DIT superadmin** | everything in the app + the **Logs** page, leads |
| `54534` (Scale 3) / `53484` (Scale 4) | 8028 HO Marketing and Wealth Management | **HO marketing** | create/approve projects in any zone, leads |
| `58382` (Scale 3) / `51373` (Scale 4) | 8001 HO Retail Banking | **HO retail** | customer leads + Excel export, read-only view of projects (no reduce units) |
| `52260` / `45195` | 7010 Durgapur Zonal Office | **Maker (Scale 1)** / **Checker (Scale 4)** | Durgapur zone only |
| `68696` / `45321` | 7014 Raipur Zonal Office | Maker (Scale 1) / Checker (Scale 4) | Raipur zone only |
| `62100` (7010), `41459` (7014) | zonal office, Scale 0 | **no access** | refused |

These numbers are the real UAT testers (the same ones exist in the UAT HRMS table); names on the laptop are invented. `600001` (branch SOL) and `600003` (retired) are invented rows for the refusal tests.

Anything else is refused. Only the ids above (and the two invented ones) exist (`999999` is "not found"); the real bank rules for refusing branch staff and scale 0 are in section 5.

Sample photos for uploads: `.brain/07_REFERENCE/sample-uploads/UCR-PROJ-0001/`.

## 1b. Look inside H2
With the `h2` profile open the web console at http://localhost:8082 (JDBC URL `jdbc:h2:tcp://localhost:9092/mem:ucorealty;MODE=Oracle;DB_CLOSE_DELAY=-1`, user `sa`, empty password). Every start gives a fresh database: all tables and the test employees, no projects. `python3 scripts/load_demo_data.py` fills it with demo projects. The Oracle-only unique index `ux_staging_one_open` is not in H2; the application still checks that rule.

## 2. Automatic check (about 5 seconds)
```bash
python3 scripts/smoke_test.py
PUBLIC_URL=... INTERNAL_URL=... python3 scripts/smoke_test.py    # other ports/hosts
```
Checks the customer-site lockdown, headers, bad SSO tokens, CSRF, roles (DIT/maker/checker/retail) and logout (35 checks). It creates nothing.

## 3. Manual run (about 20 minutes)
**A. Maker in Durgapur (700001)**
1. Open http://localhost:8081/UcoRealtyStaff/login, enter `52260`, press *Generate OTP*, enter `123456`. The header shows `ROLE_ZO_MAKER`; there is no Logs and no Customer Leads tab.
2. **+ Add New Project**. The zone is fixed to **Durgapur** (it comes from the login, never typed). Pick State, then District (or **+ Other / Custom City**). Fill name, builder, branch name, a 6-digit pincode; available units must not exceed total.
3. Add photos (4 slots, **+ Add More Photos** up to 10, 1 MB each, JPG/PNG/WEBP). **Save Draft**, reopen it, edit it. Section 7: add **PDF attachments** (brochure, floor plan, anything): choose several files at once or press **+ Add another file**; each at most 3 MB, no limit on how many.
4. **Save & Request Approval**. Leave the branch name empty first: it must be refused with the message on the same form.

**B. Checker in Durgapur (700002) and zone isolation**
5. Sign in as `45195`. The pending Durgapur project is in the queue. Open it, **Request Correction from Maker** with a remark (required). Sign in as `52260`, fix it, submit again.
6. As `45195` **approve** it. It now appears on the customer site (http://localhost:8080/UcoRealty/projects).
7. Sign in as the Raipur checker `45321`: the Durgapur project is not theirs to approve.
8. The maker `52260` cannot approve their own project (no button; the server also refuses).

**C. Customer (no login)**
9. Open :8080/projects (no pop-up). Search by pincode. Open the project: photos, contacts. **Apply for Home Loan** (sidebar and bottom of the page) opens the bank's Home Loan page (`uco.homeloan.base-url` + `/Lead_App/lead_web.jsp?utm=REALTY`) in the same tab; nothing is saved by us.
10. Unapproved or unknown project ids return 404.

**D. Head Office roles**
11. HO retail `58382`: tabs *Project Management* and *Customer Leads*. Leads: filter, **Export** to Excel. There are **no** Edit / Units / Delete buttons on any project, and `POST /admin/projects/{id}/reduce-units` returns 403.
12. HO marketing `54534`: can create and approve projects for any zone.
13. A maker or checker typing `/admin/leads` in the address bar gets *Access Denied* (customer phone numbers are protected).
14. DIT `55012`: **Logs** shows project events and every login attempt (including refused ones). Everyone else gets 403 on `/admin/audit-logs`.
15. Sign out with the button: you land on the login page; Back must not reopen admin pages.

## 3b. Changing an APPROVED project (maker → checker) — about 15 minutes
**How it works (two tables):** `realty_projects` holds approved projects only; the maker's edits live in his own row in `realty_project_staging` and reach `realty_projects` only when a checker approves. So while he edits, customers and checkers keep seeing the approved record unchanged. Use an approved Durgapur project (or approve one from section 3).

**Edit and save as draft**
1. Sign in as maker `52260`. On the approved project press **Edit**. A blue banner says the project stays live while you work.
2. Change the amenities, the available units and the budget. Optionally add a photo (slot 1) and **Remove Photo** on an existing one.
3. Press **Save as Draft**. Back on the dashboard the project still shows **Approved** with a small tag **Your change: draft**.
4. Check what others see: open http://localhost:8080/UcoRealty/projects, open the project: **old** values and **old** photos. Sign in as checker `45195`: the project row has **no** pending tag and no review button (a draft is private).

**Submit for approval, then the checker decides**
5. As `52260` press **Continue change**, then **Submit for Approval** (try it with nothing changed first: refused). The project now shows **Change pending approval**; the customer site is still unchanged.
6. As `45195` press **Review change**. You see a table *Approved (live now) → Proposed*, plus new/removed photos. Try the wrong people first: the maker `52260` and the Raipur checker `45321` cannot approve (the row has no button for the maker; a direct request is refused).
7. **Return to maker** with a remark. As `52260` the row shows **Your change: returned** with the remark. Fix, **Submit for Approval** again.
8. As `45195` **Approve and apply change**. Now the customer site shows the new values and the new photo; the removed photo is gone; no pending tag remains. Try **Reject** on another change: nothing changes on the approved project.

**Reduce units** (maker only)
9. As `52260` press **Reduce Units**, enter units and a reason, **Submit for Approval**. The unit count on the dashboard does not change yet. As `45195` review (old → new units, reason) and approve: the count drops. HO retail `58382` has no such button.

**Delete**
10. As `52260` press **Delete**, enter a reason. The project stays on the customer site and shows **Deletion pending approval**. As `45195` open **Review deletion** (reason shown; only *Approve* or *Reject* are possible). **Approve**: the project disappears from every dashboard (maker, checker, HO, DIT) and from the customer site; it is still in the database with status `DELETED` (see the **Logs** page for the whole history under `DELETE_REQUESTED` / `DELETE_APPROVED`).

**Automatic version of all of this**
```bash
python3 scripts/workflow_test.py     # 83 checks; creates two "ZZ Workflow" projects (one ends DELETED, one REJECTED)
scripts/remove_demo_data.sh          # physically removes those test projects and their staging rows from the dev database
```

## 3c. Look inside the database while you test (this is the best way to understand the model)
```bash
docker exec -i uco-oracle-free sqlplus -s uco_realty/uco_realty_pwd@//localhost/FREEPDB1 <<'SQL'
set linesize 200 pagesize 100
col project_name format a28
col status format a18
select id, project_name, status, row_version from projects order by created_at;
select id, project_id, request_type, status, created_by, base_version, row_version from realty_project_staging order by id;
select id, project_id, staging_id, file_name from realty_project_images order by id;
select id, project_id, staging_id, file_name from realty_project_attachments order by id;
SQL
```
Watch it between the steps: *Save as Draft* changes only `realty_project_staging`; *Submit for Approval* flips its status to `PENDING_APPROVAL`; *Approve* copies the values into `realty_projects` (`row_version` +1) and marks the staging row `APPROVED`; a deletion sets `realty_projects.status = 'DELETED'`.

## 3d. Regression checklist (run before every release)
| # | Area | How | Pass when |
| :-: | :--- | :--- | :--- |
| 1 | Build | `cd UcoRealty && mvn -q clean package -DskipTests`, then the same in `UcoRealtyStaff` (or `scripts/package-release.sh uat`) | both WARs built, no error |
| 2 | Start | `scripts/dev-start.sh` | both URLs answer; no ERROR in `.run/*.log` |
| 3 | Smoke | `python3 scripts/smoke_test.py` | 35/35 |
| 4 | Workflow | `python3 scripts/workflow_test.py` | 83/83 (new project, return, reject, withdraw, edit, unit reduction, delete, role/zone/self-approval refusals, concurrency, XSS) |
| 5 | Clean | `scripts/remove_demo_data.sh` | "demo data removed" |
| 6 | Manual 3A-3D | sections 3 and 3b with the personas | every expected result above |
| 7 | Roles | try `/admin/leads`, `/admin/audit-logs`, a reduce-units POST as the wrong roles | 403 / Access Denied |
| 8 | Public lockdown | `curl -i http://localhost:8080/UcoRealty/admin/projects` | 403; only catalog, project detail, files |
| 9 | UAT-like start | section 5 or 5b | login behaves as described |
| 9b | Context path | `python3 scripts/context_path_check.py` | "OK: every URL carries the context path" (no root-relative link, form, script or JSON URL) |
| 10 | Docs | `.brain/03_SYSTEM/Roles_And_Actions.md`, `Schema.md`, `API.md` match any change you made | yes |

## 4. Optional demo data
```bash
python3 scripts/load_demo_data.py     # needs the test login (h2 or uat profile); refuses to run elsewhere
scripts/remove_demo_data.sh           # removes everything whose project name ends in "(Demo)"
```

## 5. Test employees
The test employees are rows in `db_migrations/05_LOCAL_TEST_EMPLOYEES.sql`, in the tables from `03_CREATE_HRMS_MOCK_TABLES.sql`; the code reads them like the bank's HRMS tables. The same 12 work in `h2` and `uat`, with OTP `123456` (settings `uco.sso.dev-login-enabled`, `uco.sso.test-otp` in those two profile files). `600001` and `600002` are refused on purpose (branch SOL, scale 0). After changing 01, 03 or 05 run `python3 scripts/h2_schema.py`.

## 6. If something goes wrong
* Logs: `.run/public.log`, `.run/internal.log`.
* "Port already in use": `scripts/dev-stop.sh`.
* Login page shows "Single Sign-On required" and no form: the app is running with the `prod` profile.

## Testing on a real Oracle (Docker)
`scripts/dev-start.sh --oracle` starts Oracle Free in Docker (`gvenzl/oracle-free:23.6-slim-faststart`, native on Apple-silicon Macs; the bank's UAT is 19c, which has no arm64 image), creates our tables and the HRMS stand-in tables (user `HRMS_DBA`), and runs both apps on it. Then `python3 scripts/load_demo_data.py`, and the usual scripts. Needs about 5 GB of free disk; if Docker reports input/output errors the disk is full. Do NOT use the tag `23-slim-faststart` at the moment: its `libsqlplus.so` is empty and the container exits.
Result of the full run on 13 Nov 2026 (main ad9e29f): the 438-page snapshot taken on Oracle is identical to the one taken on H2 (every role, catalog, leads, audit, Excel, JSON), smoke 34/34, workflow 83/83, a dashboard with 1,108 live projects (more than Oracle's 1,000-item IN limit) opens, a 3 MB PDF and a 0.8 MB photo come back byte for byte, retired and role-less employees are refused. Not covered: the bank's real HRMS tables (column names, the EMAIL column), 19c itself, several servers.
