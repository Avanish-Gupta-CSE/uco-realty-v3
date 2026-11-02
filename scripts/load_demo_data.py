#!/usr/bin/env python3
"""OPTIONAL demo data so you can see how the screens look with real-looking content. NOT part of the product.

Loads 9 fictional projects (names end with "(Demo)") through the normal API, the same way staff would:
photos from .brain/sample-uploads, six approved, one waiting for the checker, one returned
for correction, one draft; plus 9 demo leads.

The projects are placed in the zones of the ZO users that already exist (ZO_MAKER / ZO_CHECKER), so each of
your makers and checkers has something to work on: a pending project to approve, a returned one to fix, a
draft to submit. If there are no zone users yet, a default set of zones is used.
It uses dit.superadmin as author and a temporary checker that is deleted again at the end, so no extra login
is left behind and no other password is needed.

    python3 scripts/load_demo_data.py         # asks for the dit.superadmin password
    DIT_PASSWORD=... BASE_URL=http://localhost:8080 python3 scripts/load_demo_data.py

Remove everything again with scripts/remove_demo_data.sh. Everything here is invented: names, RERA numbers,
phone numbers and addresses. Never run this against UAT or production.
"""
import getpass
import json
import os
import secrets
import sys
import urllib.error
import urllib.request
import uuid
from pathlib import Path

BASE = os.environ.get("BASE_URL", "http://localhost:8080")
PHOTOS = Path(__file__).resolve().parent.parent / ".brain" / "sample-uploads" / "UCR-PROJ-0001"


def call(method, path, token=None, body=None, raw=None, ctype=None):
    headers, data = {}, None
    if token:
        headers["Authorization"] = f"Bearer {token}"
    if body is not None:
        data, headers["Content-Type"] = json.dumps(body).encode(), "application/json"
    if raw is not None:
        data, headers["Content-Type"] = raw, ctype
    req = urllib.request.Request(BASE + path, data=data, headers=headers, method=method)
    try:
        with urllib.request.urlopen(req, timeout=60) as r:
            payload, status = r.read(), r.status
    except urllib.error.HTTPError as e:
        payload, status = e.read(), e.code
    except urllib.error.URLError as e:
        sys.exit(f"Cannot reach {BASE}: {e.reason}. Is the app running (scripts/dev-start.sh)?")
    try:
        return status, json.loads(payload) if payload else None
    except ValueError:
        return status, payload


def must(label, result, ok=(200,)):
    status, body = result
    if status not in ok:
        sys.exit(f"FAILED: {label} -> HTTP {status} {body}")
    return body


def upload(pid, token, names):
    boundary, out = uuid.uuid4().hex, b""
    for name in names:
        path = PHOTOS / name
        ctype = "image/jpeg"
        out += (f'--{boundary}\r\nContent-Disposition: form-data; name="files"; filename="{name}"\r\n'
                f"Content-Type: {ctype}\r\n\r\n").encode() + path.read_bytes() + b"\r\n"
    out += f'--{boundary}\r\nContent-Disposition: form-data; name="kind"\r\n\r\nphoto\r\n--{boundary}--\r\n'.encode()
    must(f"upload files to {pid}", call("POST", f"/api/admin/projects/{pid}/files", token, raw=out,
                                         ctype=f"multipart/form-data; boundary={boundary}"))


P = ["photo-1.jpg", "photo-2.jpg", "photo-3.jpg", "photo-4.jpg"]
AMEN = "Clubhouse, Swimming Pool, Gymnasium, Landscaped Garden, 24x7 Security, Children's Play Area, Power Backup"

