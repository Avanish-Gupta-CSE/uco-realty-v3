#!/usr/bin/env bash
# Starts UCO Realty multi-module deployment locally:
# Oracle (Docker) -> database tables -> admin accounts ->
#   1. Public Customer Portal (Port 8080 - Internet DMZ)
#   2. Internal Bank Intranet Portal (Port 8081 - Intranet SSO & Maker-Checker)
#
#   scripts/dev-start.sh            start (keeps existing data)
#   scripts/dev-start.sh --reset    wipe the database first and start from zero
#
# Passwords: 8+ characters, a letter and a digit.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
RUN="$ROOT/.run"; mkdir -p "$RUN"
PUBLIC_WAR="$ROOT/uco-realty-public/target/uco-realty-public-0.1.0.war"
INTERNAL_WAR="$ROOT/uco-realty-internal/target/uco-realty-internal-0.1.0.war"
SQ="docker exec -i uco-oracle sqlplus -s uco_realty/uco_realty_pwd@//localhost/XEPDB1"
RESET=0; [ "${1:-}" = "--reset" ] && RESET=1

say() { printf '\n\033[1m==> %s\033[0m\n' "$*"; }
die() { printf '\n\033[31mERROR: %s\033[0m\n' "$*" >&2; exit 1; }
sql() { printf 'set heading off feedback off pagesize 0 verify off\n%s\nexit\n' "$1" | $SQ | tr -d ' \t\r\n'; }

for c in docker java mvn; do command -v "$c" >/dev/null || die "'$c' is not installed"; done
docker info >/dev/null 2>&1 || die "Docker is not running. Start Docker Desktop and run this again."

for p in 8080 8081; do
  if lsof -nP -iTCP:$p -sTCP:LISTEN >/dev/null 2>&1 && [ ! -f "$RUN/pids" ]; then
    die "Port $p is already in use by another program. Stop it first."
  fi
done

say "1/5 Oracle database"
if [ "$(docker inspect -f '{{.State.Health.Status}}' uco-oracle 2>/dev/null)" != "healthy" ]; then
  docker start uco-oracle >/dev/null 2>&1 || (cd "$ROOT" && docker compose up -d oracle >/dev/null 2>&1)
fi
for i in $(seq 1 60); do
  [ "$(docker inspect -f '{{.State.Health.Status}}' uco-oracle 2>/dev/null)" = "healthy" ] && break
  [ "$i" = 60 ] && die "Oracle did not become healthy in 10 minutes (docker logs uco-oracle)"
  printf '.'; sleep 10
done
echo " ready"

say "2/5 Build applications (uco-realty-public, uco-realty-internal)"
(cd "$ROOT" && mvn -q package)
[ -f "$PUBLIC_WAR" ] || die "Public portal build failed"
[ -f "$INTERNAL_WAR" ] || die "Internal portal build failed"

say "3/5 Database tables"
docker cp "$ROOT/db_migrations" uco-oracle:/tmp/ >/dev/null
if [ "$RESET" = 1 ]; then
  "$ROOT/scripts/dev-stop.sh" >/dev/null 2>&1 || true
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

say "4/5 Admin accounts"
USERS=$(sql "select count(*) from users;")
hash_of() {
  java -Dloader.main=com.ucorealty.internal.tools.PasswordHashTool \
       -Dloader.path=WEB-INF/classes,WEB-INF/lib \
       -cp "$INTERNAL_WAR" org.springframework.boot.loader.PropertiesLauncher "$1" 2>&1
}
ask() {
  local label="$1" val="${2:-}" out
  while true; do
    if [ -z "$val" ]; then
      [ -t 0 ] || die "No terminal to ask for the $label password. Set DIT_PASSWORD and HO_PASSWORD instead."
      printf 'Choose a password for %s (8+ characters, a letter and a digit): ' "$label" >&2
      read -r -s val; echo >&2
    fi
    if out=$(hash_of "$val") && [[ "$out" == \$2a\$* ]]; then printf '%s' "$out"; return; fi
    echo "  Not accepted: $out" >&2; val=""
  done
}
if [ "$USERS" = "0" ]; then
  H1=$(ask "dit.superadmin" "${DIT_PASSWORD:-}")
  H2=$(ask "ho.admin" "${HO_PASSWORD:-}")
  printf '%s\n%s\nexit\n' "$H1" "$H2" | $SQ @/tmp/db_migrations/02_INITIAL_ADMIN_USERS.sql 2>&1 | grep -i "ORA-" && die "Creating admins failed"
  echo "created dit.superadmin and ho.admin"
else
  echo "users already exist ($USERS), password prompts skipped"
fi

say "5/5 Start Applications"
if [ -f "$RUN/pids" ]; then "$ROOT/scripts/dev-stop.sh" >/dev/null 2>&1 || true; fi

# Start Public Portal (8080)
( cd "$ROOT"; SPRING_PROFILES_ACTIVE=dev nohup java -Dserver.port=8080 -jar "$PUBLIC_WAR" > "$RUN/public.log" 2>&1 < /dev/null & echo $! > "$RUN/public.pid" )
for i in $(seq 1 60); do
  grep -q "Started PublicPortalApplication" "$RUN/public.log" 2>/dev/null && break
  grep -q "Application run failed" "$RUN/public.log" 2>/dev/null && { tail -5 "$RUN/public.log"; die "Public Portal failed to start (see .run/public.log)"; }
  [ "$i" = 60 ] && die "Public Portal did not start in 3 minutes (see .run/public.log)"
  sleep 2
done

# Start Internal Intranet Portal (8081)
( cd "$ROOT"; SPRING_PROFILES_ACTIVE=dev nohup java -Dserver.port=8081 -jar "$INTERNAL_WAR" > "$RUN/internal.log" 2>&1 < /dev/null & echo $! > "$RUN/internal.pid" )
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
  1. Public Customer Portal (Internet DMZ)   : http://localhost:8080/projects
     - Zero admin/SSO endpoints exposed (403/404 on tampering)
  2. Internal Bank Portal (Intranet SSO)     : http://localhost:8081/login
     - Bank Production SSO Handoff Endpoint  : http://localhost:8081/entry?value=...
     - Intranet Direct Forwarding Endpoint   : http://localhost:8081/sso?emp_id=...&sol_id=...
     - Project Maker-Checker Administration  : http://localhost:8081/admin/projects
     - DIT Superadmin Audit Logs             : http://localhost:8081/admin/audit-logs
  =============================================================================
  Logs        : .run/public.log & .run/internal.log
  Stop        : scripts/dev-stop.sh
  Start over  : scripts/dev-start.sh --reset
MSG
