#!/usr/bin/env bash
# Removes everything created by scripts/load_demo_data.py (projects whose name ends with "(Demo)", their files, audit rows and demo leads).
# Your own users and projects are not touched.  Defaults match the local Docker Oracle; override with DB_USER / DB_PASS if needed.
set -euo pipefail
DB_USER="${DB_USER:-uco_realty}"; DB_PASS="${DB_PASS:-uco_realty_pwd}"
printf '%s\n' "set feedback on serveroutput on" \
"delete from lead_transfer_log where lead_id in (select id from leads where customer_name like 'Demo Customer %');" \
"delete from leads where customer_name like 'Demo Customer %';" \
"delete from audit_log where entity_type = 'PROJECT' and entity_id in (select id from projects where project_name like '%(Demo)');" \
"delete from projects where project_name like '%(Demo)';" \
"commit;" "exit" | docker exec -i uco-oracle sqlplus -s "$DB_USER/$DB_PASS@//localhost/XEPDB1"
echo "demo data removed"
