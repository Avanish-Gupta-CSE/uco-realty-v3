#!/usr/bin/env python3
"""End-to-end flow test for a running UCO Realty backend (customer, DIT, HO admin, HO marketing,
HO retail, ZO maker/checker for two zones, call centre, security rules).

Run it on a FRESH database that holds only the two admins from 02_INITIAL_ADMIN_USERS.sql:
    DIT_PASSWORD=... HO_PASSWORD=... BASE_URL=http://localhost:8080 python3 scripts/e2e_flow_test.py

It creates test users and test projects, changes the DIT password and deliberately locks one account for 15 minutes
(restart the backend before running it again), so use dev or UAT only.
Uses only the Python standard library."""
import json, os, secrets, sys, uuid, urllib.request, urllib.error

BASE = os.environ.get("BASE_URL", "http://localhost:8080")
CREDS = {"dit": os.environ["DIT_PASSWORD"], "ho": os.environ["HO_PASSWORD"]}

results = []          # (section, name, ok, detail)
section = ["?"]


def sec(name):
    section[0] = name
    print(f"\n== {name}")


def check(name, ok, detail=""):
    results.append((section[0], name, bool(ok), detail))
    print(f"  [{'PASS' if ok else 'FAIL'}] {name}" + (f"  -> {detail}" if (detail and not ok) else ""))
    return ok


def call(method, path, token=None, body=None, raw=None, ctype=None):
    headers = {}
    data = None
    if token:
        headers["Authorization"] = f"Bearer {token}"
    if body is not None:
        data = json.dumps(body).encode()
        headers["Content-Type"] = "application/json"
    if raw is not None:
        data = raw
        headers["Content-Type"] = ctype
    req = urllib.request.Request(BASE + path, data=data, headers=headers, method=method)
    try:
        with urllib.request.urlopen(req, timeout=60) as r:
            status, payload, h = r.status, r.read(), dict(r.headers)
    except urllib.error.HTTPError as e:
        status, payload, h = e.code, e.read(), dict(e.headers)
    try:
        parsed = json.loads(payload) if payload else None
    except Exception:
        parsed = payload
    return status, parsed, h


def multipart(files):
    boundary = uuid.uuid4().hex
    out = b""
    for fname, ctype, content in files:
        out += (f"--{boundary}\r\nContent-Disposition: form-data; name=\"files\"; filename=\"{fname}\"\r\n"
                f"Content-Type: {ctype}\r\n\r\n").encode() + content + b"\r\n"
    out += (f"--{boundary}\r\nContent-Disposition: form-data; name=\"kind\"\r\n\r\nphoto\r\n--{boundary}--\r\n").encode()
    return out, f"multipart/form-data; boundary={boundary}"


def upload(pid, token, files):
    raw, ct = multipart(files)
    return call("POST", f"/api/admin/projects/{pid}/files", token, raw=raw, ctype=ct)


PNG = b"\x89PNG\r\n\x1a\n" + b"\x00" * 60
JPG = b"\xff\xd8\xff\xe0" + b"\x00" * 60
PDF = b"%PDF-1.4\n%test document\n" + b"0" * 40
HTML_AS_PNG = b"<html><script>alert(1)</script></html>" + b" " * 20


def login(u, p):
    s, b, _ = call("POST", "/api/auth/login", body={"username": u, "password": p})
    return s, (b or {}).get("token") if isinstance(b, dict) else None


def pw():
    return "Tst" + secrets.token_urlsafe(8).replace("-", "x").replace("_", "y") + "4"


PW = {}


def project_body(name, state="West Bengal", city="Kolkata", pin="700091", **extra):
    b = {
        "projectName": name, "builderName": "Test Builder Pvt Ltd", "stateName": state, "cityName": city,
        "pinCode": pin, "reraNo": "WBRERA/P/TEST/2026/0001", "typeOfUnit": "Residential Apartments",
        "totalInventory": 100, "unsoldInventory": 40, "budgetMin": "35 L", "budgetMax": "70 L",
        "projectAddress": "Test Road, Test Area", "salesExecName": "Test Sales", "salesMobile": "9830012345",
        "branchHeadName": "Test Branch Head", "branchHeadNumber": "033-22334455", "branchName": "Test Branch",
        "approvalDate": "2026-09-01",
    }
    b.update(extra)
    return b