# Where projects can be placed, by zone: (state, district/city, [(area, pincode), ...])
CATALOG = {
    "Ajmer": ("Rajasthan", "Ajmer", [("Vaishali Nagar", "305004"), ("Civil Lines", "305001"), ("Pushkar Road", "305001")]),
    "Bhopal": ("Madhya Pradesh", "Bhopal", [("Arera Colony", "462016"), ("MP Nagar", "462011"), ("Kolar Road", "462042")]),
    "Kolkata": ("West Bengal", "Kolkata", [("New Town, Rajarhat", "700156"), ("Salt Lake", "700091"), ("Garia", "700084")]),
    "Mumbai": ("Maharashtra", "Mumbai Suburban", [("Andheri West", "400053"), ("Powai", "400076"), ("Borivali East", "400066")]),
    "Pune": ("Maharashtra", "Pune", [("Hinjewadi Phase 1", "411057"), ("Kothrud", "411038"), ("Wakad", "411057")]),
    "Bangalore": ("Karnataka", "Bengaluru Urban", [("Whitefield", "560066"), ("Koramangala", "560034"), ("Hebbal", "560024")]),
    "Jaipur": ("Rajasthan", "Jaipur", [("Mansarovar", "302020"), ("Vaishali Nagar", "302021"), ("Malviya Nagar", "302017")]),
    "Lucknow": ("Uttar Pradesh", "Lucknow", [("Gomti Nagar", "226010"), ("Aliganj", "226024"), ("Indira Nagar", "226016")]),
    "Bhubaneswar": ("Odisha", "Khordha", [("Patia", "751024"), ("Saheed Nagar", "751007"), ("Nayapalli", "751012")]),
    "New Delhi": ("Delhi", "South West", [("Dwarka", "110075"), ("Vasant Kunj", "110070"), ("Janakpuri", "110058")]),
    "Hyderabad": ("Telangana", "Hyderabad", [("Gachibowli", "500032"), ("Kukatpally", "500072"), ("Madhapur", "500081")]),
    "Chennai": ("Tamil Nadu", "Chennai", [("Velachery", "600042"), ("Anna Nagar", "600040"), ("Adyar", "600020")]),
    "Patna": ("Bihar", "Patna", [("Boring Road", "800001"), ("Kankarbagh", "800020"), ("Rajendra Nagar", "800016")]),
    "Ahmedabad": ("Gujarat", "Ahmedabad", [("Satellite", "380015"), ("Bopal", "380058"), ("Navrangpura", "380009")]),
}
DEFAULT_ZONES = ["Kolkata", "Mumbai", "Pune", "Bangalore", "Jaipur", "Bhubaneswar", "Lucknow"]

# (name, builder, type, total, unsold, price min, price max, rate, photos, status, (towers, floors, aptPerFloor, acres, carpet min/max, built-up min/max))
TEMPLATES = [
    ("Green Heights (Demo)", "Demo Builders Pvt Ltd", "Residential Apartments", 240, 86, "38.0 L", "72.0 L", "5,800 / sq.ft.", P, "APPROVED", (3, 14, 6, "4.2 Acres", "520 sq.ft.", "980 sq.ft.", "650 sq.ft.", "1,220 sq.ft.")),
    ("Lakeview Residency (Demo)", "Sample Realty Ltd", "Residential Apartments", 180, 42, "1.10 Cr", "2.40 Cr", "21,500 / sq.ft.", [P[1], P[0], P[3]], "APPROVED", (2, 22, 4, "2.1 Acres", "480 sq.ft.", "1,050 sq.ft.", "600 sq.ft.", "1,300 sq.ft.")),
    ("Skyline Enclave (Demo)", "Example Constructions", "Residential Apartments", 320, 128, "52.0 L", "95.0 L", "7,400 / sq.ft.", [P[2], P[3], P[0], P[1]], "APPROVED", (4, 18, 5, "6.0 Acres", "560 sq.ft.", "1,100 sq.ft.", "700 sq.ft.", "1,380 sq.ft.")),
    ("Orchid Park (Demo)", "Demo Builders Pvt Ltd", "Independent Floors", 96, 31, "78.0 L", "1.35 Cr", "8,900 / sq.ft.", [P[3], P[2]], "APPROVED", (1, 4, 4, "1.8 Acres", "900 sq.ft.", "1,650 sq.ft.", "1,100 sq.ft.", "2,000 sq.ft.")),
    ("Sunrise Homes (Demo)", "Sample Realty Ltd", "Residential Apartments", 150, 64, "29.0 L", "58.0 L", "4,300 / sq.ft.", [], "APPROVED", (2, 12, 6, "3.0 Acres", "480 sq.ft.", "900 sq.ft.", "600 sq.ft.", "1,120 sq.ft.")),
    ("Temple Gardens (Demo)", "Example Constructions", "Luxury Villas", 48, 9, "1.20 Cr", "2.10 Cr", "6,800 / sq.ft.", [P[0], P[2], P[1]], "APPROVED", (0, 2, 1, "8.0 Acres", "1,800 sq.ft.", "3,200 sq.ft.", "2,300 sq.ft.", "3,900 sq.ft.")),
    ("Riverside Towers (Demo)", "Demo Builders Pvt Ltd", "Residential Apartments", 200, 200, "44.0 L", "82.0 L", "5,600 / sq.ft.", [P[1], P[3]], "PENDING_APPROVAL", (3, 16, 4, "3.6 Acres", "540 sq.ft.", "1,000 sq.ft.", "680 sq.ft.", "1,250 sq.ft.")),
    ("Maple Greens (Demo)", "Sample Realty Ltd", "Residential Apartments", 80, 80, "24.0 L", "41.0 L", "3,600 / sq.ft.", [P[0]], "NEEDS_CORRECTION", (1, 10, 8, "1.4 Acres", "450 sq.ft.", "780 sq.ft.", "560 sq.ft.", "950 sq.ft.")),
    ("Banyan Court (Demo)", "Example Constructions", "Residential Apartments", 120, 120, "31.0 L", "55.0 L", "4,700 / sq.ft.", [], "DRAFT", (2, 9, 6, "2.2 Acres", "500 sq.ft.", "860 sq.ft.", "620 sq.ft.", "1,040 sq.ft.")),
]


