#!/usr/bin/env python3
"""End-to-end test of the maker-checker workflow (STAGING model) on a RUNNING local setup (h2 or uat profile).

    python3 scripts/workflow_test.py
    PUBLIC_URL=http://localhost:8080/ucorealty-customer INTERNAL_URL=http://localhost:8081/ucorealty-staff python3 scripts/workflow_test.py

The model under test: the master table holds only APPROVED projects; everything a maker does (new project, edit, unit
reduction, deletion) is a row in the STAGING table that he edits until he sends it for approval, and only a checker's
approval applies it to the master. The test creates two projects named "ZZ Workflow ..." and walks them through every
path, checking at each step what the maker, the checker, other roles and the public customer site can see.
Only for the h2 or uat profile. Exit code 0 only when every check passes.
"""
import os
import re
import sys
from pathlib import Path

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from uco_http import Client, location  # noqa: E402
import load_demo_data as demo  # noqa: E402

PUBLIC = os.environ.get("PUBLIC_URL", "http://localhost:8080/ucorealty-customer")
INTERNAL = os.environ.get("INTERNAL_URL", "http://localhost:8081/ucorealty-staff")
PHOTO = sorted((Path(__file__).resolve().parent.parent / ".brain" / "07_REFERENCE" / "sample-uploads" / "UCR-PROJ-0001").glob("photo-*.jpg"))
results = []


def check(name, ok, detail=""):
    results.append(bool(ok))
    print(f"  [{'PASS' if ok else 'FAIL'}] {name}" + (f"  -> {detail}" if detail and not ok else ""))


def login(emp):
    c = Client(INTERNAL)
    if not c.dev_login(emp):
        sys.exit(f"Cannot sign in {emp}: run this against the h2 or uat profile (scripts/dev-start.sh)")
    return c


def row(c, pid):
    html = c.get(f"/admin/projects?search={pid}")[2].decode("utf-8", "replace")
    m = re.search(r'<tr id="row-project-%s">(.*?)</tr>' % pid, html, re.S)
    return m.group(1) if m else None


def sid(c, pid):
    m = re.search(r"/admin/projects/staging/(\d+)/", row(c, pid) or "")
    return int(m.group(1)) if m else None


def units(c, pid):
    m = re.search(r"<strong>(\d+)</strong> avail\.", row(c, pid) or "")
    return int(m.group(1)) if m else None


def status_of(c, pid):
    m = re.search(r'badge-status-%s"[^>]*>([^<]+)<' % pid, row(c, pid) or "")
    return m.group(1).strip() if m else None


def public(pid):
    s, _, b = Client(PUBLIC).get(f"/project/{pid}")
    return s, b.decode("utf-8", "replace")


def save(c, fields, stagingId=None, action="draft", files=()):
    fl = dict(fields)
    fl.update({"action": action, "_csrf": c.csrf("/admin/projects")})
    if stagingId:
        fl["stagingId"] = str(stagingId)
    return c.multipart_post("/admin/projects/staging/save", fl, files)


def decide(c, staging_id, decision, comment=""):
    return c.post(f"/admin/projects/staging/{staging_id}/decide",
                  {"decision": decision, "comment": comment, "_csrf": c.csrf("/admin/projects")})


def new_project(c, name, action="draft", photo=True, units_=60):
    fl = demo.form(1, name, "ZZ Workflow Builder", "Bihar", "Patna", "800001", 100)
    fl.update({"branchName": "Patna Main Branch", "unsoldInventory": str(units_), "amenities": "Original amenities"})
    files = [("photo1", PHOTO[0].name, PHOTO[0].read_bytes(), "image/jpeg")] if photo else []
    s, h, _ = save(c, fl, action=action, files=files)
    return s, h


maker, checker = login("700001"), login("700002")
other_maker, other_checker = login("200001"), login("200002")
retail, dit, marketing = login("400001"), login("100001"), login("400002")

print("== 1. A NEW project lives in the maker's staging copy, not in the live table")
check("maker saves a new project as a draft", new_project(maker, "ZZ Workflow Test")[0] == 302)
pid = demo.newest_project_id(maker, "ZZ Workflow Test")
check("project code allocated", pid is not None)
check("maker sees it as Draft", status_of(maker, pid) == "Draft")
check("it is NOT live: customers get 404", public(pid)[0] == 404)
check("checker, retail and other zone do not see a draft",
      all(row(c, pid) is None for c in (checker, retail, other_maker, other_checker)))