# ---------------------------------------------------------------------------------------------
sec("1. Customer (empty system)")
s, b, _ = call("GET", "/api/public/projects")
check("public list is empty", s == 200 and b["total"] == 0, (s, b))
s, b, _ = call("GET", "/api/public/meta")
check("customer filters empty (no states with projects)", s == 200 and b["states"] == [], (s, b))
s, b, _ = call("POST", "/api/public/leads", body={"name": "Test Visitor", "phone": "9876500001", "status": "VISITOR"})
check("visitor lead saved", s == 200 and "leadId" in b, (s, b))
s, b, _ = call("POST", "/api/public/leads", body={"name": "Bad Phone", "phone": "12345", "status": "VISITOR"})
check("bad phone rejected (400)", s == 400, (s, b))
s, b, _ = call("POST", "/api/public/leads", body={"name": "X", "phone": "9876500002", "status": "HACKED"})
check("invalid lead status rejected (400)", s == 400, (s, b))
s, b, _ = call("POST", "/api/public/leads", body={"name": "X", "phone": "9876500002", "status": "SUCCESSFUL_APPLIED"})
check("apply without project rejected (400)", s == 400, (s, b))
s, b, _ = call("POST", "/api/public/leads", body={"name": "X", "phone": "9876500002", "status": "SUCCESSFUL_APPLIED", "projectId": "NOPE"})
check("apply to unknown project rejected (404)", s == 404, (s, b))
s, b, _ = call("GET", "/api/admin/projects")
check("staff API without login refused (401/403)", s in (401, 403), (s, b))
s, b, _ = call("GET", "/api/public/files/1")
check("unknown file is 404", s == 404, (s, b))
check("error body has status/error/message", isinstance(b, dict) and {"status", "error", "message"} <= set(b), b)

# ---------------------------------------------------------------------------------------------
sec("2. Superadmin (DIT)")
s, dit = login("dit.superadmin", CREDS["dit"])
check("DIT login", s == 200 and dit, s)
s, _ = login("dit.superadmin", "WrongPass123")
check("wrong password refused (401)", s == 401, s)
s, b, _ = call("GET", "/api/auth/me", dit)
check("/me returns role", s == 200 and b["role"] == "SUPERADMIN_DIT", (s, b))
s, b, _ = call("GET", "/api/admin/users", dit)
check("user list has 2 admins, no password hash leaked", s == 200 and len(b) == 2 and "passwordHash" not in json.dumps(b), (s, b))
s, b, _ = call("GET", "/api/admin/geo", dit)
check("geo lists 49 zones / 36 states / districts", s == 200 and len(b["zones"]) == 49 and len(b["states"]) == 36 and "Pune" in b["stateDistricts"]["Maharashtra"], s)
PW["ho.marketing"], PW["ho.retail"] = pw(), pw()
s, b, _ = call("POST", "/api/admin/users", dit, {"username": "ho.marketing", "password": PW["ho.marketing"], "role": "HO_MARKETING"})
check("DIT creates HO_MARKETING", s == 200, (s, b))
s, b, _ = call("POST", "/api/admin/users", dit, {"username": "ho.retail", "password": PW["ho.retail"], "role": "HO_RETAIL"})
check("DIT creates HO_RETAIL", s == 200, (s, b))
s, b, _ = call("POST", "/api/admin/users", dit, {"username": "weak.user", "password": "short", "role": "CALL_CENTRE"})
check("weak password refused (400)", s == 400, (s, b))
s, b, _ = call("POST", "/api/admin/users", dit, {"username": "ho.retail", "password": pw(), "role": "HO_RETAIL"})
check("duplicate username refused (409)", s == 409, (s, b))
s, b, _ = call("POST", "/api/admin/users", dit, {"username": "bo.maker", "password": pw(), "role": "BO_MAKER"})
check("removed BO role refused (400)", s == 400, (s, b))
s, b, _ = call("GET", "/api/admin/users", dit)
dit_id = next(u["id"] for u in b if u["username"] == "dit.superadmin")
s, b, _ = call("DELETE", f"/api/admin/users/{dit_id}", dit)
check("DIT cannot delete own account (400)", s == 400, (s, b))
s, b, _ = call("PUT", f"/api/admin/users/{dit_id}/active", dit, {"active": 0})
check("DIT cannot deactivate self (400)", s == 400, (s, b))
new_dit = pw()
s, b, _ = call("POST", "/api/auth/change-password", dit, {"currentPassword": "WrongCurrent1", "newPassword": new_dit})
check("change-password with wrong current refused (400)", s == 400, (s, b))
s, b, _ = call("POST", "/api/auth/change-password", dit, {"currentPassword": CREDS["dit"], "newPassword": new_dit})
check("DIT changes own password", s == 200, (s, b))
s, _ = login("dit.superadmin", CREDS["dit"])
check("old DIT password no longer works", s == 401, s)
s, dit = login("dit.superadmin", new_dit)
check("new DIT password works", s == 200 and dit, s)
s, b, _ = call("GET", "/api/admin/projects", dit)
check("DIT sees (empty) project list", s == 200 and b == [], (s, b))

