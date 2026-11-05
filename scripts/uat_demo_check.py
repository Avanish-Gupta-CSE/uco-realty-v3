#!/usr/bin/env python3
"""Checks the UAT demo setup (uco.auth.directory=demo) on a RUNNING pair of apps: demo login, roles, one project through
maker -> checker -> customer site. The passwords are read from DemoUserDirectory.java, so they are not repeated here.

    python3 scripts/uat_demo_check.py                          # apps started with java -jar (context path "/")
    PUBLIC_URL=http://uat-host:8080/ucorealty-customer INTERNAL_URL=http://uat-host:8081/ucorealty-staff python3 scripts/uat_demo_check.py

Creates one project named "UAT Demo Check (Demo)" (remove it with scripts/remove_demo_data.sh on a local database).
Exit code 0 only when every check passes.
"""
import os
import re
import sys
from pathlib import Path

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from uco_http import Client, location  # noqa: E402
import load_demo_data as demo  # noqa: E402

PUBLIC = os.environ.get("PUBLIC_URL", "http://localhost:8080")
INTERNAL = os.environ.get("INTERNAL_URL", "http://localhost:8081")
ROOT = Path(__file__).resolve().parent.parent
SOURCE = (ROOT / "ucorealty-staff/src/main/java/com/ucorealty/hrms/DemoUserDirectory.java").read_text(encoding="utf-8")
USERS = dict(re.findall(r'new DemoUser\("(UAT\d+)",\s*"([^"]+)"', SOURCE))
PHOTO = sorted((ROOT / ".brain/07_REFERENCE/sample-uploads/UCR-PROJ-0001").glob("photo-*.jpg"))
results = []


def check(name, ok, detail=""):
    results.append(bool(ok))
    print(f"  [{'PASS' if ok else 'FAIL'}] {name}" + (f"  -> {detail}" if detail and not ok else ""))


def login(emp, password):
    c = Client(INTERNAL)
    status, headers, _ = c.post("/demo-login", {"empId": emp, "password": password, "_csrf": c.csrf("/login")})
    return c, location(headers)


def signed_in(c):
    return c.get("/admin/projects")[0] == 200


print("== 1. The login page and the doors that must stay shut")
check("four demo users found in DemoUserDirectory.java", len(USERS) == 4, USERS.keys())
page = Client(INTERNAL).get("/login")[2].decode("utf-8", "replace")
check("login page shows the demo form with employee number and password", 'id="demoLoginForm"' in page and 'id="demoPassword"' in page)
check("login page lists the four demo users, never a password", all(f'id="pill-{u}"' in page for u in USERS) and not any(p in page for p in USERS.values()))
check("no OTP form on the page", "ssoOtpForm" not in page)
anon = Client(INTERNAL)
check("dashboard needs a login", anon.get("/admin/projects")[0] in (302, 401, 403))
check("plain /sso?emp_id= handoff is refused", "error=" in (location(anon.get("/sso?emp_id=UAT001")[1]) or ""))
check("bank SSO entry is refused without a key", "sso_failed" in (location(anon.get("/entry?value=AAAA")[1]) or ""))
c, loc = login("UAT003", "wrong-password")
check("wrong password is refused", "bad_credentials" in (loc or "") and not signed_in(c))
c, loc = login("NOBODY", "x")
check("unknown employee is refused", "bad_credentials" in (loc or "") and not signed_in(c))

print("\n== 2. Each demo user signs in and gets the right role")
clients = {}
for emp, pwd in USERS.items():
    c, loc = login(emp, pwd)
    clients[emp] = c
    check(f"{emp} signs in", signed_in(c), loc)
dit, mkt, maker, checker = (clients.get(u) for u in ("UAT001", "UAT002", "UAT003", "UAT004"))
if not all((dit, mkt, maker, checker)):
    sys.exit("Cannot continue: a demo user could not sign in")
check("DIT superadmin opens the Logs page", dit.get("/admin/audit-logs")[0] == 200)
check("marketing / maker / checker cannot open the Logs page",
      all(x.get("/admin/audit-logs")[0] == 403 for x in (mkt, maker, checker)))
check("maker has the New Project page", maker.get("/admin/projects/new")[0] == 200)

print("\n== 3. One project: maker -> checker -> customer site (also proves both apps share one database)")
name = "UAT Demo Check (Demo)"
fl = demo.form(1, name, "UAT Demo Builder", "Rajasthan", "Ajmer", "305001", 80)
fl.update({"branchName": "Ajmer Main Branch", "action": "draft", "_csrf": maker.csrf("/admin/projects")})
files = [("photo1", PHOTO[0].name, PHOTO[0].read_bytes(), "image/jpeg")] if PHOTO else []
check("maker saves a draft", maker.multipart_post("/admin/projects/staging/save", fl, files)[0] == 302)
pid = demo.newest_project_id(maker, name)
check("project code allocated", pid is not None)
html = maker.get(f"/admin/projects?search={pid}")[2].decode("utf-8", "replace")
m = re.search(r"/admin/projects/staging/(\d+)/", html)
staging = int(m.group(1)) if m else None
check("maker sees the draft on the dashboard", staging is not None)
check("customer site does not show it yet", Client(PUBLIC).get(f"/project/{pid}")[0] == 404)
check("maker sends it for approval", maker.post(f"/admin/projects/staging/{staging}/submit", {"_csrf": maker.csrf("/admin/projects")})[0] == 302)
check("maker cannot approve his own request", maker.post(f"/admin/projects/staging/{staging}/decide",
      {"decision": "approve", "comment": "", "_csrf": maker.csrf("/admin/projects")})[0] == 403)
check("checker (same zone) approves", checker.post(f"/admin/projects/staging/{staging}/decide",
      {"decision": "approve", "comment": "UAT demo check", "_csrf": checker.csrf("/admin/projects")})[0] == 302)
check("customer site now shows the project", Client(PUBLIC).get(f"/project/{pid}")[0] == 200)
check("marketing and DIT see it on their dashboard",
      all(pid in x.get(f"/admin/projects?search={pid}")[2].decode("utf-8", "replace") for x in (mkt, dit)))

print("\n== 4. Sign out")
check("logout ends the session", maker.post("/logout", {"_csrf": maker.csrf("/admin/projects")})[0] == 302 and not signed_in(maker))

print(f"\n{sum(results)}/{len(results)} checks passed")
sys.exit(0 if all(results) else 1)
