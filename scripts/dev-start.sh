
#!/usr/bin/env bash
# Builds and starts both applications on this machine.
#   scripts/dev-start.sh                    profile from application.properties (h2): no Docker; the staff app creates all tables and test employees in memory
#   scripts/dev-start.sh --oracle           Oracle in Docker with the uat profile; creates the tables once (db_migrations 01, 03, 05)
#   scripts/dev-start.sh --oracle --reset   the same, wiping the Oracle tables first
# Sign in: employees 55012, 54534, 58382, 52260, 68696 ... see TESTING_GUIDE.md, OTP 123456.
set -euo pipefail

if [ -d "/opt/homebrew/opt/openjdk@25/libexec/openjdk.jdk/Contents/Home" ]; then
  export JAVA_HOME="/opt/homebrew/opt/openjdk@25/libexec/openjdk.jdk/Contents/Home"
  export PATH="$JAVA_HOME/bin:$PATH"
fi

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
RUN="$ROOT/.run"; mkdir -p "$RUN"
ORACLE=0; RESET=0
for arg in "$@"; do
  case "$arg" in --oracle) ORACLE=1;; --reset) RESET=1;; *) echo "Usage: $0 [--oracle [--reset]]"; exit 1;; esac
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

ENV_ARGS=()
if [ "$ORACLE" = 1 ]; then
  say "Oracle in Docker"
  SQ="docker exec -i uco-oracle sqlplus -s uco_realty/uco_realty_pwd@//localhost/XEPDB1"
  sql() { printf 'set heading off feedback off pagesize 0 verify off\n%s\nexit\n' "$1" | $SQ | tr -d ' \t\r\n'; }
  run_file() { show "sqlplus uco_realty@XEPDB1 @$1"; echo exit | $SQ "@/tmp/db_migrations/$1" 2>&1 | grep -i "ORA-" && die "$1 failed" || true; }
  docker info >/dev/null 2>&1 || die "Docker is not running"
  docker start uco-oracle >/dev/null 2>&1 || (cd "$ROOT" && docker compose up -d oracle >/dev/null 2>&1)
  for i in $(seq 1 60); do
    [ "$(docker inspect -f '{{.State.Health.Status}}' uco-oracle 2>/dev/null)" = "healthy" ] && break
    [ "$i" = 60 ] && die "Oracle did not become healthy (docker logs uco-oracle)"
    sleep 10
  done
  docker cp "$ROOT/db_migrations" uco-oracle:/tmp/ >/dev/null
  [ "$RESET" = 1 ] && run_file 00_DROP_ALL_TABLES.sql
  [ "$(sql "select count(*) from user_tables where table_name='REALTY_PROJECTS';")" = "0" ] && run_file 01_CREATE_TABLES.sql
  # The bank's HRMS tables belong to the database user HRMS_DBA (see util/DatabaseTables.java); our user only has SELECT on them.
  # Locally we recreate that: a user HRMS_DBA owns the stand-in tables (03 + 05) and uco_realty may read them.
  SYSQ="docker exec -i uco-oracle sqlplus -s system/uco_realty_pwd@//localhost/XEPDB1"
  HRMSQ="docker exec -i uco-oracle sqlplus -s HRMS_DBA/hrms_dba_pwd@//localhost/XEPDB1"
  if [ "$(printf 'set heading off feedback off pagesize 0\nselect count(*) from dba_tables where owner='"'"'HRMS_DBA'"'"' and table_name='"'"'EMPLOYEE_CONTACT_DETAILS'"'"';\nexit\n' | $SYSQ | tr -d ' \t\r\n')" = "0" ]; then
    show "sqlplus system: create user HRMS_DBA (owner of the stand-in HRMS tables)"
    printf 'CREATE USER HRMS_DBA IDENTIFIED BY hrms_dba_pwd QUOTA UNLIMITED ON USERS;\nGRANT CREATE SESSION, CREATE TABLE TO HRMS_DBA;\nexit\n' | $SYSQ >/dev/null 2>&1 || true
    for f in 03_CREATE_HRMS_MOCK_TABLES.sql 05_LOCAL_TEST_EMPLOYEES.sql; do
      show "sqlplus HRMS_DBA @$f"; echo exit | $HRMSQ "@/tmp/db_migrations/$f" 2>&1 | grep -i "ORA-" && die "$f failed" || true
    done
    printf 'GRANT SELECT ON HRMS_DBA.employee_contact_details TO uco_realty;\nGRANT SELECT ON HRMS_DBA.employee_tenure TO uco_realty;\nGRANT SELECT ON HRMS_DBA.LG_ENTRY_REVAMP TO uco_realty;\nexit\n' | $HRMSQ >/dev/null
  fi
  ENV_ARGS=(DB_URL=jdbc:oracle:thin:@localhost:1521/XEPDB1 DB_USER=uco_realty DB_PASS=uco_realty_pwd)
else
  say "H2 in-memory (local dev)"
  ENV_ARGS=(SPRING_PROFILES_ACTIVE=h2 DB_URL="jdbc:h2:tcp://localhost:9092/mem:ucorealty;MODE=Oracle;DB_CLOSE_DELAY=-1" DB_USER=sa DB_PASS="")
fi

say "Start"
start() {  # module, port, log name, text that means started, extra envs...
  local mod="$1" port="$2" logname="$3" trigger="$4"; shift 4
  show "${ENV_ARGS[*]:-} $* java -Dserver.port=$port -jar $mod/target/$mod.war   (log: .run/$logname.log)"
  ( cd "$ROOT"; env "${ENV_ARGS[@]+"${ENV_ARGS[@]}"}" "$@" nohup java -Dserver.port="$port" -jar "$ROOT/$mod/target/$mod.war" > "$RUN/$logname.log" 2>&1 < /dev/null & echo $! > "$RUN/$logname.pid" )
  for i in $(seq 1 90); do
    grep -q "$trigger" "$RUN/$logname.log" 2>/dev/null && return 0
    grep -q "Application run failed" "$RUN/$logname.log" 2>/dev/null && { tail -5 "$RUN/$logname.log"; die "$mod failed to start (see .run/$logname.log)"; }
    sleep 2
  done
  die "$mod did not start in 3 minutes (see .run/$logname.log)"
}
if [ "$ORACLE" = 1 ]; then
  start UcoRealtyStaff 8081 internal "Started InternalPortalApplication"
  start UcoRealty 8080 public "Started PublicPortalApplication"
else
  start UcoRealtyStaff 8081 internal "Started InternalPortalApplication" SPRING_SQL_INIT_SCHEMA_LOCATIONS=classpath:h2-init.sql SPRING_SQL_INIT_MODE=always
  start UcoRealty 8080 public "Started PublicPortalApplication"
fi

printf '\n\033[1mReady.\033[0m\n'
cat <<'MSG'
  Customer : http://localhost:8080/UcoRealty/projects
  Staff    : http://localhost:8081/UcoRealtyStaff/   (employees 55012, 54534, 58382, 52260, 68696 ... see TESTING_GUIDE.md, OTP 123456)
  Logs     : .run/public.log, .run/internal.log
  Stop     : scripts/dev-stop.sh
MSG
