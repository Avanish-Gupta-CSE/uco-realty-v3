#!/usr/bin/env python3
"""Builds backend/src/main/resources/geo/districts.json (state -> districts) from the
India Post "All India Pincode Directory" CSV pages downloaded from data.gov.in.

Only state and district names are kept; no pincodes are stored.

Usage:  python3 -I scripts/build_geo_districts.py <csv file or folder of csv files> [output.json]
"""
import csv
import json
import re
import sys
from pathlib import Path

SMALL = {"and", "of"}


def tidy(name: str) -> str:
    """UPPER CASE -> Title Case, keeping 'and'/'of' small and brackets/hyphens tidy."""
    name = re.sub(r"\s+", " ", name.strip())
    words = []
    for i, w in enumerate(name.lower().split(" ")):
        if i > 0 and w in SMALL:
            words.append(w)
        else:
            words.append(re.sub(r"(^|[-(/.])([a-z])", lambda m: m.group(1) + m.group(2).upper(), w))
    return " ".join(words)


def state_name(raw: str) -> str:
    s = tidy(raw)
    s = re.sub(r"^The ", "", s)  # "The Dadra and Nagar Haveli and Daman and Diu"
    return s


def main() -> None:
    src = Path(sys.argv[1])
    out = Path(sys.argv[2]) if len(sys.argv) > 2 else Path("src/main/resources/geo/districts.json")
    files = sorted(src.glob("*.csv")) if src.is_dir() else [src]
    result: dict[str, dict[str, str]] = {}
    rows = skipped = 0
    for f in files:
        with open(f, encoding="utf-8", errors="replace", newline="") as fh:
            for r in csv.DictReader(fh):
                rows += 1
                st, di = (r.get("statename") or "").strip(), (r.get("district") or "").strip()
                if not st or not di or st.upper() in ("NA", "N/A") or di.upper() in ("NA", "N/A"):
                    skipped += 1
                    continue
                s, d = state_name(st), tidy(di)
                result.setdefault(s, {})[d.lower()] = d
    data = {s: sorted(ds.values()) for s, ds in sorted(result.items())}
    out.parent.mkdir(parents=True, exist_ok=True)
    out.write_text(json.dumps(data, indent=1, ensure_ascii=False) + "\n", encoding="utf-8")
    print(f"{len(files)} file(s), {rows} rows read, {skipped} skipped (no state/district)")
    print(f"{len(data)} states/UTs, {sum(len(v) for v in data.values())} districts -> {out}")


if __name__ == "__main__":
    main()