s1 = sid(maker, pid)
s, h, _ = save(maker, {"amenities": "Edited in the draft"}, stagingId=s1)
check("maker edits the draft (same row)", s == 302 and sid(maker, pid) == s1)
check("another maker cannot edit his draft (403)", other_maker.get(f"/admin/projects/staging/{s1}/edit")[0] == 403)
check("checker cannot open it either (403)", checker.get(f"/admin/projects/staging/{s1}/edit")[0] == 403)
check("maker previews the unapproved project", "Edited in the draft" in maker.get(f"/admin/projects/staging/{s1}/preview")[2].decode())

print("\n== 2. Submit: the checker of the zone sees it, nobody else; still not live")
check("submit from the dashboard works", maker.post(f"/admin/projects/staging/{s1}/submit", {"_csrf": maker.csrf("/admin/projects")})[0] == 302)
check("status is Pending Approval", status_of(maker, pid) == "Pending Approval")
check("zone checker has a Review button", "link-review-" in (row(checker, pid) or ""))
check("HO retail can see it (read only, no button)", row(retail, pid) is not None and "link-review-" not in row(retail, pid))
check("checker of ANOTHER zone does not see it", row(other_checker, pid) is None)
check("still not live", public(pid)[0] == 404)
check("maker cannot approve his own (403)", decide(maker, s1, "approve")[0] == 403 and status_of(maker, pid) == "Pending Approval")
check("retail cannot approve (403)", decide(retail, s1, "approve")[0] == 403)
decide(other_checker, s1, "approve")
check("checker of another zone cannot approve", status_of(maker, pid) == "Pending Approval")
s_, _, body = checker.get(f"/admin/projects/staging/{s1}/review")
check("review page lists what the maker entered", s_ == 200 and "Edited in the draft" in body.decode() and "ZZ Workflow Builder" in body.decode())

print("\n== 3. Return, correct, approve")
decide(checker, s1, "needs-correction", "Fix the amenities")
check("returned to the maker (Needs Correction, with remark)", status_of(maker, pid) == "Needs Correction" and "Fix the amenities" in (row(maker, pid) or ""))
check("a returned request is private to the maker again", row(checker, pid) is None)
save(maker, {"amenities": "Corrected amenities"}, stagingId=s1)
maker.post(f"/admin/projects/staging/{s1}/submit", {"_csrf": maker.csrf("/admin/projects")})
decide(checker, s1, "approve", "ok")
decide(dit, s1, "approve", "again")
check("approved: now live for customers (data + photo)", public(pid)[0] == 200 and "Corrected amenities" in public(pid)[1])
check("photo went live with it", all(Client(PUBLIC).get(u)[0] == 200 for u in set(re.findall(r'(/files/\d+)', public(pid)[1]))) and "/files/" in public(pid)[1])
check("dashboard now shows an Approved live project", status_of(checker, pid) == "Approved" and status_of(maker, pid) == "Approved")
check("a second decision on the same request is refused (status unchanged)", decide(dit, s1, "reject", "late")[0] in (302, 400) and status_of(checker, pid) == "Approved")

print("\n== 4. Reject path of a new project")
new_project(maker, "ZZ Workflow Reject", action="submit", photo=False)
pid2 = demo.newest_project_id(maker, "ZZ Workflow Reject"); s2 = sid(maker, pid2)
decide(checker, s2, "reject", "Not acceptable")
check("rejected project is never live", public(pid2)[0] == 404 and status_of(maker, pid2) == "Rejected")
check("maker and checker still see it as Rejected", status_of(checker, pid2) == "Rejected")

print("\n== 5. Withdraw / discard")
new_project(maker, "ZZ Workflow Discard", photo=False); pid3 = demo.newest_project_id(maker, "ZZ Workflow Discard"); s3 = sid(maker, pid3)
maker.post(f"/admin/projects/staging/{s3}/withdraw", {"_csrf": maker.csrf("/admin/projects")})
check("a discarded draft disappears", row(maker, pid3) is None and public(pid3)[0] == 404)