# HO admin
s, hoadm = login("ho.admin", CREDS["ho"])
check("HO admin login", s == 200 and hoadm, s)
s, b, _ = call("POST", "/api/admin/projects", hoadm, {"projectName": "Should Fail"})
check("HO admin cannot create projects (403)", s == 403, (s, b))
s, b, _ = call("GET", "/api/admin/projects", hoadm)
check("HO admin can view projects", s == 200, (s, b))
s, b, _ = call("GET", "/api/admin/leads", hoadm)
check("HO admin can view leads", s == 200 and len(b) >= 1, (s, b))

# ---------------------------------------------------------------------------------------------
sec("3. HO Marketing creates zone users")
s, hom = login("ho.marketing", PW["ho.marketing"])
check("HO marketing login", s == 200 and hom, s)
users = {}
for uname, role, zone in [("kol.maker", "ZO_MAKER", "Kolkata"), ("kol.checker", "ZO_CHECKER", "Kolkata"),
                          ("mum.maker", "ZO_MAKER", "Mumbai"), ("mum.checker", "ZO_CHECKER", "Mumbai"),
                          ("call.centre", "CALL_CENTRE", None)]:
    PW[uname] = pw()
    body = {"username": uname, "password": PW[uname], "role": role}
    if zone:
        body["zone"] = zone
    s, b, _ = call("POST", "/api/admin/users", hom, body)
    check(f"creates {uname} ({role}{', ' + zone if zone else ''})", s == 200, (s, b))
s, b, _ = call("POST", "/api/admin/users", hom, {"username": "no.zone", "password": pw(), "role": "ZO_MAKER"})
check("ZO user without zone refused (400)", s == 400, (s, b))
s, b, _ = call("POST", "/api/admin/users", hom, {"username": "evil.admin", "password": pw(), "role": "HO_ADMIN"})
check("HO marketing cannot create HO_ADMIN (403)", s == 403, (s, b))
s, b, _ = call("POST", "/api/admin/users", hom, {"username": "evil.dit", "password": pw(), "role": "SUPERADMIN_DIT"})
check("HO marketing cannot create SUPERADMIN (403)", s == 403, (s, b))
s, b, _ = call("GET", "/api/admin/users", hom)
ids = {u["username"]: u["id"] for u in b}
s, b2, _ = call("POST", f"/api/admin/users/{ids['dit.superadmin']}/reset-password", hom, {"password": pw()})
check("HO marketing cannot reset DIT password (403)", s == 403, (s, b2))
s, b2, _ = call("POST", f"/api/admin/users/{ids['kol.maker']}/reset-password", hom, {"password": "weakpw"})
check("reset to weak password refused (400)", s == 400, (s, b2))
newp = pw()
s, b2, _ = call("POST", f"/api/admin/users/{ids['kol.maker']}/reset-password", hom, {"password": newp})
check("HO marketing resets a zone user's password", s == 200, (s, b2))
PW["kol.maker"] = newp
s, b2, _ = call("PUT", f"/api/admin/users/{ids['mum.maker']}/active", hom, {"active": 0})
s2, _ = login("mum.maker", PW["mum.maker"])
check("deactivated user cannot log in (401)", s == 200 and s2 == 401, (s, s2))
call("PUT", f"/api/admin/users/{ids['mum.maker']}/active", hom, {"active": 1})
s2, _ = login("mum.maker", PW["mum.maker"])
check("re-activated user can log in", s2 == 200, s2)

