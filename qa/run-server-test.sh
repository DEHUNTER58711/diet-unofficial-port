#!/usr/bin/env bash
# QA helper: start a NeoForge test server headless, wait for "Done", send "stop", wait for exit.
# usage: [CMDS=file-with-console-commands] qa/run-server-test.sh <logname> <world-name>
# Paths default to locations relative to this repository; override via environment:
#   SRV  test server directory (must contain start-server.sh)  default: <repo>/../test-server
#   OUT  log output directory                                default: <repo>/qa/logs
#   UNI  world/universe directory                            default: <repo>/../qa-worlds
set -u
NAME=$1; WORLD=$2
REPO=$(cd "$(dirname "$0")/.." && pwd)
SRV=${SRV:-$REPO/../test-server}
OUT=${OUT:-$REPO/qa/logs}
UNI=${UNI:-$REPO/../qa-worlds}
mkdir -p "$OUT" "$UNI"
cd "$SRV"
FIFO=$(mktemp -u); mkfifo "$FIFO"
START=$(date +%s)
./start-server.sh --universe "$UNI" --world "$WORLD" < "$FIFO" > "$OUT/$NAME.console.log" 2>&1 &
PID=$!
exec 3>"$FIFO"
for i in $(seq 1 300); do
  grep -q 'Done (' "$OUT/$NAME.console.log" && break
  kill -0 $PID 2>/dev/null || { echo "server exited early"; break; }
  sleep 1
done
if grep -q 'Done (' "$OUT/$NAME.console.log"; then
  echo "Done after $(( $(date +%s)-START ))s"; sleep 3
  if [ -n "${CMDS:-}" ]; then while IFS= read -r c; do [ -n "$c" ] && echo "$c" >&3; sleep 2; done < "$CMDS"; fi
  sleep 2; echo "stop" >&3
fi
for i in $(seq 1 120); do kill -0 $PID 2>/dev/null || break; sleep 1; done
if kill -0 $PID 2>/dev/null; then echo "still running -> TERM"; kill $PID; sleep 10; kill -9 $PID 2>/dev/null; fi
wait $PID; echo "exit code $?"
exec 3>&-; rm -f "$FIFO"
cp logs/latest.log "$OUT/$NAME.latest.log"; cp logs/debug.log "$OUT/$NAME.debug.log" 2>/dev/null
echo "total $(( $(date +%s)-START ))s"
