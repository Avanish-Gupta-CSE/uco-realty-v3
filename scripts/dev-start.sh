#!/usr/bin/env bash
# Builds and starts both applications on this machine.
#   scripts/dev-start.sh                    profile from application.properties (h2): no Docker; the staff app creates all tables and test employees in memory
#   scripts/dev-start.sh --oracle           Oracle in Docker with the uat profile; creates the tables once (db_migrations 01, 03, 05)
#   scripts/dev-start.sh --oracle --reset   the same, wiping the Oracle tables first
# Sign in: employee 100001 to 600002, OTP 123456.
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
for m in ucorealty-customer ucorealty-staff; do
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
  [ "$(sql "select count(*) from user_tables where table_name='PROJECTS';")" = "0" ] && run_file 01_CREATE_TABLES.sql
  if [ "$(sql "select count(*) from user_tables where table_name='EMPLOYEE_CONTACT_DETAILS';")" = "0" ]; then
    run_file 03_CREATE_HRMS_MOCK_TABLES.sql
    run_file 05_LOCAL_TEST_EMPLOYEES.sql
  fi
  ENV_ARGS=(SPRING_PROFILES_ACTIVE=uat DB_URL=jdbc:oracle:thin:@localhost:1521/XEPDB1 DB_USER=uco_realty DB_PASS=uco_realty_pwd)
fi

say "Start"
start() {  # module, port, log name, text that means started
  show "${ENV_ARGS[*]:-} java -Dserver.port=$2 -jar $1/target/$1.war   (log: .run/$3.log)"
  ( cd "$ROOT"; env "${ENV_ARGS[@]+"${ENV_ARGS[@]}"}" nohup java -Dserver.port="$2" -jar "$ROOT/$1/target/$1.war" > "$RUN/$3.log" 2>&1 < /dev/null & echo $! > "$RUN/$3.pid" )
  for i in $(seq 1 90); do
    grep -q "$4" "$RUN/$3.log" 2>/dev/null && return 0
    grep -q "Application run failed" "$RUN/$3.log" 2>/dev/null && { tail -5 "$RUN/$3.log"; die "$1 failed to start (see .run/$3.log)"; }
    sleep 2
  done
  die "$1 did not start in 3 minutes (see .run/$3.log)"
}
start ucorealty-staff 8081 internal "Started InternalPortalApplication"
start ucorealty-customer 8080 public "Started PublicPortalApplication"

printf '\n\033[1mReady.\033[0m\n'
cat <<'MSG'
  Customer : http://localhost:8080/ucorealty-customer/projects
  Staff    : http://localhost:8081/ucorealty-staff/   (employee 100001 to 600002, OTP 123456)
  Logs     : .run/public.log, .run/internal.log
  Stop     : scripts/dev-stop.sh
MSG
