#!/usr/bin/env bash
# Removes everything created by scripts/load_demo_data.py and scripts/workflow_test.py:
# projects whose name ends with "(Demo)" and the test builder "ZZ Workflow Builder" (live rows, staging rows, photos, audit rows) and demo leads.
# Your own users and projects are not touched. Works on the local Docker Oracle. Override the credentials with DB_USER / DB_PASS if needed.
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
DB_USER="${DB_USER:-uco_realty}"; DB_PASS="${DB_PASS:-uco_realty_pwd}"

STATEMENTS=(
"delete from realty_audit_log where entity_type = 'PROJECT' and (entity_id in (select id from realty_projects where project_name like '%(Demo)' or builder_name = 'ZZ Workflow Builder') or entity_id in (select project_id from realty_project_staging where project_name like '%(Demo)' or builder_name = 'ZZ Workflow Builder'));"
"delete from realty_project_staging where project_name like '%(Demo)' or builder_name = 'ZZ Workflow Builder';"
"delete from realty_projects where project_name like '%(Demo)' or builder_name = 'ZZ Workflow Builder';"
"commit;"
)

{ echo "set feedback on serveroutput on"; printf '%s\n' "${STATEMENTS[@]}"; echo "exit"; } | docker exec -i uco-oracle-free sqlplus -s "$DB_USER/$DB_PASS@//localhost/FREEPDB1"
echo "demo data removed"
