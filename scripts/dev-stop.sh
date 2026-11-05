#!/usr/bin/env bash
# Stops both applications started by dev-start.sh. With the h2 profile this also ends the in-memory database.
ROOT="$(cd "$(dirname "$0")/.." && pwd)"; RUN="$ROOT/.run"
for f in public internal; do
  [ -f "$RUN/$f.pid" ] && kill -9 "$(cat "$RUN/$f.pid")" 2>/dev/null || true
  rm -f "$RUN/$f.pid"
done
for p in 8080 8081; do
  lsof -ti :$p | xargs kill -9 2>/dev/null || true
done
echo "stopped"
