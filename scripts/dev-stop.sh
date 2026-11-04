#!/usr/bin/env bash
# Stops the applications started by dev-start.sh. Without --apps-only it also stops the H2 server (which wipes the in-memory H2 data).
ROOT="$(cd "$(dirname "$0")/.." && pwd)"; RUN="$ROOT/.run"

[ -f "$RUN/public.pid" ] && kill -9 "$(cat "$RUN/public.pid")" 2>/dev/null || true
[ -f "$RUN/internal.pid" ] && kill -9 "$(cat "$RUN/internal.pid")" 2>/dev/null || true
rm -f "$RUN/public.pid" "$RUN/internal.pid" "$RUN/backend.pid" "$RUN/pids"

pkill -f "ucorealty-customer" 2>/dev/null || true
pkill -f "ucorealty-staff" 2>/dev/null || true
pkill -f "uco-realty" 2>/dev/null || true

for p in 8080 8081; do
  lsof -ti :$p | xargs kill -9 2>/dev/null || true
done

if [ "${1:-}" != "--apps-only" ]; then
  [ -f "$RUN/h2.pid" ] && kill "$(cat "$RUN/h2.pid")" 2>/dev/null || true
  rm -f "$RUN/h2.pid"
  pkill -f "org.h2.tools.Server" 2>/dev/null || true
fi
echo "stopped"
