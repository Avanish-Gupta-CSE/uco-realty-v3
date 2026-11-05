#!/usr/bin/env bash
# Builds a clean hand-over bundle for the bank: both WARs + DBA scripts + deployment guide + SHA-256 checksums.
# It deliberately leaves out .brain/, .git/ and all sample/demo data. No tests are run (the project has none).
# Usage: scripts/package-release.sh [uat|prod]      (default: prod)
#   MVN_OPTS="-o" scripts/package-release.sh uat    # extra Maven options, e.g. offline
set -euo pipefail

MODE="${1:-prod}"
case "$MODE" in uat|prod) ;; *) echo "Usage: $0 [uat|prod]"; exit 1;; esac

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
STAMP="$(date +%Y%m%d-%H%M)"
NAME="ucorealty-$MODE-$STAMP"
OUT="$ROOT/release/$NAME"

# Java 25 is required; prefer the Homebrew one when present (same as dev-start.sh)
if [ -d "/opt/homebrew/opt/openjdk@25/libexec/openjdk.jdk/Contents/Home" ]; then
  export JAVA_HOME="/opt/homebrew/opt/openjdk@25/libexec/openjdk.jdk/Contents/Home"
  export PATH="$JAVA_HOME/bin:$PATH"
fi

echo "==> Building both WARs"
# A UAT build also carries the H2 driver (Maven profile h2) so the same WAR runs with Oracle OR, with no database, on H2.
# A production build never contains H2.
H2_PROFILE=""; [ "$MODE" = "uat" ] && H2_PROFILE="-Ph2"
# shellcheck disable=SC2086
for m in ucorealty-customer ucorealty-staff; do
  (cd "$ROOT/$m" && mvn -q ${MVN_OPTS:-} $H2_PROFILE clean package -DskipTests)
done

PUBLIC_WAR="$ROOT/ucorealty-customer/target/ucorealty-customer.war"
INTERNAL_WAR="$ROOT/ucorealty-staff/target/ucorealty-staff.war"
[ -f "$PUBLIC_WAR" ] && [ -f "$INTERNAL_WAR" ] || { echo "!! build did not produce both WARs"; exit 1; }

echo "==> Assembling $OUT"
mkdir -p "$OUT/db"
# The WAR file name becomes the URL path on an external Tomcat (ucorealty-staff.war -> /ucorealty-staff),
# so ship them without the version number.
cp "$PUBLIC_WAR" "$OUT/ucorealty-customer.war"
cp "$INTERNAL_WAR" "$OUT/ucorealty-staff.war"
cp "$ROOT"/db_migrations/*.sql "$ROOT"/db_migrations/README_DBA_DEPLOYMENT.md "$OUT/db/"
# UAT and production read the bank's real HRMS tables: never ship the mock tables or local test employees
rm -f "$OUT/db/03_CREATE_HRMS_MOCK_TABLES.sql" "$OUT/db/05_LOCAL_TEST_EMPLOYEES.sql"
# one-time upgrade scripts for old laptop databases: a new bank database only needs 01_CREATE_TABLES.sql
rm -f "$OUT/db/06_STAGING_MODEL.sql" "$OUT/db/07_DROP_UNUSED_TABLES.sql"
# the drop-everything script is for UAT resets only
[ "$MODE" = "prod" ] && rm -f "$OUT/db/00_DROP_ALL_TABLES.sql"
cp "$ROOT/DEPLOYMENT.md" "$OUT/"

(cd "$OUT" && find . -type f ! -name SHA256SUMS -print0 | sort -z | xargs -0 shasum -a 256 > SHA256SUMS)
(cd "$ROOT/release" && zip -qr "$NAME.zip" "$NAME")

echo "==> Done: $ROOT/release/$NAME.zip"
echo "    Check nothing private is inside:"
if unzip -l "$ROOT/release/$NAME.zip" | grep -Ei "brain|\.git/|node_modules|sample|demo"; then
  echo "!! private files found"; exit 1
else
  echo "    OK (no .brain, .git, node_modules, sample or demo data)"
fi
