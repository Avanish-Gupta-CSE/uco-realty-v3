# Testing UCO Realty by hand

## 1. Start
```bash
scripts/dev-start.sh          # first run asks you to choose passwords for dit.superadmin and ho.admin
```
Needs Docker Desktop running. It starts Oracle, creates the tables and the two admins, then the backend and the web app.
Stop with `scripts/dev-stop.sh`. Start again from an empty database with `scripts/dev-start.sh --reset`.

| What | Address |
|---|---|
| Customer site | http://localhost:8080/projects |
| Staff login | http://localhost:8080/login |
| Admin dashboard | http://localhost:8080/admin/projects |

The database starts with **only** `dit.superadmin` and `ho.admin`. Everything else you create in the app, as in real life.
Sample photos for upload tests: `.brain/sample-uploads/UCR-PROJ-0001/`.

## 2. Suggested order (about 30 minutes)
**A. Superadmin (dit.superadmin)**
1. Sign in. Open **Change password** (top right) and set a new one.
2. **User Management > Add New User**: create `ho.marketing` (role Head Office Marketing) and `ho.retail` (Head Office Retail). Use passwords of 8+ characters with a letter and a digit.

**B. HO Marketing (ho.marketing)**
3. Create zone users: a ZO Maker and a ZO Checker for **Kolkata**, a ZO Maker and a ZO Checker for **Mumbai**, and one Call Centre user. Zone is required for ZO users.
4. Try **reset password** and **deactivate / activate** on one of them. A deactivated user cannot sign in.

**C. ZO Maker (Kolkata)**
5. Sign in. **Create Project**: choose State, then District (try **Custom** for a place not in the list), type a 6-digit pincode, fill name, builder, RERA, inventory (available must not exceed total), price, contacts. Zone is locked to Kolkata.
6. Upload photos (they are shrunk automatically; the box shows e.g. `7.0 MB → 275 KB`). Press **Save Draft**, then reopen and edit it.
7. **Save & Request Approval**. Try submitting a project with the pincode empty: it must be refused.

**D. ZO Checker (Kolkata) and the Mumbai users**
8. Checker: open the pending project (eye icon), look at the photos, **Reject / Rectification** with a remark (a remark is required), then sign in as the maker, fix it, resubmit.
9. Checker approves. Sign in as the **Mumbai** checker: the Kolkata project is not in their queue (use **All-India Directory** to see it read-only, with a lock instead of Approve).
10. The maker cannot approve their own project (no Approve button, server also refuses).

**E. Customer (no login)**
11. Open the customer site, fill the name and mobile gate (or skip). Filters show only states and cities that have an approved project. Search by pincode.
12. Open the project: photos, contacts, details. Anything not entered shows "Not provided". Press **Apply for Home Loan** (a new tab opens the bank's home-loan page).

**F. HO Retail (ho.retail) and Call Centre**
13. HO Retail: **Customer Leads** (filter by date/status, **Export** to Excel). On an approved project use the **−** button to reduce available units with a reason. Reload the customer page: the new count shows. They cannot approve or create projects.
14. Call Centre: **Customer Leads** and Excel export only.

**G. Things that must be refused**
A maker cannot edit another zone's project; nobody can edit or add photos after submit/approval; PDFs and photos over 3 MB are refused; five wrong passwords lock an account for 15 minutes; HO Admin cannot create projects; HO Marketing cannot create admins.

## 3. Optional: sample projects to see how it looks
With the app running:
```bash
python3 scripts/load_demo_data.py          # asks for the dit.superadmin password
scripts/remove_demo_data.sh                # removes it again
```
It adds 9 fictional projects (names end with "(Demo)"; 6 approved with photos and a brochure, 1 pending, 1 returned for correction, 1 draft) and 9 demo leads, using the normal approval flow. The projects are placed in the zones of the zone users you already created, so each maker and checker has something to work on (a pending project to approve, a returned one to fix, a draft to submit). If you add users for more zones later, run `scripts/remove_demo_data.sh` and load again. Your own users and projects are not touched. Demo data is for your local database only.

## 4. Automatic check (optional)
On a **fresh** database (only the two admins), with the two admin passwords you chose:
```bash
DIT_PASSWORD='...' HO_PASSWORD='...' python3 scripts/e2e_flow_test.py
```
It runs 128 checks through all roles. It creates test users and projects and changes the DIT password, so use it on a throwaway database, then `scripts/dev-start.sh --reset`.

## 5. If something goes wrong
* `.run/backend.log` and `.run/frontend.log` hold the logs.
* "Port already in use": run `scripts/dev-stop.sh`.
* Locked out after wrong passwords: wait 15 minutes or restart the backend.
* Forgot an admin password: `scripts/dev-start.sh --reset` (wipes everything).
