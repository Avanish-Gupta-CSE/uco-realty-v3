#!/usr/bin/env bash
# Takes a page snapshot from a CLEAN start, so two snapshots can be compared (see scripts/snapshot_pages.py).
#   scripts/snapshot_fresh.sh .run/snap-before      # stop, rebuild, start on the Docker Oracle, load demo data, save the snapshot
# Do it once before a code change and once after, then:
#   python3 scripts/snapshot_pages.py compare .run/snap-before .run/snap-after
set -euo pipefail
cd "$(dirname "$0")/.."
[ $# -eq 1 ] || { echo "usage: $0 <folder>"; exit 1; }
scripts/dev-stop.sh >/dev/null 2>&1 || true
scripts/dev-start.sh >/dev/null
python3 scripts/load_demo_data.py >/dev/null
python3 scripts/snapshot_pages.py save "$1"
