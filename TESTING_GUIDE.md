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

Staff sign in with an **employee id** and the OTP `123456` (dev profile only). There are no passwords and no user-management screens. Test employees:

| Id | Role | Posting |
|---|---|---|
| `100001` | DIT Superadmin (sees **Logs**) | SOL 7114 |
| `200001` / `200002` | ZO Maker / ZO Checker | Ajmer (SOL 7015) |
| `300001` / `300002` | ZO Maker / ZO Checker | Bhopal (SOL 7022) |
| `500001` / `500002` | ZO Maker / ZO Checker | Jaipur (SOL 7030) |
| `400001` | HO Retail (reduces units, leads) | SOL 7105 |
| `400002` | HO Marketing (any zone) | SOL 7102 |
| `400003` / `61801` | HO Admin (view only) | SOL 7101 / 7115 |

Sample photos for uploads: `.brain/sample-uploads/UCR-PROJ-0001/`.

## 2. Automatic checks (about 5 seconds)
```bash
python3 scripts/smoke_test.py                       # 26 checks: lockdown, headers, SSO errors, CSRF, roles, logout
PUBLIC_URL=... INTERNAL_URL=... python3 scripts/smoke_test.py    # other ports/hosts
```
It creates nothing. Against a UAT/prod build it skips the role checks (no dev login there).

## 3. Suggested manual run (about 20 minutes)
**A. ZO Maker (200001, Ajmer)**
1. Sign in at :8081/login. **+ Add New Project**: pick State, then District (or **+ Other / Custom City** and type it); the zone is fixed to Ajmer. Pincode is 6 digits; available units must not exceed total. Branch name is required to submit.
2. Add photos (4 slots, **+ Add More Photos** up to 10, max 2 MB each, JPG/PNG/WEBP). **Save Draft**, reopen, edit.
3. **Save & Request Approval**. A project with a missing required field must be refused with the message shown on the same form.

**B. ZO Checker (200002) and the other zones**
4. Open the pending project, review, **Request Correction from Maker** with a remark (required). Sign in as the maker, fix, resubmit.
5. Checker **approves**. Sign in as the Bhopal checker `300002`: the Ajmer project is not theirs to approve.
6. The maker cannot approve their own project (no button; the server also refuses).

**C. Customer (no login)**
7. Open :8080/projects. Fill the name/mobile gate (10 digits) or skip. Filters list only states/cities with approved projects. Search by pincode.
8. Open the project (photos, contacts; missing data shows "Not provided"). **Apply for Home Loan** saves the lead and opens https://www.uco.bank.in/home-loan in the same tab.
9. Unapproved or unknown project ids return 404 on the customer site.

**D. Head Office**
10. HO Retail `400001`: **Customer Leads** (filter, **Export** to Excel); reduce available units on an approved project with a reason. No approve/create rights.
11. DIT `100001`: **Logs** shows project events and every login attempt (including refused ones). Makers and checkers get 403 on `/admin/audit-logs`.
12. Sign out (button): you land on the login page; the Back button must not reopen admin pages.

**E. Things that must be refused**
A maker cannot edit another zone's project; nothing is editable after submit/approval; photos over 2 MB and non-images are skipped; `/admin/**` without sign-in redirects to `/login`; the customer portal answers 403 on `/login`, `/admin`, `/sso`, `/entry`; an employee id that is not in the list is refused.

## 4. Optional: demo data
```bash
python3 scripts/load_demo_data.py     # dev profile only; refuses to run elsewhere
scripts/remove_demo_data.sh           # removes everything whose project name ends in "(Demo)" and the demo leads
```
Adds 9 fictional projects through the normal forms (6 approved with photos, 1 pending, 1 returned for correction, 1 draft; Ajmer and Bhopal) and 9 demo leads.

## 5. If something goes wrong
* Logs: `.run/public.log`, `.run/internal.log`.
* "Port already in use": `scripts/dev-stop.sh`.
* Login page shows "Single Sign-On required" and no form: the app is not running with the dev profile.
* To try the real HRMS path locally: load `db_migrations/03_CREATE_HRMS_MOCK_TABLES.sql` and `05_UAT_TEST_EMPLOYEES.sql`, then start the internal WAR with `--uco.hrms.mode=bank --uco.sso.dev-login-enabled=false` and open `/entry?value=<token>` (tokens are produced with `SsoCryptoUtil.encryptAesCbc`; the dev key/iv are in `application-dev.yml`).
