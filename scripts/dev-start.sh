#!/usr/bin/env bash
# Starts UCO Realty locally: database -> tables -> both applications.
#   1. Customer site  ucorealty-customer (port 8080, internet-facing, read only)
#   2. Staff site     ucorealty-staff    (port 8081, intranet, SSO + maker-checker)
#
#   scripts/dev-start.sh                 Oracle in Docker (closest to UAT); keeps existing data
#   scripts/dev-start.sh --reset         wipe the Oracle tables first and start from zero
#   scripts/dev-start.sh --h2            NO Docker: in-memory H2 (Oracle mode) with the web console at http://localhost:8082
#                                        (JDBC URL jdbc:h2:tcp://localhost:9092/mem:ucorealty, user sa, empty password).
#                                        Data lives in the H2 server process: scripts/dev-stop.sh (without --apps-only) wipes it.
#   scripts/dev-start.sh --h2 --reset    fresh empty H2 database
#
# Staff sign in through SSO/OTP (local HRMS mode: employee 100001, OTP 123456). No local accounts exist.
set -euo pipefail

# Ensure Java 25 is prioritized
if [ -d "/opt/homebrew/opt/openjdk@25/libexec/openjdk.jdk/Contents/Home" ]; then
  export JAVA_HOME="/opt/homebrew/opt/openjdk@25/libexec/openjdk.jdk/Contents/Home"
  export PATH="$JAVA_HOME/bin:$PATH"
fi

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
RUN="$ROOT/.run"; mkdir -p "$RUN"
PUBLIC_WAR="$ROOT/ucorealty-customer/target/ucorealty-customer.war"
INTERNAL_WAR="$ROOT/ucorealty-staff/target/ucorealty-staff.war"
SQ="docker exec -i uco-oracle sqlplus -s uco_realty/uco_realty_pwd@//localhost/XEPDB1"
RESET=0; DB=oracle
for arg in "$@"; do
  case "$arg" in --reset) RESET=1;; --h2) DB=h2;; *) echo "Usage: $0 [--h2] [--reset]"; exit 1;; esac
done
PROFILE=local      # ONE profile for both databases: H2 is its default; for Oracle the apps get DB_URL/DB_USER/DB_PASS below
DB_ENV=""
if [ "$DB" = oracle ]; then DB_ENV="DB_URL=jdbc:oracle:thin:@localhost:1521/XEPDB1 DB_USER=uco_realty DB_PASS=uco_realty_pwd"; fi
H2_VERSION=2.4.240      # the version Spring Boot 4.1.2 manages
H2_JAR="$HOME/.m2/repository/com/h2database/h2/$H2_VERSION/h2-$H2_VERSION.jar"
H2_URL="jdbc:h2:tcp://localhost:9092/mem:ucorealty;MODE=Oracle;DB_CLOSE_DELAY=-1"

say() { printf '\n\033[1m==> %s\033[0m\n' "$*"; }
die() { printf '\n\033[31mERROR: %s\033[0m\n' "$*" >&2; exit 1; }
sql() { printf 'set heading off feedback off pagesize 0 verify off\n%s\nexit\n' "$1" | $SQ | tr -d ' \t\r\n'; }

for c in java mvn python3; do command -v "$c" >/dev/null || die "'$c' is not installed"; done
if [ "$DB" = oracle ]; then
  command -v docker >/dev/null || die "'docker' is not installed (or use: $0 --h2)"
  docker info >/dev/null 2>&1 || die "Docker is not running. Start Docker Desktop and run this again (or use: $0 --h2)."
fi

for p in 8080 8081; do
  if lsof -nP -iTCP:$p -sTCP:LISTEN >/dev/null 2>&1 && [ ! -f "$RUN/pids" ]; then
    die "Port $p is already in use by another program. Stop it first."
  fi
done

h2_running() { lsof -nP -iTCP:9092 -sTCP:LISTEN >/dev/null 2>&1; }

if [ "$DB" = h2 ]; then
say "1/4 H2 database server (in memory, no Docker)"
[ -f "$H2_JAR" ] || (cd "$ROOT" && mvn -q dependency:get -Dartifact=com.h2database:h2:$H2_VERSION) || die "Could not download H2"
if [ "$RESET" = 1 ] && h2_running; then
  [ -f "$RUN/h2.pid" ] && kill "$(cat "$RUN/h2.pid")" 2>/dev/null || true
  sleep 2
fi
if h2_running; then
  echo "H2 server already running (data kept)"
else
  ( cd "$ROOT"; nohup java -cp "$H2_JAR" org.h2.tools.Server -tcp -tcpPort 9092 -web -webPort 8082 -ifNotExists > "$RUN/h2-server.log" 2>&1 < /dev/null & echo $! > "$RUN/h2.pid" )
  for i in $(seq 1 20); do h2_running && break; sleep 1; done
  h2_running || die "H2 server did not start (see .run/h2-server.log)"
  echo "H2 server started: console http://localhost:8082"
fi
else
say "1/4 Oracle database"
if [ "$(docker inspect -f '{{.State.Health.Status}}' uco-oracle 2>/dev/null)" != "healthy" ]; then
  docker start uco-oracle >/dev/null 2>&1 || (cd "$ROOT" && docker compose up -d oracle >/dev/null 2>&1)
