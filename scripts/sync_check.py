#!/usr/bin/env python3
"""Checks that the two applications (UcoRealty = customers, UcoRealtyStaff = staff) have not drifted apart.

    python3 scripts/sync_check.py

Read only: it opens source files and prints what is wrong. Exit code 0 = everything in sync.
Run it after any change that touches both applications (a new project field, a page that exists twice, a new JSON address).

What it checks (each check says why it exists):
 1. Files that must be IDENTICAL in both applications (there is no shared module on purpose, so we copy and compare).
 2. Every column the customer app reads (customer model/Project) also exists in the staff model (Project + ProjectFields).
 3. The page that exists twice (public-detail.jsp) have the same element ids, except the ids that
    belong to only one side (the staff preview banner, the customer Home Loan buttons ...). A page that gained a section in
    one copy only shows up here.
 4. Every JSON address (/api/...) found in the Java controllers is described in docs/openapi-*.yaml, and the files describe
    no address that does not exist any more.
 5. Amenities.parse (stored "Gym:Y" -> shown "Yes") is the same method in both applications.
"""
import os
import re
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
CUSTOMER = os.path.join(ROOT, "UcoRealty", "src", "main")
STAFF = os.path.join(ROOT, "UcoRealtyStaff", "src", "main")
JAVA = os.path.join("java", "com", "ucorealty")
JSP = os.path.join("webapp", "WEB-INF", "jsp")

problems = []


def read(path):
    with open(path, encoding="utf-8") as f:
        return f.read()


def check_identical():
    for name in ["config/H2ServerStarter.java", "config/SecurityHeadersFilter.java", "util/DocTypes.java"]:
        a = read(os.path.join(CUSTOMER, JAVA, name))
        b = read(os.path.join(STAFF, JAVA, name))
        if a != b:
            problems.append(f"1. {name} differs between the two applications: they must be identical (change both)")


def columns(path):
    return set(re.findall(r'@Column\(name\s*=\s*"([a-z_0-9]+)"', read(path)))


def check_columns():
    customer = columns(os.path.join(CUSTOMER, JAVA, "model", "Project.java"))
    staff = columns(os.path.join(STAFF, JAVA, "model", "Project.java")) | columns(os.path.join(STAFF, JAVA, "model", "ProjectFields.java"))
    for missing in sorted(customer - staff):
        problems.append(f"2. column '{missing}' is read by the customer app but is missing in the staff model (Project / ProjectFields)")
    customer = columns(os.path.join(CUSTOMER, JAVA, "model", "ProjectAttachment.java"))
    staff = columns(os.path.join(STAFF, JAVA, "model", "ProjectAttachment.java"))
    for missing in sorted(customer - staff):
        problems.append(f"2. column '{missing}' is read by the customer app but is missing in the staff model (ProjectAttachment)")


# element ids that exist on one side only, on purpose
ONLY_ONE_SIDE = {
    "public-detail.jsp": {
        "staff-preview-banner", "btn-preview-back-admin", "btn-preview-edit-project",                       # staff preview banner
        "card-apply-home-loan", "loan-card-heading", "loan-card-subheading", "btn-loan-apply-sidebar",      # Home Loan: customers only
        "bottom-apply-home-loan", "btn-loan-apply-bottom", "btn-loan-enquire-sidebar", "btn-loan-enquire-bottom",
    },
}


def element_ids(path):
    ids = set()
    for found in re.findall(r'\bid="([^"]+)"', read(path)):
        ids.add(re.sub(r"\$\{[^}]*\}|<c:out[^>]*>", "*", found))   # id="photo-${img.id}" -> photo-*
    return ids


def check_pages():
    for page, allowed in ONLY_ONE_SIDE.items():
        a = element_ids(os.path.join(CUSTOMER, JSP, page))
        b = element_ids(os.path.join(STAFF, JSP, page))
        for only_customer in sorted(a - b - allowed):
            problems.append(f"3. {page}: element id '{only_customer}' exists in the customer copy only")
        for only_staff in sorted(b - a - allowed):
            problems.append(f"3. {page}: element id '{only_staff}' exists in the staff copy only")


def api_addresses(module):
    """('GET', '/api/public/projects') for every JSON address of one application (class mapping + method mapping)."""
    found = set()
    base_dir = os.path.join(module, JAVA, "controller")
    for name in sorted(os.listdir(base_dir)):
        text = read(os.path.join(base_dir, name))
        base = re.search(r'@RequestMapping\("([^"]*)"\)\s*public class', text)
        prefix = base.group(1) if base else ""
        for path in re.findall(r'@GetMapping\((?:value\s*=\s*)?"([^"]*)"\)', text) + (["" ] if re.search(r"@GetMapping\s*\n|@GetMapping\s*public", text) else []):
            full = prefix + path
            if full.startswith("/api/"):
                found.add(full)
    return found


def openapi_paths(path):
    """The addresses written under 'paths:' in an OpenAPI file (two-space indented keys that start with /)."""
    if not os.path.exists(path):
        return None
    return set(re.findall(r"^  (/[^\s:]*):\s*$", read(path), re.M))


def check_openapi():
    for module, yaml_name in [(CUSTOMER, "openapi-customer.yaml"), (STAFF, "openapi-staff.yaml")]:
        code = api_addresses(module)
        described = openapi_paths(os.path.join(ROOT, "docs", yaml_name))
        if described is None:
            problems.append(f"4. docs/{yaml_name} is missing")
            continue
        for path in sorted(code - described):
            problems.append(f"4. {path} exists in the code but is not described in docs/{yaml_name}")
        for path in sorted(described - code):
            problems.append(f"4. docs/{yaml_name} describes {path}, which does not exist in the code")


def parse_method(path):
    text = read(path)
    start = text.index("public static Map<String, String> parse(")
    return text[start:text.index("\n  }\n", start)]


def icons_block(path):
    text = read(path)
    start = text.index("public static final Map<String, String> ICONS")
    return text[start:text.index("DEFAULT_ICON", start)]


def check_amenities():
    """Amenities.parse turns the stored text (Gym:Y,Lift:N) into Yes/No for the pages. Both apps must give the same words."""
    a = parse_method(os.path.join(CUSTOMER, JAVA, "util", "Amenities.java"))
    b = parse_method(os.path.join(STAFF, JAVA, "util", "Amenities.java"))
    ia = icons_block(os.path.join(CUSTOMER, JAVA, "util", "Amenities.java"))
    ib = icons_block(os.path.join(STAFF, JAVA, "util", "Amenities.java"))
    if ia != ib:
        problems.append("5. Amenities.ICONS (the emoji of each amenity) differs between the two applications")
    if a != b:
        problems.append("5. Amenities.parse differs between the two applications: the pages of both expect the words Yes / No")


for check in (check_identical, check_columns, check_pages, check_openapi, check_amenities):
    check()

if problems:
    print("NOT IN SYNC:")
    for p in problems:
        print("  - " + p)
    sys.exit(1)
print("OK: both applications are in sync (identical files, columns, page element ids, JSON addresses, amenity words)")