# ---------------------------------------------------------------------------------------------
sec("4. Kolkata maker -> checker flow")
s, km = login("kol.maker", PW["kol.maker"])
s, kc = login("kol.checker", PW["kol.checker"])
s, mm = login("mum.maker", PW["mum.maker"])
s, mc = login("mum.checker", PW["mum.checker"])
check("maker/checker logins", all([km, kc, mm, mc]))
s, b, _ = call("POST", "/api/admin/projects", km, {"projectName": "Test Residency Kolkata", "id": "UCR-PROJ-0001", "status": "APPROVED",
                                                    "isPublished": 1, "checkedBy": "someone", "zoneName": "Mumbai", "totalImgCount": 99})
check("draft created", s == 200 and b["status"] == "DRAFT", (s, b))
P1 = b["id"] if isinstance(b, dict) else None
check("injected id/status/publish/zone ignored", b["id"] != "UCR-PROJ-0001" and b["isPublished"] == 0 and b["zoneName"] == "Kolkata" and b["checkedBy"] is None and b["totalImgCount"] == 0, b)
s, b, _ = call("POST", "/api/admin/projects", km, {"projectName": "Bad Pin", "pinCode": "12345"})
check("bad pincode on draft refused (400)", s == 400, (s, b))
s, b, _ = call("POST", f"/api/admin/projects/{P1}/submit", km)
check("submit incomplete draft refused (400, names missing fields)", s == 400 and "Pincode" in b.get("message", ""), (s, b))
s, b, _ = call("PUT", f"/api/admin/projects/{P1}", km, project_body("Test Residency Kolkata"))
check("maker fills in details (PUT)", s == 200 and b["pinCode"] == "700091", (s, b))
s, b, _ = call("PUT", f"/api/admin/projects/{P1}", km, project_body("Test Residency Kolkata", totalInventory=10, unsoldInventory=50))
check("available > total refused (400)", s == 400, (s, b))
# format checks are enforced by the server too (not only on the screen)
for label, extra, word in [
    ("bad email", {"salesEmail": "not-an-email"}, "email"),
    ("phone too long for the database column", {"branchHeadNumber": "+91 98300 12345 ext 22"}, "Branch head number"),
    ("budget minimum above maximum", {"budgetMin": "1.2 Cr", "budgetMax": "95 L"}, "Budget"),
    ("website without http", {"website": "www.example.com"}, "Website"),
    ("completion before start", {"startDate": "2026-01-01", "completionDate": "2025-01-01"}, "Completion"),
    ("approval date in the future", {"approvalDate": "2099-01-01"}, "Approval date"),
    ("bad RERA characters", {"reraNo": "<script>"}, "RERA"),
]:
    s, b, _ = call("PUT", f"/api/admin/projects/{P1}", km, project_body("Test Residency Kolkata", **extra))
    check(f"server refuses {label} (400, clear message)", s == 400 and word.lower() in b.get("message", "").lower(), (s, b))
s, b, _ = call("POST", "/api/admin/projects", km, {"projectName": "No"})
check("project name under 3 characters refused (400)", s == 400, (s, b))
s, b, _ = call("PUT", f"/api/admin/projects/{P1}", mm, project_body("Hijack"))
check("Mumbai maker cannot edit Kolkata draft (403)", s == 403, (s, b))
s, b, _ = call("POST", f"/api/admin/projects/{P1}/submit", mm)
check("Mumbai maker cannot submit Kolkata draft (403)", s == 403, (s, b))

# files
s, b, _ = upload(P1, km, [("front.png", "image/png", PNG), ("side.jpg", "image/jpeg", JPG)])
check("two photos (png + jpg) uploaded", s == 200 and len(b) == 2, (s, b))
s3, b3, _ = upload(P1, km, [("plan.pdf", "application/pdf", PDF)])
check("PDF upload refused (400)", s3 == 400, (s3, b3))
s3, b3, _ = upload(P1, km, [("big.jpg", "image/jpeg", JPG + b"0" * (3 * 1024 * 1024 + 10))])
check("photo over 3 MB refused (400)", s3 == 400, (s3, b3))
png_url = b[0]["url"] if s == 200 else None
s, b2, _ = upload(P1, km, [("evil.png", "image/png", HTML_AS_PNG)])
check("HTML disguised as .png refused (400)", s == 400, (s, b2))
s, b2, _ = upload(P1, km, [("evil.html", "text/html", HTML_AS_PNG)])
check(".html extension refused (400)", s == 400, (s, b2))
s, b2, _ = upload(P1, km, [("big.png", "image/png", PNG + b"0" * (5 * 1024 * 1024 + 10))])
check("file over the size limit refused", s in (400, 413), (s, b2))
s, b2, _ = upload(P1, mm, [("x.png", "image/png", PNG)])
check("Mumbai maker cannot upload to Kolkata project (403)", s == 403, (s, b2))
s, b2, _ = call("GET", png_url)
check("draft photo NOT public (404)", s == 404, (s, b2))
s, b2, h = call("GET", png_url.replace("/api/public/files/", "/api/admin/files/"), kc)
check("checker can view draft photo via staff URL (200, image/png, nosniff)", s == 200 and h.get("Content-Type") == "image/png" and h.get("X-Content-Type-Options") == "nosniff", (s, h.get("Content-Type")))
s, b2, _ = call("GET", png_url.replace("/api/public/files/", "/api/admin/files/"))
check("staff file URL needs login", s in (401, 403), s)
s, b2, _ = call("GET", f"/api/public/projects/{P1}")
check("draft project detail not public (404)", s == 404, (s, b2))
s, b2, _ = call("GET", f"/api/public/projects/{P1}/images")
check("draft image list not public (404)", s == 404, (s, b2))

