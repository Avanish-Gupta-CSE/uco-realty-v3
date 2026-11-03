# Testing UCO Realty by hand (local, dev profile)

## 1. Start
```bash
scripts/dev-start.sh            # needs Docker Desktop; starts Oracle, creates tables, starts both portals
scripts/dev-stop.sh
scripts/dev-start.sh --reset    # wipe the database and start again
```
| What | Address |
|---|---|
| Customer portal | http://localhost:8080/projects |
| Staff portal (sign-in) | http://localhost:8081/login |

Staff sign in with an **employee id** and the OTP `123456` (dev profile only; UAT/production use the bank SSO). There are no passwords and no user-management screens. The roles come from the employee's posting (SOL), exactly as in the bank:

| Employee id | Posting (SOL) | Role | Can do |
|---|---|---|---|
| `100001`, `61801` | 7115 DIT | **DIT superadmin** | everything in the app + the **Logs** page, leads |
| `400003` | 8028 DIT Projects and Product Development | **HO admin** | view all projects (read only), leads |
| `400002` | 8001 Marketing and Wealth Management | **HO marketing** | create/approve projects in any zone, leads |
| `400001` | 8010 Retail Banking Department (*placeholder SOL*) | **HO retail** | customer leads + Excel export, reduce units of approved projects |
| `700001` / `700002` | 7001 Patna Zonal Office | **Maker (Scale 2)** / **Checker (Scale 4)** | Patna zone only |
| `200001` / `200002` | 7015 Ajmer Zonal Office | Maker / Checker | Ajmer zone only |
| `300001` / `300002` | 7022 Bhopal Zonal Office | Maker (Scale 1) / Checker (Scale 5) | Bhopal zone only |

Anything else is refused. In the dev profile only the ids above exist (`999999` is "not found"); the real bank rules for refusing branch staff and scale 0 are in section 5.

Sample photos for uploads: `.brain/sample-uploads/UCR-PROJ-0001/`.

## 2. Automatic check (about 5 seconds)
```bash
python3 scripts/smoke_test.py
PUBLIC_URL=... INTERNAL_URL=... python3 scripts/smoke_test.py    # other ports/hosts
```
Checks the customer-site lockdown, headers, bad SSO tokens, CSRF, roles (DIT/maker/checker/retail) and logout. It creates nothing.

## 3. Manual run (about 20 minutes)
**A. Maker in Patna (700001)**
1. Open http://localhost:8081/login, enter `700001`, press *Generate OTP*, enter `123456`. The header shows `ROLE_ZO_MAKER`; there is no Logs and no Customer Leads tab.
2. **+ Add New Project**. The zone is fixed to **Patna** (it comes from the login, never typed). Pick State, then District (or **+ Other / Custom City**). Fill name, builder, branch name, a 6-digit pincode; available units must not exceed total.
3. Add photos (4 slots, **+ Add More Photos** up to 10, 2 MB each, JPG/PNG/WEBP). **Save Draft**, reopen it, edit it.
4. **Save & Request Approval**. Leave the branch name empty first: it must be refused with the message on the same form.

**B. Checker in Patna (700002) and zone isolation**
5. Sign in as `700002`. The pending Patna project is in the queue. Open it, **Request Correction from Maker** with a remark (required). Sign in as `700001`, fix it, submit again.
6. As `700002` **approve** it. It now appears on the customer site (http://localhost:8080/projects).
7. Sign in as the Ajmer checker `200002`: the Patna project is not theirs to approve.
8. The maker `700001` cannot approve their own project (no button; the server also refuses).

**C. Customer (no login)**
9. Open :8080/projects, fill the gate (10-digit mobile) or skip. Search by pincode. Open the project: photos, contacts. **Apply for Home Loan** saves the lead and opens https://www.uco.bank.in/home-loan in the same tab.
10. Unapproved or unknown project ids return 404.

**D. Head Office roles**
11. HO retail `400001`: tabs *Project Management* and *Customer Leads*. Leads: filter, **Export** to Excel. Reduce the available units of an approved project with a reason. No approve/create rights.
12. HO marketing `400002`: can create and approve projects for any zone. HO admin `400003`: sees all projects but has no create/approve buttons; can open leads.
13. A maker or checker typing `/admin/leads` in the address bar gets *Access Denied* (customer phone numbers are protected).
14. DIT `100001`: **Logs** shows project events and every login attempt (including refused ones). Everyone else gets 403 on `/admin/audit-logs`.
15. Sign out with the button: you land on the login page; Back must not reopen admin pages.

## 4. Optional demo data
```bash
python3 scripts/load_demo_data.py     # dev profile only; refuses to run elsewhere
scripts/remove_demo_data.sh           # removes everything whose project name ends in "(Demo)" and the demo leads
```

## 5. Try the real HRMS code path on this Mac (optional)
Dev mode uses a built-in list. To exercise the same code that runs in UAT (reading `employee_contact_details`), on your laptop only:
```bash
docker cp db_migrations uco-oracle:/tmp/
# in sqlplus as uco_realty: @/tmp/db_migrations/03_CREATE_HRMS_MOCK_TABLES.sql  then  @/tmp/db_migrations/05_LOCAL_TEST_EMPLOYEES.sql
java -jar uco-realty-internal/target/uco-realty-internal-0.1.0.war --server.port=8181 \
     --spring.profiles.active=dev --uco.hrms.mode=bank --uco.sso.dev-login-enabled=false
```
Then open `/entry?value=<token>` where the token is `emp_id=<EMPNO>&sol_id=<SOL>` encrypted with the dev key/iv (`SsoCryptoUtil.encryptAesCbc`; values in `application-dev.yml`). `600001` (branch SOL) and `600002` (scale 0) must be refused with "not authorised".

## 6. If something goes wrong
* Logs: `.run/public.log`, `.run/internal.log`.
* "Port already in use": `scripts/dev-stop.sh`.
* Login page shows "Single Sign-On required" and no form: the app is not running with the dev profile.