def plan(zones):
    """Spread the templates over the zones (round robin) and give each its own area inside the zone."""
    used, out = {}, []
    for i, t in enumerate(TEMPLATES):
        zone = zones[i % len(zones)]
        state, city, areas = CATALOG[zone]
        area, pin = areas[used.get(zone, 0) % len(areas)]
        used[zone] = used.get(zone, 0) + 1
        out.append((i, zone, state, city, pin, area, t))
    return out


def body_for(i, zone, state, city, pin, area, t):
    name, builder, utype, total, unsold, pmin, pmax, rate, _, _, d = t
    towers, floors, apf, acres, cmin, cmax, bmin, bmax = d
    n = f"{i + 1:02d}"
    return {
        "projectName": name, "builderName": builder, "zoneName": zone, "stateName": state, "cityName": city, "pinCode": pin,
        "branchName": f"{area} Branch", "projectAddress": f"{area}, {city}, {state} - {pin}",
        "reraNo": f"DEMO/RERA/2026/{n}{n}{n}", "propertyId": f"DEMO-PROP-{n}", "typeOfUnit": utype,
        "approvalDate": "2026-08-14", "totalInventory": total, "unsoldInventory": unsold, "inventoryAsOnDate": "2026-10-01",
        "amenities": AMEN, "startDate": "2025-01-15", "completionDate": "2028-06-30",
        "aptPerFloor": apf or None, "noOfTowers": towers or None, "totalProjectArea": acres,
        "carpetMin": cmin, "carpetMax": cmax, "buildupMin": bmin, "buildupMax": bmax,
        "distRailway": "2.5 km", "distAirport": "14 km", "avgRate": f"₹ {rate}", "budgetMin": pmin, "budgetMax": pmax,
        "promoter1": f"{builder} (Promoter)", "salesExecName": "Demo Sales Executive", "salesMobile": f"98000 000{n}",
        "salesEmail": f"sales{n}@example.com", "website": "https://www.example.com",
        "builderRating": "4.5 / 5.0 (A Certified)" if i % 2 == 0 else "",
        "builderBg": f"{builder} is a fictional developer used only to show how this page looks. All details are invented.",
        "branchHeadName": "Demo Branch Head", "branchHeadNumber": "033-0000 0000", "relExecName": "Demo Relationship Executive",
        "relExecNumber": "98000 00099", "validUpto": "2029-03-31", "renewalPending": "No",
        "approvalSought": "Regular Approved", "propertyStatus": "Under Construction",
    }