# submit and checker flow
s, b, _ = call("POST", f"/api/admin/projects/{P1}/submit", km)
check("maker submits for approval", s == 200 and b["status"] == "PENDING_APPROVAL", (s, b))
s, b, _ = upload(P1, km, [("late.png", "image/png", PNG)])
check("no file changes while pending (400)", s == 400, (s, b))
s, b, _ = call("PUT", f"/api/admin/projects/{P1}", km, project_body("Edited While Pending"))
check("no edits while pending (400)", s == 400, (s, b))
s, b, _ = call("POST", f"/api/admin/projects/{P1}/approve", km, {"comment": "self"})
check("maker cannot approve (403)", s == 403, (s, b))
s, b, _ = call("POST", f"/api/admin/projects/{P1}/approve", mc, {"comment": "wrong zone"})
check("Mumbai checker cannot approve Kolkata project (403)", s == 403, (s, b))
s, b, _ = call("GET", "/api/admin/projects", kc)
check("Kolkata checker sees Kolkata project", s == 200 and any(p["id"] == P1 for p in b), (s, [p["id"] for p in b] if isinstance(b, list) else b))
s, b, _ = call("GET", "/api/admin/projects", mc)
check("Mumbai checker does NOT see it by default", s == 200 and not any(p["id"] == P1 for p in b), s)
s, b, _ = call("GET", "/api/admin/projects?scope=all", mc)
check("Mumbai checker sees it with All-India scope (read only)", s == 200 and any(p["id"] == P1 for p in b), s)
s, b, _ = call("POST", f"/api/admin/projects/{P1}/reject", kc, {"comment": "  "})
check("reject without remark refused (400)", s == 400, (s, b))
s, b, _ = call("POST", f"/api/admin/projects/{P1}/needs-correction", kc, {"comment": "Please correct the RERA number"})
check("checker returns for correction", s == 200 and b["status"] == "NEEDS_CORRECTION", (s, b))
s, b2, _ = call("GET", f"/api/public/projects/{P1}")
check("returned project is not public", s == 404, s)
s, b, _ = call("PUT", f"/api/admin/projects/{P1}", km, project_body("Test Residency Kolkata", reraNo="WBRERA/P/TEST/2026/0002"))
check("maker corrects the project", s == 200, (s, b))
s, b, _ = call("POST", f"/api/admin/projects/{P1}/submit", km)
check("maker resubmits", s == 200 and b["status"] == "PENDING_APPROVAL", (s, b))
s, b, _ = call("POST", f"/api/admin/projects/{P1}/approve", kc, {"comment": "Documents verified"})
check("Kolkata checker approves", s == 200 and b["status"] == "APPROVED" and b["isPublished"] == 1 and b["checkedBy"] == "kol.checker", (s, b))
s, b, _ = upload(P1, km, [("after.png", "image/png", PNG)])
check("no file changes after approval (400)", s == 400, (s, b))
s, b, _ = call("PUT", f"/api/admin/projects/{P1}", km, project_body("Edited After Approval"))
check("no edits after approval (400)", s == 400, (s, b))
s, b, _ = call("GET", f"/api/admin/projects/{P1}/audit", kc)
actions = [a["action"] for a in b][::-1] if isinstance(b, list) else b
check("audit trail complete and ordered", actions == ["CREATE_DRAFT", "EDIT_DRAFT", "SUBMIT_FOR_APPROVAL", "CHECKER_NEEDS_CORRECTION", "EDIT_DRAFT", "SUBMIT_FOR_APPROVAL", "CHECKER_APPROVE"] or
      (isinstance(actions, list) and actions[0] == "CREATE_DRAFT" and actions[-1] == "CHECKER_APPROVE" and "CHECKER_NEEDS_CORRECTION" in actions), actions)

