#!/usr/bin/env bash
set -euo pipefail

# Find PID matching pattern or use first argument
APP_NAME_OR_PID="${1:-CoroutineForensics}"
OUTPUT_DIR="${2:-./telemetry-output}"

mkdir -p "$OUTPUT_DIR"

if [[ "$APP_NAME_OR_PID" =~ ^[0-9]+$ ]]; then
    PID="$APP_NAME_OR_PID"
else
    PID=$(jcmd | grep -i "$APP_NAME_OR_PID" | awk '{print $1}' | head -n 1 || true)
fi

if [ -z "$PID" ]; then
    echo "[!] Error: Process '$APP_NAME_OR_PID' not found."
    echo "Available JVM processes:"
    jcmd -l
    exit 1
fi

TIMESTAMP=$(date +"%Y%m%d_%H%M%S")
DUMP_FILE="$OUTPUT_DIR/thread_dump_${PID}_${TIMESTAMP}.txt"

echo "[*] Capturing thread dump for PID $PID..."
jcmd "$PID" Thread.print -l > "$DUMP_FILE"

echo "[+] Thread dump saved to: $DUMP_FILE"
echo ""
echo "=== Thread State Summary ==="
printf "%-20s: %s\n" "Total Threads" "$(grep -c 'nid=' "$DUMP_FILE" || true)"
printf "%-20s: %s\n" "RUNNABLE"      "$(grep -c 'java.lang.Thread.State: RUNNABLE' "$DUMP_FILE" || true)"
printf "%-20s: %s\n" "BLOCKED"       "$(grep -c 'java.lang.Thread.State: BLOCKED' "$DUMP_FILE" || true)"
printf "%-20s: %s\n" "WAITING"       "$(grep -c 'java.lang.Thread.State: WAITING' "$DUMP_FILE" || true)"
printf "%-20s: %s\n" "TIMED_WAITING" "$(grep -c 'java.lang.Thread.State: TIMED_WAITING' "$DUMP_FILE" || true)"
echo "============================"

# Highlight lock contention if any blocked threads exist
BLOCKED_COUNT=$(grep -c 'java.lang.Thread.State: BLOCKED' "$DUMP_FILE" || true)
if [ "$BLOCKED_COUNT" -gt 0 ]; then
    echo -e "\n[!] WARNING: $BLOCKED_COUNT threads in BLOCKED state. Lock contention detected:"
    grep -E "parking to wait for|waiting to lock|locked <" "$DUMP_FILE" | head -n 10
fi
