# Testing UCO Realty by hand (local, local profile)

## 1. Start
```bash
scripts/dev-start.sh            # needs Docker Desktop; starts Oracle, creates tables, starts both portals
scripts/dev-stop.sh
scripts/dev-start.sh --reset    # wipe the database and start again
```
| What | Address |
|---|---|
| Customer portal | http://localhost:8080/ucorealty-customer/projects |
| Staff portal (sign-in) | http://localhost:8081/ucorealty-staff/login |

Staff sign in with an **employee id** and the OTP `123456` (local profile only; UAT/production use the bank SSO). There are no passwords and no user-management screens. The roles come from the employee's posting (SOL), exactly as in the bank:

| Employee id | Posting (SOL) | Role | Can do |
|---|---|---|---|
| `100001`, `61801` | 7115 DIT | **DIT superadmin** | everything in the app + the **Logs** page, leads |
| `400002` | 8028 HO Marketing and Wealth Management | **HO marketing** | create/approve projects in any zone, leads |
| `400001` | 8001 HO Retail Banking | **HO retail** | customer leads + Excel export, read-only view of projects (no reduce units) |
| `700001` / `700002` | 7001 Patna Zonal Office | **Maker (Scale 2)** / **Checker (Scale 4)** | Patna zone only |
| `200001` / `200002` | 7015 Ajmer Zonal Office | Maker / Checker | Ajmer zone only |
| `300001` / `300002` | 7022 Bhopal Zonal Office | Maker (Scale 1) / Checker (Scale 5) | Bhopal zone only |

Anything else is refused. In the local profile only the ids above exist (`999999` is "not found"); the real bank rules for refusing branch staff and scale 0 are in section 5.

Sample photos for uploads: `.brain/sample-uploads/UCR-PROJ-0001/`.

## 1b. No Docker: in-memory H2 (optional)
```bash
scripts/dev-start.sh --h2            # H2 database server + both apps; no Docker needed
scripts/dev-start.sh --h2 --reset    # fresh empty database
scripts/dev-stop.sh                  # also stops H2 (the in-memory data is gone); dev-stop.sh --apps-only keeps it
```
* The tables come from the **same** DBA script `db_migrations/01_CREATE_TABLES.sql`, converted on the fly by `scripts/h2_schema.py` (H2 in Oracle mode). Every start of a fresh H2 runs the DROP + CREATE statements, so it is always clean.
* **H2 console:** http://localhost:8082, JDBC URL `jdbc:h2:tcp://localhost:9092/mem:ucorealty`, user `sa`, empty password. Both apps share this one database (that is why H2 runs as its own small server: an in-memory database lives inside one process, and the customer and staff apps are two).
* The H2 driver is added only by the `h2` Maven build option (`-Ph2`) (`mvn -Ph2 clean package` inside each app folder, done by the script); the normal build, and so every UAT/production WAR, contains no H2.
* Same results as Oracle: `smoke_test.py` 35/35, `workflow_test.py` 83/83, `context_path_check.py` OK. What H2 cannot check: the Oracle-only unique index `ux_staging_one_open` (the app still enforces it), the SecureFiles photo storage, the real HRMS tables, and schema validation (H2 and Oracle name the number types differently). Use Oracle in Docker (default start) before you hand over, and the UAT database for the final run (`SPRING_PROFILES_ACTIVE=uat`, `DB_URL`, `DB_USER`, `DB_PASS`, the DBA runs `db_migrations/01_CREATE_TABLES.sql`).
* `scripts/remove_demo_data.sh` works on both.

## 2. Automatic check (about 5 seconds)
```bash
python3 scripts/smoke_test.py
PUBLIC_URL=... INTERNAL_URL=... python3 scripts/smoke_test.py    # other ports/hosts
```
Checks the customer-site lockdown, headers, bad SSO tokens, CSRF, roles (DIT/maker/checker/retail) and logout (35 checks). It creates nothing.

## 3. Manual run (about 20 minutes)
**A. Maker in Patna (700001)**
1. Open http://localhost:8081/ucorealty-staff/login, enter `700001`, press *Generate OTP*, enter `123456`. The header shows `ROLE_ZO_MAKER`; there is no Logs and no Customer Leads tab.
2. **+ Add New Project**. The zone is fixed to **Patna** (it comes from the login, never typed). Pick State, then District (or **+ Other / Custom City**). Fill name, builder, branch name, a 6-digit pincode; available units must not exceed total.
3. Add photos (4 slots, **+ Add More Photos** up to 10, 2 MB each, JPG/PNG/WEBP). **Save Draft**, reopen it, edit it.
4. **Save & Request Approval**. Leave the branch name empty first: it must be refused with the message on the same form.