# ---------------------------------------------------------------------------------------------
sec("5. Customer sees the approved project")
s, b, _ = call("GET", "/api/public/projects")
check("public list shows 1 project", s == 200 and b["total"] == 1 and b["items"][0]["id"] == P1, (s, b))
item = b["items"][0] if s == 200 and b["items"] else {}
check("card shows builder, price, RERA, pincode", item.get("builder") and item.get("price") == "₹ 35 L – 70 L" and item.get("rera") and item.get("pinCode") == "700091", item)
s, b, _ = call("GET", "/api/public/meta")
check("customer filters now list West Bengal / Kolkata only", b.get("states") == ["West Bengal"] and b["stateCities"]["West Bengal"] == ["Kolkata"], b)
s, b, _ = call("GET", "/api/public/projects?state=West%20Bengal&city=Kolkata")
check("state+city filter finds it", b["total"] == 1, b)
s, b, _ = call("GET", "/api/public/projects?state=Maharashtra")
check("other state filter finds nothing", b["total"] == 0, b)
s, b, _ = call("GET", "/api/public/projects?search=700091")
check("pincode search works", b["total"] == 1, b)
s, b, _ = call("GET", f"/api/public/projects/{P1}")
leak = [k for k in b if k in ("createdBy", "checkedBy", "checkerComment", "status", "isActive", "isPublished", "id_internal")]
check("detail page has contacts and NO internal fields", s == 200 and b["salesMobile"] == "9830012345" and not leak, leak)
check("detail has 2 files", len(b.get("images", [])) == 2, b.get("images"))
s, b2, h = call("GET", png_url)
check("approved project's photo is public (200)", s == 200 and h.get("Content-Type") == "image/png", s)
s, b, _ = call("POST", "/api/public/leads", body={"name": "Test Applicant", "phone": "9876500003", "status": "SUCCESSFUL_APPLIED", "projectId": P1, "branchSelected": "Salt Lake"})
check("customer applies for home loan (lead saved)", s == 200, (s, b))

# ---------------------------------------------------------------------------------------------
sec("6. Mumbai flow, HO Marketing as maker, HO Retail")
s, b, _ = call("POST", "/api/admin/projects", mm, project_body("Test Heights Mumbai", state="Maharashtra", city="Mumbai", pin="400069"))
P2 = b["id"] if s == 200 else None
check("Mumbai maker creates project", s == 200 and b["zoneName"] == "Mumbai", (s, b))
s, b, _ = call("POST", f"/api/admin/projects/{P2}/submit", mm)
check("Mumbai maker submits", s == 200, (s, b))
s, hor = login("ho.retail", PW["ho.retail"])
s, b, _ = call("POST", f"/api/admin/projects/{P2}/approve", hor, {"comment": "HO retail approval attempt"})
check("HO Retail cannot approve projects (403)", s == 403, (s, b))
s, b, _ = call("POST", f"/api/admin/projects/{P2}/approve", mc, {"comment": "Mumbai checker approves"})
check("Mumbai checker approves the Mumbai project", s == 200 and b["status"] == "APPROVED", (s, b))
s, b, _ = call("POST", "/api/admin/projects", hom, project_body("Test HO Marketing Project", state="Goa", city="North Goa", pin="403001", zoneName="Pune"))
P3 = b["id"] if s == 200 else None
check("HO marketing can create a project in any zone", s == 200 and b["zoneName"] == "Pune", (s, b))
s, b, _ = call("POST", f"/api/admin/projects/{P3}/submit", hom)
s, b, _ = call("POST", f"/api/admin/projects/{P3}/approve", hom, {"comment": "self"})
check("HO marketing cannot approve own project (403)", s == 403, (s, b))

# HO Retail: reduce available units (their discretion, reason required, audited)
def reduce(pid, tok, units, reason):
    return call("POST", f"/api/admin/projects/{pid}/inventory/reduce", tok, {"units": units, "reason": reason})
