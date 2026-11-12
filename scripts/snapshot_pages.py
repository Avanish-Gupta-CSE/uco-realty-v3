#!/usr/bin/env python3
"""Page snapshot: proves that a code clean-up changed NOTHING that a user can see.

    python3 scripts/snapshot_pages.py save  .run/snap-before     # crawl every page for every role, store the cleaned HTML
    python3 scripts/snapshot_pages.py compare .run/snap-before .run/snap-after

Needs the local apps running (scripts/dev-start.sh) with the demo data (scripts/load_demo_data.py).
How it works: it signs in as each test employee, opens every page it can reach (dashboard, project pages, leads with filters and
paging, audit logs, the public catalog, the JSON endpoints, the Excel export) and writes what the server answered into a folder.
Values that are different on every run (security tokens, random project codes, dates and times) are replaced by fixed words first,
so two runs on the same code give identical folders. After a change: save again, then compare. Any difference is printed.
Read only: it never creates, changes or deletes anything.
"""
import difflib
import hashlib
import io
import os
import re
import sys
import zipfile

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from uco_http import Client  # noqa: E402

PUBLIC = os.environ.get("PUBLIC_URL", "http://localhost:8080/UcoRealty")
INTERNAL = os.environ.get("INTERNAL_URL", "http://localhost:8081/UcoRealtyStaff")

# every test employee, in a fixed order
STAFF = [("dit", "55012"), ("marketing", "54534"), ("retail", "58382"), ("maker-durgapur", "52260"),
         ("checker-durgapur", "45195"), ("maker-raipur", "68696"), ("checker-raipur", "45321"), ("no-access", "62100")]

# links worth opening (everything else on a page is a button that changes data, which this script never presses)
STAFF_LINKS = re.compile(r'(?:href|src)="(/UcoRealtyStaff(/admin/projects/staging/\d+/(?:edit|review|preview)'
                         r'|/admin/projects/UCR-PROJ-[0-9A-F]{8}/change|/project/UCR-PROJ-[0-9A-F]{8}|/files/\d+))"')
PUBLIC_LINKS = re.compile(r'(?:href|src)="(/UcoRealty(/project/UCR-PROJ-[0-9A-F]{8}|/files/\d+))"')

PROJECT_CODE = re.compile(r"UCR-PROJ-[0-9A-F]{8}")
VOLATILE = [  # (pattern, replacement): things that differ on every run
    (re.compile(r'(name="_csrf" value=")[^"]+'), r"\1CSRF"),
    (re.compile(r'(name="_csrf" content=")[^"]+'), r"\1CSRF"),
    (re.compile(r'\d{4}-\d{2}-\d{2}[T ]\d{2}:\d{2}(:\d{2})?(\.\d+)?'), "TIMESTAMP"),
    (re.compile(r'\d{2}[-/ ][A-Za-z]{3}[-/ ]\d{4}[ ,]+\d{1,2}:\d{2}(:\d{2})?( ?[APap][Mm])?'), "TIMESTAMP"),
    (re.compile(r'\d{2}/\d{2}/\d{4} \d{1,2}:\d{2}(:\d{2})?( ?[APap][Mm])?'), "TIMESTAMP"),
    (re.compile(r'\d{2}-\d{2}-\d{4} \d{1,2}:\d{2}(:\d{2})?'), "TIMESTAMP"),   # 12-11-2026 17:53:33 (audit log pages)
]


class Cleaner:
    """Replaces random project codes by P1, P2 ... in order of first appearance, and other changing values by fixed words."""

    def __init__(self):
        self.codes = {}

    def code(self, m):
        return self.codes.setdefault(m.group(0), f"PROJ{len(self.codes) + 1:02d}")

    def clean(self, text):
        for pattern, word in VOLATILE:
            text = pattern.sub(word, text)
        return PROJECT_CODE.sub(self.code, text)


def excel_text(raw):
    """The words and numbers inside an .xlsx file (the zip itself carries a creation time, so it cannot be compared as bytes)."""
    out = []
    with zipfile.ZipFile(io.BytesIO(raw)) as z:
        for name in sorted(z.namelist()):
            if name.startswith("xl/worksheets/") or name == "xl/sharedStrings.xml":
                out.append(f"--- {name}\n" + z.read(name).decode("utf-8", "replace"))
    return "\n".join(out)


def fetch(client, path, cleaner):
    """(status, cleaned text) of one page."""
    status, headers, body = client.get(path)
    kind = {k.lower(): v for k, v in headers.items()}.get("content-type", "")
    if "spreadsheetml" in kind:
        try:
            text = excel_text(body)
        except zipfile.BadZipFile:
            text = "NOT A VALID XLSX"
    elif kind.startswith(("image/", "application/pdf", "application/octet-stream")):
        text = f"binary {kind} {len(body)} bytes sha1={hashlib.sha1(body).hexdigest()}"
    else:
        text = body.decode("utf-8", "replace")
    return status, cleaner.clean(text), headers


