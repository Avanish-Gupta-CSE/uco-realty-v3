
#!/usr/bin/env bash
# Builds and starts both applications on this machine, on Oracle in Docker (the closest copy of UAT we have).
#   scripts/dev-start.sh            creates our tables once (db_migrations 01) and the stand-in HRMS tables (03, 05), then starts both apps
#   scripts/dev-start.sh --reset    the same, wiping our Oracle tables first
# What a server reads from C:\Cert\<App>.properties is given here as environment variables (nothing secret or environment-specific
# is written in the WARs). The development SSO key is random and kept in .run/sso.key (tests that build tokens read it).
# Sign in: employees 55012, 54534, 58382, 52260, 68696 ... see docs/TESTING_GUIDE.md, OTP 123456.
set -euo pipefail

if [ -d "/opt/homebrew/opt/openjdk@25/libexec/openjdk.jdk/Contents/Home" ]; then
  export JAVA_HOME="/opt/homebrew/opt/openjdk@25/libexec/openjdk.jdk/Contents/Home"
  export PATH="$JAVA_HOME/bin:$PATH"
fi

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
RUN="$ROOT/.run"; mkdir -p "$RUN"
RESET=0
for arg in "$@"; do
  case "$arg" in --oracle) ;; --reset) RESET=1;; *) echo "Usage: $0 [--reset]"; exit 1;; esac   # --oracle is accepted for old habits: Oracle is the only mode
done

show() { printf '   \033[2m$ %s\033[0m\n' "$*"; }
say() { printf '\n\033[1m==> %s\033[0m\n' "$*"; }
die() { printf '\n\033[31mERROR: %s\033[0m\n' "$*" >&2; exit 1; }

"$ROOT/scripts/dev-stop.sh" >/dev/null 2>&1 || true

say "Build"
for m in UcoRealty UcoRealtyStaff; do
  show "(cd $m && mvn -q clean package -DskipTests)"
  (cd "$ROOT/$m" && mvn -q clean package -DskipTests)
done

say "Oracle in Docker"
SQ="docker exec -i uco-oracle-free sqlplus -s uco_realty/uco_realty_pwd@//localhost/FREEPDB1"
sql() { printf 'set heading off feedback off pagesize 0 verify off\n%s\nexit\n' "$1" | $SQ | tr -d ' \t\r\n'; }
run_file() { show "sqlplus uco_realty@FREEPDB1 @$1"; echo exit | $SQ "@/tmp/db_migrations/$1" 2>&1 | grep -i "ORA-" && die "$1 failed" || true; }
docker info >/dev/null 2>&1 || die "Docker is not running"
docker start uco-oracle-free >/dev/null 2>&1 || (cd "$ROOT" && docker compose up -d oracle >/dev/null 2>&1)
for i in $(seq 1 60); do
  [ "$(docker inspect -f '{{.State.Health.Status}}' uco-oracle-free 2>/dev/null)" = "healthy" ] && break
  [ "$i" = 60 ] && die "Oracle did not become healthy (docker logs uco-oracle-free)"
  sleep 10