print("\n== 6. Edit of an APPROVED project: private draft, pending, return, approve, reject")
check("maker sees Edit / Units / Delete on the live project", all(k in (row(maker, pid) or "") for k in ("link-change-project-", "btn-reduce-units-", "btn-delete-project-")))
check("retail has no change buttons", not any(k in (row(c, pid) or "") for c in (retail,) for k in ("btn-reduce-units-", "link-change-project-", "btn-delete-project-")))
check("opening the change form works (200)", maker.get(f"/admin/projects/{pid}/change")[0] == 200)
cs = sid(maker, pid); check("a working copy (UPDATE) exists", cs is not None)
s, h, _ = save(maker, {"amenities": "DRAFT-ONLY amenities", "unsoldInventory": "55"}, stagingId=cs)
check("save the change as a draft", s == 302)
check("live project unchanged for customers", "Corrected amenities" in public(pid)[1] and "DRAFT-ONLY" not in public(pid)[1])
check("checker sees the old units (60) and no pending tag", units(checker, pid) == 60 and "badge-change-" not in (row(checker, pid) or ""))
check("maker sees 'Your change: draft'", "Your change: draft" in (row(maker, pid) or ""))
s_, h_, _ = dit.get(f"/admin/projects/{pid}/change")
check("only ONE open request per project: another maker is refused", s_ == 302 and location(h_).endswith("/admin/projects"))
check("checker cannot review a draft (private)", checker.get(f"/admin/projects/staging/{cs}/review")[0] == 302)
save(maker, {"amenities": "DRAFT-ONLY amenities", "unsoldInventory": "55"}, stagingId=cs, action="submit")
check("sent for approval: live row shows 'Change pending approval'", "Change pending approval" in (row(checker, pid) or ""))
check("live data still the old data", "Corrected amenities" in public(pid)[1])
s_, _, body = checker.get(f"/admin/projects/staging/{cs}/review")
check("review shows approved -> proposed", s_ == 200 and "Corrected amenities" in body.decode() and "DRAFT-ONLY amenities" in body.decode())
check("maker cannot approve his change (403)", decide(maker, cs, "approve")[0] == 403)
check("retail cannot approve (403)", decide(retail, cs, "approve")[0] == 403)
decide(other_checker, cs, "approve")
check("another zone's checker cannot approve", "Change pending approval" in (row(checker, pid) or ""))
check("maker cannot edit while it is pending", maker.get(f"/admin/projects/{pid}/change")[0] == 302)
decide(checker, cs, "needs-correction", "Add the full list")
check("returned: maker sees 'returned' + remark, live data unchanged", "Your change: returned" in (row(maker, pid) or "") and "Corrected amenities" in public(pid)[1])
maker.get(f"/admin/projects/{pid}/change")
save(maker, {"amenities": "Final amenities list", "unsoldInventory": "55"}, stagingId=cs, action="submit")
decide(checker, cs, "approve", "ok")
check("approved change is live", "Final amenities list" in public(pid)[1] and units(checker, pid) == 55)
maker.get(f"/admin/projects/{pid}/change"); cs = sid(maker, pid)
save(maker, {"amenities": "REJECT-ME"}, stagingId=cs, action="submit")
decide(checker, cs, "reject", "No")
check("rejected change leaves the live project as it was", "REJECT-ME" not in public(pid)[1] and "Final amenities list" in public(pid)[1] and "Your change" not in (row(maker, pid) or ""))

print("\n== 6b. Zone limits: a maker of ANOTHER zone cannot touch this project")
before = units(checker, pid)
other_maker.post(f"/admin/projects/{pid}/reduce-units", {"units": "1", "reason": "x", "_csrf": other_maker.csrf("/admin/projects")})
other_maker.post(f"/admin/projects/{pid}/delete-request", {"reason": "x", "_csrf": other_maker.csrf("/admin/projects")})
check("no unit reduction or deletion request was created by the other zone", units(checker, pid) == before and "pending" not in (row(checker, pid) or "").lower())
s_, h_, _ = other_maker.get(f"/admin/projects/{pid}/change")
check("the other zone cannot open a change form either", s_ == 302 and location(h_).endswith("/admin/projects") and row(checker, pid) is not None and "Your change" not in (row(other_maker, pid) or ""))
check("the other zone's maker does not even see the project", row(other_maker, pid) is None)