**B. Checker in Patna (700002) and zone isolation**
5. Sign in as `700002`. The pending Patna project is in the queue. Open it, **Request Correction from Maker** with a remark (required). Sign in as `700001`, fix it, submit again.
6. As `700002` **approve** it. It now appears on the customer site (http://localhost:8080/ucorealty-customer/projects).
7. Sign in as the Ajmer checker `200002`: the Patna project is not theirs to approve.
8. The maker `700001` cannot approve their own project (no button; the server also refuses).

**C. Customer (no login)**
9. Open :8080/projects, fill the gate (10-digit mobile) or skip. Search by pincode. Open the project: photos, contacts. **Apply for Home Loan** saves the lead and opens https://www.uco.bank.in/home-loan in the same tab.
10. Unapproved or unknown project ids return 404.

**D. Head Office roles**
11. HO retail `400001`: tabs *Project Management* and *Customer Leads*. Leads: filter, **Export** to Excel. There are **no** Edit / Units / Delete buttons on any project, and `POST /admin/projects/{id}/reduce-units` returns 403.
12. HO marketing `400002`: can create and approve projects for any zone.
13. A maker or checker typing `/admin/leads` in the address bar gets *Access Denied* (customer phone numbers are protected).
14. DIT `100001`: **Logs** shows project events and every login attempt (including refused ones). Everyone else gets 403 on `/admin/audit-logs`.
15. Sign out with the button: you land on the login page; Back must not reopen admin pages.

## 3b. Changing an APPROVED project (maker → checker) — about 15 minutes
**How it works (two tables):** `projects` holds approved projects only; the maker's edits live in his own row in `project_staging` and reach `projects` only when a checker approves. So while he edits, customers and checkers keep seeing the approved record unchanged. Use an approved Patna project (or approve one from section 3).

**Edit and save as draft**
1. Sign in as maker `700001`. On the approved project press **Edit**. A blue banner says the project stays live while you work.
2. Change the amenities, the available units and the budget. Optionally add a photo (slot 1) and **Remove Photo** on an existing one.
3. Press **Save as Draft**. Back on the dashboard the project still shows **Approved** with a small tag **Your change: draft**.
4. Check what others see: open http://localhost:8080/ucorealty-customer/projects, open the project: **old** values and **old** photos. Sign in as checker `700002`: the project row has **no** pending tag and no review button (a draft is private).

**Send for approval, then the checker decides**
5. As `700001` press **Continue change**, then **Send for Approval** (try it with nothing changed first: refused). The project now shows **Change pending approval**; the customer site is still unchanged.
6. As `700002` press **Review change**. You see a table *Approved (live now) → Proposed*, plus new/removed photos. Try the wrong people first: the maker `700001` and the Ajmer checker `200002` cannot approve (the row has no button for the maker; a direct request is refused).
7. **Return to maker** with a remark. As `700001` the row shows **Your change: returned** with the remark. Fix, **Send for Approval** again.
8. As `700002` **Approve and apply change**. Now the customer site shows the new values and the new photo; the removed photo is gone; no pending tag remains. Try **Reject** on another change: nothing changes on the approved project.

**Reduce units** (maker only)
9. As `700001` press **Reduce Units**, enter units and a reason, **Send for Approval**. The unit count on the dashboard does not change yet. As `700002` review (old → new units, reason) and approve: the count drops. HO retail `400001` has no such button.

**Delete**
10. As `700001` press **Delete**, enter a reason. The project stays on the customer site and shows **Deletion pending approval**. As `700002` open **Review deletion** (reason shown; only *Approve* or *Reject* are possible). **Approve**: the project disappears from every dashboard (maker, checker, HO, DIT) and from the customer site; it is still in the database with status `DELETED` (see the **Logs** page for the whole history under `DELETE_REQUESTED` / `DELETE_APPROVED`).

**Automatic version of all of this**
```bash
python3 scripts/workflow_test.py     # 83 checks; creates two "ZZ Workflow" projects (one ends DELETED, one REJECTED)
scripts/remove_demo_data.sh          # physically removes those test projects and their staging rows from the dev database
```

## 3c. Look inside the database while you test (this is the best way to understand the model)
```bash
docker exec -i uco-oracle sqlplus -s uco_realty/uco_realty_pwd@//localhost/XEPDB1 <<'SQL'
set linesize 200 pagesize 100
col project_name format a28
col status format a18
select id, project_name, status, row_version from projects order by created_at;
select id, project_id, request_type, status, created_by, base_version, row_version from project_staging order by id;
select id, project_id, staging_id, file_name from project_images order by id;
SQL
```
Watch it between the steps: *Save as Draft* changes only `project_staging`; *Send for Approval* flips its status to `PENDING_APPROVAL`; *Approve* copies the values into `projects` (`row_version` +1) and marks the staging row `APPROVED`; a deletion sets `projects.status = 'DELETED'`.

## 3d. Regression checklist (run before every release)
| # | Area | How | Pass when |
| :-: | :--- | :--- | :--- |
| 1 | Build | `cd ucorealty-customer && mvn -q clean package -DskipTests`, then the same in `ucorealty-staff` (or `scripts/package-release.sh uat`) | both WARs built, no error |
| 2 | Start | `scripts/dev-start.sh` | both URLs answer; no ERROR in `.run/*.log` |
| 3 | Smoke | `python3 scripts/smoke_test.py` | 35/35 |
| 4 | Workflow | `python3 scripts/workflow_test.py` | 83/83 (new project, return, reject, withdraw, edit, unit reduction, delete, role/zone/self-approval refusals, concurrency, XSS) |
| 5 | Clean | `scripts/remove_demo_data.sh` | "demo data removed" |
| 6 | Manual 3A-3D | sections 3 and 3b with the personas | every expected result above |
| 7 | Roles | try `/admin/leads`, `/admin/audit-logs`, a reduce-units POST as the wrong roles | 403 / Access Denied |
| 8 | Public lockdown | `curl -i http://localhost:8080/ucorealty-customer/admin/projects` | 403; only catalog, project detail, lead, files |
| 9 | UAT-like start | section 5 or 5b | login behaves as described |
| 9b | Context path | `python3 scripts/context_path_check.py` | "OK: every URL carries the context path" (no root-relative link, form, script or JSON URL) |
| 10 | Docs | `.brain/Roles_And_Actions.md`, `Schema.md`, `API.md` match any change you made | yes |

## 4. Optional demo data
```bash
python3 scripts/load_demo_data.py     # local profile only; refuses to run elsewhere
scripts/remove_demo_data.sh           # removes everything whose project name ends in "(Demo)" and the demo leads
```

## 5. Employees and sign-in on this Mac
There are no test employees in the Java code. The application always reads employees from the HRMS tables; on your Mac those tables are stand-ins (`db_migrations/03_CREATE_HRMS_MOCK_TABLES.sql`) filled with 12 test employees (`05_LOCAL_TEST_EMPLOYEES.sql`). `scripts/dev-start.sh` loads them into Oracle, and `scripts/h2_schema.py` adds them to H2, so the sign-in code is the same as in UAT. The `local` profile sets the test OTP (`uco.sso.test-otp=123456`), so any employee in those tables signs in with their number and `123456`.

To try the bank-style token entry instead of the form (laptop only), run the staff app with the form switched off and open `/entry?value=<token>`, where the token is `emp_id=<EMPNO>&sol_id=<SOL>` encrypted with the dev key/iv (`SsoCryptoUtil.encryptAesCbc`; values in `application-local.properties`):
```bash
DB_URL=jdbc:oracle:thin:@localhost:1521/XEPDB1 DB_USER=uco_realty DB_PASS=uco_realty_pwd \
java -jar ucorealty-staff/target/ucorealty-staff.war --server.port=8181 \
     --spring.profiles.active=local --uco.sso.dev-login-enabled=false
```
`600001` (branch SOL) and `600002` (scale 0) must be refused with "not authorised".

## 5b. UAT before the SSO is wired (closed test only)
Until the bank PC and key arrive, the UAT server can offer a **basic employee-id + test-OTP page** against the real HRMS data. It is off by default and needs both settings (JVM `-D...` or environment variables):
```
UCO_SSO_DEV_LOGIN_ENABLED=true      # shows the employee-id form on /login
UCO_SSO_TEST_OTP=<a secret you choose, 6 digits>      # the one OTP that works for any real HRMS employee
```
The form lists no test employees and shows no OTP hint. Every sign-in with the test OTP is written as a warning in the log and in the login audit. The **prod profile refuses to start** if `UCO_SSO_TEST_OTP` is set. Remove both settings the day the SSO goes live; nothing else changes.

## 6. If something goes wrong
* Logs: `.run/public.log`, `.run/internal.log`.
* "Port already in use": `scripts/dev-stop.sh`.
* Login page shows "Single Sign-On required" and no form: the app is not running with the local profile.