def file_name(owner, path):
    return owner + "__" + re.sub(r"[^A-Za-z0-9._-]+", "_", cleaner_path(path)) + ".txt"


def cleaner_path(path):
    return PROJECT_CODE.sub("PROJ", path)


def save(folder):
    os.makedirs(folder, exist_ok=True)
    for old in os.listdir(folder):
        os.remove(os.path.join(folder, old))
    cleaner = Cleaner()
    count = 0

    def write(owner, path, status, text):
        nonlocal count
        with open(os.path.join(folder, file_name(owner, path)), "w") as f:
            f.write(f"HTTP {status}\n{text}")
        count += 1

    def visit(owner, client, paths, link_pattern=None):
        seen = set()
        queue = list(paths)
        while queue:
            path = queue.pop(0)
            if path in seen:
                continue
            seen.add(path)
            status, text, _ = fetch(client, path, cleaner)
            # the file name uses the cleaned code, so two runs give the same names
            write(owner, cleaner.clean(path), status, text)
            if link_pattern and status == 200:
                raw = client.get(path)[2].decode("utf-8", "replace")
                for m in link_pattern.finditer(raw):
                    queue.append(m.group(2))

    # ---- customers (no sign-in)
    pub = Client(PUBLIC)
    visit("customer", pub, ["/projects", "/projects?state=Chhattisgarh", "/projects?state=West+Bengal&city=Durgapur",
                            "/projects?search=Green", "/projects?pageNo=2", "/projects?pageNo=99", "/api/public/projects",
                            "/api/public/projects?size=3&page=1", "/api/public/projects?state=Chhattisgarh", "/api/public/meta",
                            "/project/UCR-PROJ-NOPE", "/this-page-does-not-exist"], PUBLIC_LINKS)

    # ---- every staff role
    for role, emp in STAFF:
        client = Client(INTERNAL)
        if not client.dev_login(emp):
            write("staff-" + role, "/admin/projects", "LOGIN REFUSED", "")
            continue
        first = ["/admin/projects", "/admin/projects/new", "/projects", "/api/admin/geo",
                 "/admin/leads", "/admin/leads?pageNo=2", "/admin/leads?pageNo=3", "/admin/leads?pageNo=99",
                 "/admin/leads?search=Rohit", "/admin/leads?search=9811100002", "/admin/leads?from=2020-01-01&to=2099-12-31",
                 "/admin/leads?from=not-a-date", "/admin/leads/export.xlsx", "/admin/leads/export.xlsx?search=Imran",
                 "/api/admin/leads", "/api/admin/leads/export.xlsx",
                 "/admin/audit-logs", "/admin/audit-logs?tab=login", "/admin/audit-logs?pageActivity=1", "/admin/audit-logs?tab=login&pageLogin=1"]
        # statuses offered by the leads page are part of the page itself; also open one status filter if present
        status, _, body = client.get("/admin/leads")
        if status == 200:
            m = re.search(r'<option value="([^"]+)"', body.decode("utf-8", "replace"))
            if m:
                first.append("/admin/leads?status=" + m.group(1))
        visit("staff-" + role, client, first, STAFF_LINKS)
        client.get("/logout")

    # ---- the sign-in pages seen by nobody
    anon = Client(INTERNAL)
    visit("staff-anonymous", anon, ["/login", "/otp", "/error-page", "/error-page?reason=unauthorized", "/error-page?reason=not_authorized",
                                    "/error-page?reason=invalid_employee", "/admin/projects", "/home", "/entry", "/projects"])
    print(f"saved {count} pages in {folder}")


def read_clean(path):
    """The saved page, with the changing values replaced again (so a folder saved by an older version of this script still compares)."""
    text = open(path).read()
    for pattern, word in VOLATILE:
        text = pattern.sub(word, text)
    return text.splitlines()


def compare(before, after):
    names = sorted(set(os.listdir(before)) | set(os.listdir(after)))
    different = 0
    for name in names:
        a, b = os.path.join(before, name), os.path.join(after, name)
        if not os.path.exists(a):
            print(f"NEW PAGE   {name}")
            different += 1
        elif not os.path.exists(b):
            print(f"MISSING    {name}")
            different += 1
        else:
            x, y = read_clean(a), read_clean(b)
            if x != y:
                different += 1
                print(f"DIFFERENT  {name}")
                for line in list(difflib.unified_diff(x, y, "before", "after", lineterm="", n=1))[:14]:
                    print("    " + line[:200])
    print(f"\n{len(names)} pages compared, {different} different")
    return different


if __name__ == "__main__":
    if len(sys.argv) == 3 and sys.argv[1] == "save":
        save(sys.argv[2])
    elif len(sys.argv) == 4 and sys.argv[1] == "compare":
        sys.exit(1 if compare(sys.argv[2], sys.argv[3]) else 0)
    else:
        sys.exit(__doc__)