done
docker cp "$ROOT/db_migrations" uco-oracle-free:/tmp/ >/dev/null
[ "$RESET" = 1 ] && run_file 00_DROP_ALL_TABLES.sql
[ "$(sql "select count(*) from user_tables where table_name='REALTY_PROJECTS';")" = "0" ] && run_file 01_CREATE_TABLES.sql
# The bank's HRMS tables belong to the database user HRMS_DBA (see util/DatabaseTables.java); our user only has SELECT on them.
# Locally we recreate that: a user HRMS_DBA owns the stand-in tables (03 + 05) and uco_realty may read them.
SYSQ="docker exec -i uco-oracle-free sqlplus -s system/uco_realty_pwd@//localhost/FREEPDB1"
HRMSQ="docker exec -i uco-oracle-free sqlplus -s HRMS_DBA/hrms_dba_pwd@//localhost/FREEPDB1"
if [ "$(printf 'set heading off feedback off pagesize 0\nselect count(*) from dba_tables where owner='"'"'HRMS_DBA'"'"' and table_name='"'"'EMPLOYEE_CONTACT_DETAILS'"'"';\nexit\n' | $SYSQ | tr -d ' \t\r\n')" = "0" ]; then
  show "sqlplus system: create user HRMS_DBA (owner of the stand-in HRMS tables)"
  printf 'CREATE USER HRMS_DBA IDENTIFIED BY hrms_dba_pwd QUOTA UNLIMITED ON USERS;\nGRANT CREATE SESSION, CREATE TABLE TO HRMS_DBA;\nexit\n' | $SYSQ >/dev/null 2>&1 || true
  for f in 03_CREATE_HRMS_MOCK_TABLES.sql 05_LOCAL_TEST_EMPLOYEES.sql; do
    show "sqlplus HRMS_DBA @$f"; echo exit | $HRMSQ "@/tmp/db_migrations/$f" 2>&1 | grep -i "ORA-" && die "$f failed" || true
  done
  printf 'GRANT SELECT ON HRMS_DBA.employee_contact_details TO uco_realty;\nGRANT SELECT ON HRMS_DBA.employee_tenure TO uco_realty;\nGRANT SELECT ON HRMS_DBA.LG_ENTRY_REVAMP TO uco_realty;\nexit\n' | $HRMSQ >/dev/null
fi
# A random development SSO key (16..32 characters), the same for every start of this checkout.
[ -s "$RUN/sso.key" ] || LC_ALL=C tr -dc 'A-Za-z0-9' < /dev/urandom | head -c 32 > "$RUN/sso.key"
ENV_ARGS=(
  SPRING_DATASOURCE_URL=jdbc:oracle:thin:@localhost:1521/FREEPDB1
  SPRING_DATASOURCE_USERNAME=uco_realty
  SPRING_DATASOURCE_PASSWORD=uco_realty_pwd
  SSO_ENCRYPTION_KEY="$(cat "$RUN/sso.key")"
  UCO_SSO_DEV_LOGIN_ENABLED=true
  UCO_SSO_TEST_OTP=123456
  UCO_RBAC_SUPERADMIN_SOLS=7115,7103
  UCO_HOMELOAN_BASE_URL=http://localhost:8080
)

say "Start"
start() {  # module, port, log name, text that means started, extra environment settings...
  local mod="$1" port="$2" logname="$3" trigger="$4"; shift 4
  show "java -Dserver.port=$port -jar $mod/target/$mod.war   (settings as environment variables; log: .run/$logname.log; log files: .run/logs/$mod)"
  ( cd "$ROOT"; env "${ENV_ARGS[@]}" UCO_LOG_DIR="$RUN/logs/$mod" "$@" nohup java -Dserver.port="$port" -jar "$ROOT/$mod/target/$mod.war" > "$RUN/$logname.log" 2>&1 < /dev/null & echo $! > "$RUN/$logname.pid" )
  for i in $(seq 1 90); do
    grep -q "$trigger" "$RUN/$logname.log" 2>/dev/null && return 0
    grep -q "Application run failed" "$RUN/$logname.log" 2>/dev/null && { tail -5 "$RUN/$logname.log"; die "$mod failed to start (see .run/$logname.log)"; }
    sleep 2
  done
  die "$mod did not start in 3 minutes (see .run/$logname.log)"
}
start UcoRealtyStaff 8081 internal "Started UcoRealtyStaffApplication"
start UcoRealty 8080 public "Started UcoRealtyApplication"

printf '\n\033[1mReady.\033[0m\n'
cat <<'MSG'
  Customer : http://localhost:8080/UcoRealty/projects
  Staff    : http://localhost:8081/UcoRealtyStaff/   (employees 55012, 54534, 58382, 52260, 68696 ... see docs/TESTING_GUIDE.md, OTP 123456)
  Logs     : .run/public.log, .run/internal.log
  Stop     : scripts/dev-stop.sh
MSG
