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
NAME="uco-realty-$MODE-$STAMP"
OUT="$ROOT/release/$NAME"

# Java 25 is required; prefer the Homebrew one when present (same as dev-start.sh)
if [ -d "/opt/homebrew/opt/openjdk@25/libexec/openjdk.jdk/Contents/Home" ]; then
  export JAVA_HOME="/opt/homebrew/opt/openjdk@25/libexec/openjdk.jdk/Contents/Home"
  export PATH="$JAVA_HOME/bin:$PATH"
fi

echo "==> Building both WARs"
# shellcheck disable=SC2086
(cd "$ROOT" && mvn -q ${MVN_OPTS:-} clean package -DskipTests)

PUBLIC_WAR="$ROOT/uco-realty-public/target/uco-realty-public-0.1.0.war"
INTERNAL_WAR="$ROOT/uco-realty-internal/target/uco-realty-internal-0.1.0.war"
[ -f "$PUBLIC_WAR" ] && [ -f "$INTERNAL_WAR" ] || { echo "!! build did not produce both WARs"; exit 1; }

echo "==> Assembling $OUT"
mkdir -p "$OUT/db"
# The WAR file name becomes the URL path on an external Tomcat (uco-realty-internal.war -> /uco-realty-internal),
# so ship them without the version number.
cp "$PUBLIC_WAR" "$OUT/uco-realty-public.war"
cp "$INTERNAL_WAR" "$OUT/uco-realty-internal.war"
cp "$ROOT"/db_migrations/*.sql "$ROOT"/db_migrations/README_DBA_DEPLOYMENT.md "$OUT/db/"
if [ "$MODE" = "prod" ]; then
  # never ship destructive or test-only scripts to production
  rm -f "$OUT/db/00_DROP_ALL_TABLES.sql" "$OUT/db/03_CREATE_HRMS_MOCK_TABLES.sql" "$OUT/db/05_UAT_TEST_EMPLOYEES.sql"
fi
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
