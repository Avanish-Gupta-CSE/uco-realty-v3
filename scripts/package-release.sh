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

# A production bundle must not carry any of the UAT / laptop settings. Every problem is listed, then the build stops.
if [ "$MODE" = "prod" ]; then
  STAFF_PROPS="$ROOT/UcoRealtyStaff/src/main/resources/application.properties"
  PUBLIC_PROPS="$ROOT/UcoRealty/src/main/resources/application.properties"
  PROBLEMS=0
  problem() { echo "!! $1"; PROBLEMS=1; }
  for props in "$STAFF_PROPS" "$PUBLIC_PROPS"; do
    name="$(basename "$(dirname "$(dirname "$(dirname "$(dirname "$props")")")")")"
    grep -q '^spring.profiles.active=h2' "$props" && problem "$name: delete the line spring.profiles.active=h2 (it switches the application to the laptop database)"
    grep -q 'CHANGE_ME_DB_PASSWORD' "$props" && problem "$name: put the real database password in place of CHANGE_ME_DB_PASSWORD"
  done
  grep -q '^uco.sso.dev-login-enabled=.*true' "$STAFF_PROPS" && problem "UcoRealtyStaff: set uco.sso.dev-login-enabled=false (the test login must be off)"
  grep -q '^uco.sso.test-otp=' "$STAFF_PROPS" && problem "UcoRealtyStaff: delete the line uco.sso.test-otp (the test one-time password)"
  grep -q 'UCOBANKSSOKEY2026' "$STAFF_PROPS" && problem "UcoRealtyStaff: replace the UAT SSO key (sso.encryption.key) by the production key"
  grep -E '^uco.rbac.superadmin-sols=' "$STAFF_PROPS" | grep -q '7103' && problem "UcoRealtyStaff: uco.rbac.superadmin-sols must be only 7115 (7103 is the UAT DIT posting)"
  [ "$PROBLEMS" = "0" ] || { echo "Fix the lines above, then run this script again."; exit 1; }
fi

echo "==> Building both WARs"
# shellcheck disable=SC2086
for m in UcoRealty UcoRealtyStaff; do
  (cd "$ROOT/$m" && mvn -q ${MVN_OPTS:-} clean package -DskipTests)
done

PUBLIC_WAR="$ROOT/UcoRealty/target/UcoRealty.war"
INTERNAL_WAR="$ROOT/UcoRealtyStaff/target/UcoRealtyStaff.war"
[ -f "$PUBLIC_WAR" ] && [ -f "$INTERNAL_WAR" ] || { echo "!! build did not produce both WARs"; exit 1; }

echo "==> Assembling $OUT"
mkdir -p "$OUT/db"
# The WAR file name becomes the URL path on an external Tomcat (UcoRealtyStaff.war -> /UcoRealtyStaff),
# so ship them without the version number.
cp "$PUBLIC_WAR" "$OUT/UcoRealty.war"
cp "$INTERNAL_WAR" "$OUT/UcoRealtyStaff.war"
cp "$ROOT"/db_migrations/*.sql "$ROOT"/db_migrations/README_DBA_DEPLOYMENT.md "$OUT/db/"
# 03 / 05 create stand-in HRMS tables (test employees and a fake LG_ENTRY_REVAMP with fake leads). Both UAT and production read the
# bank's real HRMS tables, which the stand-ins would hide: never ship them in a bundle.
rm -f "$OUT/db/03_CREATE_HRMS_MOCK_TABLES.sql" "$OUT/db/05_LOCAL_TEST_EMPLOYEES.sql"
# one-time upgrade scripts for old laptop databases: a new bank database only needs 01_CREATE_TABLES.sql
rm -f "$OUT/db/06_STAGING_MODEL.sql" "$OUT/db/07_DROP_UNUSED_TABLES.sql"
# the drop-everything script is for UAT resets only
[ "$MODE" = "prod" ] && rm -f "$OUT/db/00_DROP_ALL_TABLES.sql"
cp "$ROOT/docs/DEPLOYMENT.md" "$OUT/"
cp "$ROOT/docs/CONSTANTS_TO_CHANGE.md" "$OUT/"
cp "$ROOT/docs/ER_DIAGRAM.md" "$OUT/"
cp "$ROOT/UcoRealtyStaff/src/main/resources/db_migrations/01_CREATE_TABLES_AND_INDEXES_CLEAN.sql" "$OUT/db/"
(cd "$OUT" && find . -type f ! -name SHA256SUMS -print0 | sort -z | xargs -0 shasum -a 256 > SHA256SUMS)
(cd "$ROOT/release" && zip -qr "$NAME.zip" "$NAME")

echo "==> Done: $ROOT/release/$NAME.zip"
echo "    Check nothing private is inside:"
if unzip -l "$ROOT/release/$NAME.zip" | grep -Ei "brain|\.git/|node_modules|sample|demo"; then
  echo "!! private files found"; exit 1
else
  echo "    OK (no .brain, .git, node_modules, sample or demo data)"
fi