s, b, _ = reduce(P2, hor, 5, "Sanction for 5 units")
check("HO Retail reduces available units 40 -> 35", s == 200 and b["unsoldInventory"] == 35, (s, b))
s, b, _ = call("GET", "/api/public/projects?search=Heights")
check("customers see the new count at once", s == 200 and b["items"][0]["availableInventory"] == 35, b)
s, b, _ = reduce(P2, hor, 1, "   ")
check("reduction without reason refused (400)", s == 400, (s, b))
s, b, _ = reduce(P2, hor, 0, "x")
check("reduce by 0 refused (400)", s == 400, (s, b))
s, b, _ = reduce(P2, hor, 36, "too many")
check("cannot reduce below zero (400)", s == 400, (s, b))
s, b, _ = reduce(P3, hor, 1, "not approved yet")
check("cannot reduce a project that is not approved (400)", s == 400, (s, b))
s, b, _ = reduce(P2, mm, 1, "maker tries")
check("maker cannot reduce units (403)", s == 403, (s, b))
s, b, _ = reduce(P2, hom, 1, "ho marketing tries")
check("HO Marketing cannot reduce units (403)", s == 403, (s, b))
s, b, _ = call("GET", f"/api/admin/projects/{P2}/audit", hor)
check("reduction is in the audit trail with old and new count", isinstance(b, list) and any(a["action"] == "INVENTORY_REDUCED" and a["oldVal"] == "available: 40" and "available: 35" in a["newVal"] and a["byUser"] == "ho.retail" for a in b), b)
s, b, _ = call("GET", "/api/public/projects")
check("public list reflects approvals", b["total"] >= 2, b["total"])

# ---------------------------------------------------------------------------------------------
sec("7. Call centre and leads")
s, cc = login("call.centre", PW["call.centre"])
s, b, _ = call("GET", "/api/admin/leads", cc)
check("call centre lists leads", s == 200 and len(b) >= 2, (s, len(b) if isinstance(b, list) else b))
s, b, _ = call("GET", "/api/admin/leads?status=SUCCESSFUL_APPLIED", cc)
check("filter by status works", s == 200 and all(l["status"] == "SUCCESSFUL_APPLIED" for l in b) and len(b) == 1, b)
s, b, _ = call("GET", "/api/admin/leads?from=not-a-date", cc)
check("bad date refused (400)", s == 400, (s, b))
s, b, h = call("GET", "/api/admin/leads/export.xlsx", cc)
check("Excel export is a real xlsx", s == 200 and isinstance(b, (bytes, bytearray)) and b[:2] == b"PK" and "spreadsheet" in h.get("Content-Type", ""), (s, h.get("Content-Type")))
s, b, _ = call("GET", "/api/admin/leads/export.xlsx", km)
check("maker cannot export leads (403)", s == 403, s)
s, b, _ = call("GET", "/api/admin/leads", hor)
check("HO retail can see leads", s == 200, s)
s, b, _ = call("GET", "/api/admin/users", cc)
check("call centre cannot list users (403)", s == 403, s)
s, b, _ = call("POST", "/api/admin/projects", cc, {"projectName": "x"})
check("call centre cannot create projects (403)", s == 403, s)

# ---------------------------------------------------------------------------------------------
sec("8. Account security")
s, b, _ = call("PUT", f"/api/admin/users/{ids['kol.checker']}/active", hom, {"active": 0})
s, b, _ = call("GET", "/api/admin/projects", kc)
check("deactivated user's existing token stops working at once", s in (401, 403), s)
call("PUT", f"/api/admin/users/{ids['kol.checker']}/active", hom, {"active": 1})
s, b, _ = call("GET", "/api/admin/projects", kc)
check("re-activated user's token works again", s == 200, s)
codes = [login("mum.checker", "Wrong" + str(i) + "Pass1")[0] for i in range(5)]
s6, _ = login("mum.checker", PW["mum.checker"])
check("5 wrong passwords lock the account (correct password then gets 429)", codes == [401] * 5 and s6 == 429, (codes, s6))

# ---------------------------------------------------------------------------------------------
print("\n" + "=" * 70)
fails = [r for r in results if not r[2]]
print(f"TOTAL {len(results)}   PASS {len(results) - len(fails)}   FAIL {len(fails)}")
for sct, name, ok, d in fails:
    print(f"  FAIL [{sct}] {name}: {d}")
sys.exit(1 if fails else 0)