print("\n== 7. Reduce units = an edit that needs a checker; retail cannot")
check("retail cannot request a reduction (403)", retail.post(f"/admin/projects/{pid}/reduce-units", {"units": "1", "reason": "x", "_csrf": retail.csrf("/admin/projects")})[0] == 403)
maker.post(f"/admin/projects/{pid}/reduce-units", {"units": "5", "reason": "Sold outside the portal", "_csrf": maker.csrf("/admin/projects")})
check("live units unchanged until approval", units(checker, pid) == 55 and "Change pending approval" in (row(checker, pid) or ""))
cs = sid(checker, pid)
s_, _, body = checker.get(f"/admin/projects/staging/{cs}/review")
check("review shows 55 -> 50 and the reason", "Sold outside the portal" in body.decode() and "50" in body.decode())
decide(checker, cs, "approve", "")
check("units are 50 after approval", units(checker, pid) == 50)
maker.post(f"/admin/projects/{pid}/reduce-units", {"units": "500", "reason": "too many", "_csrf": maker.csrf("/admin/projects")})
check("cannot reduce by more than available", units(checker, pid) == 50 and "Change pending" not in (row(checker, pid) or ""))

print("\n== 8. Photos of a change stay hidden until approved")
maker.get(f"/admin/projects/{pid}/change"); cs = sid(maker, pid)
live_ids = set(re.findall(r'/files/(\d+)', public(pid)[1]))
save(maker, {}, stagingId=cs, files=[("photo1", PHOTO[1].name, PHOTO[1].read_bytes(), "image/jpeg")])
form_ids = set(re.findall(r'/files/(\d+)"', maker.get(f"/admin/projects/staging/{cs}/edit")[2].decode()))
new_ids = form_ids - live_ids
check("maker sees the new photo on the form, customers do not", len(new_ids) == 1 and not (new_ids & set(re.findall(r'/files/(\d+)', public(pid)[1]))))
check("its URL is 404 for customers", all(Client(PUBLIC).get(f"/files/{i}")[0] == 404 for i in new_ids))
save(maker, {}, stagingId=cs, action="submit"); decide(checker, cs, "approve", "")
check("after approval the photo is public", all(Client(PUBLIC).get(f"/files/{i}")[0] == 200 for i in new_ids))
maker.get(f"/admin/projects/{pid}/change"); cs = sid(maker, pid); victim = next(iter(new_ids))
maker.post(f"/admin/projects/staging/{cs}/photos/{victim}/remove", {"_csrf": maker.csrf("/admin/projects")})
check("a live photo stays live until the removal is approved", Client(PUBLIC).get(f"/files/{victim}")[0] == 200)
save(maker, {}, stagingId=cs, action="submit"); decide(checker, cs, "approve", "")
check("after approval the removed photo is gone", Client(PUBLIC).get(f"/files/{victim}")[0] == 404)

print("\n== 9. Deletion needs a checker; the record is kept but hidden")
maker.post(f"/admin/projects/{pid}/delete-request", {"reason": "Duplicate entry", "_csrf": maker.csrf("/admin/projects")})
check("stays live while the deletion is pending", public(pid)[0] == 200 and "Deletion pending approval" in (row(checker, pid) or ""))
cs = sid(checker, pid)
check("review shows the reason", "Duplicate entry" in checker.get(f"/admin/projects/staging/{cs}/review")[2].decode())
check("maker cannot approve (403)", decide(maker, cs, "approve")[0] == 403)
decide(checker, cs, "needs-correction", "x")
check("a deletion can only be approved or rejected", "Deletion pending approval" in (row(checker, pid) or ""))
decide(checker, cs, "approve", "")
check("deleted: gone from every dashboard", all(row(c, pid) is None for c in (maker, checker, dit, retail, marketing)))
check("deleted: gone from the customer site", public(pid)[0] == 404)
check("deleted: staff cannot open or change it", maker.get(f"/project/{pid}")[0] == 404 and maker.get(f"/admin/projects/{pid}/change")[0] == 302)

print("\n== 10. Roles")
check("HO marketing (national maker) can open the new-project form", marketing.get("/admin/projects/new")[0] == 200)
check("checker and retail cannot (403)", all(c.get("/admin/projects/new")[0] == 403 for c in (checker, retail)))

print("\n== 11. A Head Office maker creates a project for another zone; only that zone's checker decides")
fl = demo.form(2, "ZZ Workflow National", "ZZ Workflow Builder", "Rajasthan", "Ajmer", "305001", 40)
fl.update({"zoneName": "Ajmer", "branchName": "Ajmer Main Branch", "unsoldInventory": "30"})
fl["_csrf"] = marketing.csrf("/admin/projects"); fl["action"] = "submit"
check("HO marketing submits a project for the Ajmer zone", marketing.multipart_post("/admin/projects/staging/save", fl, [])[0] == 302)
pid4 = demo.newest_project_id(marketing, "ZZ Workflow National"); s4 = sid(marketing, pid4)
check("the Patna checker does not see it, the Ajmer checker does", row(checker, pid4) is None and "link-review-" in (row(other_checker, pid4) or ""))
decide(checker, s4, "approve", "")
check("the Patna checker cannot approve an Ajmer project", public(pid4)[0] == 404)
decide(other_checker, s4, "approve", "ok")
check("the Ajmer checker approves: it is live", public(pid4)[0] == 200)
check("a Patna maker does not see an Ajmer project", row(maker, pid4) is None)

