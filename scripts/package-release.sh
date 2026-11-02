#!/usr/bin/env bash
# Builds a clean hand-over bundle for the bank: executable WAR + DBA scripts + deployment guide.
# It deliberately leaves out .brain/, .git/, and all sample data.
# Usage: scripts/package-release.sh [uat|prod]      (default: prod)
set -euo pipefail

MODE="${1:-prod}"
case "$MODE" in uat|prod) ;; *) echo "Usage: $0 [uat|prod]"; exit 1;; esac

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
STAMP="$(date +%Y%m%d-%H%M)"
OUT="$ROOT/release/uco-realty-$MODE-$STAMP"

echo "==> Application: tests + war"
(cd "$ROOT" && mvn -q clean package)

echo "==> Assembling $OUT"
mkdir -p "$OUT/db"
cp "$ROOT/target/uco-realty-0.1.0.war" "$OUT/"
cp "$ROOT"/db_migrations/*.sql "$ROOT"/db_migrations/README_DBA_DEPLOYMENT.md "$OUT/db/"
[ "$MODE" = "prod" ] && rm -f "$OUT/db/00_DROP_ALL_TABLES.sql"
cp "$ROOT/DEPLOYMENT.md" "$OUT/"

(cd "$OUT" && find . -type f ! -name SHA256SUMS -print0 | sort -z | xargs -0 shasum -a 256 > SHA256SUMS)
(cd "$ROOT/release" && zip -qr "uco-realty-$MODE-$STAMP.zip" "uco-realty-$MODE-$STAMP")

echo "==> Done: $ROOT/release/uco-realty-$MODE-$STAMP.zip"
echo "    Check nothing private is inside:"
unzip -l "$ROOT/release/uco-realty-$MODE-$STAMP.zip" | grep -Ei "brain|\.git|node_modules|sample" && { echo "!! private files found"; exit 1; } || echo "    OK (no .brain, .git, node_modules or sample data)"
