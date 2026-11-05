#!/usr/bin/env python3
"""Smoke test for a RUNNING local setup (scripts/dev-start.sh): public portal on 8080, internal portal on 8081.

    python3 scripts/smoke_test.py
    PUBLIC_URL=http://localhost:8080/ucorealty-customer INTERNAL_URL=http://localhost:8081/ucorealty-staff python3 scripts/smoke_test.py

Checks the customer lockdown, security headers, SSO error paths, CSRF, role isolation and logout. The role checks
need the local profile (employee-id + OTP form with the test employees from db_migrations/05); against a UAT/prod build it runs
the checks that do not need a login and says what it skipped. It never creates projects.
Exit code is 0 only when every check passes.
"""
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from uco_http import Client, location  # noqa: E402

PUBLIC = os.environ.get("PUBLIC_URL", "http://localhost:8080/ucorealty-customer")
INTERNAL = os.environ.get("INTERNAL_URL", "http://localhost:8081/ucorealty-staff")
results = []


def check(name, ok, detail=""):
    results.append(bool(ok))
    print(f"  [{'PASS' if ok else 'FAIL'}] {name}" + (f"  -> {detail}" if detail and not ok else ""))


print("== Public portal (customers)")
pub = Client(PUBLIC)
s, h, b = pub.get("/projects")
check("catalog loads", s == 200, s)
hl = {k.lower() for k in h}
check("security headers present", {"content-security-policy", "x-frame-options", "x-content-type-options"} <= hl, sorted(hl))
for path in ("/login", "/admin/projects", "/sso", "/entry", "/api/admin/geo"):
    check(f"{path} is refused", pub.get(path)[0] == 403, pub.get(path)[0])
check("unknown project is 404", pub.get("/project/UCR-PROJ-NOPE")[0] == 404)
check("customer name with markup is refused (400)", pub.post("/lead", {"name": "<b>x</b>", "phone": "9876543210"},
      headers={"X-Requested-With": "XMLHttpRequest"})[0] == 400)
check("bad phone in lead is 400", pub.post("/lead", {"name": "Smoke", "phone": "123"},
      headers={"X-Requested-With": "XMLHttpRequest"})[0] == 400)

print("\n== Internal portal (staff)")
internal = Client(INTERNAL)
s, h, b = internal.get("/login")
check("login page loads", s == 200, s)
dev_login = b"ssoOtpForm" in b
check("security headers present", "content-security-policy" in {k.lower() for k in h})
s, h, _ = internal.get("/admin/projects")
check("anonymous /admin/projects redirects to /login", s == 302 and location(h).endswith("/login"), (s, location(h)))
s, h, _ = internal.get("/entry?value=not-a-real-token")
check("bad SSO token is refused", s == 302 and "sso_failed" in location(h), (s, location(h)))
check("POST without CSRF token is 403", internal.post("/sso/generate-otp", {"empId": "100001"})[0] == 403)

if not dev_login:
    print("\n  (dev login form not present: this looks like a UAT/prod build, role checks skipped)")
else:
    print("\n== Roles (local profile test employees)")
    dit, maker, checker, retail = Client(INTERNAL), Client(INTERNAL), Client(INTERNAL), Client(INTERNAL)
    check("DIT 100001 (SOL 7115) signs in", dit.dev_login("100001"))
    check("Maker 200001 signs in", maker.dev_login("200001"))
    check("Checker 200002 signs in", checker.dev_login("200002"))
    check("Patna maker 700001 (SOL 7001) signs in", Client(INTERNAL).dev_login("700001"))
    check("HO retail 400001 signs in", retail.dev_login("400001"))
    check("HO retail can open Customer Leads", retail.get("/admin/leads")[0] == 200)
    check("Maker cannot open Customer Leads (403)", maker.get("/admin/leads")[0] == 403)
    check("Checker cannot open Customer Leads (403)", checker.get("/admin/leads")[0] == 403)
    check("DIT can open Customer Leads", dit.get("/admin/leads")[0] == 200)
    check("Checker cannot open the new-project form (403)", checker.get("/admin/projects/new")[0] == 403)
    check("HO retail cannot open the new-project form (403)", retail.get("/admin/projects/new")[0] == 403)
    check("DIT can open Logs", dit.get("/admin/audit-logs")[0] == 200)
    check("Maker cannot open Logs (403)", maker.get("/admin/audit-logs")[0] == 403)
    check("Checker cannot open Logs (403)", checker.get("/admin/audit-logs")[0] == 403)
    check("Maker can open the new-project form", maker.get("/admin/projects/new")[0] == 200)
    check("GET /logout does not log out", (dit.get("/logout"), dit.get("/admin/projects"))[1][0] == 200)
    token = dit.csrf("/admin/projects")
    check("POST /logout without token is 403", dit.post("/logout")[0] == 403)
    s, h, _ = dit.post("/logout", {"_csrf": token})
    check("POST /logout with token works", s == 302 and "logout" in location(h), (s, location(h)))
    check("session is gone after logout", dit.get("/admin/projects")[0] == 302)
    check("unauthorised employee id is refused", not Client(INTERNAL).dev_login("999999"))

failed = results.count(False)
print(f"\n{len(results) - failed}/{len(results)} checks passed")
sys.exit(1 if failed else 0)
