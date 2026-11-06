#!/usr/bin/env python3
"""Proves the apps work under a context path (on Tomcat the WAR file name is the URL prefix:
/ucorealty-customer, /ucorealty-staff). It crawls the pages as several roles and fails when a page
contains a root-relative link/form/script URL that does NOT start with the context path, when a redirect
leaves the context path, or when the JS files or the public JSON API build such a URL.

    python3 scripts/context_path_check.py                       # dev defaults (h2 or uat profile runs under the context paths)
    PUBLIC_URL=http://host/ucorealty-customer INTERNAL_URL=http://host/ucorealty-staff python3 scripts/context_path_check.py
"""
import json
import os
import re
import sys
import urllib.parse

sys.path.insert(0, os.path.dirname(__file__))
from uco_http import Client, location

PUBLIC = os.environ.get("PUBLIC_URL", "http://localhost:8080/ucorealty-customer").rstrip("/")
INTERNAL = os.environ.get("INTERNAL_URL", "http://localhost:8081/ucorealty-staff").rstrip("/")

problems, pages = [], 0
URL_ATTR = re.compile(r'(?:href|src|action|data-[a-z-]*url)\s*=\s*"(/[^"]*)"', re.I)
JS_LITERAL = re.compile(r'''["'](/(?:api|admin|files|projects?|lead|login|logout|otp|sso|entry|assets)(?:[/?#][^"']*)?)["']''')


def ctx_of(base):
    return urllib.parse.urlparse(base).path.rstrip("/")


def audit_page(client, base, path, who):
    global pages
    ctx = ctx_of(base)
    s, h, body = client.get(path)
    pages += 1
    if s in (301, 302, 303, 307, 308):
        loc = location(h)
        target = urllib.parse.urlparse(loc).path if loc else ""
        if not loc.startswith("http") and not target.startswith(ctx + "/") and target != ctx:
            problems.append(f"[{who}] {path}: redirect leaves the context path -> {loc}")
        return None
    text = body.decode("utf-8", "replace")
    for m in URL_ATTR.finditer(text):
        url = m.group(1)
        if url.startswith("//"):
            continue
        if not (url == ctx or url.startswith(ctx + "/")):
            problems.append(f"[{who}] {path}: URL without context path: {url}")
    return text


def crawl(base, who, client, start, limit=60):
    ctx = ctx_of(base)
    todo, seen = list(start), set()
    while todo and len(seen) < limit:
        path = todo.pop(0)
        if path in seen:
            continue
        seen.add(path)
        text = audit_page(client, base, path, who)
        if not text:
            continue
        for m in re.finditer(r'href="(' + re.escape(ctx) + r'/[^"#]*)"', text):
            link = m.group(1)[len(ctx):]
            if "logout" in link or link.endswith(".xlsx") or "/staging/" in link and "/decide" in link:
                continue
            link = link.replace("&amp;", "&")
            if link not in seen and len(todo) < 200:
                todo.append(link)
    return seen


def audit_assets(base, who):
    c = Client(base)
    s, _, html = c.get("/projects" if "customer" in base else "/login")
    refs = set(re.findall(r'src="[^"]*?(/assets/js/[a-z0-9_.-]+\.js)[^"]*"', html.decode("utf-8", "replace")))
    refs |= {"/assets/js/app.js"}
    for ref in sorted(refs):
        s, _, js = c.get(ref)
        if s != 200:
            continue
        for lineno, line in enumerate(js.decode("utf-8", "replace").splitlines(), 1):
            if line.strip().startswith(("//", "*", "/*")):
                continue
            for m in JS_LITERAL.finditer(line):
                before = line[:m.start()]
                if re.search(r'(context|ctx|contextPath|base)\s*\+\s*$', before.strip()) or "indexOf" in line or "pathname" in line or "startsWith" in line:
                    continue
                problems.append(f"[{who}] {ref}:{lineno}: root-relative literal {m.group(1)!r} (prefix it with the context path)")


# ---- customer site
pub = Client(PUBLIC)
crawl(PUBLIC, "customer", pub, ["/projects"])
s, _, body = pub.get("/api/public/projects?size=1")
try:
    items = json.loads(body).get("content") or json.loads(body).get("projects") or []
except Exception:
    items = []
if items:
    pid = items[0].get("id")
    s, _, body = pub.get(f"/api/public/projects/{pid}")
    for img in json.loads(body).get("images", []):
        if not img.startswith(ctx_of(PUBLIC) + "/"):
            problems.append(f"[customer] /api/public/projects/{pid}: image URL without context path: {img}")
audit_assets(PUBLIC, "customer")

# ---- staff site, several roles
for emp, label in (("200001", "maker"), ("200002", "checker"), ("400001", "retail"), ("100001", "dit")):
    c = Client(INTERNAL)
    if not c.dev_login(emp):
        problems.append(f"[{label}] could not sign in as {emp} (h2 or uat profile only)")
        continue
    start = ["/admin/projects", "/admin/projects/new"] + (["/admin/leads"] if label in ("retail", "dit") else []) + \
            (["/admin/audit-logs"] if label == "dit" else [])
    crawl(INTERNAL, label, c, start)
audit_assets(INTERNAL, "staff")
anon = Client(INTERNAL)
for p in ("/admin/projects", "/login", "/entry", "/error-page?reason=invalid_employee"):
    audit_page(anon, INTERNAL, p, "anonymous")

print(f"{pages} page fetches audited under {ctx_of(PUBLIC)} and {ctx_of(INTERNAL)}")
if problems:
    print("PROBLEMS:")
    for p in sorted(set(problems)):
        print("  -", p)
    sys.exit(1)
print("OK: every URL carries the context path")
