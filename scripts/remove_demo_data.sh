#!/usr/bin/env bash
# Removes everything created by scripts/load_demo_data.py and scripts/workflow_test.py:
# projects whose name ends with "(Demo)" and the test builder "ZZ Workflow Builder" (live rows, staging rows, photos, audit rows) and demo leads.
# Your own users and projects are not touched. Works on the local Docker Oracle, or on the in-memory H2 when
# `scripts/dev-start.sh --h2` is running. Override Oracle credentials with DB_USER / DB_PASS if needed.
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
DB_USER="${DB_USER:-uco_realty}"; DB_PASS="${DB_PASS:-uco_realty_pwd}"
H2_JAR="$HOME/.m2/repository/com/h2database/h2/2.4.240/h2-2.4.240.jar"

STATEMENTS=(
"delete from leads where customer_name like 'Demo Customer %';"
"delete from audit_log where entity_type = 'PROJECT' and (entity_id in (select id from projects where project_name like '%(Demo)' or builder_name = 'ZZ Workflow Builder') or entity_id in (select project_id from project_staging where project_name like '%(Demo)' or builder_name = 'ZZ Workflow Builder'));"
"delete from project_staging where project_name like '%(Demo)' or builder_name = 'ZZ Workflow Builder';"
"delete from projects where project_name like '%(Demo)' or builder_name = 'ZZ Workflow Builder';"
"commit;"
)

if [ -f "$ROOT/.run/h2.pid" ] && lsof -nP -iTCP:9092 -sTCP:LISTEN >/dev/null 2>&1; then
  printf '%s\n' "${STATEMENTS[@]}" > "$ROOT/.run/h2-remove-demo.sql"
  java -cp "$H2_JAR" org.h2.tools.RunScript -url "jdbc:h2:tcp://localhost:9092/mem:ucorealty;MODE=Oracle;DB_CLOSE_DELAY=-1" -user sa -script "$ROOT/.run/h2-remove-demo.sql"
else
  { echo "set feedback on serveroutput on"; printf '%s\n' "${STATEMENTS[@]}"; echo "exit"; } | docker exec -i uco-oracle sqlplus -s "$DB_USER/$DB_PASS@//localhost/XEPDB1"
fi
echo "demo data removed"