print("\n== 12. Input safety (markup is refused, other special characters are shown escaped)")
fl = demo.form(3, "ZZ Workflow <b>Bold</b>", "ZZ Workflow Builder", "Bihar", "Patna", "800001", 20)
fl.update({"branchName": "Patna Main Branch", "_csrf": maker.csrf("/admin/projects"), "action": "draft"})
s_, _, body = maker.multipart_post("/admin/projects/staging/save", fl, [])
check("a project name with < > is refused (form shown again with the message)", s_ == 200 and "must not contain" in body.decode() and demo.newest_project_id(maker, "ZZ Workflow Bold") is None)
check("a customer name with markup is refused (400)", Client(PUBLIC).post("/lead", {"name": "<b>x</b>", "phone": "9876543210", "status": "Visitor"}, headers={"X-Requested-With": "XMLHttpRequest"})[0] == 400)
fl["projectName"] = "ZZ Workflow Tom & Jerry"
maker.multipart_post("/admin/projects/staging/save", fl, [])
html = maker.get("/admin/projects?search=Jerry")[2].decode()
check("an allowed '&' is escaped on screen (&amp;)", "Tom &amp; Jerry" in html and "Tom & Jerry" not in html)
pid5 = demo.newest_project_id(maker, "ZZ Workflow Tom"); maker.post(f"/admin/projects/staging/{sid(maker, pid5)}/withdraw", {"_csrf": maker.csrf("/admin/projects")})

print("\n== 13. The browser's real request shape: all text fields packed into ONE part (Tomcat refuses more than 50 parts)")
import urllib.parse  # noqa: E402


def save_packed(c, fields, action="draft", files=()):
    """What project-form.js sends: _csrf and action as parts, every other text field urlencoded into __packed, photos as file parts."""
    parts = {"_csrf": c.csrf("/admin/projects"), "action": action, "__packed": urllib.parse.urlencode(fields)}
    return c.multipart_post("/admin/projects/staging/save", parts, files)


fl = demo.form(5, "ZZ Workflow Packed & Co", "ZZ Workflow Builder", "Bihar", "Patna", "800001", 40)
fl.update({"branchName": "Patna Main Branch", "amenities": "Park & Gym + Pool\nSecond line \u00e9"})
for extra in ("promoter1", "promoter2", "promoter3", "promoter4", "promoter5", "salesExecName", "salesMobile", "salesEmail", "website",
              "builderRating", "branchHeadName", "branchHeadNumber", "relExecName", "relExecNumber", "approvalSought", "renewalPending"):
    fl.setdefault(extra, "")
photos3 = [(f"photo{i + 1}", p.name, p.read_bytes(), "image/jpeg") for i, p in enumerate(PHOTO[:3])]
check("a browser-style request has far fewer than 50 parts", 3 + len(photos3) <= 14 and len(fl) > 40)
s_, _, _ = save_packed(maker, fl, files=photos3)
check("packed save as draft works (302)", s_ == 302)
pid6 = demo.newest_project_id(maker, "ZZ Workflow Packed")
check("the packed fields were unpacked: project exists as a Draft", pid6 is not None and status_of(maker, pid6) == "Draft")
sid6 = sid(maker, pid6)
prev = maker.get(f"/admin/projects/staging/{sid6}/preview")[2].decode()
check("special characters survived the packing (& + new line, accent)", "Park &amp; Gym + Pool" in prev and "\u00e9" in prev)
fl["stagingId"] = str(sid6)
s_, _, _ = save_packed(maker, fl, action="submit")
check("packed 'Send for Approval' works (302) and the status is Pending Approval", s_ == 302 and status_of(maker, pid6) == "Pending Approval")
decide(checker, sid6, "reject", "test only")

print(f"\n{results.count(True)}/{len(results)} checks passed")
print("(test projects ZZ Workflow ... remain: one DELETED in the master table, one REJECTED in staging; scripts/remove_demo_data.sh removes them)")
sys.exit(1 if False in results else 0)
