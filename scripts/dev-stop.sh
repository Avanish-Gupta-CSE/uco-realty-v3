#!/usr/bin/env bash
# Stops the multi-module applications started by dev-start.sh
ROOT="$(cd "$(dirname "$0")/.." && pwd)"; RUN="$ROOT/.run"

[ -f "$RUN/public.pid" ] && kill -9 "$(cat "$RUN/public.pid")" 2>/dev/null || true
[ -f "$RUN/internal.pid" ] && kill -9 "$(cat "$RUN/internal.pid")" 2>/dev/null || true
rm -f "$RUN/public.pid" "$RUN/internal.pid" "$RUN/backend.pid" "$RUN/pids"

pkill -f "uco-realty-public" 2>/dev/null || true
pkill -f "uco-realty-internal" 2>/dev/null || true
pkill -f "uco-realty" 2>/dev/null || true

for p in 8080 8081; do
  lsof -ti :$p | xargs kill -9 2>/dev/null || true
done

echo "stopped"
