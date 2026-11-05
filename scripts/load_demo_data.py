#!/usr/bin/env python3
"""OPTIONAL demo data so you can see how the screens look. NOT part of the product. LOCAL DEV PROFILE ONLY.

Creates 9 fictional projects (names end with "(Demo)") through the normal web forms (staging copy -> checker approval), signing in as the local-profile
test employees: makers 200001 (Ajmer) and 300001 (Bhopal) create and submit, checkers 200002 / 300002 decide.
Result: 6 approved (with photos from .brain/07_REFERENCE/sample-uploads), 1 waiting for the checker, 1 returned for correction,
1 draft, plus 9 demo leads.

    python3 scripts/load_demo_data.py
    INTERNAL_URL=http://localhost:8081/ucorealty-staff python3 scripts/load_demo_data.py

Remove it again with scripts/remove_demo_data.sh. Everything here is invented (names, RERA numbers, phones).
It refuses to run unless the internal portal offers the dev employee-id login, so it cannot touch UAT or production.
"""
import os
import sys
from datetime import date, timedelta
from pathlib import Path

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from uco_http import Client, location  # noqa: E402

BASE = os.environ.get("INTERNAL_URL", "http://localhost:8081/ucorealty-staff")
PHOTOS = Path(__file__).resolve().parent.parent / ".brain" / "07_REFERENCE" / "sample-uploads" / "UCR-PROJ-0001"

CREW = {  # zone -> (maker, checker)
    "Ajmer": ("200001", "200002"),
    "Bhopal": ("300001", "300002"),
}

# (name, builder, zone, state, city, pincode, units, fate)
PROJECTS = [
    ("Green Heights (Demo)", "Shree Builders", "Ajmer", "Rajasthan", "Ajmer", "305001", 120, "approved"),
    ("Aravali Residency (Demo)", "Marudhar Infra", "Ajmer", "Rajasthan", "Pushkar", "305022", 80, "approved"),
    ("Lakeview Towers (Demo)", "Sunrise Realty", "Ajmer", "Rajasthan", "Beawar", "305901", 200, "approved"),
    ("Sanchi Greens (Demo)", "Vindhya Developers", "Bhopal", "Madhya Pradesh", "Bhopal", "462001", 150, "approved"),
    ("Kolar Meadows (Demo)", "Narmada Homes", "Bhopal", "Madhya Pradesh", "Bhopal", "462042", 96, "approved"),
    ("Upper Lake Heights (Demo)", "Ganga Constructions", "Bhopal", "Madhya Pradesh", "Sehore", "466001", 60, "approved"),
    ("Ana Sagar Court (Demo)", "Chauhan Estates", "Ajmer", "Rajasthan", "Ajmer", "305002", 48, "pending"),
    ("Taragarh View (Demo)", "Pearl Builders", "Bhopal", "Madhya Pradesh", "Bhopal", "462016", 72, "correction"),
    ("Dargah Plaza (Demo)", "Heritage Infra", "Ajmer", "Rajasthan", "Ajmer", "305003", 36, "draft"),
]


def form(i, name, builder, state, city, pin, units):
    today = date.today()
    return {
        "projectName": name, "builderName": builder, "branchName": f"{city} Main Branch", "stateName": state, "cityName": city, "pinCode": pin,
        "projectAddress": f"Plot {10 + i}, Demo Nagar, {city}", "reraNo": f"DEMO/RERA/{1000 + i}",
        "typeOfUnit": "2BHK / 3BHK", "totalInventory": str(units), "unsoldInventory": str(int(units * 0.6)),
        "inventoryAsOnDate": (today - timedelta(days=5)).isoformat(),
        "approvalDate": (today - timedelta(days=60)).isoformat(),
        "validUpto": (today + timedelta(days=700)).isoformat(),
        "startDate": (today - timedelta(days=400)).isoformat(),
        "completionDate": (today + timedelta(days=500)).isoformat(),
        "carpetMin": "650 sq.ft.", "carpetMax": "1100 sq.ft.", "buildupMin": "800 sq.ft.", "buildupMax": "1350 sq.ft.",
        "budgetMin": "32 L", "budgetMax": "68 L", "avgRate": "4800 per sq.ft.", "totalProjectArea": "3.5 acres",
        "distRailway": "4 km", "distAirport": "22 km", "aptPerFloor": "4", "noOfTowers": "3",
        "amenities": "Park, Gym, Clubhouse, 24x7 security, Power backup",
        "builderBg": f"{builder} has delivered residential projects in the region for over 15 years.",
        "builderRating": "4.2", "website": "https://example.com", "salesExecName": "Demo Sales Executive",
        "salesMobile": "9000000000", "salesEmail": "sales@example.com",
        "branchHeadName": "Demo Branch Head", "branchHeadNumber": "9000000001",
        "relExecName": "Demo Relationship Executive", "relExecNumber": "9000000002",
    }