def main():
    pw = os.environ.get("DIT_PASSWORD") or getpass.getpass("Password of dit.superadmin: ")
    s, b = call("POST", "/api/auth/login", body={"username": "dit.superadmin", "password": pw})
    if s != 200:
        sys.exit(f"Sign-in as dit.superadmin failed (HTTP {s}). Check the password.")
    dit = b["token"]
    existing = must("list projects", call("GET", "/api/admin/projects?scope=all", dit))
    if any("(Demo)" in (p.get("projectName") or "") for p in existing):
        sys.exit("Demo projects are already loaded. Run scripts/remove_demo_data.sh first if you want to reload them.")
    for ph in P:
        if not (PHOTOS / ph).exists():
            sys.exit(f"Missing sample file {PHOTOS / ph}")

    users = must("list users", call("GET", "/api/admin/users", dit))
    user_zones = sorted({u["zone"] for u in users if u["role"] in ("ZO_MAKER", "ZO_CHECKER") and u.get("zone")})
    zones = [z for z in user_zones if z in CATALOG]
    for z in user_zones:
        if z not in CATALOG:
            print(f"  (no demo locations for the {z} zone yet, skipped)")
    if zones:
        print(f"  Placing the projects in the zones of your existing users: {', '.join(zones)}")
    else:
        zones = DEFAULT_ZONES
        print(f"  No zone users found, using default zones: {', '.join(zones)}")

    checker_pw = "Dm" + secrets.token_urlsafe(10).replace("-", "x").replace("_", "y") + "7"
    must("create temporary checker", call("POST", "/api/admin/users", dit, {"username": "demo.checker", "password": checker_pw, "role": "HO_MARKETING"}))
    s, b = call("POST", "/api/auth/login", body={"username": "demo.checker", "password": checker_pw})
    checker = b["token"]

    try:
        for i, zone, state, city, pin, area, t in plan(zones):
            status, photos = t[9], t[8]
            created = must(f"create {t[0]}", call("POST", "/api/admin/projects", dit, body_for(i, zone, state, city, pin, area, t)))
            pid = created["id"]
            if photos:
                upload(pid, dit, photos)
            if status != "DRAFT":
                must("submit", call("POST", f"/api/admin/projects/{pid}/submit", dit))
            if status == "APPROVED":
                must("approve", call("POST", f"/api/admin/projects/{pid}/approve", checker, {"comment": "Demo data: documents verified."}))
            elif status == "NEEDS_CORRECTION":
                must("return", call("POST", f"/api/admin/projects/{pid}/needs-correction", checker, {"comment": "Demo data: please attach the RERA certificate and correct the pincode."}))
            print(f"  {status:17} {t[0]:28} {zone:12} {city:16} {pin}  {len(photos)} photos")
        # a few demo leads
        approved = [p for p in must("list", call("GET", "/api/public/projects?size=50"))["items"]]
        for k in range(1, 6):
            call("POST", "/api/public/leads", body={"name": f"Demo Customer {k}", "phone": f"900000000{k}", "status": "Visitor"})
        for k, item in enumerate(approved[:4], start=6):
            call("POST", "/api/public/leads", body={"name": f"Demo Customer {k}", "phone": f"90000000{k:02d}", "status": "Showed interest in Home Loan",
                                                    "projectId": item["id"]})
        print("  + 9 demo leads (5 visitors, 4 home-loan applications)")
    finally:
        for u in call("GET", "/api/admin/users", dit)[1] or []:
            if u["username"] == "demo.checker":
                call("DELETE", f"/api/admin/users/{u['id']}", dit)
    print("\nDone. Customer site: http://localhost:5173/   (6 approved projects are visible to customers)")
    print("Staff: the queue also shows one pending, one returned for correction and one draft. Remove with scripts/remove_demo_data.sh")


if __name__ == "__main__":
    main()