fi
for i in $(seq 1 60); do
  [ "$(docker inspect -f '{{.State.Health.Status}}' uco-oracle 2>/dev/null)" = "healthy" ] && break
  [ "$i" = 60 ] && die "Oracle did not become healthy in 10 minutes (docker logs uco-oracle)"
  printf '.'; sleep 10
done
echo " ready"
fi

say "2/4 Build applications (ucorealty-customer, ucorealty-staff)"
if [ "$DB" = h2 ]; then
  (cd "$ROOT" && mvn -q -Ph2 clean package -DskipTests)     # -Ph2 adds the H2 driver; the normal build never contains it
else
  (cd "$ROOT" && mvn -q clean package -DskipTests)
fi
[ -f "$PUBLIC_WAR" ] || die "Public portal build failed"
[ -f "$INTERNAL_WAR" ] || die "Internal portal build failed"

say "3/4 Database tables"
if [ "$DB" = h2 ]; then
  H2_SCRIPT="$RUN/h2-schema.sql"
  python3 "$ROOT/scripts/h2_schema.py" > "$H2_SCRIPT" || die "Could not generate the H2 schema"
  HAS=$(java -cp "$H2_JAR" org.h2.tools.Shell -url "$H2_URL" -user sa -sql "select count(*) from information_schema.tables where table_name='PROJECTS'" 2>/dev/null | sed -n 2p | tr -d ' ')
  if [ "$RESET" = 1 ] || [ "$HAS" != "1" ]; then
    java -cp "$H2_JAR" org.h2.tools.RunScript -url "$H2_URL" -user sa -script "$H2_SCRIPT" || die "Creating the H2 tables failed"
    echo "tables created (db_migrations/01_CREATE_TABLES.sql, converted for H2)"
  else
    echo "tables already exist"
  fi
else
docker cp "$ROOT/db_migrations" uco-oracle:/tmp/ >/dev/null
if [ "$RESET" = 1 ]; then
  "$ROOT/scripts/dev-stop.sh" --apps-only >/dev/null 2>&1 || true
  echo exit | $SQ @/tmp/db_migrations/00_DROP_ALL_TABLES.sql >/dev/null 2>&1
  echo "database wiped"
fi
TABLES=$(sql "select count(*) from user_tables;")
if [ "$TABLES" = "0" ]; then
  echo exit | $SQ @/tmp/db_migrations/01_CREATE_TABLES.sql 2>&1 | grep -i "ORA-\|error" && die "Creating tables failed"
  echo "tables created"
else
  echo "tables already exist ($TABLES)"
fi
fi

say "4/4 Start Applications"
if [ -f "$RUN/pids" ]; then "$ROOT/scripts/dev-stop.sh" --apps-only >/dev/null 2>&1 || true; fi

# Start Public Portal (8080)
( cd "$ROOT"; env SPRING_PROFILES_ACTIVE=$PROFILE $DB_ENV nohup java -Dserver.port=8080 -jar "$PUBLIC_WAR" > "$RUN/public.log" 2>&1 < /dev/null & echo $! > "$RUN/public.pid" )
for i in $(seq 1 60); do
  grep -q "Started PublicPortalApplication" "$RUN/public.log" 2>/dev/null && break
  grep -q "Application run failed" "$RUN/public.log" 2>/dev/null && { tail -5 "$RUN/public.log"; die "Public Portal failed to start (see .run/public.log)"; }
  [ "$i" = 60 ] && die "Public Portal did not start in 3 minutes (see .run/public.log)"
  sleep 2
done

# Start Internal Intranet Portal (8081)
( cd "$ROOT"; env SPRING_PROFILES_ACTIVE=$PROFILE $DB_ENV nohup java -Dserver.port=8081 -jar "$INTERNAL_WAR" > "$RUN/internal.log" 2>&1 < /dev/null & echo $! > "$RUN/internal.pid" )
for i in $(seq 1 60); do
  grep -q "Started InternalPortalApplication" "$RUN/internal.log" 2>/dev/null && break
  grep -q "Application run failed" "$RUN/internal.log" 2>/dev/null && { tail -5 "$RUN/internal.log"; die "Internal Portal failed to start (see .run/internal.log)"; }
  [ "$i" = 60 ] && die "Internal Portal did not start in 3 minutes (see .run/internal.log)"
  sleep 2
done

touch "$RUN/pids"

printf '\n\033[1mReady.\033[0m\n'
cat <<'MSG'
  =============================================================================
  1. Public Customer Portal (Internet DMZ)   : http://localhost:8080/ucorealty-customer/projects
     - Zero admin/SSO endpoints exposed (403/404 on tampering)
  2. Internal Bank Portal (Intranet SSO)     : http://localhost:8081/ucorealty-staff/login
     - Bank Production SSO Handoff Endpoint  : http://localhost:8081/ucorealty-staff/entry?value=...
     - Dev-only quick login (employee id+OTP): http://localhost:8081/ucorealty-staff/login  (OTP 123456)
     - Project Maker-Checker Administration  : http://localhost:8081/ucorealty-staff/admin/projects
     - DIT Superadmin Audit Logs             : http://localhost:8081/ucorealty-staff/admin/audit-logs
  =============================================================================
  Logs        : .run/public.log & .run/internal.log
  Stop        : scripts/dev-stop.sh
  Start over  : scripts/dev-start.sh --reset
MSG