def photos(n=3):
    files = []
    for k, p in enumerate(sorted(PHOTOS.glob("photo-*.jpg"))[:n], start=1):
        files.append((f"photo{k}", p.name, p.read_bytes(), "image/jpeg"))
    return files


def newest_project_id(client, name):
    import re
    html = client.get(f"/admin/projects?search={name.split(' (')[0].replace(' ', '+')}")[2].decode("utf-8", "replace")
    m = re.search(r"UCR-PROJ-[0-9A-F]{8}", html)
    return m.group(0) if m else None


def staging_id(client, pid):
    """The id of the maker's staging request behind a dashboard row (from its action links)."""
    import re
    html = client.get(f"/admin/projects?search={pid}")[2].decode("utf-8", "replace")
    m = re.search(r"/admin/projects/staging/(\d+)/", html)
    return int(m.group(1)) if m else None


def main():
    probe = Client(BASE)
    s, _, body = probe.get("/login")
    if s != 200 or b"ssoOtpForm" not in body:
        sys.exit("Refusing to run: the dev employee-id login is not available here. "
                 "This script is for the local local profile only.")

    sessions = {}
    for zone, (maker, checker) in CREW.items():
        mk, ck = Client(BASE), Client(BASE)
        if not (mk.dev_login(maker) and ck.dev_login(checker)):
            sys.exit(f"Could not sign in test employees {maker}/{checker}. Is the local profile running?")
        sessions[zone] = (mk, ck)

    approved = 0
    for i, (name, builder, zone, state, city, pin, units, fate) in enumerate(PROJECTS, start=1):
        mk, ck = sessions[zone]
        fields = form(i, name, builder, state, city, pin, units)
        fields["_csrf"] = mk.csrf("/admin/projects/new")
        fields["action"] = "draft" if fate == "draft" else "submit"
        files = photos(3 if fate in ("approved", "pending") else 1)
        s, h, _ = mk.multipart_post("/admin/projects/staging/save", fields, files)
        if s != 302:
            sys.exit(f"Saving '{name}' failed (HTTP {s}); the form probably showed a validation error.")
        pid = newest_project_id(mk, name)
        if fate in ("approved", "correction"):
            if not pid:
                sys.exit(f"Could not find the saved project '{name}' in the queue.")
            sid = staging_id(mk, pid)
            decision = "approve" if fate == "approved" else "needs-correction"
            comment = "Verified against the sanctioned plan." if fate == "approved" else "Please upload the latest RERA certificate."
            s, _, _ = ck.post(f"/admin/projects/staging/{sid}/decide",
                              {"decision": decision, "comment": comment, "_csrf": ck.csrf("/admin/projects")})
            if s != 302:
                sys.exit(f"Decision on '{name}' failed (HTTP {s}).")
            approved += fate == "approved"
        print(f"  {fate:10} {name}  ({pid or '-'})")

    anon = Client(BASE)
    for n in range(1, 10):
        anon.post("/lead", {"name": f"Demo Customer {n}", "phone": f"98000000{n:02d}", "status": "Visitor"},
                  headers={"X-Requested-With": "XMLHttpRequest"})
    print(f"\nDone: {approved} approved, 1 pending, 1 returned, 1 draft, 9 demo leads.")
    print("Remove with scripts/remove_demo_data.sh")


if __name__ == "__main__":
    main()
